package com.userservice.dtos;

import com.userservice.models.enums.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    private Gender gender;

    @Past(message = "Date of birth should be in the past")
    private LocalDate dateOfBirth;
    private ActivityLevel activityLevel;
    private GoalType goalType;
    private List<Allergy> allergies;
    private List<DietPreference> dietPreferences;
    private List<HealthCondition> healthConditions;

    @Min(value = 30, message = "Height must be valid")
    @Max(value = 300)
    private Integer height;

    @Min(value = 20, message = "Weight must be valid")
    @Max(value = 500)
    private Double weight;
}
