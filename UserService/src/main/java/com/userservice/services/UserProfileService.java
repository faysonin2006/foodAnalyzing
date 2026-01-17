package com.userservice.services;

import com.userservice.dtos.CreateProfileRequest;
import com.userservice.dtos.UserProfileResponse;
import com.userservice.dtos.UserProfileUpdateRequest;
import com.userservice.models.AllergyModel;
import com.userservice.models.DietPreferenceModel;
import com.userservice.models.HealthConditionModel;
import com.userservice.models.UserProfile;
import com.userservice.models.enums.ActivityLevel;
import com.userservice.models.enums.DietPreference;
import com.userservice.models.enums.Gender;
import com.userservice.models.enums.GoalType;
import com.userservice.repositories.UserAllergyRepository;
import com.userservice.repositories.UserDietRepository;
import com.userservice.repositories.UserHealthConditionRepository;
import com.userservice.repositories.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository profileRepository;
    private final UserAllergyRepository allergyRepository;
    private final UserDietRepository dietRepository;
    private final UserHealthConditionRepository healthConditionRepository;
    @Transactional
    public void createProfile(CreateProfileRequest request) {
        if (profileRepository.existsById(request.getUserId())) {
            return;
        }

        var profile = UserProfile.builder()
                .id(request.getUserId())
                .email(request.getEmail())
                .build();
        profileRepository.save(profile);
    }

    public UserProfileResponse getProfileByEmail(String email) {
        var profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return mapToResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request) {
        var profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        if (request.getHeight() != null) profile.setHeight(request.getHeight());
        if (request.getWeight() != null) profile.setWeight(request.getWeight());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getActivityLevel() != null) profile.setActivityLevel(request.getActivityLevel());
        if (request.getGoalType() != null) profile.setGoalType(request.getGoalType());

        if (request.getAllergies() != null) {
            List<AllergyModel> allergies = allergyRepository.findAllById(request.getAllergies());
            profile.setAllergies(allergies);
        }

        if (request.getDietPreferences() != null) {
            List<DietPreferenceModel> diets = dietRepository.findAllById(request.getDietPreferences());
            profile.setDietPreferences(diets);
        }

        if (request.getHealthConditions() != null) {
            List<HealthConditionModel> conditions = healthConditionRepository.findAllById(request.getHealthConditions());
            profile.setHealthConditions(conditions);
        }
        calculateAndSetCalories(profile);

        profileRepository.save(profile);

        return mapToResponse(profile);

    }

    private void calculateAndSetCalories(UserProfile profile) {
        if (profile.getWeight() == null || profile.getHeight() == null ||
                profile.getDateOfBirth() == null || profile.getGender() == null ||
                profile.getActivityLevel() == null || profile.getGoalType() == null) {
            return;
        }

        int age = Period.between(
                profile.getDateOfBirth(),
                LocalDate.now()
        ).getYears();

        double bmr;
        if (profile.getGender().equals(Gender.MALE)) {
            bmr = (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * age) + 5;
        } else {
            bmr = (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * age) - 161;
        }

        int targetCalories = getTargetCalories(profile, bmr);

        profile.setTargetCaloriesPerDay(targetCalories);
    }

    private static int getTargetCalories(UserProfile profile, double bmr) {
        double activityMultiplier = switch (profile.getActivityLevel()) {
            case SEDENTARY -> ActivityLevel.SEDENTARY.getMultiplier();
            case LIGHTLY_ACTIVE -> ActivityLevel.LIGHTLY_ACTIVE.getMultiplier();
            case MODERATELY_ACTIVE -> ActivityLevel.MODERATELY_ACTIVE.getMultiplier();
            case VERY_ACTIVE -> ActivityLevel.VERY_ACTIVE.getMultiplier();
            case EXTRA_ACTIVE -> ActivityLevel.EXTRA_ACTIVE.getMultiplier();
        };

        double goalAdjustment = switch (profile.getGoalType()) {
            case LOSE_WEIGHT -> GoalType.LOSE_WEIGHT.getAdjustmentFactor();
            case GAIN_MUSCLE -> GoalType.GAIN_MUSCLE.getAdjustmentFactor();
            case MAINTAIN_WEIGHT -> GoalType.MAINTAIN_WEIGHT.getAdjustmentFactor();
        };

        int targetCalories = (int) ((bmr * activityMultiplier) * (1 + goalAdjustment));

        if (targetCalories < 1200) {
            targetCalories = 1200;
        }
        return targetCalories;
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .activityLevel(profile.getActivityLevel())
                .allergies(profile.getAllergies())
                .gender(profile.getGender())
                .dateOfBirth(profile.getDateOfBirth())
                .dietPreferences(profile.getDietPreferences())
                .healthConditions(profile.getHealthConditions())
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .targetCalories(profile.getTargetCaloriesPerDay())
                .goalType(profile.getGoalType())
                .build();
    }
}
