package recipes.recipesfromdbservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import recipes.recipesfromdbservice.configs.exceptionhandler.GlobalExceptionHandler;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.RecipeNotFoundException;
import recipes.recipesfromdbservice.controllers.RecipeController;
import recipes.recipesfromdbservice.dtos.CardFullRecipeResponse;
import recipes.recipesfromdbservice.dtos.CardRecipeRequest;
import recipes.recipesfromdbservice.dtos.CardRecipeResponse;
import recipes.recipesfromdbservice.dtos.CreateRecipeCommentRequest;
import recipes.recipesfromdbservice.dtos.Languages;
import recipes.recipesfromdbservice.dtos.responseDtos.RecipeCommentDto;
import recipes.recipesfromdbservice.services.RecipeService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new RecipeController(recipeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void searchRecipesShouldReturnOk() throws Exception {
        when(recipeService.getRecipes(any())).thenReturn(List.of(
                CardRecipeResponse.builder()
                        .recipeId(1L)
                        .title("Chicken Bowl")
                        .category("dinner")
                        .build()
        ));

        CardRecipeRequest request = CardRecipeRequest.builder()
                .lang(Languages.EN)
                .page(1)
                .size(10)
                .build();

        mockMvc.perform(post("/api/recipes/db/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipeId").value(1))
                .andExpect(jsonPath("$[0].title").value("Chicken Bowl"));
    }

    @Test
    void searchRecipesShouldReturnBadRequestOnValidationError() throws Exception {
        CardRecipeRequest request = CardRecipeRequest.builder()
                .lang(Languages.EN)
                .page(0)
                .size(50)
                .build();

        mockMvc.perform(post("/api/recipes/db/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.page").exists())
                .andExpect(jsonPath("$.validationErrors.size").exists());
    }

    @Test
    void getRecipeByIdShouldReturnRecipe() throws Exception {
        when(recipeService.getRecipe(eq(7L), any())).thenReturn(CardFullRecipeResponse.builder()
                .recipeId(7L)
                .title("Soup")
                .category("dinner")
                .build());

        mockMvc.perform(get("/api/recipes/db/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(7))
                .andExpect(jsonPath("$.category").value("dinner"));
    }

    @Test
    void getRecipeByIdShouldReturnNotFound() throws Exception {
        when(recipeService.getRecipe(eq(999L), any())).thenThrow(new RecipeNotFoundException("Recipe not found: 999"));

        mockMvc.perform(get("/api/recipes/db/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recipe not found: 999"));
    }

    @Test
    void createRecipeCommentShouldReturnCreated() throws Exception {
        when(recipeService.addRecipeComment(eq(7L), any(), eq("cook@example.com"), any())).thenReturn(
                RecipeCommentDto.builder()
                        .id(15L)
                        .recipeId(7L)
                        .authorName("Cook")
                        .body("Loved this one.")
                        .build()
        );

        CreateRecipeCommentRequest request = new CreateRecipeCommentRequest("Loved this one.", null);

        mockMvc.perform(post("/api/recipes/db/7/comments")
                        .principal(() -> "cook@example.com")
                        .requestAttr("authenticatedUserId", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.recipeId").value(7))
                .andExpect(jsonPath("$.authorName").value("Cook"))
                .andExpect(jsonPath("$.body").value("Loved this one."));
    }

    @Test
    void likeRecipeCommentShouldReturnOk() throws Exception {
        when(recipeService.setRecipeCommentLike(eq(15L), any(), eq(true))).thenReturn(
                RecipeCommentDto.builder()
                        .id(15L)
                        .recipeId(7L)
                        .likeCount(3)
                        .likedByMe(true)
                        .build()
        );

        mockMvc.perform(post("/api/recipes/db/comments/15/like")
                        .principal(() -> "cook@example.com")
                        .requestAttr("authenticatedUserId", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.likeCount").value(3))
                .andExpect(jsonPath("$.likedByMe").value(true));
    }
}
