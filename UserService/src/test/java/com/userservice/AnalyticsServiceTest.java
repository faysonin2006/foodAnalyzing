package com.userservice;

import com.userservice.analytics.dto.DailyAnalyticsResponse;
import com.userservice.analytics.dto.MacroSummaryResponse;
import com.userservice.analytics.dto.WeeklyAnalyticsResponse;
import com.userservice.analytics.service.AnalyticsService;
import com.userservice.meals.repository.MealEntryRepository;
import com.userservice.pantry.model.PantryItem;
import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.repository.PantryItemRepository;
import com.userservice.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private MealEntryRepository mealEntryRepository;
    @Mock
    private PantryItemRepository pantryItemRepository;
    @Mock
    private UserProfileRepository userProfileRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(mealEntryRepository, pantryItemRepository, userProfileRepository);
        ReflectionTestUtils.setField(analyticsService, "expiringSoonThresholdDays", 3);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestDataFactory.EMAIL, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getDailySummaryShouldAggregateMeals() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(mealEntryRepository.findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(any(), any(), any()))
                .thenReturn(List.of(TestDataFactory.mealEntry()));
        when(pantryItemRepository.findAllByUserIdAndStatusAndExpiresAtBetween(any(), any(), any(), any()))
                .thenReturn(List.of(TestDataFactory.pantryItem()));

        DailyAnalyticsResponse response = analyticsService.getDailySummary(LocalDate.of(2026, 3, 28));

        assertEquals(2500, response.getTargetCalories());
        assertEquals(420, response.getTotalCalories());
        assertEquals(30.0, response.getProteins());
        assertEquals(18.0, response.getFats());
        assertEquals(24.0, response.getCarbohydrates());
        assertEquals(1, response.getMealsCount());
        assertEquals(1, response.getExpiringSoonCount());
    }

    @Test
    void getWeeklySummaryShouldBuildDailyPoints() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(mealEntryRepository.findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(any(), any(), any()))
                .thenReturn(List.of(TestDataFactory.mealEntry()));
        PantryItem consumed = TestDataFactory.pantryItem();
        consumed.setStatus(PantryItemStatus.CONSUMED);
        when(pantryItemRepository.findAllByUserIdAndStatus(any(), any())).thenReturn(List.of(consumed));

        WeeklyAnalyticsResponse response = analyticsService.getWeeklySummary(LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 28));

        assertEquals(420, response.getTotalCalories());
        assertEquals(1, response.getUsedPantryItemsCount());
        assertEquals(7, response.getDailyCalories().size());
    }

    @Test
    void getMacroSummaryShouldAggregateMacros() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(mealEntryRepository.findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(any(), any(), any()))
                .thenReturn(List.of(TestDataFactory.mealEntry()));

        MacroSummaryResponse response = analyticsService.getMacroSummary(LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 28));

        assertEquals(30.0, response.getProteins());
        assertEquals(18.0, response.getFats());
        assertEquals(24.0, response.getCarbohydrates());
    }
}
