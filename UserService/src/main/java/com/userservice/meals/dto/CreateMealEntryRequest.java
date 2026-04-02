package com.userservice.meals.dto;

import com.userservice.meals.model.enums.MealSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateMealEntryRequest {

    @NotBlank(message = "Meal title is required")
    @Size(max = 160, message = "Meal title must not exceed 160 characters")
    private String title;

    @NotNull(message = "Calories are required")
    @PositiveOrZero(message = "Calories must be zero or positive")
    private Integer calories;

    @PositiveOrZero(message = "Proteins must be zero or positive")
    private Double proteins;

    @PositiveOrZero(message = "Fats must be zero or positive")
    private Double fats;

    @PositiveOrZero(message = "Carbohydrates must be zero or positive")
    private Double carbohydrates;

    @NotNull(message = "Eaten at timestamp is required")
    private LocalDateTime eatenAt;

    private MealSource source;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;
}
