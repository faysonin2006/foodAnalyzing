package com.aiimageservice;

import com.aiimageservice.dtos.SaveFoodAnalysisRequest;
import com.aiimageservice.httpinterfaceconfig.httpuserserviceclient.HttpUserMealsClient;
import com.aiimageservice.mappers.FoodAnalysisMapper;
import com.aiimageservice.repositories.FoodAnalysisRepository;
import com.aiimageservice.services.FoodAnalysisService;
import com.aiimageservice.services.S3Service;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodAnalysisSaveIntegrationTest {

    private static final String SERVICE_TOKEN = "service-token";

    @Mock
    private FoodAnalysisRepository repository;
    @Mock
    private S3Service s3Service;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private MockWebServer userServiceServer;
    private FoodAnalysisService foodAnalysisService;

    @BeforeEach
    void setUp() throws IOException {
        userServiceServer = new MockWebServer();
        userServiceServer.start();

        HttpUserMealsClient httpUserMealsClient = createMealsClient(userServiceServer.url("/").toString());
        FoodAnalysisMapper mapper = Mappers.getMapper(FoodAnalysisMapper.class);
        foodAnalysisService = new FoodAnalysisService(repository, s3Service, rabbitTemplate, mapper, httpUserMealsClient);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestDataFactory.USER_EMAIL, null, List.of())
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        SecurityContextHolder.clearContext();
        userServiceServer.shutdown();
    }

    @Test
    void saveAnalysisShouldCallUserServiceInternalMealsEndpoint() throws Exception {
        when(repository.findById(TestDataFactory.ANALYSIS_ID)).thenReturn(Optional.of(TestDataFactory.completedAnalysis()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        userServiceServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id":"44444444-4444-4444-4444-444444444444",
                          "title":"Chicken salad",
                          "calories":420,
                          "proteins":30.0,
                          "fats":18.0,
                          "carbohydrates":24.0,
                          "eatenAt":"2026-03-28T14:00:00",
                          "notes":"Lunch",
                          "imageUrl":"https://example.com/food.jpg",
                          "createdAt":"2026-03-28T14:05:00"
                        }
                        """));

        var response = foodAnalysisService.saveAnalysis(
                TestDataFactory.ANALYSIS_ID,
                SaveFoodAnalysisRequest.builder().notes("Lunch").build()
        );

        assertEquals(TestDataFactory.ANALYSIS_ID, response.getAnalysisId());
        assertEquals(TestDataFactory.MEAL_ENTRY_ID, response.getMealEntryId());

        RecordedRequest recordedRequest = userServiceServer.takeRequest();
        assertEquals("/api/meals/internal/user%40example.com", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("Bearer " + SERVICE_TOKEN, recordedRequest.getHeader("Authorization"));
        String body = recordedRequest.getBody().readUtf8();
        assertTrue(body.contains("\"title\":\"Chicken salad\""));
        assertTrue(body.contains("\"source\":\"AI_ANALYSIS\""));
    }

    private HttpUserMealsClient createMealsClient(String baseUrl) {
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(SERVICE_TOKEN);
                    return execution.execute(request, body);
                })
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(HttpUserMealsClient.class);
    }
}
