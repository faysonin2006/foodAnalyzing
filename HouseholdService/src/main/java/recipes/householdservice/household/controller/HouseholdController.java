package recipes.householdservice.household.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
import recipes.householdservice.household.controller.api.HouseholdControllerApi;
import recipes.householdservice.household.service.HouseholdService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/households")
@RequiredArgsConstructor
public class HouseholdController implements HouseholdControllerApi {

    private final HouseholdService householdService;

    @Override
    @PostMapping
    public ResponseEntity<HouseholdResponse> createHousehold(@RequestBody @Valid CreateHouseholdRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(householdService.createHousehold(request));
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<List<HouseholdSummaryResponse>> getMyHouseholds() {
        return ResponseEntity.ok(householdService.getMyHouseholds());
    }

    @Override
    @GetMapping("/{householdId}")
    public ResponseEntity<HouseholdResponse> getHousehold(@PathVariable UUID householdId) {
        return ResponseEntity.ok(householdService.getHousehold(householdId));
    }

    @Override
    @PostMapping("/{householdId}/invitations")
    public ResponseEntity<HouseholdInvitationResponse> createInvitation(
            @PathVariable UUID householdId,
            @RequestBody @Valid CreateHouseholdInvitationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(householdService.createInvitation(householdId, request));
    }

    @Override
    @GetMapping("/invitations/me")
    public ResponseEntity<List<HouseholdInvitationResponse>> getMyInvitations() {
        return ResponseEntity.ok(householdService.getMyInvitations());
    }

    @Override
    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<HouseholdResponse> acceptInvitation(@PathVariable UUID invitationId) {
        return ResponseEntity.ok(householdService.acceptInvitation(invitationId));
    }

    @Override
    @PostMapping("/invitations/{invitationId}/decline")
    public ResponseEntity<HouseholdInvitationResponse> declineInvitation(@PathVariable UUID invitationId) {
        return ResponseEntity.ok(householdService.declineInvitation(invitationId));
    }

    @Override
    @PostMapping("/{householdId}/members")
    public ResponseEntity<HouseholdMemberResponse> addMember(@PathVariable UUID householdId, @RequestBody @Valid AddHouseholdMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(householdService.addMember(householdId, request));
    }

    @Override
    @DeleteMapping("/{householdId}/members/{memberUserId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID householdId, @PathVariable UUID memberUserId) {
        householdService.removeMember(householdId, memberUserId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{householdId}/shopping-items")
    public ResponseEntity<List<HouseholdShoppingItemResponse>> getShoppingItems(@PathVariable UUID householdId) {
        return ResponseEntity.ok(householdService.getShoppingItems(householdId));
    }

    @Override
    @PostMapping("/{householdId}/shopping-items")
    public ResponseEntity<HouseholdShoppingItemResponse> addShoppingItem(
            @PathVariable UUID householdId,
            @RequestBody @Valid CreateHouseholdShoppingItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(householdService.addShoppingItem(householdId, request));
    }

    @Override
    @PatchMapping("/{householdId}/shopping-items/{itemId}/check")
    public ResponseEntity<HouseholdShoppingItemResponse> toggleShoppingItem(@PathVariable UUID householdId, @PathVariable UUID itemId) {
        return ResponseEntity.ok(householdService.toggleShoppingItem(householdId, itemId));
    }

    @Override
    @DeleteMapping("/{householdId}/shopping-items/{itemId}")
    public ResponseEntity<Void> deleteShoppingItem(@PathVariable UUID householdId, @PathVariable UUID itemId) {
        householdService.deleteShoppingItem(householdId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{householdId}/messages")
    public ResponseEntity<List<HouseholdMessageResponse>> getMessages(@PathVariable UUID householdId) {
        return ResponseEntity.ok(householdService.getMessages(householdId));
    }

    @Override
    @PostMapping("/{householdId}/messages")
    public ResponseEntity<HouseholdMessageResponse> createMessage(
            @PathVariable UUID householdId,
            @RequestBody @Valid CreateHouseholdMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(householdService.createMessage(householdId, request));
    }
}
