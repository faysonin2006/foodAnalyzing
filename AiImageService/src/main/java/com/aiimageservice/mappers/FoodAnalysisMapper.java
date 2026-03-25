package com.aiimageservice.mappers;

import com.aiimageservice.dtos.FoodAnalysisDetailResponse;
import com.aiimageservice.dtos.FoodAnalysisResponse;
import com.aiimageservice.models.FoodAnalysis;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FoodAnalysisMapper {
    FoodAnalysisResponse toResponse(FoodAnalysis analysis);
    FoodAnalysisDetailResponse toDetailResponse(FoodAnalysis analysis);
}
