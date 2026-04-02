package com.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userservice.common.exceptions.GlobalExceptionHandler;
import com.userservice.common.exceptions.PantryItemNotFoundException;
import com.userservice.common.security.JwtAuthenticationFilter;
import com.userservice.common.security.RestAccessDeniedHandler;
import com.userservice.common.security.RestAuthenticationEntryPoint;
import com.userservice.pantry.controller.PantryController;
import com.userservice.pantry.dto.PantryBarcodeLookupResponse;
import com.userservice.pantry.dto.PantryItemResponse;
import com.userservice.pantry.dto.PantryListItemResponse;
import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.model.enums.PantryUnit;
import com.userservice.pantry.service.PantryBarcodeLookupService;
import com.userservice.pantry.service.PantryItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PantryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PantryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PantryItemService pantryItemService;
    @MockBean
    private PantryBarcodeLookupService pantryBarcodeLookupService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void createPantryItemShouldReturnCreated() throws Exception {
        PantryItemResponse response = PantryItemResponse.builder()
                .id(TestDataFactory.PANTRY_ITEM_ID)
                .name("Milk")
                .quantity(new BigDecimal("1.00"))
                .unit(PantryUnit.LITER)
                .status(PantryItemStatus.ACTIVE)
                .build();

        when(pantryItemService.createPantryItem(any())).thenReturn(response);

        mockMvc.perform(post("/api/pantry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.pantryCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Milk"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createPantryItemShouldReturnBadRequestWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/pantry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getPantryItemsShouldReturnOk() throws Exception {
        when(pantryItemService.getPantryItems()).thenReturn(List.of(
                PantryListItemResponse.builder()
                        .id(TestDataFactory.PANTRY_ITEM_ID)
                        .name("Milk")
                        .quantity(new BigDecimal("1.00"))
                        .unit(PantryUnit.LITER)
                        .expiresAt(LocalDate.of(2026, 3, 30))
                        .status(PantryItemStatus.ACTIVE)
                        .build()
        ));

        mockMvc.perform(get("/api/pantry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Milk"));
    }

    @Test
    void lookupProductByBarcodeShouldReturnPrefill() throws Exception {
        when(pantryBarcodeLookupService.lookupByBarcode("5449000000996")).thenReturn(PantryBarcodeLookupResponse.builder()
                .barcode("5449000000996")
                .name("Coca-Cola Original Taste")
                .brand("Coca-Cola")
                .category("Beverages")
                .suggestedQuantity(new BigDecimal("330"))
                .suggestedUnit(PantryUnit.MILLILITER)
                .source("OPEN_FOOD_FACTS")
                .build());

        mockMvc.perform(get("/api/pantry/barcode/{barcode}", "5449000000996"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Coca-Cola Original Taste"))
                .andExpect(jsonPath("$.suggestedUnit").value("MILLILITER"));
    }

    @Test
    void getPantryItemByIdShouldReturnNotFound() throws Exception {
        doThrow(new PantryItemNotFoundException("Pantry item not found"))
                .when(pantryItemService).getPantryItemById(TestDataFactory.PANTRY_ITEM_ID);

        mockMvc.perform(get("/api/pantry/{pantryItemId}", TestDataFactory.PANTRY_ITEM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pantry item not found"));
    }

    @Test
    void uploadPantryItemImageShouldReturnUpdatedItem() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "milk.png", "image/png", "png".getBytes());
        when(pantryItemService.uploadPantryItemImage(TestDataFactory.PANTRY_ITEM_ID, file))
                .thenReturn(PantryItemResponse.builder()
                        .id(TestDataFactory.PANTRY_ITEM_ID)
                        .name("Milk")
                        .imageUrl("https://cdn.example.com/pantry/milk.png")
                        .status(PantryItemStatus.ACTIVE)
                        .build());

        mockMvc.perform(multipart("/api/pantry/{pantryItemId}/image", TestDataFactory.PANTRY_ITEM_ID)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/pantry/milk.png"));
    }
}
