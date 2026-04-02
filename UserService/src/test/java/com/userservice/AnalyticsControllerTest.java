package com.userservice;

import com.userservice.analytics.controller.AnalyticsController;
import com.userservice.analytics.dto.DailyAnalyticsResponse;
import com.userservice.analytics.dto.MacroSummaryResponse;
import com.userservice.analytics.dto.WeeklyAnalyticsResponse;
import com.userservice.analytics.service.AnalyticsService;
import com.userservice.common.exceptions.GlobalExceptionHandler;
import com.userservice.common.security.JwtAuthenticationFilter;
import com.userservice.common.security.RestAccessDeniedHandler;
import com.userservice.common.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void getDailySummaryShouldReturnOk() throws Exception {
        when(analyticsService.getDailySummary(LocalDate.of(2026, 3, 28))).thenReturn(DailyAnalyticsResponse.builder()
                .date(LocalDate.of(2026, 3, 28))
                .targetCalories(2500)
                .totalCalories(420)
                .proteins(30.0)
                .fats(18.0)
                .carbohydrates(24.0)
                .mealsCount(1)
                .expiringSoonCount(1)
                .build());

        mockMvc.perform(get("/api/analytics/daily").param("date", "2026-03-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCalories").value(2500))
                .andExpect(jsonPath("$.totalCalories").value(420))
                .andExpect(jsonPath("$.proteins").value(30.0))
                .andExpect(jsonPath("$.mealsCount").value(1));
    }

    @Test
    void getWeeklySummaryShouldReturnOk() throws Exception {
        when(analyticsService.getWeeklySummary(LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 28)))
                .thenReturn(WeeklyAnalyticsResponse.builder()
                        .dateFrom(LocalDate.of(2026, 3, 22))
                        .dateTo(LocalDate.of(2026, 3, 28))
                        .totalCalories(1200)
                        .mealsCount(3)
                        .usedPantryItemsCount(2)
                        .dailyCalories(List.of())
                        .build());

        mockMvc.perform(get("/api/analytics/weekly")
                        .param("dateFrom", "2026-03-22")
                        .param("dateTo", "2026-03-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalories").value(1200))
                .andExpect(jsonPath("$.usedPantryItemsCount").value(2));
    }

    @Test
    void getMacroSummaryShouldReturnOk() throws Exception {
        when(analyticsService.getMacroSummary(LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 28)))
                .thenReturn(MacroSummaryResponse.builder()
                        .dateFrom(LocalDate.of(2026, 3, 22))
                        .dateTo(LocalDate.of(2026, 3, 28))
                        .proteins(80.0)
                        .fats(40.0)
                        .carbohydrates(120.0)
                        .totalCalories(1200)
                        .build());

        mockMvc.perform(get("/api/analytics/macros")
                        .param("dateFrom", "2026-03-22")
                        .param("dateTo", "2026-03-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proteins").value(80.0))
                .andExpect(jsonPath("$.totalCalories").value(1200));
    }
}
