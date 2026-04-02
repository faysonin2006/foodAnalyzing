package com.recipeservice.dtos.internal.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer targetCalories;
    private List<ReferenceItemResponse> dietPreferences;
    private List<ReferenceItemResponse> allergies;
    private List<ReferenceItemResponse> healthConditions;
}
