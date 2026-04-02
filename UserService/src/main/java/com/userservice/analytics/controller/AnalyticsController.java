package com.userservice.analytics.controller;

import com.userservice.analytics.controller.api.AnalyticsControllerApi;
import com.userservice.analytics.dto.DailyAnalyticsResponse;
import com.userservice.analytics.dto.MacroSummaryResponse;
import com.userservice.analytics.dto.WeeklyAnalyticsResponse;
import com.userservice.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController implements AnalyticsControllerApi {

    private final AnalyticsService analyticsService;

    @Override
    @GetMapping("/daily")
    public ResponseEntity<DailyAnalyticsResponse> getDailySummary(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(analyticsService.getDailySummary(date));
    }

    @Override
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyAnalyticsResponse> getWeeklySummary(
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(analyticsService.getWeeklySummary(dateFrom, dateTo));
    }

    @Override
    @GetMapping("/macros")
    public ResponseEntity<MacroSummaryResponse> getMacroSummary(
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(analyticsService.getMacroSummary(dateFrom, dateTo));
    }
}
