package com.recipeservice.dtos.recommendations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedRecipeItem {

    private Long recipeId;
    private String title;
    private String image;
    private String category;
    private Integer estimatedCalories;
    private double matchScore;
    private int matchingIngredientsCount;
    private int missingIngredientsCount;
    private List<String> matchingIngredients;
    private List<String> missingIngredients;
}
