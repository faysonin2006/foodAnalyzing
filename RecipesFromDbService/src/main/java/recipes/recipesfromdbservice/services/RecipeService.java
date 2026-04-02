package recipes.recipesfromdbservice.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.BadRequestException;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.RecipeNotFoundException;
import recipes.recipesfromdbservice.constants.AppMessages;
import recipes.recipesfromdbservice.dtos.CardFullRecipeResponse;
import recipes.recipesfromdbservice.dtos.CardRecipeRequest;
import recipes.recipesfromdbservice.dtos.CardRecipeResponse;
import recipes.recipesfromdbservice.dtos.responseDtos.ConstraintDto;
import recipes.recipesfromdbservice.dtos.responseDtos.IngredientDto;
import recipes.recipesfromdbservice.dtos.responseDtos.InstructionStepDto;
import recipes.recipesfromdbservice.dtos.responseDtos.NutritionDto;
import recipes.recipesfromdbservice.dtos.responseDtos.RecipeTimesDto;
import recipes.recipesfromdbservice.repositories.RecipeRepository;
import recipes.recipesfromdbservice.repositories.projections.CardFullRecipeRow;
import recipes.recipesfromdbservice.repositories.projections.RecipeCardListRow;
import recipes.recipesfromdbservice.searchml.SemanticSearchCandidate;
import recipes.recipesfromdbservice.searchml.TensorFlowSearchReranker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private static final Pattern FIRST_NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:[.,]\\d+)?)");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int MIN_CANDIDATE_POOL = 120;
    private static final int CANDIDATE_POOL_MULTIPLIER = 4;

    private final RecipeRepository recipeRepository;
    private final ObjectMapper objectMapper;
    private final TensorFlowSearchReranker tensorFlowSearchReranker;

    public List<CardRecipeResponse> getRecipes(CardRecipeRequest request) {
        if (request == null) {
            throw new BadRequestException(AppMessages.REQUEST_MUST_NOT_BE_NULL);
        }

        SearchPlan plan = buildSearchPlan(request);
        int candidatePool = Math.max(plan.page() * plan.size() * CANDIDATE_POOL_MULTIPLIER, MIN_CANDIDATE_POOL);

        List<RecipeSearchCandidate> candidates = recipeRepository.findRecipes(
                        plan.lang(),
                        plan.title(),
                        plan.category(),
                        plan.includeIngredients(),
                        plan.sortBy(),
                        plan.sortDir(),
                        candidatePool,
                        0
                ).stream()
                .map(this::toSearchCandidate)
                .filter(candidate -> !containsExcludedTerms(candidate, plan.excludeIngredients()))
                .filter(candidate -> matchesExplicitNutritionFilters(candidate, plan))
                .toList();

        List<RankedRecipeCandidate> rankedCandidates = rerankWithTensorFlow(plan, candidates);

        int fromIndex = Math.min((plan.page() - 1) * plan.size(), rankedCandidates.size());
        int toIndex = Math.min(fromIndex + plan.size(), rankedCandidates.size());
        if (fromIndex >= toIndex) {
            return List.of();
        }

        return rankedCandidates.subList(fromIndex, toIndex).stream()
                .map(this::toResponse)
                .toList();
    }

    public CardFullRecipeResponse getRecipe(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException(AppMessages.RECIPE_ID_MUST_BE_POSITIVE);
        }

        CardFullRecipeRow row = recipeRepository.getFullRecipeInfo(id)
                .orElseThrow(() -> new RecipeNotFoundException(AppMessages.RECIPE_NOT_FOUND_PREFIX + id));
        return toCardFullRecipeResponse(row);
    }

    private SearchPlan buildSearchPlan(CardRecipeRequest request) {
        String lang = request.getLang() == null ? "en" : request.getLang().getLowerCaseString();
        int size = request.getSize() == null ? DEFAULT_PAGE_SIZE : Math.max(1, Math.min(request.getSize(), MAX_PAGE_SIZE));
        int page = request.getPage() == null ? 1 : Math.max(1, request.getPage());

        return new SearchPlan(
                lang,
                page,
                size,
                blankToNull(request.getTitle()),
                blankToNull(request.getCategory()),
                normalizeSearchTerms(request.getIncludeIngredients()),
                normalizeSearchTerms(request.getExcludeIngredients()),
                normalizeSortBy(request.getSortBy()),
                normalizeSortDir(request.getSortDir()),
                request.getMaxCalories(),
                request.getMinProtein(),
                request.getMaxFats(),
                request.getMaxCarbohydrates()
        );
    }

    private List<RankedRecipeCandidate> rerankWithTensorFlow(SearchPlan plan, List<RecipeSearchCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        String semanticQuery = buildSemanticQuery(plan);
        if (semanticQuery.isBlank()) {
            return candidates.stream()
                    .map(candidate -> new RankedRecipeCandidate(candidate, 0.0))
                    .toList();
        }

        List<SemanticSearchCandidate> payload = candidates.stream()
                .map(candidate -> new SemanticSearchCandidate(
                        candidate.response().getRecipeId(),
                        candidate.response().getTitle(),
                        candidate.response().getCategory(),
                        candidate.ingredientTexts()
                ))
                .toList();

        Map<Long, Double> semanticScores = tensorFlowSearchReranker.rerank(semanticQuery, payload);
        if (semanticScores.isEmpty()) {
            return candidates.stream()
                    .map(candidate -> new RankedRecipeCandidate(candidate, 0.0))
                    .toList();
        }

        List<RankedRecipeCandidate> ranked = new ArrayList<>();
        for (RecipeSearchCandidate candidate : candidates) {
            long recipeId = candidate.response().getRecipeId() == null ? 0L : candidate.response().getRecipeId();
            ranked.add(new RankedRecipeCandidate(candidate, semanticScores.getOrDefault(recipeId, 0.0)));
        }

        Map<Long, Integer> baseOrder = new LinkedHashMap<>();
        for (int index = 0; index < ranked.size(); index++) {
            Long recipeId = ranked.get(index).candidate().response().getRecipeId();
            if (recipeId != null) {
                baseOrder.put(recipeId, index);
            }
        }

        return ranked.stream()
                .sorted((left, right) -> {
                    int bySemantic = Double.compare(right.semanticScore(), left.semanticScore());
                    if (bySemantic != 0) {
                        return bySemantic;
                    }

                    int bySqlScore = Integer.compare(right.candidate().searchScore(), left.candidate().searchScore());
                    if (bySqlScore != 0) {
                        return bySqlScore;
                    }

                    Integer leftIndex = baseOrder.getOrDefault(left.candidate().response().getRecipeId(), Integer.MAX_VALUE);
                    Integer rightIndex = baseOrder.getOrDefault(right.candidate().response().getRecipeId(), Integer.MAX_VALUE);
                    return Integer.compare(leftIndex, rightIndex);
                })
                .toList();
    }

    private String buildSemanticQuery(SearchPlan plan) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (plan.title() != null && !plan.title().isBlank()) {
            parts.add(plan.title().trim());
        }
        if (plan.category() != null && !plan.category().isBlank()) {
            parts.add(plan.category().trim());
        }
        for (String includeIngredient : plan.includeIngredients()) {
            if (includeIngredient != null && !includeIngredient.isBlank()) {
                parts.add(includeIngredient.trim());
            }
        }
        return String.join(" ", parts).trim();
    }

    private RecipeSearchCandidate toSearchCandidate(RecipeCardListRow row) {
        List<IngredientDto> ingredients = readJson(
                row.getIngredientsJson(),
                new TypeReference<List<IngredientDto>>() {},
                List.of()
        );
        List<NutritionDto> nutritions = readJson(
                row.getNutritionsJson(),
                new TypeReference<List<NutritionDto>>() {},
                List.of()
        );

        return new RecipeSearchCandidate(
                CardRecipeResponse.builder()
                        .recipeId(row.getRecipeId())
                        .title(row.getTitle())
                        .image(row.getImage())
                        .category(row.getCategory())
                        .ingredientsCount(defaultInt(row.getIngredientsCount()))
                        .instructionsCount(defaultInt(row.getInstructionsCount()))
                        .nutritions(nutritions)
                        .times(readJson(row.getTimesJson(), RecipeTimesDto.class, new RecipeTimesDto()))
                        .searchMatchReasons(List.of())
                        .build(),
                ingredients,
                nutritions,
                defaultInt(row.getSearchScore())
        );
    }

    private CardRecipeResponse toResponse(RankedRecipeCandidate candidate) {
        List<String> reasons = candidate.semanticScore() > 0 ? List.of("semantic") : List.of();
        return candidate.candidate().response().toBuilder()
                .searchMatchReasons(reasons)
                .build();
    }

    private CardFullRecipeResponse toCardFullRecipeResponse(CardFullRecipeRow row) {
        return CardFullRecipeResponse.builder()
                .recipeId(row.getRecipeId())
                .title(row.getTitle())
                .image(row.getImage())
                .category(row.getCategory())
                .ingredientsCount(defaultInt(row.getIngredientsCount()))
                .instructionsCount(defaultInt(row.getInstructionsCount()))
                .ingredients(readJson(row.getIngredientsJson(), new TypeReference<List<IngredientDto>>() {}, List.of()))
                .instructionSteps(readJson(row.getInstructionStepsJson(), new TypeReference<List<InstructionStepDto>>() {}, List.of()))
                .nutritions(readJson(row.getNutritionsJson(), new TypeReference<List<NutritionDto>>() {}, List.of()))
                .times(readJson(row.getTimesJson(), RecipeTimesDto.class, new RecipeTimesDto()))
                .blockDietKeys(readJson(row.getBlockDietKeysJson(), new TypeReference<List<String>>() {}, List.of()))
                .blockAllergyKeys(readJson(row.getBlockAllergyKeysJson(), new TypeReference<List<String>>() {}, List.of()))
                .blockHealthKeys(readJson(row.getBlockHealthKeysJson(), new TypeReference<List<String>>() {}, List.of()))
                .cautionHealthKeys(readJson(row.getCautionHealthKeysJson(), new TypeReference<List<String>>() {}, List.of()))
                .constraints(readJson(row.getConstraintsJson(), new TypeReference<List<ConstraintDto>>() {}, List.of()))
                .build();
    }

    private boolean containsExcludedTerms(RecipeSearchCandidate candidate, String[] excludeTerms) {
        if (excludeTerms == null || excludeTerms.length == 0) {
            return false;
        }
        String searchableText = candidate.searchableText();
        for (String excludeTerm : excludeTerms) {
            String normalizedTerm = normalizeText(excludeTerm);
            if (!normalizedTerm.isEmpty() && searchableText.contains(normalizedTerm)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesExplicitNutritionFilters(RecipeSearchCandidate candidate, SearchPlan plan) {
        Double calories = extractNutritionValue(candidate.nutritions(), "calories", "calorie", "kcal", "energy", "кал");
        Double protein = extractNutritionValue(candidate.nutritions(), "protein", "белок");
        Double fats = extractNutritionValue(candidate.nutritions(), "fat", "fats", "жир");
        Double carbs = extractNutritionValue(candidate.nutritions(), "carbohydrate", "carbohydrates", "carb", "carbs", "углевод");

        if (plan.maxCalories() != null && calories != null && calories > plan.maxCalories()) {
            return false;
        }
        if (plan.minProtein() != null && protein != null && protein < plan.minProtein()) {
            return false;
        }
        if (plan.maxFats() != null && fats != null && fats > plan.maxFats()) {
            return false;
        }
        if (plan.maxCarbohydrates() != null && carbs != null && carbs > plan.maxCarbohydrates()) {
            return false;
        }
        return true;
    }

    private Double extractNutritionValue(List<NutritionDto> nutritions, String... names) {
        if (nutritions == null || nutritions.isEmpty()) {
            return null;
        }
        for (NutritionDto nutrition : nutritions) {
            String nutrientName = normalizeText(nutrition.getNutrient());
            if (nutrientName.isEmpty()) {
                continue;
            }
            boolean matches = false;
            for (String name : names) {
                if (nutrientName.contains(normalizeText(name))) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                continue;
            }
            Double amount = parseLeadingNumber(nutrition.getAmount());
            if (amount != null) {
                return amount;
            }
        }
        return null;
    }

    private Double parseLeadingNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher matcher = FIRST_NUMBER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String[] normalizeSearchTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return new String[0];
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String term : terms) {
            String normalizedTerm = normalizeText(term);
            if (!normalizedTerm.isEmpty()) {
                normalized.add(normalizedTerm);
            }
        }
        return normalized.toArray(String[]::new);
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "search_score";
        }
        return switch (sortBy.trim().toLowerCase(Locale.ROOT)) {
            case "search_score", "title", "category", "ingredients_count", "instructions_count", "recipe_id" ->
                    sortBy.trim().toLowerCase(Locale.ROOT);
            default -> "search_score";
        };
    }

    private String normalizeSortDir(String sortDir) {
        if (sortDir == null || sortDir.isBlank()) {
            return "desc";
        }
        return "asc".equalsIgnoreCase(sortDir.trim()) ? "asc" : "desc";
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private <T> T readJson(String raw, Class<T> clazz, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(raw, clazz);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private <T> T readJson(String raw, TypeReference<T> typeReference, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(raw, typeReference);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record SearchPlan(
            String lang,
            int page,
            int size,
            String title,
            String category,
            String[] includeIngredients,
            String[] excludeIngredients,
            String sortBy,
            String sortDir,
            Integer maxCalories,
            Integer minProtein,
            Integer maxFats,
            Integer maxCarbohydrates
    ) {
    }

    private record RankedRecipeCandidate(RecipeSearchCandidate candidate, double semanticScore) {
    }

    private record RecipeSearchCandidate(
            CardRecipeResponse response,
            List<IngredientDto> ingredients,
            List<NutritionDto> nutritions,
            int searchScore
    ) {
        private List<String> ingredientTexts() {
            if (ingredients == null || ingredients.isEmpty()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (IngredientDto ingredient : ingredients) {
                StringBuilder builder = new StringBuilder();
                append(builder, ingredient.getIngredient());
                append(builder, ingredient.getNote());
                append(builder, ingredient.getRawText());
                String value = builder.toString().trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
            return values;
        }

        private String searchableText() {
            StringBuilder builder = new StringBuilder();
            append(builder, response.getTitle());
            append(builder, response.getCategory());
            for (String ingredientText : ingredientTexts()) {
                append(builder, ingredientText);
            }
            return builder.toString().trim().toLowerCase(Locale.ROOT);
        }

        private static void append(StringBuilder builder, String value) {
            if (value == null || value.isBlank()) {
                return;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
    }
}
