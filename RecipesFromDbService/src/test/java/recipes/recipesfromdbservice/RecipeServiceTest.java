package recipes.recipesfromdbservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.BadRequestException;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.RecipeNotFoundException;
import recipes.recipesfromdbservice.dtos.CardFullRecipeResponse;
import recipes.recipesfromdbservice.dtos.CardRecipeRequest;
import recipes.recipesfromdbservice.dtos.CardRecipeResponse;
import recipes.recipesfromdbservice.dtos.Languages;
import recipes.recipesfromdbservice.repositories.RecipeRepository;
import recipes.recipesfromdbservice.repositories.projections.CardFullRecipeRow;
import recipes.recipesfromdbservice.repositories.projections.RecipeCardListRow;
import recipes.recipesfromdbservice.searchml.TensorFlowSearchReranker;
import recipes.recipesfromdbservice.services.RecipeService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private TensorFlowSearchReranker tensorFlowSearchReranker;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipeRepository, new ObjectMapper(), tensorFlowSearchReranker);
        lenient().when(tensorFlowSearchReranker.rerank(any(), any())).thenReturn(Map.of());
    }

    @Test
    void getRecipesShouldApplyOnlyExplicitManualFilters() {
        when(recipeRepository.findRecipes(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        recipeRow(
                                1L,
                                "Chicken Bowl",
                                "dinner",
                                """
                                        [{"ingredient":"chicken breast"},{"ingredient":"rice"}]
                                        """,
                                """
                                        [{"nutrient":"Calories","amount":"420 kcal"},{"nutrient":"Protein","amount":"32 g"},{"nutrient":"Fat","amount":"12 g"},{"nutrient":"Carbohydrates","amount":"41 g"}]
                                        """
                        ),
                        recipeRow(
                                2L,
                                "Chicken Peanut Salad",
                                "salad",
                                """
                                        [{"ingredient":"chicken"},{"ingredient":"peanut"}]
                                        """,
                                """
                                        [{"nutrient":"Calories","amount":"680 kcal"},{"nutrient":"Protein","amount":"21 g"},{"nutrient":"Fat","amount":"28 g"},{"nutrient":"Carbohydrates","amount":"38 g"}]
                                        """
                        )
                ));

        List<CardRecipeResponse> response = recipeService.getRecipes(CardRecipeRequest.builder()
                .lang(Languages.EN)
                .title("quick chicken without peanut")
                .excludeIngredients(List.of("peanut"))
                .maxCalories(500)
                .minProtein(25)
                .page(1)
                .size(10)
                .build());

        assertEquals(1, response.size());
        assertEquals(1L, response.getFirst().getRecipeId());
        assertEquals(List.of(), response.getFirst().getSearchMatchReasons());
    }

    @Test
    void getRecipesShouldApplyPagingAfterManualFilters() {
        when(recipeRepository.findRecipes(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        recipeRow(1L, "Recipe 1", "dinner", "[{\"ingredient\":\"chicken\"}]", "[{\"nutrient\":\"Calories\",\"amount\":\"300\"}]"),
                        recipeRow(2L, "Recipe 2", "dinner", "[{\"ingredient\":\"chicken\"}]", "[{\"nutrient\":\"Calories\",\"amount\":\"320\"}]")
                ));

        List<CardRecipeResponse> response = recipeService.getRecipes(CardRecipeRequest.builder()
                .lang(Languages.EN)
                .includeIngredients(List.of("chicken"))
                .page(2)
                .size(1)
                .build());

        assertEquals(1, response.size());
        assertEquals(2L, response.getFirst().getRecipeId());
    }

    @Test
    void getRecipesShouldNotInferHiddenRestrictionsFromQueryText() {
        when(recipeRepository.findRecipes(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        recipeRow(1L, "Quick Chicken Bowl", "dinner", "[{\"ingredient\":\"chicken breast\"},{\"ingredient\":\"rice\"}]", "[{\"nutrient\":\"Calories\",\"amount\":\"410 kcal\"}]"),
                        recipeRow(2L, "Cheesy Chicken Bake", "dinner", "[{\"ingredient\":\"chicken\"},{\"ingredient\":\"cheese\"}]", "[{\"nutrient\":\"Calories\",\"amount\":\"430 kcal\"}]")
                ));

        List<CardRecipeResponse> response = recipeService.getRecipes(CardRecipeRequest.builder()
                .lang(Languages.EN)
                .title("quick chicken without cheese")
                .page(1)
                .size(10)
                .build());

        assertEquals(2, response.size());
    }

    @Test
    void getRecipesShouldApplyTensorFlowSemanticRerankWhenAvailable() {
        when(recipeRepository.findRecipes(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        recipeRow(1L, "Weeknight Rice", "dinner", "[{\"ingredient\":\"rice\"},{\"ingredient\":\"vegetables\"}]", "[{\"nutrient\":\"Calories\",\"amount\":\"320 kcal\"}]"),
                        recipeRow(2L, "Chicken Rice Bowl", "dinner", "[{\"ingredient\":\"chicken breast\"},{\"ingredient\":\"rice\"}]", "[{\"nutrient\":\"Calories\",\"amount\":\"410 kcal\"}]")
                ));
        when(tensorFlowSearchReranker.rerank(any(), any()))
                .thenReturn(Map.of(2L, 0.91, 1L, 0.24));

        List<CardRecipeResponse> response = recipeService.getRecipes(CardRecipeRequest.builder()
                .lang(Languages.EN)
                .title("healthy chicken rice")
                .sortBy("search_score")
                .sortDir("desc")
                .page(1)
                .size(10)
                .build());

        assertEquals(2, response.size());
        assertEquals(2L, response.getFirst().getRecipeId());
        assertTrue(response.getFirst().getSearchMatchReasons().contains("semantic"));
    }

    @Test
    void getRecipeShouldReturnFullRecipe() {
        when(recipeRepository.getFullRecipeInfo(7L)).thenReturn(Optional.of(fullRecipeRow()));

        CardFullRecipeResponse response = recipeService.getRecipe(7L);

        assertEquals(7L, response.getRecipeId());
        assertEquals("Soup", response.getTitle());
        assertEquals("dinner", response.getCategory());
        assertEquals(1, response.getIngredients().size());
    }

    @Test
    void getRecipeShouldRejectInvalidId() {
        assertThrows(BadRequestException.class, () -> recipeService.getRecipe(0L));
    }

    @Test
    void getRecipeShouldThrowWhenMissing() {
        when(recipeRepository.getFullRecipeInfo(999L)).thenReturn(Optional.empty());

        assertThrows(RecipeNotFoundException.class, () -> recipeService.getRecipe(999L));
        verify(recipeRepository).getFullRecipeInfo(999L);
    }

    private RecipeCardListRow recipeRow(
            Long recipeId,
            String title,
            String category,
            String ingredientsJson,
            String nutritionsJson
    ) {
        return recipeRow(recipeId, title, category, ingredientsJson, nutritionsJson, "{\"total\":\"30 min\"}");
    }

    private RecipeCardListRow recipeRow(
            Long recipeId,
            String title,
            String category,
            String ingredientsJson,
            String nutritionsJson,
            String timesJson
    ) {
        return new RecipeCardListRow() {
            @Override
            public Long getRecipeId() {
                return recipeId;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getImage() {
                return "https://example.com/" + recipeId + ".jpg";
            }

            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public Integer getSearchScore() {
                return 0;
            }

            @Override
            public Integer getIngredientsCount() {
                return 2;
            }

            @Override
            public Integer getInstructionsCount() {
                return 1;
            }

            @Override
            public String getIngredientsJson() {
                return ingredientsJson;
            }

            @Override
            public String getNutritionsJson() {
                return nutritionsJson;
            }

            @Override
            public String getTimesJson() {
                return timesJson;
            }

            @Override
            public String getBlockDietKeysJson() {
                return "[]";
            }

            @Override
            public String getBlockAllergyKeysJson() {
                return "[]";
            }

            @Override
            public String getBlockHealthKeysJson() {
                return "[]";
            }

            @Override
            public String getCautionHealthKeysJson() {
                return "[]";
            }
        };
    }

    private CardFullRecipeRow fullRecipeRow() {
        return new CardFullRecipeRow() {
            @Override
            public Long getRecipeId() {
                return 7L;
            }

            @Override
            public String getTitle() {
                return "Soup";
            }

            @Override
            public String getImage() {
                return "https://example.com/soup.jpg";
            }

            @Override
            public String getCategory() {
                return "dinner";
            }

            @Override
            public Integer getIngredientsCount() {
                return 1;
            }

            @Override
            public Integer getInstructionsCount() {
                return 2;
            }

            @Override
            public String getIngredientsJson() {
                return """
                        [{"ingredient":"water","rawText":"water","quantityValue":1.0,"unit":"cup"}]
                        """;
            }

            @Override
            public String getInstructionStepsJson() {
                return """
                        [{"stepNumber":1,"description":"Boil water"}]
                        """;
            }

            @Override
            public String getNutritionsJson() {
                return """
                        [{"nutrient":"Calories","amount":"10 kcal"}]
                        """;
            }

            @Override
            public String getTimesJson() {
                return "{\"total\":\"10 min\"}";
            }

            @Override
            public String getBlockDietKeysJson() {
                return "[]";
            }

            @Override
            public String getBlockAllergyKeysJson() {
                return "[]";
            }

            @Override
            public String getBlockHealthKeysJson() {
                return "[]";
            }

            @Override
            public String getCautionHealthKeysJson() {
                return "[]";
            }

            @Override
            public String getConstraintsJson() {
                return "[]";
            }
        };
    }
}
