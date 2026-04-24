package com.userservice.meals.service;

import com.userservice.common.constants.AppMessages;
import com.userservice.common.exceptions.MealEntryNotFoundException;
import com.userservice.common.exceptions.ProfileNotFoundException;
import com.userservice.common.security.SecurityUtils;
import com.userservice.meals.dto.CreateMealEntryRequest;
import com.userservice.meals.dto.MealEntryResponse;
import com.userservice.meals.dto.MealListItemResponse;
import com.userservice.meals.mapper.MealEntryMapper;
import com.userservice.meals.model.MealEntry;
import com.userservice.meals.model.enums.MealSource;
import com.userservice.meals.repository.MealEntryRepository;
import com.userservice.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MealEntryService {

    private final MealEntryRepository mealEntryRepository;
    private final MealEntryMapper mealEntryMapper;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public MealEntryResponse createMeal(CreateMealEntryRequest request) {
        return createMealForEmail(SecurityUtils.getCurrentUsername(), request, MealSource.MANUAL);
    }

    @Transactional
    public MealEntryResponse createMealForEmail(String email, CreateMealEntryRequest request) {
        MealSource defaultSource = request.getSource() == null ? MealSource.MANUAL : request.getSource();
        return createMealForEmail(email, request, defaultSource);
    }

    @Transactional(readOnly = true)
    public List<MealListItemResponse> getMeals(LocalDate dateFrom, LocalDate dateTo) {
        UUID userId = resolveCurrentUserId();
        List<MealEntry> mealEntries;

        if (dateFrom != null || dateTo != null) {
            LocalDate from = dateFrom == null ? LocalDate.of(1970, 1, 1) : dateFrom;
            LocalDate to = dateTo == null ? LocalDate.now() : dateTo;
            mealEntries = mealEntryRepository.findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
                    userId,
                    from.atStartOfDay(),
                    to.plusDays(1).atStartOfDay().minusNanos(1)
            );
        } else {
            mealEntries = mealEntryRepository.findAllByUserIdOrderByEatenAtDesc(userId);
        }

        return mealEntries.stream()
                .map(mealEntryMapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MealEntryResponse getMealById(UUID mealEntryId) {
        UUID userId = resolveCurrentUserId();
        MealEntry mealEntry = mealEntryRepository.findByIdAndUserId(mealEntryId, userId)
                .orElseThrow(() -> new MealEntryNotFoundException(AppMessages.MEAL_ENTRY_NOT_FOUND));
        return mealEntryMapper.toResponse(mealEntry);
    }

    @Transactional
    public MealEntryResponse updateMeal(UUID mealEntryId, CreateMealEntryRequest request) {
        UUID userId = resolveCurrentUserId();
        MealEntry mealEntry = mealEntryRepository.findByIdAndUserId(mealEntryId, userId)
                .orElseThrow(() -> new MealEntryNotFoundException(AppMessages.MEAL_ENTRY_NOT_FOUND));

        mealEntry.setTitle(request.getTitle());
        mealEntry.setCalories(request.getCalories());
        mealEntry.setProteins(request.getProteins());
        mealEntry.setFats(request.getFats());
        mealEntry.setCarbohydrates(request.getCarbohydrates());
        mealEntry.setEatenAt(request.getEatenAt());
        mealEntry.setAmountEaten(request.getAmountEaten());
        mealEntry.setAmountMode(request.getAmountMode());
        mealEntry.setEatenRatio(request.getEatenRatio());
        mealEntry.setTotalWeightGrams(request.getTotalWeightGrams());
        mealEntry.setEatenWeightGrams(request.getEatenWeightGrams());
        mealEntry.setPackageFractionNumerator(request.getPackageFractionNumerator());
        mealEntry.setPackageFractionDenominator(request.getPackageFractionDenominator());
        mealEntry.setFullPortionCalories(request.getFullPortionCalories());
        mealEntry.setFullPortionProteins(request.getFullPortionProteins());
        mealEntry.setFullPortionFats(request.getFullPortionFats());
        mealEntry.setFullPortionCarbohydrates(request.getFullPortionCarbohydrates());
        mealEntry.setNotes(request.getNotes());
        mealEntry.setImageUrl(request.getImageUrl());
        if (request.getSource() != null) {
            mealEntry.setSource(request.getSource());
        }

        MealEntry saved = mealEntryRepository.save(mealEntry);
        return mealEntryMapper.toResponse(saved);
    }

    @Transactional
    public void deleteMeal(UUID mealEntryId) {
        UUID userId = resolveCurrentUserId();
        MealEntry mealEntry = mealEntryRepository.findByIdAndUserId(mealEntryId, userId)
                .orElseThrow(() -> new MealEntryNotFoundException(AppMessages.MEAL_ENTRY_NOT_FOUND));
        mealEntryRepository.delete(mealEntry);
    }

    private UUID resolveCurrentUserId() {
        return resolveUserIdByEmail(SecurityUtils.getCurrentUsername());
    }

    private MealEntryResponse createMealForEmail(String email, CreateMealEntryRequest request, MealSource defaultSource) {
        MealEntry mealEntry = mealEntryMapper.toEntity(request);
        mealEntry.setUserId(resolveUserIdByEmail(email));
        mealEntry.setSource(request.getSource() == null ? defaultSource : request.getSource());
        MealEntry saved = mealEntryRepository.save(mealEntry);
        return mealEntryMapper.toResponse(saved);
    }

    private UUID resolveUserIdByEmail(String email) {
        return userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ProfileNotFoundException(AppMessages.PROFILE_NOT_FOUND))
                .getId();
    }
}
