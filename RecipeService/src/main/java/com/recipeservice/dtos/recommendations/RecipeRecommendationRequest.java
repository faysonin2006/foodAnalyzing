package com.recipeservice.dtos.recommendations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeRecommendationRequest {

    @Min(1)
    @Max(50)
    private Integer size;

    private String sortBy;

    private String lang;

    private Integer maxCalories;

    private List<String> excludedIngredients;
}
