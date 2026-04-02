package com.recipeservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipeservice.clients.RecipesDbClient;
import com.recipeservice.clients.UserPantryClient;
import com.recipeservice.clients.UserProfileClient;
import com.recipeservice.clients.UserShoppingListClient;
import com.recipeservice.configs.spoonacularyclient.SpoonacularClient;
import com.recipeservice.dtos.recommendations.AddMissingIngredientsResponse;
import com.recipeservice.dtos.recommendations.RecipeRecommendationRequest;
import com.recipeservice.dtos.recommendations.RecipeRecommendationResponse;
import com.recipeservice.services.RecipeService;
import com.recipeservice.services.SpoonacularRequestMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RecipeServiceIntegrationTest {

    private static final String USER_EMAIL = "user@example.com";
    private static final String SERVICE_TOKEN = "service-token";

    private MockWebServer userServiceServer;
    private MockWebServer recipesDbServer;
    private RecipeService recipeService;

    @BeforeEach
    void setUp() throws IOException {
        userServiceServer = new MockWebServer();
        recipesDbServer = new MockWebServer();
        userServiceServer.start();
        recipesDbServer.start();

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        UserProfileClient userProfileClient = createClient(userServiceServer.url("/").toString(), UserProfileClient.class, true);
        UserPantryClient userPantryClient = createClient(userServiceServer.url("/").toString(), UserPantryClient.class, true);
        UserShoppingListClient userShoppingListClient = createClient(userServiceServer.url("/").toString(), UserShoppingListClient.class, true);
        RecipesDbClient recipesDbClient = createClient(recipesDbServer.url("/").toString(), RecipesDbClient.class, false);

        recipeService = new RecipeService(
                new SpoonacularRequestMapper(objectMapper),
                mock(SpoonacularClient.class),
                userProfileClient,
                userPantryClient,
                userShoppingListClient,
                recipesDbClient
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_EMAIL, null, List.of())
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        SecurityContextHolder.clearContext();
        userServiceServer.shutdown();
        recipesDbServer.shutdown();
    }

    @Test
    void recommendRecipesShouldCallUserAndRecipesDbServices() throws Exception {
        userServiceServer.enqueue(jsonResponse("""
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "email": "user@example.com",
                  "targetCalories": 700,
                  "dietPreferences": [{"id":"HIGH_PROTEIN","description":"high protein"}],
                  "allergies": [],
                  "healthConditions": []
                }
                """));
        userServiceServer.enqueue(jsonResponse("""
                [
                  {"id":"22222222-2222-2222-2222-222222222222","name":"chicken","quantity":1,"unit":"kg","status":"ACTIVE"},
                  {"id":"33333333-3333-3333-3333-333333333333","name":"tomato","quantity":2,"unit":"piece","status":"ACTIVE"}
                ]
                """));
        recipesDbServer.enqueue(jsonResponse("""
                [
                  {
                    "recipeId": 1,
                    "title": "Chicken Salad",
                    "image": "https://example.com/chicken-salad.jpg",
                    "category": "salad",
                    "ingredientsCount": 3,
                    "instructionsCount": 2,
                    "nutritions": [{"nutrient":"Calories","amount":"420 kcal"}],
                    "times": {"total":"20 min"}
                  }
                ]
                """));
        recipesDbServer.enqueue(jsonResponse("""
                {
                  "recipeId": 1,
                  "title": "Chicken Salad",
                  "image": "https://example.com/chicken-salad.jpg",
                  "ingredientsCount": 3,
                  "instructionsCount": 2,
                  "ingredients": [
                    {"ingredient":"chicken"},
                    {"ingredient":"tomato"},
                    {"ingredient":"lettuce"}
                  ],
                  "nutritions": [{"nutrient":"Calories","amount":"420 kcal"}],
                  "blockDietKeys": [],
                  "blockAllergyKeys": [],
                  "blockHealthKeys": [],
                  "cautionHealthKeys": []
                }
                """));

        RecipeRecommendationResponse response = recipeService.recommendRecipes(
                RecipeRecommendationRequest.builder().size(5).sortBy("match").build()
        );

        assertEquals(1, response.getRecipes().size());
        assertEquals(2, response.getRecipes().getFirst().getMatchingIngredientsCount());
        assertEquals(1, response.getRecipes().getFirst().getMissingIngredientsCount());

        RecordedRequest profileRequest = userServiceServer.takeRequest();
        RecordedRequest pantryRequest = userServiceServer.takeRequest();
        RecordedRequest searchRequest = recipesDbServer.takeRequest();
        RecordedRequest detailsRequest = recipesDbServer.takeRequest();

        assertEquals("/api/profiles/internal/user%40example.com", profileRequest.getPath());
        assertEquals("Bearer " + SERVICE_TOKEN, profileRequest.getHeader("Authorization"));
        assertEquals("/api/pantry/internal/user%40example.com", pantryRequest.getPath());
        assertEquals("/api/recipes/db/search", searchRequest.getPath());
        assertEquals("POST", searchRequest.getMethod());
        assertTrue(searchRequest.getBody().readUtf8().contains("\"requiredDietKeys\":[\"HIGH_PROTEIN\"]"));
        assertEquals("/api/recipes/db/1", detailsRequest.getPath());
    }

    @Test
    void addMissingIngredientsToShoppingListShouldUseSharedHttpContracts() throws Exception {
        userServiceServer.enqueue(jsonResponse("""
                [
                  {"id":"22222222-2222-2222-2222-222222222222","name":"chicken","quantity":1,"unit":"kg","status":"ACTIVE"}
                ]
                """));
        recipesDbServer.enqueue(jsonResponse("""
                {
                  "recipeId": 7,
                  "title": "Chicken Wrap",
                  "image": "https://example.com/wrap.jpg",
                  "ingredientsCount": 2,
                  "instructionsCount": 1,
                  "ingredients": [
                    {"ingredient":"chicken","quantityValue":1.0,"unit":"kg"},
                    {"ingredient":"tortilla","quantityValue":4.0,"unit":"piece"}
                  ],
                  "nutritions": [{"nutrient":"Calories","amount":"500 kcal"}],
                  "blockDietKeys": [],
                  "blockAllergyKeys": [],
                  "blockHealthKeys": [],
                  "cautionHealthKeys": []
                }
                """));
        userServiceServer.enqueue(jsonResponse("""
                [
                  {
                    "id":"44444444-4444-4444-4444-444444444444",
                    "name":"tortilla",
                    "quantity":4,
                    "unit":"piece",
                    "checked":false,
                    "createdAt":"2026-03-28T14:00:00",
                    "updatedAt":"2026-03-28T14:00:00"
                  }
                ]
                """));

        AddMissingIngredientsResponse response = recipeService.addMissingIngredientsToShoppingList(7L);

        assertEquals(7L, response.getRecipeId());
        assertEquals(1, response.getAddedItemsCount());
        assertEquals("tortilla", response.getItems().getFirst().getName());

        RecordedRequest pantryRequest = userServiceServer.takeRequest();
        RecordedRequest recipeRequest = recipesDbServer.takeRequest();
        RecordedRequest shoppingRequest = userServiceServer.takeRequest();

        assertEquals("/api/pantry/internal/user%40example.com", pantryRequest.getPath());
        assertEquals("/api/recipes/db/7", recipeRequest.getPath());
        assertEquals("/api/shopping-lists/internal/user%40example.com/items", shoppingRequest.getPath());
        assertEquals("POST", shoppingRequest.getMethod());
        assertEquals("Bearer " + SERVICE_TOKEN, shoppingRequest.getHeader("Authorization"));
        assertTrue(shoppingRequest.getBody().readUtf8().contains("\"name\":\"tortilla\""));
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private <T> T createClient(String baseUrl, Class<T> clientType, boolean attachServiceToken) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (attachServiceToken) {
            builder.requestInterceptor((request, body, execution) -> {
                request.getHeaders().setBearerAuth(SERVICE_TOKEN);
                return execution.execute(request, body);
            });
        }
        RestClient restClient = builder.build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(clientType);
    }
}
