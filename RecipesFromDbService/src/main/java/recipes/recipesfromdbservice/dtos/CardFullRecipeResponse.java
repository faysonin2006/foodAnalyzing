package recipes.recipesfromdbservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import recipes.recipesfromdbservice.dtos.responseDtos.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardFullRecipeResponse {

    private Long recipeId;
    private String title;
    private String image;
    private String category;
    private int ingredientsCount;
    private int instructionsCount;

    private List<IngredientDto> ingredients;
    private List<InstructionStepDto> instructionSteps;
    private List<NutritionDto> nutritions;
    private RecipeTimesDto times;

    private List<String> blockDietKeys;
    private List<String> blockAllergyKeys;
    private List<String> blockHealthKeys;
    private List<String> cautionHealthKeys;
    private List<ConstraintDto> constraints;
    private List<RecipeCommentDto> comments;
}
