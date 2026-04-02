package com.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userservice.common.exceptions.GlobalExceptionHandler;
import com.userservice.common.exceptions.MealEntryNotFoundException;
import com.userservice.common.security.JwtAuthenticationFilter;
import com.userservice.common.security.RestAccessDeniedHandler;
import com.userservice.common.security.RestAuthenticationEntryPoint;
import com.userservice.meals.controller.MealEntryController;
import com.userservice.meals.dto.MealEntryResponse;
import com.userservice.meals.dto.MealListItemResponse;
import com.userservice.meals.model.enums.MealSource;
import com.userservice.meals.service.MealEntryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealEntryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MealEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MealEntryService mealEntryService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void createMealShouldReturnCreated() throws Exception {
        when(mealEntryService.createMeal(any())).thenReturn(MealEntryResponse.builder()
                .id(TestDataFactory.MEAL_ENTRY_ID)
                .title("Chicken salad")
                .calories(420)
                .source(MealSource.MANUAL)
                .build());

        mockMvc.perform(post("/api/meals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.mealCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Chicken salad"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void createMealShouldReturnBadRequestWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/meals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "calories": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getMealsShouldReturnOk() throws Exception {
        when(mealEntryService.getMeals(null, null)).thenReturn(List.of(
                MealListItemResponse.builder()
                        .id(TestDataFactory.MEAL_ENTRY_ID)
                        .title("Chicken salad")
                        .calories(420)
                        .source(MealSource.MANUAL)
                        .build()
        ));

        mockMvc.perform(get("/api/meals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Chicken salad"));
    }

    @Test
    void getMealByIdShouldReturnNotFound() throws Exception {
        doThrow(new MealEntryNotFoundException("Meal entry not found"))
                .when(mealEntryService).getMealById(TestDataFactory.MEAL_ENTRY_ID);

        mockMvc.perform(get("/api/meals/{mealEntryId}", TestDataFactory.MEAL_ENTRY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Meal entry not found"));
    }
}
