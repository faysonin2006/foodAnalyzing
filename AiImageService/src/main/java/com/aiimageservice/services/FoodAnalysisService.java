package com.aiimageservice.services;

import com.aiimageservice.constants.AppMessages;
import com.aiimageservice.dtos.FoodAnalysisRequest;
import com.aiimageservice.dtos.FoodAnalysisDetailResponse;
import com.aiimageservice.dtos.FoodAnalysisResponse;
import com.aiimageservice.dtos.SaveFoodAnalysisRequest;
import com.aiimageservice.dtos.SaveFoodAnalysisResponse;
import com.aiimageservice.dtos.meals.CreateMealEntryInternalRequest;
import com.aiimageservice.dtos.meals.MealEntryResponse;
import com.aiimageservice.dtos.meals.enums.MealSource;
import com.aiimageservice.exceptions.AnalysisNotFoundException;
import com.aiimageservice.exceptions.BadRequestException;
import com.aiimageservice.exceptions.ConflictException;
import com.aiimageservice.exceptions.ForbiddenOperationException;
import com.aiimageservice.exceptions.StorageException;
import com.aiimageservice.exceptions.UpstreamServiceException;
import com.aiimageservice.httpinterfaceconfig.httpuserserviceclient.HttpUserMealsClient;
import com.aiimageservice.mappers.FoodAnalysisMapper;
import com.aiimageservice.models.FoodAnalysis;
import com.aiimageservice.models.enums.AnalysisStatus;
import com.aiimageservice.repositories.FoodAnalysisRepository;
import com.aiimageservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodAnalysisService {

    private final FoodAnalysisRepository repository;
    private final S3Service s3Service;
    private final RabbitTemplate rabbitTemplate;
    private final FoodAnalysisMapper foodAnalysisMapper;
    private final HttpUserMealsClient httpUserMealsClient;

    @Value("${spring.rabbitmq.template.exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.template.routing-key}")
    private String routingKey;

    @Transactional
    public FoodAnalysisResponse uploadAndAnalyze(MultipartFile file, String questions) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(AppMessages.FILE_MUST_NOT_BE_EMPTY);
        }

        String userId = SecurityUtils.getCurrentUsername();
        String imageUrl;
        try {
            imageUrl = s3Service.uploadImage(file, userId);
        } catch (Exception ex) {
            throw new StorageException(AppMessages.FAILED_TO_UPLOAD_IMAGE, ex);
        }

        FoodAnalysis analysis = FoodAnalysis.builder()
                .userId(userId)
                .imageUrl(imageUrl)
                .status(AnalysisStatus.PROCESSING)
                .build();
        FoodAnalysis savedAnalysis = repository.save(analysis);

        FoodAnalysisRequest request = new FoodAnalysisRequest(savedAnalysis.getId(), imageUrl, userId, questions);
        rabbitTemplate.convertAndSend(exchange, routingKey, request);

        return foodAnalysisMapper.toResponse(savedAnalysis);
    }

    @Transactional(readOnly = true)
    public FoodAnalysisDetailResponse getAnalysisById(UUID id) {
        String userId = SecurityUtils.getCurrentUsername();
        FoodAnalysis analysis = repository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException(AppMessages.ANALYSIS_NOT_FOUND));

        if (!analysis.getUserId().equals(userId)) {
            throw new ForbiddenOperationException(AppMessages.ACCESS_DENIED);
        }

        return foodAnalysisMapper.toDetailResponse(analysis);
    }

    @Transactional(readOnly = true)
    public List<FoodAnalysisResponse> getUserHistory() {
        String userId = SecurityUtils.getCurrentUsername();
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(foodAnalysisMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteAnalysis(UUID id) {
        String userId = SecurityUtils.getCurrentUsername();
        FoodAnalysis analysis = repository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException(AppMessages.ANALYSIS_NOT_FOUND));

        if (!analysis.getUserId().equals(userId)) {
            throw new ForbiddenOperationException(AppMessages.ACCESS_DENIED);
        }

        repository.delete(analysis);
    }

    @Transactional
    public SaveFoodAnalysisResponse saveAnalysis(UUID id, SaveFoodAnalysisRequest request) {
        String userId = SecurityUtils.getCurrentUsername();
        FoodAnalysis analysis = repository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException(AppMessages.ANALYSIS_NOT_FOUND));

        if (!analysis.getUserId().equals(userId)) {
            throw new ForbiddenOperationException(AppMessages.ACCESS_DENIED);
        }
        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new BadRequestException(AppMessages.ANALYSIS_NOT_COMPLETED);
        }
        if (analysis.getSavedMealId() != null) {
            throw new ConflictException(AppMessages.ANALYSIS_ALREADY_SAVED);
        }

        CreateMealEntryInternalRequest mealRequest = buildMealRequest(analysis, request);
        MealEntryResponse mealEntryResponse;
        try {
            mealEntryResponse = httpUserMealsClient.createMealForUser(userId, mealRequest);
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_SAVE_MEAL, exception);
        }

        LocalDateTime savedAt = LocalDateTime.now();
        analysis.setSavedMealId(mealEntryResponse.getId());
        analysis.setSavedAt(savedAt);
        repository.save(analysis);

        return SaveFoodAnalysisResponse.builder()
                .analysisId(analysis.getId())
                .mealEntryId(mealEntryResponse.getId())
                .savedAt(savedAt)
                .build();
    }

    private CreateMealEntryInternalRequest buildMealRequest(FoodAnalysis analysis, SaveFoodAnalysisRequest request) {
        SaveFoodAnalysisRequest effectiveRequest = request == null ? new SaveFoodAnalysisRequest() : request;

        if (Boolean.FALSE.equals(analysis.getFoodDetected())) {
            throw new BadRequestException("Analysis result is not food");
        }

        String title = effectiveRequest.getTitle();
        if (title == null || title.isBlank()) {
            title = analysis.getDishName();
        }

        Integer calories = effectiveRequest.getCalories() != null ? effectiveRequest.getCalories() : analysis.getCalories();
        Double proteins = effectiveRequest.getProteins() != null ? effectiveRequest.getProteins() : analysis.getProtein();
        Double fats = effectiveRequest.getFats() != null ? effectiveRequest.getFats() : analysis.getFats();
        Double carbohydrates = effectiveRequest.getCarbohydrates() != null ? effectiveRequest.getCarbohydrates() : analysis.getCarbs();
        LocalDateTime eatenAt = effectiveRequest.getEatenAt() != null ? effectiveRequest.getEatenAt() : LocalDateTime.now();

        if (title == null || title.isBlank() || calories == null) {
            throw new BadRequestException(AppMessages.ANALYSIS_RESULT_IS_INCOMPLETE);
        }

        return CreateMealEntryInternalRequest.builder()
                .title(title)
                .calories(calories)
                .proteins(proteins)
                .fats(fats)
                .carbohydrates(carbohydrates)
                .eatenAt(eatenAt)
                .source(MealSource.AI_ANALYSIS)
                .notes(effectiveRequest.getNotes())
                .imageUrl(analysis.getImageUrl())
                .build();
    }
}
