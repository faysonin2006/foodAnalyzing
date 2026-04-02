package com.userservice.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyAnalyticsResponse {
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private int totalCalories;
    private int mealsCount;
    private int usedPantryItemsCount;
    private List<DailyAnalyticsPoint> dailyCalories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyAnalyticsPoint {
        private LocalDate date;
        private int calories;
    }
}
