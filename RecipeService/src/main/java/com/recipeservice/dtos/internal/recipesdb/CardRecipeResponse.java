package com.recipeservice.dtos.internal.recipesdb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardRecipeResponse {
    private Long recipeId;
    private String title;
    private String image;
    private String category;
    private int ingredientsCount;
    private int instructionsCount;
    private List<NutritionDto> nutritions;
    private RecipeTimesDto times;
}
