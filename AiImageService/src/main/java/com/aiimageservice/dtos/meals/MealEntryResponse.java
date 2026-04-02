package com.aiimageservice.dtos.meals;

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
public class MealEntryResponse {

    private UUID id;
    private String title;
    private Integer calories;
    private Double proteins;
    private Double fats;
    private Double carbohydrates;
    private LocalDateTime eatenAt;
    private String notes;
    private String imageUrl;
    private LocalDateTime createdAt;
}
