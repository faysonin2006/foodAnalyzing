package com.aiimageservice.services;

import com.aiimageservice.dtos.FoodAnalysisRequest;
import com.aiimageservice.dtos.GeminiNutritionData;
import com.aiimageservice.dtos.profiles.UserProfileResponse;
import com.aiimageservice.httpinterfaceconfig.httpuserserviceclient.HttpUserServiceClient;
import com.aiimageservice.models.enums.AnalysisStatus;
import com.aiimageservice.repositories.FoodAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodAnalysisConsumer {

    private final FoodAnalysisRepository repository;
    private final S3Service s3Service;
    private final GeminiService geminiService;
    private final HttpUserServiceClient userServiceClient;

    @RabbitListener(queues = "${spring.rabbitmq.template.default-receive-queue}")
    @Transactional
    public void consume(FoodAnalysisRequest request) {
        log.info("Processing ID: {}", request.getAnalysisId());
        try {
            byte[] img = s3Service.downloadImageByUrl(request.getImageUrl());

            UserProfileResponse userProfile = userServiceClient.getUserProfileById(request.getUserId());
            GeminiNutritionData data = geminiService.analyzeFood(img, userProfile, request.getQuestions());

            repository.findById(request.getAnalysisId()).ifPresent(a -> {
                a.setStatus(AnalysisStatus.COMPLETED);
                a.setDishName(data.getDish_name());
                a.setCalories(data.getCalories());
                a.setProtein(data.getProtein());
                a.setCarbs(data.getCarbs());
                a.setFats(data.getFats());
                a.setExtraInfo(data.getExtraInfo());
                repository.save(a);
            });
        } catch (Exception e) {
            log.error("Failed ID: {}", request.getAnalysisId(), e);
            repository.findById(request.getAnalysisId()).ifPresent(a -> {
                a.setStatus(AnalysisStatus.FAILED);
                a.setErrorMessage(e.getMessage());
                repository.save(a);
            });
        }
    }
}
