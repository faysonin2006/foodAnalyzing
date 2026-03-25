package com.aiimageservice;

import com.aiimageservice.models.FoodAnalysis;
import com.aiimageservice.models.enums.AnalysisStatus;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class TestDataFactory {

    public static final UUID ANALYSIS_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final String USER_EMAIL = "user@example.com";

    public static FoodAnalysis analysis() {
        return FoodAnalysis.builder()
                .id(ANALYSIS_ID)
                .userId(USER_EMAIL)
                .imageUrl("https://example.com/food.jpg")
                .status(AnalysisStatus.PROCESSING)
                .dishName("Salad")
                .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();
    }
}
