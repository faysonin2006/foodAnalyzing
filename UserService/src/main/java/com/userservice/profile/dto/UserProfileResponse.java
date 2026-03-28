package com.userservice.profile.dto;

import com.userservice.profile.model.AllergyModel;
import com.userservice.profile.model.DietPreferenceModel;
import com.userservice.profile.model.HealthConditionModel;
import com.userservice.profile.model.enums.ActivityLevel;
import com.userservice.profile.model.enums.Gender;
import com.userservice.profile.model.enums.GoalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID id;
    private String name;
    private String email;

    private LocalDate dateOfBirth;
    private Gender gender;
    private Integer height;
    private Double weight;

    private ActivityLevel activityLevel;
    private GoalType goalType;

    private Integer targetCalories;

    private List<DietPreferenceModel> dietPreferences;
    private List<AllergyModel> allergies;
    private List<HealthConditionModel> healthConditions;

    private LocalDate createdAt;
}
