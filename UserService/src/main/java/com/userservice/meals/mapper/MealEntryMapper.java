package com.userservice.meals.mapper;

import com.userservice.meals.dto.CreateMealEntryRequest;
import com.userservice.meals.dto.MealEntryResponse;
import com.userservice.meals.dto.MealListItemResponse;
import com.userservice.meals.model.MealEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MealEntryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MealEntry toEntity(CreateMealEntryRequest request);

    MealEntryResponse toResponse(MealEntry mealEntry);

    MealListItemResponse toListItemResponse(MealEntry mealEntry);
}
