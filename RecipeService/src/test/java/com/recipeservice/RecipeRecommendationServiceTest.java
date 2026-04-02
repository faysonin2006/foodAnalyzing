package com.recipeservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipeservice.clients.RecipesDbClient;
import com.recipeservice.clients.UserPantryClient;
import com.recipeservice.clients.UserProfileClient;
import com.recipeservice.clients.UserShoppingListClient;
import com.recipeservice.dtos.internal.pantry.PantryListItemResponse;
import com.recipeservice.dtos.internal.profile.ReferenceItemResponse;
import com.recipeservice.dtos.internal.profile.UserProfileResponse;
import com.recipeservice.dtos.internal.recipesdb.CardFullRecipeResponse;
import com.recipeservice.dtos.internal.recipesdb.CardRecipeResponse;
import com.recipeservice.dtos.internal.recipesdb.IngredientDto;
import com.recipeservice.dtos.internal.recipesdb.NutritionDto;
import com.recipeservice.dtos.internal.shopping.ShoppingListItemResponse;
import com.recipeservice.dtos.recommendations.AddMissingIngredientsResponse;
import com.recipeservice.dtos.recommendations.RecipeRecommendationRequest;
import com.recipeservice.dtos.recommendations.RecipeRecommendationResponse;
import com.recipeservice.services.RecipeService;
import com.recipeservice.services.SpoonacularRequestMapper;
import com.recipeservice.configs.spoonacularyclient.SpoonacularClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeRecommendationServiceTest {

    @Mock
    private SpoonacularClient spoonacularClient;
    @Mock
    private UserProfileClient userProfileClient;
    @Mock
    private UserPantryClient userPantryClient;
    @Mock
    private RecipesDbClient recipesDbClient;
    @Mock
    private UserShoppingListClient userShoppingListClient;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(
                new SpoonacularRequestMapper(new ObjectMapper()),
                spoonacularClient,
                userProfileClient,
                userPantryClient,
                userShoppingListClient,
                recipesDbClient
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recommendRecipesShouldReturnMatchedAndMissingIngredients() {
        when(userProfileClient.getProfile("user@example.com")).thenReturn(
                UserProfileResponse.builder()
                        .id(UUID.randomUUID())
                        .targetCalories(900)
                        .dietPreferences(List.of(ReferenceItemResponse.builder().id("OMNIVORE").build()))
                        .allergies(List.of())
                        .healthConditions(List.of())
                        .build()
        );
        when(userPantryClient.getPantry("user@example.com")).thenReturn(List.of(
                PantryListItemResponse.builder().name("chicken").build(),
                PantryListItemResponse.builder().name("tomato").build()
        ));
        when(recipesDbClient.search(any())).thenReturn(List.of(
                CardRecipeResponse.builder()
                        .recipeId(1L)
                        .title("Chicken Salad")
                        .category("salad")
                        .nutritions(List.of(NutritionDto.builder().nutrient("Calories").amount("420").build()))
                        .build()
        ));
        when(recipesDbClient.getById(1L)).thenReturn(
                CardFullRecipeResponse.builder()
                        .recipeId(1L)
                        .title("Chicken Salad")
                        .ingredients(List.of(
                                IngredientDto.builder().ingredient("chicken").build(),
                                IngredientDto.builder().ingredient("tomato").build(),
                                IngredientDto.builder().ingredient("lettuce").build()
                        ))
                        .build()
        );

        RecipeRecommendationResponse response = recipeService.recommendRecipes(
                RecipeRecommendationRequest.builder().size(10).sortBy("match").build()
        );

        assertEquals(1, response.getRecipes().size());
        assertEquals(2, response.getRecipes().get(0).getMatchingIngredientsCount());
        assertEquals(1, response.getRecipes().get(0).getMissingIngredientsCount());
        assertEquals("Chicken Salad", response.getRecipes().get(0).getTitle());
    }

    @Test
    void recommendRecipesShouldPrioritizeAbsoluteIngredientMatchesAndSupportPartialMatches() {
        when(userProfileClient.getProfile("user@example.com")).thenReturn(
                UserProfileResponse.builder()
                        .id(UUID.randomUUID())
                        .targetCalories(1200)
                        .dietPreferences(List.of())
                        .allergies(List.of())
                        .healthConditions(List.of())
                        .build()
        );
        when(userPantryClient.getPantry("user@example.com")).thenReturn(List.of(
                PantryListItemResponse.builder().name("chicken breast").build(),
                PantryListItemResponse.builder().name("tomatoes").build(),
                PantryListItemResponse.builder().name("olive oil").build()
        ));
        when(recipesDbClient.search(any())).thenReturn(List.of(
                CardRecipeResponse.builder()
                        .recipeId(1L)
                        .title("Big Match Bowl")
                        .category("main")
                        .nutritions(List.of(NutritionDto.builder().nutrient("Calories").amount("480").build()))
                        .build(),
                CardRecipeResponse.builder()
                        .recipeId(2L)
                        .title("Perfect Ratio Snack")
                        .category("snack")
                        .nutritions(List.of(NutritionDto.builder().nutrient("Calories").amount("250").build()))
                        .build()
        ));
        when(recipesDbClient.getById(1L)).thenReturn(
                CardFullRecipeResponse.builder()
                        .recipeId(1L)
                        .title("Big Match Bowl")
                        .ingredients(List.of(
                                IngredientDto.builder().ingredient("chicken").build(),
                                IngredientDto.builder().ingredient("tomato").build(),
                                IngredientDto.builder().ingredient("olive oil").build(),
                                IngredientDto.builder().ingredient("lettuce").build()
                        ))
                        .build()
        );
        when(recipesDbClient.getById(2L)).thenReturn(
                CardFullRecipeResponse.builder()
                        .recipeId(2L)
                        .title("Perfect Ratio Snack")
                        .ingredients(List.of(
                                IngredientDto.builder().ingredient("tomato").build(),
                                IngredientDto.builder().ingredient("olive oil").build()
                        ))
                        .build()
        );

        RecipeRecommendationResponse response = recipeService.recommendRecipes(
                RecipeRecommendationRequest.builder().size(10).sortBy("matchScore").build()
        );

        assertEquals(2, response.getRecipes().size());
        assertEquals("Big Match Bowl", response.getRecipes().get(0).getTitle());
        assertEquals(3, response.getRecipes().get(0).getMatchingIngredientsCount());
        assertEquals(1, response.getRecipes().get(0).getMissingIngredientsCount());
        assertTrue(response.getRecipes().get(0).getMatchingIngredients().contains("chicken"));
        assertTrue(response.getRecipes().get(0).getMatchingIngredients().contains("tomato"));
        assertEquals("Perfect Ratio Snack", response.getRecipes().get(1).getTitle());
        assertEquals(2, response.getRecipes().get(1).getMatchingIngredientsCount());
    }

    @Test
    void recommendRecipesShouldAvoidFalsePositivePrefixMatches() {
        when(userProfileClient.getProfile("user@example.com")).thenReturn(
                UserProfileResponse.builder()
                        .id(UUID.randomUUID())
                        .targetCalories(1200)
                        .dietPreferences(List.of())
                        .allergies(List.of())
                        .healthConditions(List.of())
                        .build()
        );
        when(userPantryClient.getPantry("user@example.com")).thenReturn(List.of(
                PantryListItemResponse.builder().name("молоко").build()
        ));
        when(recipesDbClient.search(any())).thenReturn(List.of(
                CardRecipeResponse.builder()
                        .recipeId(3L)
                        .title("Pepper Mix")
                        .category("spice")
                        .nutritions(List.of(NutritionDto.builder().nutrient("Calories").amount("60").build()))
                        .build()
        ));
        when(recipesDbClient.getById(3L)).thenReturn(
                CardFullRecipeResponse.builder()
                        .recipeId(3L)
                        .title("Pepper Mix")
                        .ingredients(List.of(
                                IngredientDto.builder().ingredient("молотый перец").build()
                        ))
                        .build()
        );

        RecipeRecommendationResponse response = recipeService.recommendRecipes(
                RecipeRecommendationRequest.builder().size(10).sortBy("match").build()
        );

        assertEquals(1, response.getRecipes().size());
        assertEquals(0, response.getRecipes().getFirst().getMatchingIngredientsCount());
        assertEquals(1, response.getRecipes().getFirst().getMissingIngredientsCount());
    }

    @Test
    void addMissingIngredientsToShoppingListShouldCreateOnlyMissingItems() {
        when(userPantryClient.getPantry("user@example.com")).thenReturn(List.of(
                PantryListItemResponse.builder().name("chicken").build(),
                PantryListItemResponse.builder().name("tomato").build()
        ));
        when(recipesDbClient.getById(1L)).thenReturn(
                CardFullRecipeResponse.builder()
                        .recipeId(1L)
                        .title("Chicken Salad")
                        .ingredients(List.of(
                                IngredientDto.builder().ingredient("chicken").quantityValue(1.0).unit("kg").build(),
                                IngredientDto.builder().ingredient("lettuce").quantityValue(2.0).unit("piece").build()
                        ))
                        .build()
        );
        when(userShoppingListClient.createItems(any(), any())).thenReturn(List.of(
                ShoppingListItemResponse.builder().name("lettuce").quantity(new java.math.BigDecimal("2.0")).unit("piece").build()
        ));

        AddMissingIngredientsResponse response = recipeService.addMissingIngredientsToShoppingList(1L);

        assertEquals(1L, response.getRecipeId());
        assertEquals(1, response.getAddedItemsCount());
        assertEquals("lettuce", response.getItems().get(0).getName());
    }
}
