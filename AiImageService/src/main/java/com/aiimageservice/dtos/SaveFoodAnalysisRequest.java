package com.aiimageservice.dtos;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveFoodAnalysisRequest {

    @Size(max = 160, message = "Meal title must not exceed 160 characters")
    private String title;

    @PositiveOrZero(message = "Calories must be zero or positive")
    private Integer calories;

    @PositiveOrZero(message = "Proteins must be zero or positive")
    private Double proteins;

    @PositiveOrZero(message = "Fats must be zero or positive")
    private Double fats;

    @PositiveOrZero(message = "Carbohydrates must be zero or positive")
    private Double carbohydrates;

    private LocalDateTime eatenAt;

    @Size(max = 80, message = "Amount eaten must not exceed 80 characters")
    private String amountEaten;

    @Size(max = 32, message = "Amount mode must not exceed 32 characters")
    private String amountMode;

    @PositiveOrZero(message = "Eaten ratio must be zero or positive")
    private Double eatenRatio;

    @PositiveOrZero(message = "Total weight grams must be zero or positive")
    private Double totalWeightGrams;

    @PositiveOrZero(message = "Eaten weight grams must be zero or positive")
    private Double eatenWeightGrams;

    @PositiveOrZero(message = "Package fraction numerator must be zero or positive")
    private Integer packageFractionNumerator;

    @PositiveOrZero(message = "Package fraction denominator must be zero or positive")
    private Integer packageFractionDenominator;

    @PositiveOrZero(message = "Full portion calories must be zero or positive")
    private Integer fullPortionCalories;

    @PositiveOrZero(message = "Full portion proteins must be zero or positive")
    private Double fullPortionProteins;

    @PositiveOrZero(message = "Full portion fats must be zero or positive")
    private Double fullPortionFats;

    @PositiveOrZero(message = "Full portion carbohydrates must be zero or positive")
    private Double fullPortionCarbohydrates;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
