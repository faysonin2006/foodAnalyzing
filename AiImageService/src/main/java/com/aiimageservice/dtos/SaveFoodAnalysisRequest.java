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

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
