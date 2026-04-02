package com.userservice;

import com.userservice.dtos.UserProfileUpdateRequest;
import com.userservice.models.AllergyModel;
import com.userservice.models.UserLikesModel;
import com.userservice.models.UserProfile;
import com.userservice.models.enums.ActivityLevel;
import com.userservice.models.enums.Allergy;
import com.userservice.models.enums.Gender;
import com.userservice.models.enums.GoalType;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class TestDataFactory {

    public static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final String EMAIL = "user@example.com";

    public static UserProfile profile() {
        return UserProfile.builder()
                .id(USER_ID)
                .email(EMAIL)
                .name("Alex")
                .gender(Gender.MALE)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .height(180)
                .weight(80.0)
                .activityLevel(ActivityLevel.MODERATELY_ACTIVE)
                .goalType(GoalType.MAINTAIN_WEIGHT)
                .targetCaloriesPerDay(2500)
                .build();
    }

    public static UserProfileUpdateRequest updateRequest() {
        return new UserProfileUpdateRequest(
                Gender.MALE,
                "Alex Updated",
                LocalDate.of(1999, 5, 5),
                ActivityLevel.VERY_ACTIVE,
                GoalType.GAIN_MUSCLE,
                List.of(Allergy.PEANUTS),
                null,
                null,
                182,
                82.0
        );
    }

    public static AllergyModel allergy() {
        return new AllergyModel(Allergy.PEANUTS, "Peanut");
    }

    public static UserLikesModel like() {
        return UserLikesModel.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .recipeId(42L)
                .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();
    }
}
