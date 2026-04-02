package com.aiimageservice;

import com.aiimageservice.dtos.FoodAnalysisRequest;
import com.aiimageservice.dtos.FoodAnalysisResponse;
import com.aiimageservice.dtos.SaveFoodAnalysisRequest;
import com.aiimageservice.dtos.SaveFoodAnalysisResponse;
import com.aiimageservice.exceptions.BadRequestException;
import com.aiimageservice.exceptions.ConflictException;
import com.aiimageservice.exceptions.ForbiddenOperationException;
import com.aiimageservice.httpinterfaceconfig.httpuserserviceclient.HttpUserMealsClient;
import com.aiimageservice.mappers.FoodAnalysisMapper;
import com.aiimageservice.models.FoodAnalysis;
import com.aiimageservice.repositories.FoodAnalysisRepository;
import com.aiimageservice.services.FoodAnalysisService;
import com.aiimageservice.services.S3Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodAnalysisServiceTest {

    @Mock
    private FoodAnalysisRepository repository;
    @Mock
    private S3Service s3Service;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private HttpUserMealsClient httpUserMealsClient;

    private FoodAnalysisService service;

    @BeforeEach
    void setUp() {
        FoodAnalysisMapper mapper = Mappers.getMapper(FoodAnalysisMapper.class);
        service = new FoodAnalysisService(repository, s3Service, rabbitTemplate, mapper, httpUserMealsClient);
        ReflectionTestUtils.setField(service, "exchange", "food.analysis.exchange");
        ReflectionTestUtils.setField(service, "routingKey", "food.analysis.tracking");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestDataFactory.USER_EMAIL, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadAndAnalyzeShouldUploadPersistAndPublish() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "food.jpg", "image/jpeg", new byte[]{1, 2, 3});
        FoodAnalysis analysis = TestDataFactory.analysis();
        when(s3Service.uploadImage(file, TestDataFactory.USER_EMAIL)).thenReturn("https://example.com/food.jpg");
        when(repository.save(any(FoodAnalysis.class))).thenReturn(analysis);

        FoodAnalysisResponse response = service.uploadAndAnalyze(file, "question");

        assertEquals(TestDataFactory.ANALYSIS_ID, response.getId());
        assertEquals(analysis.getStatus(), response.getStatus());
        verify(rabbitTemplate).convertAndSend(
                eq("food.analysis.exchange"),
                eq("food.analysis.tracking"),
                any(FoodAnalysisRequest.class)
        );
    }

    @Test
    void uploadAndAnalyzeShouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[]{});

        assertThrows(BadRequestException.class, () -> service.uploadAndAnalyze(file, ""));
        verifyNoInteractions(repository);
    }

    @Test
    void getAnalysisByIdShouldRejectForeignAnalysis() {
        FoodAnalysis analysis = TestDataFactory.analysis();
        analysis.setUserId("another@example.com");
        when(repository.findById(TestDataFactory.ANALYSIS_ID)).thenReturn(Optional.of(analysis));

        assertThrows(ForbiddenOperationException.class, () -> service.getAnalysisById(TestDataFactory.ANALYSIS_ID));
    }

    @Test
    void saveAnalysisShouldCreateMealAndMarkAnalysisAsSaved() {
        FoodAnalysis analysis = TestDataFactory.completedAnalysis();
        when(repository.findById(TestDataFactory.ANALYSIS_ID)).thenReturn(Optional.of(analysis));
        when(httpUserMealsClient.createMealForUser(eq(TestDataFactory.USER_EMAIL), any()))
                .thenReturn(TestDataFactory.mealEntryResponse());
        when(repository.save(any(FoodAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaveFoodAnalysisResponse response = service.saveAnalysis(TestDataFactory.ANALYSIS_ID, SaveFoodAnalysisRequest.builder().notes("Lunch").build());

        assertEquals(TestDataFactory.ANALYSIS_ID, response.getAnalysisId());
        assertEquals(TestDataFactory.MEAL_ENTRY_ID, response.getMealEntryId());
        assertEquals(TestDataFactory.MEAL_ENTRY_ID, analysis.getSavedMealId());
        verify(repository).save(analysis);
    }

    @Test
    void saveAnalysisShouldRejectAlreadySavedAnalysis() {
        FoodAnalysis analysis = TestDataFactory.completedAnalysis();
        analysis.setSavedMealId(TestDataFactory.MEAL_ENTRY_ID);
        when(repository.findById(TestDataFactory.ANALYSIS_ID)).thenReturn(Optional.of(analysis));

        assertThrows(ConflictException.class, () -> service.saveAnalysis(TestDataFactory.ANALYSIS_ID, new SaveFoodAnalysisRequest()));
    }

    @Test
    void getUserHistoryShouldReturnDetailedItems() {
        FoodAnalysis analysis = TestDataFactory.completedAnalysis();
        analysis.setSavedMealId(TestDataFactory.MEAL_ENTRY_ID);
        when(repository.findByUserIdOrderByCreatedAtDesc(TestDataFactory.USER_EMAIL))
                .thenReturn(List.of(analysis));

        List<FoodAnalysisResponse> history = service.getUserHistory();

        assertEquals(1, history.size());
        assertEquals("Chicken salad", history.get(0).getDishName());
        assertEquals(420, history.get(0).getCalories());
        assertEquals(TestDataFactory.MEAL_ENTRY_ID, history.get(0).getSavedMealId());
    }

    @Test
    void deleteAnalysisShouldDeleteOwnedRecord() {
        FoodAnalysis analysis = TestDataFactory.completedAnalysis();
        when(repository.findById(TestDataFactory.ANALYSIS_ID)).thenReturn(Optional.of(analysis));

        service.deleteAnalysis(TestDataFactory.ANALYSIS_ID);

        verify(repository).delete(analysis);
    }
}
