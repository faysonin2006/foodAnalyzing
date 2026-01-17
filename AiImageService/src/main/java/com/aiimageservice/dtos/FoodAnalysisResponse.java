package com.aiimageservice.dtos;

import com.aiimageservice.models.enums.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalysisResponse {
    private UUID id;
    private AnalysisStatus status;
}
