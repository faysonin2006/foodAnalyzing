package com.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userservice.common.exceptions.GlobalExceptionHandler;
import com.userservice.common.exceptions.ShoppingListItemNotFoundException;
import com.userservice.common.security.JwtAuthenticationFilter;
import com.userservice.common.security.RestAccessDeniedHandler;
import com.userservice.common.security.RestAuthenticationEntryPoint;
import com.userservice.shopping.controller.ShoppingListController;
import com.userservice.shopping.dto.ShoppingListItemResponse;
import com.userservice.shopping.service.ShoppingListService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShoppingListController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShoppingListService shoppingListService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void createItemShouldReturnCreated() throws Exception {
        when(shoppingListService.createItem(any())).thenReturn(ShoppingListItemResponse.builder()
                .id(TestDataFactory.SHOPPING_ITEM_ID)
                .name("Tomatoes")
                .quantity(new BigDecimal("2.00"))
                .unit("kg")
                .checked(false)
                .build());

        mockMvc.perform(post("/api/shopping-lists/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.shoppingCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tomatoes"))
                .andExpect(jsonPath("$.checked").value(false));
    }

    @Test
    void getItemsShouldReturnOk() throws Exception {
        when(shoppingListService.getItems()).thenReturn(List.of(ShoppingListItemResponse.builder()
                .id(TestDataFactory.SHOPPING_ITEM_ID)
                .name("Tomatoes")
                .quantity(new BigDecimal("2.00"))
                .unit("kg")
                .checked(false)
                .build()));

        mockMvc.perform(get("/api/shopping-lists/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tomatoes"));
    }

    @Test
    void toggleCheckedShouldReturnUpdatedItem() throws Exception {
        when(shoppingListService.toggleChecked(TestDataFactory.SHOPPING_ITEM_ID)).thenReturn(ShoppingListItemResponse.builder()
                .id(TestDataFactory.SHOPPING_ITEM_ID)
                .name("Tomatoes")
                .checked(true)
                .build());

        mockMvc.perform(patch("/api/shopping-lists/items/{itemId}/check", TestDataFactory.SHOPPING_ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true));
    }

    @Test
    void deleteItemShouldReturnNotFound() throws Exception {
        doThrow(new ShoppingListItemNotFoundException("Shopping list item not found"))
                .when(shoppingListService).deleteItem(TestDataFactory.SHOPPING_ITEM_ID);

        mockMvc.perform(delete("/api/shopping-lists/items/{itemId}", TestDataFactory.SHOPPING_ITEM_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Shopping list item not found"));
    }
}
