package com.aiimageservice.services;

import com.aiimageservice.constants.AppMessages;
import com.aiimageservice.dtos.FoodAnalysisRequest;
import com.aiimageservice.dtos.FoodAnalysisDetailResponse;
import com.aiimageservice.dtos.FoodAnalysisResponse;
import com.aiimageservice.exceptions.AnalysisNotFoundException;
import com.aiimageservice.exceptions.BadRequestException;
import com.aiimageservice.exceptions.ForbiddenOperationException;
import com.aiimageservice.exceptions.StorageException;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodAnalysisService {

    private final FoodAnalysisRepository repository;
    private final S3Service s3Service;
    private final RabbitTemplate rabbitTemplate;
    private final FoodAnalysisMapper foodAnalysisMapper;

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
}
