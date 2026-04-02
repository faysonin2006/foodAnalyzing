package com.recipeservice.controllers;

import com.recipeservice.controllers.api.RecipeControllerApi;
import com.recipeservice.dtos.recommendations.AddMissingIngredientsResponse;
import com.recipeservice.dtos.recommendations.RecipeRecommendationRequest;
import com.recipeservice.dtos.recommendations.RecipeRecommendationResponse;
import com.recipeservice.dtos.spoonacular.SpoonAnalyzedInstructionDto;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularRequest;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularResponse;
import com.recipeservice.services.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController implements RecipeControllerApi {

    private final RecipeService recipeService;

    @Override
    @GetMapping("/search")
    public ResponseEntity<SpoonacularResponse> searchRecipes(@ModelAttribute SpoonacularRequest request) {
        SpoonacularResponse spoonacularResponse = recipeService.searchRecipe(request);
        return new ResponseEntity<>(spoonacularResponse, HttpStatus.OK);
    }

    @Override
    @GetMapping("/search/{id}/instructions")
    public ResponseEntity<List<SpoonAnalyzedInstructionDto>> searchInstructions(@PathVariable String id) {
        List<SpoonAnalyzedInstructionDto> instructionDto = recipeService.getInstructions(id);
        return new ResponseEntity<>(instructionDto, HttpStatus.OK);
    }

    @Override
    @PostMapping("/recommendations")
    public ResponseEntity<RecipeRecommendationResponse> recommendRecipes(
            @RequestBody RecipeRecommendationRequest request
    ) {
        RecipeRecommendationResponse response = recipeService.recommendRecipes(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @PostMapping("/{recipeId}/shopping-list")
    public ResponseEntity<AddMissingIngredientsResponse> addMissingIngredientsToShoppingList(
            @PathVariable Long recipeId
    ) {
        AddMissingIngredientsResponse response = recipeService.addMissingIngredientsToShoppingList(recipeId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
