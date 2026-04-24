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

        MacroTargets macroTargets = calculateMacroTargets(profile);

        return DailyAnalyticsResponse.builder()
                .date(targetDate)
                .targetCalories(profile.getTargetCaloriesPerDay())
                .targetProteins(macroTargets.proteins())
                .targetFats(macroTargets.fats())
                .targetCarbohydrates(macroTargets.carbohydrates())
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

    private MacroTargets calculateMacroTargets(UserProfile profile) {
        Integer targetCalories = profile.getTargetCaloriesPerDay();
        Double weight = profile.getWeight();
        if (targetCalories == null || targetCalories <= 0) {
            return new MacroTargets(null, null, null);
        }
        if (weight == null || weight <= 0) {
            return fallbackMacroTargets(targetCalories, profile);
        }

        double preferredProteins = round1(weight * preferredProteinPerKg(profile));
        double minimumProteins = round1(weight * minimumProteinPerKg(profile));
        double preferredFats = round1(weight * preferredFatPerKg(profile));
        double minimumFats = round1(weight * minimumFatPerKg(profile));
        double targetMinimumCarbs = round1(weight * minimumCarbohydratesPerKg(profile));

        double proteins = preferredProteins;
        double fats = preferredFats;
        double availableCarbCalories = targetCalories - proteins * 4.0 - fats * 9.0;

        if (availableCarbCalories < targetMinimumCarbs * 4.0) {
            double adjustedFats = (targetCalories - proteins * 4.0 - targetMinimumCarbs * 4.0) / 9.0;
            fats = round1(Math.max(minimumFats, adjustedFats));
            availableCarbCalories = targetCalories - proteins * 4.0 - fats * 9.0;
        }

        if (availableCarbCalories < 0 && proteins > minimumProteins) {
            double adjustedProteins = (targetCalories - fats * 9.0) / 4.0;
            proteins = round1(Math.max(minimumProteins, adjustedProteins));
            availableCarbCalories = targetCalories - proteins * 4.0 - fats * 9.0;
        }

        double carbohydrates = round1(Math.max(0.0, availableCarbCalories / 4.0));
        return new MacroTargets(proteins, fats, carbohydrates);
    }

    private MacroTargets fallbackMacroTargets(int targetCalories, UserProfile profile) {
        double proteinRatio;
        double fatRatio;
        double carbRatio;

        if (profile.getGoalType() == null) {
            proteinRatio = 0.25;
            fatRatio = 0.30;
            carbRatio = 0.45;
        } else {
            switch (profile.getGoalType()) {
                case LOSE_WEIGHT -> {
                    proteinRatio = 0.30;
                    fatRatio = 0.28;
                    carbRatio = 0.42;
                }
                case GAIN_MUSCLE -> {
                    proteinRatio = 0.28;
                    fatRatio = 0.25;
                    carbRatio = 0.47;
                }
                case MAINTAIN_WEIGHT -> {
                    proteinRatio = 0.25;
                    fatRatio = 0.30;
                    carbRatio = 0.45;
                }
                default -> {
                    proteinRatio = 0.25;
                    fatRatio = 0.30;
                    carbRatio = 0.45;
                }
            }
        }

        return new MacroTargets(
                round1((targetCalories * proteinRatio) / 4.0),
                round1((targetCalories * fatRatio) / 9.0),
                round1((targetCalories * carbRatio) / 4.0)
        );
    }

    private double preferredProteinPerKg(UserProfile profile) {
        double goalBase;
        if (profile.getGoalType() == null) {
            goalBase = 1.6;
        } else {
            switch (profile.getGoalType()) {
                case LOSE_WEIGHT -> goalBase = 1.9;
                case GAIN_MUSCLE -> goalBase = 1.8;
                case MAINTAIN_WEIGHT -> goalBase = 1.6;
                default -> goalBase = 1.6;
            }
        }
        return goalBase + activityProteinBonus(profile);
    }

    private double minimumProteinPerKg(UserProfile profile) {
        double goalBase;
        if (profile.getGoalType() == null) {
            goalBase = 1.3;
        } else {
            switch (profile.getGoalType()) {
                case LOSE_WEIGHT -> goalBase = 1.6;
                case GAIN_MUSCLE -> goalBase = 1.6;
                case MAINTAIN_WEIGHT -> goalBase = 1.3;
                default -> goalBase = 1.3;
            }
        }
        return goalBase + activityProteinBonus(profile) * 0.5;
    }

    private double preferredFatPerKg(UserProfile profile) {
        if (profile.getGoalType() == null) {
            return 0.85;
        }
        return switch (profile.getGoalType()) {
            case LOSE_WEIGHT -> 0.8;
            case GAIN_MUSCLE -> 0.8;
            case MAINTAIN_WEIGHT -> 0.9;
        };
    }

    private double minimumFatPerKg(UserProfile profile) {
        if (profile.getGoalType() == null) {
            return 0.65;
        }
        return switch (profile.getGoalType()) {
            case LOSE_WEIGHT -> 0.6;
            case GAIN_MUSCLE -> 0.6;
            case MAINTAIN_WEIGHT -> 0.7;
        };
    }

    private double minimumCarbohydratesPerKg(UserProfile profile) {
        if (profile.getActivityLevel() == null) {
            return 1.5;
        }
        double base = switch (profile.getActivityLevel()) {
            case SEDENTARY -> 1.5;
            case LIGHTLY_ACTIVE -> 2.0;
            case MODERATELY_ACTIVE -> 2.5;
            case VERY_ACTIVE -> 3.0;
            case EXTRA_ACTIVE -> 3.5;
        };

        if (profile.getGoalType() == null) {
            return base;
        }
        return switch (profile.getGoalType()) {
            case LOSE_WEIGHT -> Math.max(1.2, base - 0.5);
            case GAIN_MUSCLE -> base + 0.5;
            case MAINTAIN_WEIGHT -> base;
        };
    }

    private double activityProteinBonus(UserProfile profile) {
        if (profile.getActivityLevel() == null) {
            return 0.0;
        }
        return switch (profile.getActivityLevel()) {
            case SEDENTARY -> 0.0;
            case LIGHTLY_ACTIVE -> 0.1;
            case MODERATELY_ACTIVE -> 0.2;
            case VERY_ACTIVE -> 0.3;
            case EXTRA_ACTIVE -> 0.35;
        };
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record MacroTargets(Double proteins, Double fats, Double carbohydrates) {
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
