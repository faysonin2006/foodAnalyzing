package recipes.householdservice.household.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import recipes.householdservice.common.exceptions.ErrorResponse;
import recipes.householdservice.dto.household.AddHouseholdMemberRequest;
import recipes.householdservice.dto.household.CreateHouseholdMessageRequest;
import recipes.householdservice.dto.household.CreateHouseholdRequest;
import recipes.householdservice.dto.household.CreateHouseholdInvitationRequest;
import recipes.householdservice.dto.household.CreateHouseholdShoppingItemRequest;
import recipes.householdservice.dto.household.HouseholdInvitationResponse;
import recipes.householdservice.dto.household.HouseholdMemberResponse;
import recipes.householdservice.dto.household.HouseholdMessageResponse;
import recipes.householdservice.dto.household.HouseholdResponse;
import recipes.householdservice.dto.household.HouseholdShoppingItemResponse;
import recipes.householdservice.dto.household.HouseholdSummaryResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Households", description = "Family and shared household collaboration")
public interface HouseholdControllerApi {

    @Operation(summary = "Create household", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Household created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<HouseholdResponse> createHousehold(@Valid @RequestBody CreateHouseholdRequest request);

    @Operation(summary = "Get my households", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<List<HouseholdSummaryResponse>> getMyHouseholds();

    @Operation(summary = "Get household details", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<HouseholdResponse> getHousehold(
            @Parameter(example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID householdId
    );

    @Operation(summary = "Create household invitation", description = "Owner invites another registered user. Membership is created only after accept.", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<HouseholdInvitationResponse> createInvitation(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateHouseholdInvitationRequest request
    );

    @Operation(summary = "Get my pending household invitations", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<List<HouseholdInvitationResponse>> getMyInvitations();

    @Operation(summary = "Accept household invitation", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<HouseholdResponse> acceptInvitation(@PathVariable UUID invitationId);

    @Operation(summary = "Decline household invitation", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<HouseholdInvitationResponse> declineInvitation(@PathVariable UUID invitationId);

    @Operation(summary = "Add household member", description = "Legacy direct add flow. Prefer invitation endpoints with explicit accept/decline on the invited user's side.", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<HouseholdMemberResponse> addMember(
            @PathVariable UUID householdId,
            @Valid @RequestBody AddHouseholdMemberRequest request
    );

    @Operation(summary = "Remove household member", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Void> removeMember(@PathVariable UUID householdId, @PathVariable UUID memberUserId);

    @Operation(summary = "Get shared shopping items", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<List<HouseholdShoppingItemResponse>> getShoppingItems(@PathVariable UUID householdId);

    @Operation(summary = "Add shared shopping item", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<HouseholdShoppingItemResponse> addShoppingItem(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateHouseholdShoppingItemRequest request
    );

    @Operation(summary = "Toggle shared shopping item", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<HouseholdShoppingItemResponse> toggleShoppingItem(@PathVariable UUID householdId, @PathVariable UUID itemId);

    @Operation(summary = "Delete shared shopping item", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Void> deleteShoppingItem(@PathVariable UUID householdId, @PathVariable UUID itemId);

    @Operation(summary = "Get household messages", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<List<HouseholdMessageResponse>> getMessages(@PathVariable UUID householdId);

    @Operation(summary = "Create household message", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<HouseholdMessageResponse> createMessage(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateHouseholdMessageRequest request
    );
}
