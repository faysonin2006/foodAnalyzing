package recipes.recipesfromdbservice.dtos;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import recipes.recipesfromdbservice.dtos.responseDtos.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRecipeResponse {
    private Long recipeId;
    private String title;
    private String image;
    private String category;
    private int ingredientsCount;
    private int instructionsCount;

//    private List<IngredientDto> ingredients;
//    private List<InstructionStepDto> instructionSteps;
    private List<NutritionDto> nutritions;
    private RecipeTimesDto times;

//    private List<String> blockDietKeys;
//    private List<String> blockAllergyKeys;
//    private List<String> blockHealthKeys;
//    private List<String> cautionHealthKeys;
//    private List<ConstraintDto> constraints;


}

