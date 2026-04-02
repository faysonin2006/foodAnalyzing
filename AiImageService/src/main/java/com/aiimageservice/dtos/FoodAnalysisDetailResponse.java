package com.aiimageservice.dtos;

import com.aiimageservice.models.enums.AnalysisStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FoodAnalysisDetailResponse {
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
    private String errorMessage;
    private LocalDateTime createdAt;
    private String extraInfo;
    private UUID savedMealId;
    private LocalDateTime savedAt;
}
