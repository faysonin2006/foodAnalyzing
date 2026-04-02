package com.userservice.analytics.controller.api;

import com.userservice.analytics.dto.DailyAnalyticsResponse;
import com.userservice.analytics.dto.MacroSummaryResponse;
import com.userservice.analytics.dto.WeeklyAnalyticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "Analytics", description = "Nutrition analytics")
public interface AnalyticsControllerApi {

    @Operation(summary = "Get daily analytics", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<DailyAnalyticsResponse> getDailySummary(
            @Parameter(example = "2026-03-28") @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );

    @Operation(summary = "Get weekly analytics", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<WeeklyAnalyticsResponse> getWeeklySummary(
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    );

    @Operation(summary = "Get macro analytics", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<MacroSummaryResponse> getMacroSummary(
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    );
}
