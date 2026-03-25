package com.recipeservice.dtos.spoonacular.complexSearch;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpoonacularRequest {

    private String query;
    private String cuisine;
    private String excludeCuisine;
    private String diet;
    private String intolerances;
    private String includeIngredients;
    private String excludeIngredients;
    private String type;

    @Builder.Default
    private Boolean instructionsRequired = true;
    @Builder.Default
    private Boolean addRecipeInformation = true;
    @Builder.Default
    private Boolean addRecipeNutrition = true;

    private Integer minCarbs;
    private Integer maxCarbs;
    private Integer minProtein;
    private Integer maxProtein;
    private Integer minCalories;
    private Integer maxCalories;
    private Integer minFat;
    private Integer maxFat;

    @Builder.Default
    private Integer number = 1;
    @Builder.Default
    private Integer offset = 0;
}
