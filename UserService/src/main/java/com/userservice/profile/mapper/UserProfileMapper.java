package com.userservice.profile.mapper;

import com.userservice.profile.dto.UserProfileResponse;
import com.userservice.profile.dto.UserProfileUpdateRequest;
import com.userservice.profile.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserProfileMapper {

    @Mapping(target = "targetCalories", source = "targetCaloriesPerDay")
    UserProfileResponse toResponse(UserProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "targetCaloriesPerDay", ignore = true)
    @Mapping(target = "dietPreferences", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "healthConditions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProfileFromRequest(UserProfileUpdateRequest request, @MappingTarget UserProfile profile);
}
