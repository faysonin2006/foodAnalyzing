package com.recipeservice.services;

import com.recipeservice.configs.spoonacularyclient.SpoonacularClient;
import com.recipeservice.constants.AppMessages;
import com.recipeservice.dtos.spoonacular.SpoonAnalyzedInstructionDto;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularRequest;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularResponse;
import com.recipeservice.exceptions.BadRequestException;
import com.recipeservice.exceptions.UpstreamServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final SpoonacularRequestMapper mapper;
    private final SpoonacularClient spoonacularClient;

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
}
