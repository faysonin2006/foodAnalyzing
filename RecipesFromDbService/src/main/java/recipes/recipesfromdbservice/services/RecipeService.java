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
import recipes.recipesfromdbservice.dtos.CreateRecipeCommentRequest;
import recipes.recipesfromdbservice.dtos.responseDtos.ConstraintDto;
import recipes.recipesfromdbservice.dtos.responseDtos.IngredientDto;
import recipes.recipesfromdbservice.dtos.responseDtos.InstructionStepDto;
import recipes.recipesfromdbservice.dtos.responseDtos.NutritionDto;
import recipes.recipesfromdbservice.dtos.responseDtos.RecipeCommentDto;
import recipes.recipesfromdbservice.dtos.responseDtos.RecipeTimesDto;
import recipes.recipesfromdbservice.models.RecipeComment;
import recipes.recipesfromdbservice.models.RecipeCommentLike;
import recipes.recipesfromdbservice.repositories.RecipeCommentRepository;
import recipes.recipesfromdbservice.repositories.RecipeCommentLikeRepository;
import recipes.recipesfromdbservice.repositories.RecipeRepository;
import recipes.recipesfromdbservice.repositories.projections.CardFullRecipeRow;
import recipes.recipesfromdbservice.repositories.projections.RecipeCardListRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private static final Pattern FIRST_NUMBER_PATTERN = Pattern.compile("(-?\\d+(?:[.,]\\d+)?)");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int MAX_COMMENT_LENGTH = 1000;
    private static final String COMMENT_NOT_FOUND_PREFIX = "Comment not found: ";
    private static final int MIN_CANDIDATE_POOL = 60;
    private static final int MAX_CANDIDATE_POOL = 160;
    private static final int CANDIDATE_POOL_MULTIPLIER = 3;
    private static final Map<String, String> PHRASE_ALIASES = Map.ofEntries(
            Map.entry("bell peppers", "bell pepper"),
            Map.entry("capsicums", "bell pepper"),
            Map.entry("capsicum", "bell pepper"),
            Map.entry("garbanzo beans", "chickpea"),
            Map.entry("garbanzo bean", "chickpea"),
            Map.entry("beef mince", "ground beef"),
            Map.entry("minced beef", "ground beef"),
            Map.entry("green onions", "green onion"),
            Map.entry("spring onions", "green onion"),
            Map.entry("scallions", "green onion"),
            Map.entry("rocket leaves", "arugula"),
            Map.entry("rocket leaf", "arugula"),
            Map.entry("coriander leaves", "cilantro"),
            Map.entry("coriander leaf", "cilantro")
    );
    private static final Map<String, String> TOKEN_ALIASES = Map.ofEntries(
            Map.entry("aubergine", "eggplant"),
            Map.entry("aubergines", "eggplant"),
            Map.entry("courgette", "zucchini"),
            Map.entry("courgettes", "zucchini"),
            Map.entry("garbanzo", "chickpea"),
            Map.entry("garbanzos", "chickpea"),
            Map.entry("coriander", "cilantro"),
            Map.entry("yoghurt", "yogurt"),
            Map.entry("prawn", "shrimp"),
            Map.entry("prawns", "shrimp"),
            Map.entry("chilli", "chili"),
            Map.entry("chilies", "chili"),
            Map.entry("chiles", "chili"),
            Map.entry("mince", "ground"),
            Map.entry("minced", "ground"),
            Map.entry("biscuits", "cookies")
    );

    private final RecipeRepository recipeRepository;
    private final RecipeCommentRepository recipeCommentRepository;
    private final RecipeCommentLikeRepository recipeCommentLikeRepository;
    private final ObjectMapper objectMapper;

    public List<CardRecipeResponse> getRecipes(CardRecipeRequest request) {
        if (request == null) {
            throw new BadRequestException(AppMessages.REQUEST_MUST_NOT_BE_NULL);
        }

        SearchPlan plan = buildSearchPlan(request);
        int candidatePool = Math.min(
                Math.max(plan.page() * plan.size() * CANDIDATE_POOL_MULTIPLIER, MIN_CANDIDATE_POOL),
                MAX_CANDIDATE_POOL
        );

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

        List<RankedRecipeCandidate> rankedCandidates = candidates.stream()
                .map(RankedRecipeCandidate::new)
                .toList();

        int fromIndex = Math.min((plan.page() - 1) * plan.size(), rankedCandidates.size());
        int toIndex = Math.min(fromIndex + plan.size(), rankedCandidates.size());
        if (fromIndex >= toIndex) {
            return List.of();
        }

        return rankedCandidates.subList(fromIndex, toIndex).stream()
                .map(this::toResponse)
                .toList();
    }

    public CardFullRecipeResponse getRecipe(Long id, UUID currentUserId) {
        if (id == null || id <= 0) {
            throw new BadRequestException(AppMessages.RECIPE_ID_MUST_BE_POSITIVE);
        }

        CardFullRecipeRow row = recipeRepository.getFullRecipeInfo(id)
                .orElseThrow(() -> new RecipeNotFoundException(AppMessages.RECIPE_NOT_FOUND_PREFIX + id));
        List<RecipeCommentDto> comments = buildCommentTree(
                recipeCommentRepository.findByRecipeIdOrderByCreatedAtAscIdAsc(id),
                currentUserId
        );
        return toCardFullRecipeResponse(row, comments);
    }

    public RecipeCommentDto addRecipeComment(
            Long recipeId,
            CreateRecipeCommentRequest request,
            String userEmail,
            UUID userId
    ) {
        if (recipeId == null || recipeId <= 0) {
            throw new BadRequestException(AppMessages.RECIPE_ID_MUST_BE_POSITIVE);
        }
        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("Authentication is required to comment");
        }
        if (request == null) {
            throw new BadRequestException(AppMessages.REQUEST_MUST_NOT_BE_NULL);
        }

        String commentBody = normalizeCommentBody(request.getText());
        if (commentBody.isBlank()) {
            throw new BadRequestException("Comment text must not be blank");
        }
        if (commentBody.length() > MAX_COMMENT_LENGTH) {
            throw new BadRequestException("Comment is too long");
        }
        if (!recipeRepository.existsById(recipeId)) {
            throw new RecipeNotFoundException(AppMessages.RECIPE_NOT_FOUND_PREFIX + recipeId);
        }

        Long parentCommentId = request.getParentCommentId();
        if (parentCommentId != null) {
            RecipeComment parentComment = recipeCommentRepository.findByIdAndRecipeId(parentCommentId, recipeId)
                    .orElseThrow(() -> new BadRequestException("Parent comment not found"));
            if (parentComment.getParentCommentId() != null) {
                throw new BadRequestException("Replies can only be added to top-level comments");
            }
        }

        RecipeComment comment = recipeCommentRepository.save(
                RecipeComment.builder()
                        .recipeId(recipeId)
                        .parentCommentId(parentCommentId)
                        .authorUserId(userId)
                        .authorEmail(userEmail.trim().toLowerCase(Locale.ROOT))
                        .authorName(buildAuthorName(userEmail))
                        .body(commentBody)
                        .build()
        );
        return toRecipeCommentDto(comment, 0, false, List.of());
    }

    public RecipeCommentDto setRecipeCommentLike(Long commentId, UUID userId, boolean liked) {
        if (commentId == null || commentId <= 0) {
            throw new BadRequestException("Comment id must be positive");
        }
        if (userId == null) {
            throw new BadRequestException("Authentication is required to like comments");
        }

        RecipeComment comment = recipeCommentRepository.findById(commentId)
                .orElseThrow(() -> new RecipeNotFoundException(COMMENT_NOT_FOUND_PREFIX + commentId));
        RecipeCommentLike existingLike = recipeCommentLikeRepository.findByCommentIdAndUserId(commentId, userId).orElse(null);

        if (liked && existingLike == null) {
            recipeCommentLikeRepository.save(
                    RecipeCommentLike.builder()
                            .commentId(commentId)
                            .userId(userId)
                            .build()
            );
        } else if (!liked && existingLike != null) {
            recipeCommentLikeRepository.delete(existingLike);
        }

        int likeCount = Math.toIntExact(recipeCommentLikeRepository.findByCommentIdIn(List.of(commentId)).stream().count());
        return toRecipeCommentDto(comment, likeCount, liked, List.of());
    }

    private SearchPlan buildSearchPlan(CardRecipeRequest request) {
        String lang = request.getLang() == null ? null : request.getLang().getLowerCaseString();
        int size = request.getSize() == null ? DEFAULT_PAGE_SIZE : Math.max(1, Math.min(request.getSize(), MAX_PAGE_SIZE));
        int page = request.getPage() == null ? 1 : Math.max(1, request.getPage());

        return new SearchPlan(
                lang,
                page,
                size,
                normalizeSearchPhrase(request.getTitle()),
                normalizeSearchPhrase(request.getCategory()),
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

    private RecipeSearchCandidate toSearchCandidate(RecipeCardListRow row) {
        List<IngredientDto> ingredients = recipeRepository.findIngredientsByRecipeId(row.getRecipeId())
                .stream().map(IngredientDto::fromRow).toList();
        
        List<NutritionDto> nutritions = recipeRepository.findNutritionsByRecipeId(row.getRecipeId())
                .stream().map(NutritionDto::fromRow).toList();

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
                defaultInt(row.getSearchScore()),
                row.getSearchDocument()
        );
    }

    private CardRecipeResponse toResponse(RankedRecipeCandidate candidate) {
        List<String> reasons = new ArrayList<>();
        if (candidate.candidate().searchScore() > 0) {
            reasons.add("text_match");
        }
        return candidate.candidate().response().toBuilder()
                .searchMatchReasons(reasons)
                .build();
    }

    private CardFullRecipeResponse toCardFullRecipeResponse(CardFullRecipeRow row, List<RecipeCommentDto> comments) {
        List<IngredientDto> ingredients = recipeRepository.findIngredientsByRecipeId(row.getRecipeId())
                .stream().map(IngredientDto::fromRow).toList();
        
        List<InstructionStepDto> steps = recipeRepository.findInstructionsByRecipeId(row.getRecipeId())
                .stream().map(InstructionStepDto::fromRow).toList();
        
        List<NutritionDto> nutritions = recipeRepository.findNutritionsByRecipeId(row.getRecipeId())
                .stream().map(NutritionDto::fromRow).toList();
        
        List<ConstraintDto> constraints = recipeRepository.findConstraintsByRecipeId(row.getRecipeId())
                .stream().map(ConstraintDto::fromRow).toList();

        return CardFullRecipeResponse.builder()
                .recipeId(row.getRecipeId())
                .title(row.getTitle())
                .image(row.getImage())
                .category(row.getCategory())
                .ingredientsCount(defaultInt(row.getIngredientsCount()))
                .instructionsCount(defaultInt(row.getInstructionsCount()))
                .ingredients(ingredients)
                .instructionSteps(steps)
                .nutritions(nutritions)
                .times(readJson(row.getTimesJson(), RecipeTimesDto.class, new RecipeTimesDto()))
                .blockDietKeys(readJson(row.getBlockDietKeysJson(), new TypeReference<List<String>>() {}, List.of()))
                .blockAllergyKeys(readJson(row.getBlockAllergyKeysJson(), new TypeReference<List<String>>() {}, List.of()))
                .blockHealthKeys(readJson(row.getBlockHealthKeysJson(), new TypeReference<List<String>>() {}, List.of()))
                .cautionHealthKeys(readJson(row.getCautionHealthKeysJson(), new TypeReference<List<String>>() {}, List.of()))
                .constraints(constraints)
                .comments(comments == null ? List.of() : comments)
                .build();
    }

    private List<RecipeCommentDto> buildCommentTree(List<RecipeComment> comments, UUID currentUserId) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }

        List<Long> commentIds = comments.stream()
                .map(RecipeComment::getId)
                .toList();
        Map<Long, Integer> likeCounts = new LinkedHashMap<>();
        Set<Long> likedIds = new LinkedHashSet<>();

        for (RecipeCommentLike like : recipeCommentLikeRepository.findByCommentIdIn(commentIds)) {
            if (like.getCommentId() == null) {
                continue;
            }
            likeCounts.merge(like.getCommentId(), 1, Integer::sum);
            if (currentUserId != null && currentUserId.equals(like.getUserId())) {
                likedIds.add(like.getCommentId());
            }
        }

        Map<Long, List<RecipeComment>> repliesByParent = new LinkedHashMap<>();
        List<RecipeComment> roots = new ArrayList<>();
        for (RecipeComment comment : comments) {
            if (comment.getParentCommentId() == null) {
                roots.add(comment);
                continue;
            }
            repliesByParent.computeIfAbsent(comment.getParentCommentId(), ignored -> new ArrayList<>()).add(comment);
        }

        List<RecipeCommentDto> result = new ArrayList<>();
        for (RecipeComment root : roots) {
            List<RecipeCommentDto> replies = repliesByParent.getOrDefault(root.getId(), List.of()).stream()
                    .map(reply -> toRecipeCommentDto(
                            reply,
                            likeCounts.getOrDefault(reply.getId(), 0),
                            likedIds.contains(reply.getId()),
                            List.of()
                    ))
                    .toList();
            result.add(toRecipeCommentDto(
                    root,
                    likeCounts.getOrDefault(root.getId(), 0),
                    likedIds.contains(root.getId()),
                    replies
            ));
        }
        return result;
    }

    private RecipeCommentDto toRecipeCommentDto(
            RecipeComment comment,
            int likeCount,
            boolean likedByMe,
            List<RecipeCommentDto> replies
    ) {
        return RecipeCommentDto.builder()
                .id(comment.getId())
                .recipeId(comment.getRecipeId())
                .parentCommentId(comment.getParentCommentId())
                .authorName(comment.getAuthorName())
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .replyCount(replies == null ? 0 : replies.size())
                .replies(replies == null ? List.of() : replies)
                .build();
    }

    private String normalizeCommentBody(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }

    private String buildAuthorName(String userEmail) {
        String normalizedEmail = userEmail == null ? "" : userEmail.trim();
        String base = normalizedEmail;
        int atIndex = normalizedEmail.indexOf('@');
        if (atIndex > 0) {
            base = normalizedEmail.substring(0, atIndex);
        }
        base = base.replaceAll("[._-]+", " ").replaceAll("\\s+", " ").trim();
        if (base.isBlank()) {
            return "User";
        }

        StringBuilder builder = new StringBuilder();
        for (String token : base.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.toString().trim();
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

    private String normalizeSearchPhrase(String value) {
        String normalized = normalizeText(blankToNull(value));
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> alias : PHRASE_ALIASES.entrySet()) {
            normalized = normalized.replaceAll(
                    "\\b" + Pattern.quote(alias.getKey()) + "\\b",
                    Matcher.quoteReplacement(alias.getValue())
            );
        }

        normalized = normalized
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            return normalized;
        }

        StringBuilder builder = new StringBuilder();
        for (String token : normalized.split("\\s+")) {
            String mappedToken = TOKEN_ALIASES.getOrDefault(token, token);
            if (mappedToken.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(mappedToken);
        }
        return builder.toString();
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

    private record RankedRecipeCandidate(RecipeSearchCandidate candidate) {
    }

    private record RecipeSearchCandidate(
            CardRecipeResponse response,
            List<IngredientDto> ingredients,
            List<NutritionDto> nutritions,
            int searchScore,
            String searchDocument
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

        private List<String> keywordTerms() {
            LinkedHashSet<String> terms = new LinkedHashSet<>();
            collectTerms(terms, response.getTitle());
            collectTerms(terms, response.getCategory());
            collectTerms(terms, searchDocument);
            return terms.stream()
                    .filter(term -> term.length() >= 3)
                    .limit(12)
                    .toList();
        }

        private String searchableText() {
            if (searchDocument != null && !searchDocument.isBlank()) {
                return searchDocument.trim().toLowerCase(Locale.ROOT);
            }
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

        private static void collectTerms(LinkedHashSet<String> target, String value) {
            if (value == null || value.isBlank()) {
                return;
            }
            for (String token : value.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!token.isBlank()) {
                    target.add(token.trim());
                }
            }
        }
    }
}
