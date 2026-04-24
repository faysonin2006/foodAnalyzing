package com.aiimageservice.dtos.meals;

import com.aiimageservice.dtos.meals.enums.MealSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMealEntryInternalRequest {

    private String title;
    private Integer calories;
    private Double proteins;
    private Double fats;
    private Double carbohydrates;
    private LocalDateTime eatenAt;
    private MealSource source;
    private String amountEaten;
    private String amountMode;
    private Double eatenRatio;
    private Double totalWeightGrams;
    private Double eatenWeightGrams;
    private Integer packageFractionNumerator;
    private Integer packageFractionDenominator;
    private Integer fullPortionCalories;
    private Double fullPortionProteins;
    private Double fullPortionFats;
    private Double fullPortionCarbohydrates;
    private String notes;
    private String imageUrl;
}
