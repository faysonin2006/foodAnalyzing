package com.recipeservice.dtos.internal.recipesdb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeTimesDto {
    private Integer totalMinutes;
}
