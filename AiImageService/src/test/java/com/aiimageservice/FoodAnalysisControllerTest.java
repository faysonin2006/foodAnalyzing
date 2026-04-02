package com.aiimageservice;

import com.aiimageservice.controllers.FoodAnalysisController;
import com.aiimageservice.dtos.FoodAnalysisDetailResponse;
import com.aiimageservice.dtos.FoodAnalysisResponse;
import com.aiimageservice.dtos.SaveFoodAnalysisResponse;
import com.aiimageservice.models.enums.AnalysisStatus;
import com.aiimageservice.security.JwtAuthenticationFilter;
import com.aiimageservice.security.RestAccessDeniedHandler;
import com.aiimageservice.security.RestAuthenticationEntryPoint;
import com.aiimageservice.services.FoodAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
class FoodAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FoodAnalysisService foodAnalysisService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void analyzeFoodShouldReturnAccepted() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "food.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(foodAnalysisService.uploadAndAnalyze(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("question")))
                .thenReturn(FoodAnalysisResponse.builder()
                        .id(TestDataFactory.ANALYSIS_ID)
                        .status(AnalysisStatus.PROCESSING)
                        .build());

        mockMvc.perform(multipart("/api/food/analyze")
                        .file(file)
                        .param("extraQuestions", "question"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void getAnalysisShouldReturnOk() throws Exception {
        when(foodAnalysisService.getAnalysisById(TestDataFactory.ANALYSIS_ID))
                .thenReturn(FoodAnalysisDetailResponse.builder().id(TestDataFactory.ANALYSIS_ID).dishName("Salad").status(AnalysisStatus.COMPLETED).build());

        mockMvc.perform(get("/api/food/analysis/{id}", TestDataFactory.ANALYSIS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dishName").value("Salad"));
    }

    @Test
    void saveAnalysisShouldReturnOk() throws Exception {
        when(foodAnalysisService.saveAnalysis(org.mockito.ArgumentMatchers.eq(TestDataFactory.ANALYSIS_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(SaveFoodAnalysisResponse.builder()
                        .analysisId(TestDataFactory.ANALYSIS_ID)
                        .mealEntryId(TestDataFactory.MEAL_ENTRY_ID)
                        .build());

        mockMvc.perform(post("/api/food/analyze/{id}/save", TestDataFactory.ANALYSIS_ID)
                        .contentType("application/json")
                        .content("""
                                {
                                  "notes": "Lunch"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealEntryId").value(TestDataFactory.MEAL_ENTRY_ID.toString()));
    }

    @Test
    void deleteHistoryItemShouldReturnNoContent() throws Exception {
        doNothing().when(foodAnalysisService).deleteAnalysis(TestDataFactory.ANALYSIS_ID);

        mockMvc.perform(delete("/api/food/history/{id}", TestDataFactory.ANALYSIS_ID))
                .andExpect(status().isNoContent());
    }
}
