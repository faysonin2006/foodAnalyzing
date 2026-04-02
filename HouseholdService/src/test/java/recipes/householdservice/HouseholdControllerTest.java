package recipes.householdservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import recipes.householdservice.common.exceptions.GlobalExceptionHandler;
import recipes.householdservice.common.security.JwtAuthenticationFilter;
import recipes.householdservice.common.security.RestAccessDeniedHandler;
import recipes.householdservice.common.security.RestAuthenticationEntryPoint;
import recipes.householdservice.dto.household.HouseholdInvitationResponse;
import recipes.householdservice.dto.household.HouseholdMessageResponse;
import recipes.householdservice.dto.household.HouseholdResponse;
import recipes.householdservice.dto.household.HouseholdSummaryResponse;
import recipes.householdservice.household.controller.HouseholdController;
import recipes.householdservice.household.service.HouseholdService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HouseholdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HouseholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HouseholdService householdService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @MockBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void createHouseholdShouldReturnCreated() throws Exception {
        when(householdService.createHousehold(any())).thenReturn(HouseholdResponse.builder()
                .id(TestDataFactory.HOUSEHOLD_ID)
                .name("Family")
                .createdAt(LocalDateTime.of(2026, 3, 28, 12, 0))
                .members(List.of())
                .uncheckedItemsCount(0)
                .build());

        mockMvc.perform(post("/api/households")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.createHouseholdRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Family"));
    }

    @Test
    void getMyHouseholdsShouldReturnOk() throws Exception {
        when(householdService.getMyHouseholds()).thenReturn(List.of(
                HouseholdSummaryResponse.builder()
                        .id(TestDataFactory.HOUSEHOLD_ID)
                        .name("Family")
                        .membersCount(2)
                        .uncheckedItemsCount(1)
                        .build()
        ));

        mockMvc.perform(get("/api/households/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].membersCount").value(2));
    }

    @Test
    void createInvitationShouldReturnCreated() throws Exception {
        when(householdService.createInvitation(any(), any())).thenReturn(HouseholdInvitationResponse.builder()
                .id(TestDataFactory.INVITATION_ID)
                .householdId(TestDataFactory.HOUSEHOLD_ID)
                .householdName("Family")
                .invitedEmail(TestDataFactory.MEMBER_EMAIL)
                .invitedDisplayName("Member")
                .invitedByName("Owner")
                .status("PENDING")
                .build());

        mockMvc.perform(post("/api/households/{householdId}/invitations", TestDataFactory.HOUSEHOLD_ID)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.createInvitationRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invitedEmail").value(TestDataFactory.MEMBER_EMAIL));
    }

    @Test
    void acceptInvitationShouldReturnHousehold() throws Exception {
        when(householdService.acceptInvitation(TestDataFactory.INVITATION_ID)).thenReturn(HouseholdResponse.builder()
                .id(TestDataFactory.HOUSEHOLD_ID)
                .name("Family")
                .members(List.of())
                .uncheckedItemsCount(0)
                .build());

        mockMvc.perform(post("/api/households/invitations/{invitationId}/accept", TestDataFactory.INVITATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Family"));
    }

    @Test
    void getMyInvitationsShouldReturnOk() throws Exception {
        when(householdService.getMyInvitations()).thenReturn(List.of(
                HouseholdInvitationResponse.builder()
                        .id(TestDataFactory.INVITATION_ID)
                        .householdId(TestDataFactory.HOUSEHOLD_ID)
                        .householdName("Family")
                        .invitedEmail(TestDataFactory.MEMBER_EMAIL)
                        .status("PENDING")
                        .build()
        ));

        mockMvc.perform(get("/api/households/invitations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void declineInvitationShouldReturnUpdatedInvitation() throws Exception {
        when(householdService.declineInvitation(TestDataFactory.INVITATION_ID)).thenReturn(HouseholdInvitationResponse.builder()
                .id(TestDataFactory.INVITATION_ID)
                .householdId(TestDataFactory.HOUSEHOLD_ID)
                .householdName("Family")
                .invitedEmail(TestDataFactory.MEMBER_EMAIL)
                .status("DECLINED")
                .build());

        mockMvc.perform(post("/api/households/invitations/{invitationId}/decline", TestDataFactory.INVITATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void createMessageShouldReturnCreated() throws Exception {
        when(householdService.createMessage(any(), any())).thenReturn(HouseholdMessageResponse.builder()
                .id(TestDataFactory.MESSAGE_ID)
                .authorName("Owner")
                .message("Buy bread too")
                .build());

        mockMvc.perform(post("/api/households/{householdId}/messages", TestDataFactory.HOUSEHOLD_ID)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.createMessageRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Buy bread too"));
    }

    @Test
    void createMessageShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/households/{householdId}/messages", TestDataFactory.HOUSEHOLD_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
