package com.recipeservice.services;

import com.recipeservice.clients.RecipesDbClient;
import com.recipeservice.clients.UserPantryClient;
import com.recipeservice.clients.UserProfileClient;
import com.recipeservice.clients.UserShoppingListClient;
import com.recipeservice.configs.spoonacularyclient.SpoonacularClient;
import com.recipeservice.constants.AppMessages;
import com.recipeservice.dtos.internal.pantry.PantryListItemResponse;
import com.recipeservice.dtos.internal.profile.ReferenceItemResponse;
import com.recipeservice.dtos.internal.profile.UserProfileResponse;
import com.recipeservice.dtos.internal.recipesdb.CardFullRecipeResponse;
import com.recipeservice.dtos.internal.recipesdb.CardRecipeRequest;
import com.recipeservice.dtos.internal.recipesdb.CardRecipeResponse;
import com.recipeservice.dtos.internal.recipesdb.IngredientDto;
import com.recipeservice.dtos.internal.recipesdb.NutritionDto;
import com.recipeservice.dtos.internal.shopping.CreateShoppingListItemRequest;
import com.recipeservice.dtos.internal.shopping.ShoppingListItemResponse;
import com.recipeservice.dtos.recommendations.AddMissingIngredientsResponse;
import com.recipeservice.dtos.recommendations.AddedShoppingListItem;
import com.recipeservice.dtos.recommendations.RecipeRecommendationRequest;
import com.recipeservice.dtos.recommendations.RecipeRecommendationResponse;
import com.recipeservice.dtos.recommendations.RecommendedRecipeItem;
import com.recipeservice.dtos.spoonacular.SpoonAnalyzedInstructionDto;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularRequest;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularResponse;
import com.recipeservice.exceptions.BadRequestException;
import com.recipeservice.exceptions.UpstreamServiceException;
import com.recipeservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private static final int MIN_TOKEN_LENGTH = 3;
    private static final int MIN_PREFIX_MATCH_LENGTH = 5;
    private static final int MAX_PREFIX_EXTRA_CHARS = 3;
    private static final List<String> TOKEN_CANONICAL_SUFFIXES = List.of(
            "иями",
            "ями",
            "ами",
            "ого",
            "его",
            "ому",
            "ему",
            "ыми",
            "ими",
            "ов",
            "ев",
            "ей",
            "ом",
            "ем",
            "ам",
            "ям",
            "ах",
            "ях",
            "ую",
            "юю",
            "ый",
            "ий",
            "ой",
            "ая",
            "яя",
            "ое",
            "ее",
            "ые",
            "ие",
            "ых",
            "их",
            "es",
            "s",
            "ы",
            "и",
            "а",
            "я",
            "о",
            "е",
            "у",
            "ю",
            "ь"
    );

    private final SpoonacularRequestMapper mapper;
    private final SpoonacularClient spoonacularClient;
    private final UserProfileClient userProfileClient;
    private final UserPantryClient userPantryClient;
    private final UserShoppingListClient userShoppingListClient;
    private final RecipesDbClient recipesDbClient;

    public SpoonacularResponse searchRecipe(SpoonacularRequest request) {
        if (request == null) {
            throw new BadRequestException(AppMessages.SEARCH_REQUEST_MUST_NOT_BE_NULL);
        }

        try {
            return spoonacularClient.complexSearch(mapper.toMap(request));
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_FETCH_RECIPES, exception);
        }

    }

    public List<SpoonAnalyzedInstructionDto> getInstructions(String id) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException(AppMessages.RECIPE_ID_MUST_NOT_BE_BLANK);
        }

        try {
            return spoonacularClient.getAnalyzedInstructions(id);
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_FETCH_INSTRUCTIONS, exception);
        }
    }

    public RecipeRecommendationResponse recommendRecipes(RecipeRecommendationRequest request) {
        if (request == null) {
            throw new BadRequestException(AppMessages.RECOMMENDATION_REQUEST_MUST_NOT_BE_NULL);
        }

        String email = SecurityUtils.getCurrentUsername();
        UserProfileResponse profile = fetchProfile(email);
        List<PantryListItemResponse> pantryItems = fetchPantry(email);

        CardRecipeRequest recipesDbRequest = CardRecipeRequest.builder()
                .lang(normalizeRecommendationLang(request.getLang()))
                .requiredDietKeys(extractIds(profile.getDietPreferences()))
                .preferredHealthKeys(extractIds(profile.getHealthConditions()))
                .allergyKeys(extractIds(profile.getAllergies()))
                .healthConditionKeys(extractIds(profile.getHealthConditions()))
                .page(1)
                .size(Math.max(request.getSize() == null ? 10 : request.getSize(), 1))
                .sortBy("recipe_id")
                .sortDir("desc")
                .build();

        List<CardRecipeResponse> recipes;
        try {
            recipes = recipesDbClient.search(recipesDbRequest);
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_FETCH_RECIPES_DB, exception);
        }

        Set<String> pantryNames = pantryItems.stream()
                .map(PantryListItemResponse::getName)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> excluded = request.getExcludedIngredients() == null
                ? Set.of()
                : request.getExcludedIngredients().stream().map(this::normalize).collect(java.util.stream.Collectors.toSet());

        List<RecommendedRecipeItem> recommended = new ArrayList<>();
        for (CardRecipeResponse recipe : recipes) {
            CardFullRecipeResponse full;
            try {
                full = recipesDbClient.getById(recipe.getRecipeId());
            } catch (Exception exception) {
                throw new UpstreamServiceException(AppMessages.FAILED_TO_FETCH_RECIPES_DB, exception);
            }

            if (containsExcludedIngredients(full, excluded)) {
                continue;
            }

            Integer estimatedCalories = extractCalories(recipe.getNutritions());
            Integer maxCalories = request.getMaxCalories() != null ? request.getMaxCalories() : profile.getTargetCalories();
            if (maxCalories != null && estimatedCalories != null && estimatedCalories > maxCalories) {
                continue;
            }

            recommended.add(buildRecommendedRecipe(recipe, full, pantryNames, estimatedCalories));
        }

        Comparator<RecommendedRecipeItem> comparator = comparatorFor(request.getSortBy());
        recommended = recommended.stream()
                .sorted(comparator)
                .limit(request.getSize() == null ? 10 : request.getSize())
                .toList();

        return RecipeRecommendationResponse.builder()
                .pantryItemsCount(pantryItems.size())
                .recipes(recommended)
                .build();
    }

    private String normalizeRecommendationLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return "EN";
        }
        return "RU".equalsIgnoreCase(lang.trim()) ? "RU" : "EN";
    }

    public AddMissingIngredientsResponse addMissingIngredientsToShoppingList(Long recipeId) {
        if (recipeId == null || recipeId <= 0) {
            throw new BadRequestException(AppMessages.INVALID_RECIPE_ID);
        }

        String email = SecurityUtils.getCurrentUsername();
        List<PantryListItemResponse> pantryItems = fetchPantry(email);
        CardFullRecipeResponse recipe = fetchRecipe(recipeId);
        Set<String> pantryNames = pantryItems.stream()
                .map(PantryListItemResponse::getName)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        List<CreateShoppingListItemRequest> requests = (recipe.getIngredients() == null ? List.<IngredientDto>of() : recipe.getIngredients())
                .stream()
                .filter(ingredient -> !matchesPantry(normalize(resolveIngredientName(ingredient)), pantryNames))
                .map(this::toShoppingListItemRequest)
                .filter(item -> item.getName() != null && !item.getName().isBlank())
                .toList();

        List<ShoppingListItemResponse> createdItems;
        if (requests.isEmpty()) {
            createdItems = List.of();
        } else {
            try {
                createdItems = userShoppingListClient.createItems(email, requests);
            } catch (Exception exception) {
                throw new UpstreamServiceException(AppMessages.FAILED_TO_ADD_MISSING_INGREDIENTS, exception);
            }
        }

        return AddMissingIngredientsResponse.builder()
                .recipeId(recipe.getRecipeId())
                .recipeTitle(recipe.getTitle())
                .addedItemsCount(createdItems.size())
                .items(createdItems.stream()
                        .map(item -> AddedShoppingListItem.builder()
                                .name(item.getName())
                                .quantity(item.getQuantity())
                                .unit(item.getUnit())
                                .build())
                        .toList())
                .build();
    }

    private RecommendedRecipeItem buildRecommendedRecipe(
            CardRecipeResponse recipe,
            CardFullRecipeResponse full,
            Set<String> pantryNames,
            Integer estimatedCalories
    ) {
        List<String> matching = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (IngredientDto ingredientDto : full.getIngredients() == null ? List.<IngredientDto>of() : full.getIngredients()) {
            String ingredientName = ingredientDto.getIngredient() != null && !ingredientDto.getIngredient().isBlank()
                    ? ingredientDto.getIngredient()
                    : ingredientDto.getRawText();
            String normalized = normalize(ingredientName);
            if (normalized.isBlank()) {
                continue;
            }
            if (matchesPantry(normalized, pantryNames)) {
                matching.add(ingredientName);
            } else {
                missing.add(ingredientName);
            }
        }

        int totalIngredients = matching.size() + missing.size();
        double score = totalIngredients == 0 ? 0.0 : ((double) matching.size() / totalIngredients) * 100.0;

        return RecommendedRecipeItem.builder()
                .recipeId(recipe.getRecipeId())
                .title(recipe.getTitle())
                .image(recipe.getImage())
                .category(recipe.getCategory())
                .estimatedCalories(estimatedCalories)
                .matchScore(Math.round(score * 10.0) / 10.0)
                .matchingIngredientsCount(matching.size())
                .missingIngredientsCount(missing.size())
                .matchingIngredients(matching)
                .missingIngredients(missing)
                .build();
    }

    private Comparator<RecommendedRecipeItem> comparatorFor(String sortBy) {
        String normalized = sortBy == null ? "match" : sortBy.trim().toLowerCase(Locale.ROOT);
        Comparator<RecommendedRecipeItem> bestMatchComparator = Comparator
                .comparingInt(RecommendedRecipeItem::getMatchingIngredientsCount)
                .reversed()
                .thenComparing(Comparator.comparingDouble(RecommendedRecipeItem::getMatchScore).reversed())
                .thenComparingInt(RecommendedRecipeItem::getMissingIngredientsCount)
                .thenComparing(item -> item.getEstimatedCalories() == null ? Integer.MAX_VALUE : item.getEstimatedCalories());
        return switch (normalized) {
            case "calories" -> Comparator.comparing(
                    item -> item.getEstimatedCalories() == null ? Integer.MAX_VALUE : item.getEstimatedCalories()
            );
            case "relevance", "popularity", "match", "matching", "matchscore" -> bestMatchComparator;
            default -> bestMatchComparator;
        };
    }

    private UserProfileResponse fetchProfile(String email) {
        try {
            return userProfileClient.getProfile(email);
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_FETCH_PROFILE, exception);
        }
    }

    private List<PantryListItemResponse> fetchPantry(String email) {
        try {
            return userPantryClient.getPantry(email);
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_FETCH_PANTRY, exception);
        }
    }

    private CardFullRecipeResponse fetchRecipe(Long recipeId) {
        try {
            return recipesDbClient.getById(recipeId);
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_FETCH_RECIPES_DB, exception);
        }
    }

    private boolean containsExcludedIngredients(CardFullRecipeResponse full, Set<String> excluded) {
        if (excluded.isEmpty() || full.getIngredients() == null) {
            return false;
        }
        return full.getIngredients().stream()
                .map(ingredient -> normalize(ingredient.getIngredient() != null ? ingredient.getIngredient() : ingredient.getRawText()))
                .anyMatch(excluded::contains);
    }

    private Integer extractCalories(List<NutritionDto> nutritions) {
        if (nutritions == null) {
            return null;
        }
        return nutritions.stream()
                .filter(nutrition -> nutrition.getNutrient() != null && nutrition.getNutrient().toLowerCase(Locale.ROOT).contains("cal"))
                .map(NutritionDto::getAmount)
                .map(this::parseFirstInteger)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    private Integer parseFirstInteger(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9.]", "");
        if (digits.isBlank()) {
            return null;
        }
        return (int) Math.round(Double.parseDouble(digits));
    }

    private List<String> extractIds(List<ReferenceItemResponse> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(ReferenceItemResponse::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
    }

    private boolean matchesPantry(String ingredientName, Set<String> pantryNames) {
        if (ingredientName == null || ingredientName.isBlank()) {
            return false;
        }
        for (String pantryName : pantryNames) {
            if (areIngredientsCompatible(ingredientName, pantryName)) {
                return true;
            }
        }
        return false;
    }

    private boolean areIngredientsCompatible(String ingredientName, String pantryName) {
        if (ingredientName.equals(pantryName)) {
            return true;
        }

        if (hasMeaningfulPhraseOverlap(ingredientName, pantryName)) {
            return true;
        }

        List<String> ingredientTokens = tokenizeForMatching(ingredientName);
        List<String> pantryTokens = tokenizeForMatching(pantryName);
        for (String ingredientToken : ingredientTokens) {
            for (String pantryToken : pantryTokens) {
                if (tokensPartiallyMatch(ingredientToken, pantryToken)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasMeaningfulPhraseOverlap(String left, String right) {
        if (!left.contains(" ") && !right.contains(" ")) {
            return false;
        }
        int shorterLength = Math.min(left.length(), right.length());
        if (shorterLength < MIN_PREFIX_MATCH_LENGTH) {
            return false;
        }
        return left.contains(right) || right.contains(left);
    }

    private List<String> tokenizeForMatching(String value) {
        return Stream.of(value.split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
                .toList();
    }

    private boolean tokensPartiallyMatch(String left, String right) {
        if (left.equals(right)) {
            return true;
        }

        String leftCanonical = canonicalizeToken(left);
        String rightCanonical = canonicalizeToken(right);
        if (leftCanonical.equals(rightCanonical)) {
            return true;
        }

        return hasSafePrefixOverlap(left, right) || hasSafePrefixOverlap(leftCanonical, rightCanonical);
    }

    private boolean hasSafePrefixOverlap(String left, String right) {
        String shorter = left.length() <= right.length() ? left : right;
        String longer = shorter.equals(left) ? right : left;
        if (shorter.length() < MIN_PREFIX_MATCH_LENGTH) {
            return false;
        }
        if (!longer.startsWith(shorter)) {
            return false;
        }
        return longer.length() - shorter.length() <= MAX_PREFIX_EXTRA_CHARS;
    }

    private String canonicalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.endsWith("ies") && token.length() > MIN_TOKEN_LENGTH + 2) {
            return token.substring(0, token.length() - 3) + "y";
        }
        for (String suffix : TOKEN_CANONICAL_SUFFIXES) {
            if (!token.endsWith(suffix)) {
                continue;
            }
            String trimmed = token.substring(0, token.length() - suffix.length());
            if (trimmed.length() >= MIN_TOKEN_LENGTH) {
                return trimmed;
            }
        }
        return token;
    }

    private CreateShoppingListItemRequest toShoppingListItemRequest(IngredientDto ingredient) {
        return CreateShoppingListItemRequest.builder()
                .name(resolveIngredientName(ingredient))
                .quantity(ingredient.getQuantityValue() == null ? null : BigDecimal.valueOf(ingredient.getQuantityValue()))
                .unit(ingredient.getUnit() == null || ingredient.getUnit().isBlank() ? null : ingredient.getUnit())
                .build();
    }

    private String resolveIngredientName(IngredientDto ingredientDto) {
        if (ingredientDto == null) {
            return "";
        }
        if (ingredientDto.getIngredient() != null && !ingredientDto.getIngredient().isBlank()) {
            return ingredientDto.getIngredient();
        }
        return ingredientDto.getRawText() == null ? "" : ingredientDto.getRawText();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
