package com.aiimageservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalysisRequest  implements Serializable {

    private UUID analysisId;
    private String imageUrl;
    private String userId;
}
