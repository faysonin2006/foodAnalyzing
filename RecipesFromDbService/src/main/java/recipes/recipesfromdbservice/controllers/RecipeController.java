package recipes.recipesfromdbservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import recipes.recipesfromdbservice.controllers.api.RecipeControllerApi;
import recipes.recipesfromdbservice.dtos.CardFullRecipeResponse;
import recipes.recipesfromdbservice.dtos.CardRecipeRequest;
import recipes.recipesfromdbservice.dtos.CardRecipeResponse;
import recipes.recipesfromdbservice.dtos.CreateRecipeCommentRequest;
import recipes.recipesfromdbservice.dtos.responseDtos.RecipeCommentDto;
import recipes.recipesfromdbservice.services.RecipeService;

import java.security.Principal;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/recipes/db")
@RequiredArgsConstructor
public class RecipeController implements RecipeControllerApi {

    private final RecipeService recipeService;

    @Override
    @PostMapping("/search")
    public ResponseEntity<List<CardRecipeResponse>> searchRecipes(
            @Valid @RequestBody CardRecipeRequest request
    ) {
        return ResponseEntity.ok(recipeService.getRecipes(request));
    }

    @Override
    @GetMapping("/{recipeId}")
    public ResponseEntity<CardFullRecipeResponse> getRecipeById(
            @PathVariable Long recipeId,
            Principal principal,
            @RequestAttribute(value = "authenticatedUserId", required = false) UUID authenticatedUserId
    ) {
        return ResponseEntity.ok(recipeService.getRecipe(recipeId, authenticatedUserId));
    }

    @Override
    @PostMapping("/{recipeId}/comments")
    public ResponseEntity<RecipeCommentDto> createRecipeComment(
            @PathVariable Long recipeId,
            @Valid @RequestBody CreateRecipeCommentRequest request,
            Principal principal,
            @RequestAttribute(value = "authenticatedUserId", required = false) UUID authenticatedUserId
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RecipeCommentDto comment = recipeService.addRecipeComment(
                recipeId,
                request,
                principal.getName(),
                authenticatedUserId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @Override
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<RecipeCommentDto> likeRecipeComment(
            @PathVariable Long commentId,
            Principal principal,
            @RequestAttribute(value = "authenticatedUserId", required = false) UUID authenticatedUserId
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank() || authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(recipeService.setRecipeCommentLike(commentId, authenticatedUserId, true));
    }

    @Override
    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<RecipeCommentDto> unlikeRecipeComment(
            @PathVariable Long commentId,
            Principal principal,
            @RequestAttribute(value = "authenticatedUserId", required = false) UUID authenticatedUserId
    ) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank() || authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(recipeService.setRecipeCommentLike(commentId, authenticatedUserId, false));
    }
}
