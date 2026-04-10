package recipes.recipesfromdbservice.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import recipes.recipesfromdbservice.configs.exceptionhandler.ErrorResponse;
import recipes.recipesfromdbservice.dtos.CardFullRecipeResponse;
import recipes.recipesfromdbservice.dtos.CardRecipeRequest;
import recipes.recipesfromdbservice.dtos.CardRecipeResponse;
import recipes.recipesfromdbservice.dtos.CreateRecipeCommentRequest;
import recipes.recipesfromdbservice.dtos.responseDtos.RecipeCommentDto;

import java.security.Principal;
import java.util.UUID;
import java.util.List;

@Tag(name = "Recipes DB", description = "Recipe search and recipe details backed by the local recipe database")
public interface RecipeControllerApi {

    @Operation(
            summary = "Search recipes in local database",
            description = "Returns paged recipe cards filtered by title, category, diet, allergy and health constraints.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipes returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<CardRecipeResponse>> searchRecipes(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Recipe search payload",
                    content = @Content(
                            schema = @Schema(implementation = CardRecipeRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "lang": "EN",
                                      "title": "chicken",
                                      "category": "dinner",
                                      "requiredDietKeys": ["HIGH_PROTEIN"],
                                      "allergyKeys": ["GLUTEN"],
                                      "page": 1,
                                      "size": 10,
                                      "sortBy": "recipe_id",
                                      "sortDir": "desc"
                                    }
                                    """)
                    )
            )
            CardRecipeRequest request
    );

    @Operation(
            summary = "Get recipe details from local database",
            description = "Returns full recipe information by internal recipe id.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe returned"),
            @ApiResponse(responseCode = "400", description = "Recipe id is invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<CardFullRecipeResponse> getRecipeById(
            @Parameter(description = "Internal recipe id", example = "204896", required = true) Long recipeId,
            @Parameter(hidden = true) Principal principal,
            @Parameter(hidden = true) UUID authenticatedUserId
    );

    @Operation(
            summary = "Add a comment to a recipe",
            description = "Creates a new user comment for the selected recipe.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment created"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<RecipeCommentDto> createRecipeComment(
            @Parameter(description = "Internal recipe id", example = "204896", required = true) Long recipeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Recipe comment payload",
                    content = @Content(
                            schema = @Schema(implementation = CreateRecipeCommentRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "text": "Очень понравился рецепт, получилось с первого раза."
                                    }
                                    """)
                    )
            )
            CreateRecipeCommentRequest request,
            @Parameter(hidden = true) Principal principal,
            @Parameter(hidden = true) UUID authenticatedUserId
    );

    @Operation(
            summary = "Like a recipe comment",
            description = "Adds the current user like to the selected recipe comment.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment liked"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<RecipeCommentDto> likeRecipeComment(
            @Parameter(description = "Recipe comment id", example = "15", required = true) Long commentId,
            @Parameter(hidden = true) Principal principal,
            @Parameter(hidden = true) UUID authenticatedUserId
    );

    @Operation(
            summary = "Remove like from a recipe comment",
            description = "Removes the current user like from the selected recipe comment.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment unliked"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Comment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<RecipeCommentDto> unlikeRecipeComment(
            @Parameter(description = "Recipe comment id", example = "15", required = true) Long commentId,
            @Parameter(hidden = true) Principal principal,
            @Parameter(hidden = true) UUID authenticatedUserId
    );

}
