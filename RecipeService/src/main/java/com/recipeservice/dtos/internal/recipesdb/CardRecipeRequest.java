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
public class CardRecipeRequest {
    private String lang;
    private String title;
    private String category;
    private List<String> requiredDietKeys;
    private List<String> preferredHealthKeys;
    private List<String> allergyKeys;
    private List<String> healthConditionKeys;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDir;
}
