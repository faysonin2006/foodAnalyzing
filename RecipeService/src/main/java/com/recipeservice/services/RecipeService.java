package com.recipeservice.services;

import com.recipeservice.configs.spoonacularyclient.SpoonacularClient;
import com.recipeservice.dtos.spoonacular.SpoonAnalyzedInstructionDto;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularRequest;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final SpoonacularRequestMapper mapper;
    private final SpoonacularClient spoonacularClient;
    public SpoonacularResponse searchRecipe(SpoonacularRequest request) {
        return spoonacularClient.complexSearch(mapper.toMap(request));

    }

    public List<SpoonAnalyzedInstructionDto> getInstructions(String id) {
        return spoonacularClient.getAnalyzedInstructions(id);
    }
}
