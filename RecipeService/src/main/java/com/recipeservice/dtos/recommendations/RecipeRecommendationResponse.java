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
public class RecipeRecommendationResponse {

    private int pantryItemsCount;
    private List<RecommendedRecipeItem> recipes;
}
