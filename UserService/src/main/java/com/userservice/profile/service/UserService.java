package com.userservice.profile.service;

import com.userservice.common.constants.AppMessages;
import com.userservice.profile.dto.CreateProfileRequest;
import com.userservice.profile.dto.UserProfileResponse;
import com.userservice.profile.dto.UserProfileUpdateRequest;
import com.userservice.profile.dto.likes.LikeActionResponse;
import com.userservice.profile.dto.likes.LikedRecipeResponse;
import com.userservice.common.exceptions.BadRequestException;
import com.userservice.common.exceptions.ProfileNotFoundException;
import com.userservice.profile.mapper.UserProfileMapper;
import com.userservice.profile.model.AllergyModel;
import com.userservice.profile.model.DietPreferenceModel;
import com.userservice.profile.model.HealthConditionModel;
import com.userservice.profile.model.UserLikesModel;
import com.userservice.profile.model.UserProfile;
import com.userservice.profile.model.enums.ActivityLevel;
import com.userservice.profile.model.enums.Gender;
import com.userservice.profile.model.enums.GoalType;
import com.userservice.profile.repository.UserAllergyRepository;
import com.userservice.profile.repository.UserDietRepository;
import com.userservice.profile.repository.UserHealthConditionRepository;
import com.userservice.profile.repository.UserLikesRepository;
import com.userservice.profile.repository.UserProfileRepository;
import com.userservice.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MIN_TARGET_CALORIES = 1200;

    private final UserProfileRepository profileRepository;
    private final UserAllergyRepository allergyRepository;
    private final UserDietRepository dietRepository;
    private final UserHealthConditionRepository healthConditionRepository;
    private final UserLikesRepository likesRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional
    public void createProfile(CreateProfileRequest request) {
        if (profileRepository.existsById(request.getUserId())) {
            return;
        }

        UserProfile profile = UserProfile.builder()
                .id(request.getUserId())
                .email(request.getEmail())
                .build();
        profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        return getProfileByEmail(SecurityUtils.getCurrentUsername());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByEmail(String email) {
        return userProfileMapper.toResponse(findProfileByEmail(email));
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(UserProfileUpdateRequest request) {
        return updateProfile(SecurityUtils.getCurrentUsername(), request);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request) {
        UserProfile profile = findProfileByEmail(email);
        userProfileMapper.updateProfileFromRequest(request, profile);
        applyReferenceCollections(profile, request);
        calculateAndSetCalories(profile);
        return userProfileMapper.toResponse(profileRepository.save(profile));
    }

    @Transactional
    public LikeActionResponse createLike(Long recipeId) {
        validateRecipeId(recipeId);
        UUID userId = resolveUserIdByEmail(SecurityUtils.getCurrentUsername());
        int inserted = likesRepository.insertIgnore(UUID.randomUUID(), userId, recipeId);
        UserLikesModel row = likesRepository.findByUserIdAndRecipeId(userId, recipeId)
                .orElseThrow(() -> new IllegalStateException(AppMessages.LIKE_ROW_MISSING));

        return LikeActionResponse.builder()
                .recipeId(recipeId)
                .liked(true)
                .changed(inserted > 0)
                .createdAt(row.getCreatedAt())
                .build();
    }

    @Transactional
    public LikeActionResponse removeLike(Long recipeId) {
        validateRecipeId(recipeId);
        UUID userId = resolveUserIdByEmail(SecurityUtils.getCurrentUsername());
        boolean changed = likesRepository.deleteByUserIdAndRecipeId(userId, recipeId) > 0;

        return LikeActionResponse.builder()
                .recipeId(recipeId)
                .liked(false)
                .changed(changed)
                .build();
    }

    @Transactional(readOnly = true)
    public List<LikedRecipeResponse> getAllLikes() {
        UUID userId = resolveUserIdByEmail(SecurityUtils.getCurrentUsername());
        return likesRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(like -> LikedRecipeResponse.builder()
                        .recipeId(like.getRecipeId())
                        .createdAt(like.getCreatedAt())
                        .build())
                .toList();
    }

    private void validateRecipeId(Long recipeId) {
        if (recipeId == null || recipeId <= 0) {
            throw new BadRequestException(AppMessages.INVALID_RECIPE_ID);
        }
    }

    private UserProfile findProfileByEmail(String email) {
        return profileRepository.findByEmail(email)
                .orElseThrow(() -> new ProfileNotFoundException(AppMessages.PROFILE_NOT_FOUND));
    }

    private UUID resolveUserIdByEmail(String email) {
        return findProfileByEmail(email).getId();
    }

    private void applyReferenceCollections(UserProfile profile, UserProfileUpdateRequest request) {
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
    }

    private void calculateAndSetCalories(UserProfile profile) {
        if (profile.getWeight() == null
                || profile.getHeight() == null
                || profile.getDateOfBirth() == null
                || profile.getGender() == null
                || profile.getActivityLevel() == null
                || profile.getGoalType() == null) {
            return;
        }

        int age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
        double bmr = profile.getGender() == Gender.MALE
                ? (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * age) + 5
                : (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * age) - 161;

        profile.setTargetCaloriesPerDay(getTargetCalories(profile, bmr));
    }

    private int getTargetCalories(UserProfile profile, double bmr) {
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
        return Math.max(targetCalories, MIN_TARGET_CALORIES);
    }
}
