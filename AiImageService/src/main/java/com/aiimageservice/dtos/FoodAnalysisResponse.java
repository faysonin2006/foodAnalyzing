package com.aiimageservice.dtos;

import com.aiimageservice.models.enums.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalysisResponse {
    private UUID id;
    private String imageUrl;
    private AnalysisStatus status;
    private String dishName;
    private Integer calories;
    private Double protein;
    private Double carbs;
    private Double fats;
    private Boolean foodDetected;
    private Integer healthScore;
    private Integer estimatedWeightGrams;
    private String errorMessage;
    private LocalDateTime createdAt;
    private String extraInfo;
    private String analysisBasis;
    private UUID savedMealId;
    private LocalDateTime savedAt;
}
