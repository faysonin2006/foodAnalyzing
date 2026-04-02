package com.recipeservice.controllers.api;

import com.recipeservice.dtos.recommendations.AddMissingIngredientsResponse;
import com.recipeservice.dtos.recommendations.RecipeRecommendationRequest;
import com.recipeservice.dtos.recommendations.RecipeRecommendationResponse;
import com.recipeservice.dtos.spoonacular.SpoonAnalyzedInstructionDto;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularRequest;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularResponse;
import com.recipeservice.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "External Recipes", description = "Recipe search endpoints backed by Spoonacular")
public interface RecipeControllerApi {

    @Operation(
            summary = "Search external recipes",
            description = "Requests recipes from Spoonacular using optional search, cuisine, diet and nutrition filters.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipes returned"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream recipe provider error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<SpoonacularResponse> searchRecipes(
            @Parameter(
                    description = "Search filters passed as query parameters. Example: query=pasta&diet=vegetarian&number=10&offset=0",
                    examples = @ExampleObject(value = "query=pasta&diet=vegetarian&number=10&offset=0")
            )
            SpoonacularRequest request
    );

    @Operation(
            summary = "Get recipe instructions",
            description = "Returns analyzed cooking instructions for a Spoonacular recipe id.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instructions returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SpoonAnalyzedInstructionDto.class)))),
            @ApiResponse(responseCode = "400", description = "Recipe id is invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream recipe provider error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<SpoonAnalyzedInstructionDto>> searchInstructions(
            @Parameter(description = "Spoonacular recipe id", example = "716429", required = true) String id
    );

    @Operation(
            summary = "Get pantry-based recipe recommendations",
            description = "Builds recipe recommendations from pantry items and profile constraints using the local recipe database.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendations returned",
                    content = @Content(schema = @Schema(implementation = RecipeRecommendationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid recommendation request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream pantry or recipe database error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<RecipeRecommendationResponse> recommendRecipes(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Recommendation request with size, sorting and locale options.",
                    content = @Content(schema = @Schema(implementation = RecipeRecommendationRequest.class))
            )
            RecipeRecommendationRequest request
    );

    @Operation(
            summary = "Add missing ingredients to shopping list",
            description = "Compares recipe ingredients with pantry items and creates shopping list entries for missing ingredients.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Missing ingredients added to shopping list",
                    content = @Content(schema = @Schema(implementation = AddMissingIngredientsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Recipe id is invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream pantry, recipe database or shopping list error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<AddMissingIngredientsResponse> addMissingIngredientsToShoppingList(
            @Parameter(description = "Local recipe id from the recipe database", example = "123", required = true) Long recipeId
    );
}
