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
public class MacroSummaryResponse {
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private double proteins;
    private double fats;
    private double carbohydrates;
    private int totalCalories;
}
