package com.recipeservice.controllers.api;

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
}
