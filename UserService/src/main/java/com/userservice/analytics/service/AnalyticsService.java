package com.userservice.analytics.service;

import com.userservice.analytics.dto.DailyAnalyticsResponse;
import com.userservice.analytics.dto.MacroSummaryResponse;
import com.userservice.analytics.dto.WeeklyAnalyticsResponse;
import com.userservice.common.constants.AppMessages;
import com.userservice.common.exceptions.ProfileNotFoundException;
import com.userservice.common.security.SecurityUtils;
import com.userservice.meals.model.MealEntry;
import com.userservice.meals.repository.MealEntryRepository;
import com.userservice.pantry.model.PantryItem;
import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.repository.PantryItemRepository;
import com.userservice.profile.model.UserProfile;
import com.userservice.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MealEntryRepository mealEntryRepository;
    private final PantryItemRepository pantryItemRepository;
    private final UserProfileRepository userProfileRepository;

    @Value("${pantry.expiring-soon-threshold-days:3}")
    private int expiringSoonThresholdDays;

    @Transactional(readOnly = true)
    public DailyAnalyticsResponse getDailySummary(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        UserProfile profile = resolveCurrentUserProfile();
        UUID userId = profile.getId();
        List<MealEntry> meals = mealEntryRepository.findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
                userId,
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay().minusNanos(1)
        );

        double proteins = meals.stream()
                .map(MealEntry::getProteins)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        double fats = meals.stream()
                .map(MealEntry::getFats)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        double carbohydrates = meals.stream()
                .map(MealEntry::getCarbohydrates)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        return DailyAnalyticsResponse.builder()
                .date(targetDate)
                .targetCalories(profile.getTargetCaloriesPerDay())
                .totalCalories(meals.stream().map(MealEntry::getCalories).filter(v -> v != null).mapToInt(Integer::intValue).sum())
                .proteins(proteins)
                .fats(fats)
                .carbohydrates(carbohydrates)
                .totalProteins(proteins)
                .totalFats(fats)
                .totalCarbohydrates(carbohydrates)
                .mealsCount(meals.size())
                .expiringSoonCount(getExpiringSoonCount(userId, targetDate))
                .build();
    }

    @Transactional(readOnly = true)
    public WeeklyAnalyticsResponse getWeeklySummary(LocalDate dateFrom, LocalDate dateTo) {
        LocalDate from = dateFrom == null ? LocalDate.now().minusDays(6) : dateFrom;
        LocalDate to = dateTo == null ? LocalDate.now() : dateTo;
        UUID userId = resolveCurrentUserId();

        List<MealEntry> meals = mealEntryRepository.findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
                userId,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay().minusNanos(1)
        );

        List<WeeklyAnalyticsResponse.DailyAnalyticsPoint> points = from.datesUntil(to.plusDays(1))
                .map(day -> WeeklyAnalyticsResponse.DailyAnalyticsPoint.builder()
                        .date(day)
                        .calories(meals.stream()
                                .filter(meal -> meal.getEatenAt() != null && meal.getEatenAt().toLocalDate().equals(day))
                                .map(MealEntry::getCalories)
                                .filter(v -> v != null)
                                .mapToInt(Integer::intValue)
                                .sum())
                        .build())
                .toList();

        int usedPantryItemsCount = pantryItemRepository.findAllByUserIdAndStatus(userId, PantryItemStatus.CONSUMED).size();

        return WeeklyAnalyticsResponse.builder()
                .dateFrom(from)
                .dateTo(to)
                .totalCalories(meals.stream().map(MealEntry::getCalories).filter(v -> v != null).mapToInt(Integer::intValue).sum())
                .mealsCount(meals.size())
                .usedPantryItemsCount(usedPantryItemsCount)
                .dailyCalories(points)
                .build();
    }

    @Transactional(readOnly = true)
    public MacroSummaryResponse getMacroSummary(LocalDate dateFrom, LocalDate dateTo) {
        LocalDate from = dateFrom == null ? LocalDate.now().minusDays(6) : dateFrom;
        LocalDate to = dateTo == null ? LocalDate.now() : dateTo;
        UUID userId = resolveCurrentUserId();
        List<MealEntry> meals = mealEntryRepository.findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
                userId,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay().minusNanos(1)
        );

        return MacroSummaryResponse.builder()
                .dateFrom(from)
                .dateTo(to)
                .proteins(meals.stream().map(MealEntry::getProteins).filter(v -> v != null).mapToDouble(Double::doubleValue).sum())
                .fats(meals.stream().map(MealEntry::getFats).filter(v -> v != null).mapToDouble(Double::doubleValue).sum())
                .carbohydrates(meals.stream().map(MealEntry::getCarbohydrates).filter(v -> v != null).mapToDouble(Double::doubleValue).sum())
                .totalCalories(meals.stream().map(MealEntry::getCalories).filter(v -> v != null).mapToInt(Integer::intValue).sum())
                .build();
    }

    private int getExpiringSoonCount(UUID userId, LocalDate currentDate) {
        LocalDate thresholdDate = currentDate.plusDays(Math.max(expiringSoonThresholdDays, 0));
        List<PantryItem> items = pantryItemRepository.findAllByUserIdAndStatusAndExpiresAtBetween(
                userId,
                PantryItemStatus.ACTIVE,
                currentDate,
                thresholdDate
        );
        return items.size();
    }

    private UUID resolveCurrentUserId() {
        return resolveCurrentUserProfile().getId();
    }

    private UserProfile resolveCurrentUserProfile() {
        String email = SecurityUtils.getCurrentUsername();
        return userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ProfileNotFoundException(AppMessages.PROFILE_NOT_FOUND))
                ;
    }
}
