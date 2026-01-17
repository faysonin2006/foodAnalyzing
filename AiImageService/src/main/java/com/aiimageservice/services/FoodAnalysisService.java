package com.aiimageservice.services;

import com.aiimageservice.dtos.*;
import com.aiimageservice.httpinterfaceconfig.httpuserserviceclient.HttpUserServiceClient;
import com.aiimageservice.models.FoodAnalysis;
import com.aiimageservice.models.enums.AnalysisStatus;
import com.aiimageservice.repositories.FoodAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodAnalysisService {

    private final FoodAnalysisRepository repository;
    private final S3Service s3Service;
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.template.exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.template.routing-key}")
    private String routingKey;

    @Transactional
    public FoodAnalysisResponse uploadAndAnalyze(MultipartFile file, String userId) {
        String imageUrl = "";
        try {
            imageUrl = s3Service.uploadImage(file, userId);
        }
        catch (Exception e) {
            System.out.println("upload image exception");
        }

        FoodAnalysis analysis = FoodAnalysis.builder()
                .userId(userId)
                .imageUrl(imageUrl)
                .status(AnalysisStatus.PROCESSING)
                .build();
        repository.save(analysis);

        FoodAnalysisRequest request = new FoodAnalysisRequest(analysis.getId(), imageUrl, userId);
        rabbitTemplate.convertAndSend(exchange, routingKey, request);

        return new FoodAnalysisResponse(analysis.getId(), AnalysisStatus.PROCESSING);
    }

    public FoodAnalysisDetailResponse getAnalysisById(UUID id, String userId) {
        FoodAnalysis analysis = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Analysis not found with id: " + id));

        System.out.println(analysis.getUserId());
        System.out.println(userId);
        if (!analysis.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: You cannot view this analysis");
        }

        return mapToDetailResponse(analysis);
    }

    public List<FoodAnalysisResponse> getUserHistory(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(a -> new FoodAnalysisResponse(a.getId(), a.getStatus()))
                .collect(Collectors.toList());
    }

    private FoodAnalysisDetailResponse mapToDetailResponse(FoodAnalysis analysis) {
        return FoodAnalysisDetailResponse.builder()
                .id(analysis.getId())
                .imageUrl(analysis.getImageUrl())
                .status(analysis.getStatus())
                .dishName(analysis.getDishName())
                .calories(analysis.getCalories())
                .protein(analysis.getProtein())
                .carbs(analysis.getCarbs())
                .fats(analysis.getFats())
                .errorMessage(analysis.getErrorMessage())
                .createdAt(analysis.getCreatedAt())
                .extraInfo(analysis.getExtraInfo())
                .build();
    }
}
