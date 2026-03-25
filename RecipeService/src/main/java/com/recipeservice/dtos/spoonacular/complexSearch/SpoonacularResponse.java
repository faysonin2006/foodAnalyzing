package com.recipeservice.dtos.spoonacular.complexSearch;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class SpoonacularResponse {

    private List<RecipeResult> results;
    private Integer offset;
    private Integer number;
    private Integer totalResults;

    @Data
    @NoArgsConstructor
    public static class RecipeResult {
        private Long id;
        private String title;
        private String image;
        private String imageType;
        private Nutrition nutrition;
        private Integer readyInMinutes;
        private Integer maxReadyTime;
    }

    @Data
    @NoArgsConstructor
    public static class Nutrition {
        private List<Nutrient> nutrients;
    }

    @Data
    @NoArgsConstructor
    public static class Nutrient {
        private String name;
        private Double amount;
        private String unit;
    }
}
