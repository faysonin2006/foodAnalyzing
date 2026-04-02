package com.recipeservice.dtos.internal.recipesdb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientDto {
    private String quantityText;
    private Double quantityValue;
    private String unit;
    private String ingredient;
    private String note;
    private String rawText;
}
