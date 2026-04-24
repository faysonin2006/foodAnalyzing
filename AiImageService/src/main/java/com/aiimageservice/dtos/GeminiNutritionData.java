package com.aiimageservice.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiNutritionData {

    @JsonProperty("dish_name")
    private String dish_name;

    @JsonProperty("calories")
    private Integer calories;

    @JsonProperty("protein")
    private Double protein;

    @JsonProperty("carbs")
    private Double carbs;

    @JsonProperty("fats")
    private Double fats;

    @JsonProperty("is_food")
    private Boolean foodDetected;

    @JsonProperty("health_score")
    private Integer healthScore;

    @JsonProperty("estimated_weight_grams")
    private Integer estimatedWeightGrams;

    @JsonProperty("extra_info")
    private String extraInfo;

    @JsonProperty("extra_questions")
    private String questions;
}
