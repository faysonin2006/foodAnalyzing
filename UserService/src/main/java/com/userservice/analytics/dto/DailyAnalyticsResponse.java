package com.userservice.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAnalyticsResponse {
    private LocalDate date;
    private Integer targetCalories;
    private Double targetProteins;
    private Double targetFats;
    private Double targetCarbohydrates;
    private int totalCalories;
    private double proteins;
    private double fats;
    private double carbohydrates;
    private double totalProteins;
    private double totalFats;
    private double totalCarbohydrates;
    private int mealsCount;
    private int expiringSoonCount;
}
