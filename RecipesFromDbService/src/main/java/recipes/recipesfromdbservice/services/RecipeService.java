package recipes.recipesfromdbservice.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.BadRequestException;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.RecipeNotFoundException;
import recipes.recipesfromdbservice.dtos.CardFullRecipeResponse;
import recipes.recipesfromdbservice.dtos.CardRecipeRequest;
import recipes.recipesfromdbservice.dtos.CardRecipeResponse;
import recipes.recipesfromdbservice.dtos.Languages;
import recipes.recipesfromdbservice.dtos.responseDtos.*;
import recipes.recipesfromdbservice.repositories.RecipeRepository;
import recipes.recipesfromdbservice.repositories.projections.CardFullRecipeRow;
import recipes.recipesfromdbservice.repositories.projections.RecipeCardListRow;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ObjectMapper objectMapper;

    public List<CardRecipeResponse> getRecipes(CardRecipeRequest request){
        if (request == null){
            throw new BadRequestException("Request can't be null");
        }
        String lang = request.getLang() == null ? "en" : request.getLang().getLowerCaseString();
        int size = request.getSize() == null ? 10 : Math.max(1, Math.min(request.getSize(), 20));
        int page = request.getPage() == null ? 1 : Math.max(1, request.getPage());
        int offset = (page - 1) * size;

        String title = blankToNull(request.getTitle());
        String sortBy = normalizeSortBy(request.getSortBy());
        String sortDir = normalizeSortDir(request.getSortDir());
        String category = blankToNull(request.getCategory());

        String[] allergyKeys = normalizeLists(request.getAllergyKeys());
        String[] healthConditionKeys = normalizeLists(request.getHealthConditionKeys());
        String[] preferredHealthKeys = normalizeLists(request.getPreferredHealthKeys());
        String[] requiredDietKeys = normalizeLists(request.getRequiredDietKeys());

        return recipeRepository.findRecipes(
                        lang, title, category,
                        requiredDietKeys, preferredHealthKeys, allergyKeys, healthConditionKeys,
                        sortBy, sortDir, size, offset
                ).stream()
                .map(this::toCardRecipeResponse)
                .toList();
    }

    private CardRecipeResponse toCardRecipeResponse(RecipeCardListRow row) {
        return CardRecipeResponse.builder()
                .recipeId(row.getRecipeId())
                .title(row.getTitle())
                .image(row.getImage())
                .category(row.getCategory())
                .ingredientsCount(defaultInt(row.getIngredientsCount()))
                .instructionsCount(defaultInt(row.getInstructionsCount()))
                .nutritions(readJson(row.getNutritionsJson(), new TypeReference<List<NutritionDto>>() {}, List.of()))
                .times(readJson(row.getTimesJson(), RecipeTimesDto.class, new RecipeTimesDto()))
                .build();
    }

    private CardFullRecipeResponse toCardFullRecipeResponse(CardFullRecipeRow row) {
        return CardFullRecipeResponse.builder()
                .recipeId(row.getRecipeId())
                .title(row.getTitle())
                .image(row.getImage())
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

    private int defaultInt(Integer v) {
        return v == null ? 0 : v;
    }

    private <T> T readJson(String json, TypeReference<T> ref, T fallback) {
        try {
            if (json == null || json.isBlank() || "null".equalsIgnoreCase(json)) return fallback;
            return objectMapper.readValue(json, ref);
        } catch (Exception e) {
            return fallback;
        }
    }

    private <T> T readJson(String json, Class<T> clazz, T fallback) {
        try {
            if (json == null || json.isBlank() || "null".equalsIgnoreCase(json)) return fallback;
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return fallback;
        }
    }


    public CardFullRecipeResponse getRecipe(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Recipe id must be positive");
        }

        CardFullRecipeRow row = recipeRepository.getFullRecipeInfo(id)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found: " + id));

        return toCardFullRecipeResponse(row);
    }

    private String blankToNull(String value){
        if (value == null) return null;
        String res = value.trim();
        return res.isEmpty() ? null : res;
    }

    private String[] normalizeLists(List<String> list){
        if (list == null || list.isEmpty()) return new String[0];
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toArray(String[]::new);
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) return "recipe_id";
        return switch (sortBy.trim().toLowerCase()) {
            case "title", "category", "ingredients_count", "instructions_count", "recipe_id" -> sortBy.trim().toLowerCase();
            default -> "recipe_id";
        };
    }

    private String normalizeSortDir(String sortDir) {
        if (sortDir == null) return "desc";
        String result = sortDir.trim().toLowerCase();
        return result.equals("asc") ? "asc" : "desc";
    }
}

