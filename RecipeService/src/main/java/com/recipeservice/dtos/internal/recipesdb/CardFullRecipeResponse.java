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
public class CardFullRecipeResponse {
    private Long recipeId;
    private String title;
    private String image;
    private int ingredientsCount;
    private int instructionsCount;
    private List<IngredientDto> ingredients;
    private List<NutritionDto> nutritions;
    private List<String> blockDietKeys;
    private List<String> blockAllergyKeys;
    private List<String> blockHealthKeys;
    private List<String> cautionHealthKeys;
}
