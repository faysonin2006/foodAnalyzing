package com.userservice.meals.dto;

import com.userservice.meals.model.enums.MealSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealListItemResponse {

    private UUID id;
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
    private String imageUrl;
}
