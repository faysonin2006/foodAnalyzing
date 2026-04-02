package recipes.recipesfromdbservice.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRecipeRequest {
    private Languages lang;
    private String title;
    private String category;
    private List<String> includeIngredients;
    private List<String> excludeIngredients;

    private List<String> requiredDietKeys;
    private List<String> preferredHealthKeys;


    private List<String> allergyKeys;
    private List<String> healthConditionKeys;

    @Min(1)
    private Integer page;
    @Min(1)
    @Max(20)
    private Integer size;
    private String sortBy;
    private String sortDir;

    @Min(0)
    private Integer maxCalories;
    @Min(0)
    private Integer minProtein;
    @Min(0)
    private Integer maxFats;
    @Min(0)
    private Integer maxCarbohydrates;
}
