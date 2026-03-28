package com.userservice;

import com.userservice.profile.controller.UserController;
import com.userservice.profile.dto.UserProfileResponse;
import com.userservice.profile.dto.likes.LikeActionResponse;
import com.userservice.common.security.JwtAuthenticationFilter;
import com.userservice.common.security.RestAccessDeniedHandler;
import com.userservice.common.security.RestAuthenticationEntryPoint;
import com.userservice.profile.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void createLikeShouldReturnCreated() throws Exception {
        when(userService.createLike(42L)).thenReturn(LikeActionResponse.builder().recipeId(42L).liked(true).changed(true).build());

        mockMvc.perform(post("/api/profiles/likes/42"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.liked").value(true));
    }

    @Test
    void getMyProfileShouldReturnOk() throws Exception {
        when(userService.getCurrentProfile()).thenReturn(UserProfileResponse.builder().email("user@example.com").targetCalories(2400).build());

        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }
}
