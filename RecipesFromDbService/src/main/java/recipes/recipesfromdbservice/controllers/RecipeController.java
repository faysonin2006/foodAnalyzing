package recipes.recipesfromdbservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import recipes.recipesfromdbservice.controllers.api.RecipeControllerApi;
import recipes.recipesfromdbservice.dtos.CardFullRecipeResponse;
import recipes.recipesfromdbservice.dtos.CardRecipeRequest;
import recipes.recipesfromdbservice.dtos.CardRecipeResponse;
import recipes.recipesfromdbservice.services.RecipeService;

import java.util.List;

@RestController
@RequestMapping("/api/recipes/db")
@RequiredArgsConstructor
public class RecipeController implements RecipeControllerApi {

    private final RecipeService recipeService;

    @Override
    @PostMapping("/search")
    public ResponseEntity<List<CardRecipeResponse>> searchRecipes(
            @RequestBody CardRecipeRequest request
    ) {
        return ResponseEntity.ok(recipeService.getRecipes(request));
    }

    @Override
    @GetMapping("/{recipeId}")
    public ResponseEntity<CardFullRecipeResponse> getRecipeById(
            @PathVariable Long recipeId
    ) {
        return ResponseEntity.ok(recipeService.getRecipe(recipeId));
    }
}
