package recipes.householdservice;

import lombok.experimental.UtilityClass;
import recipes.householdservice.dto.household.AddHouseholdMemberRequest;
import recipes.householdservice.dto.household.CreateHouseholdMessageRequest;
import recipes.householdservice.dto.household.CreateHouseholdRequest;
import recipes.householdservice.dto.household.CreateHouseholdInvitationRequest;
import recipes.householdservice.dto.household.CreateHouseholdShoppingItemRequest;
import recipes.householdservice.dto.internal.profile.UserProfileResponse;
import recipes.householdservice.household.model.Household;
import recipes.householdservice.household.model.HouseholdInvitation;
import recipes.householdservice.household.model.HouseholdMember;
import recipes.householdservice.household.model.HouseholdMessage;
import recipes.householdservice.household.model.HouseholdShoppingItem;
import recipes.householdservice.household.model.modelenums.HouseholdInvitationStatus;
import recipes.householdservice.household.model.modelenums.HouseholdMessageType;
import recipes.householdservice.household.model.modelenums.HouseholdRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class TestDataFactory {

    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID MEMBER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID HOUSEHOLD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID HOUSEHOLD_MEMBER_ROW_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID SHOPPING_ITEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID MESSAGE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final UUID INVITATION_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    public static final String EMAIL = "owner@example.com";
    public static final String MEMBER_EMAIL = "member@example.com";

    public static UserProfileResponse ownerProfile() {
        return UserProfileResponse.builder()
                .id(USER_ID)
                .email(EMAIL)
                .name("Owner")
                .build();
    }

    public static UserProfileResponse memberProfile() {
        return UserProfileResponse.builder()
                .id(MEMBER_ID)
                .email(MEMBER_EMAIL)
                .name("Member")
                .build();
    }

    public static CreateHouseholdRequest createHouseholdRequest() {
        return CreateHouseholdRequest.builder()
                .name("Family")
                .build();
    }

    public static AddHouseholdMemberRequest addMemberRequest() {
        return AddHouseholdMemberRequest.builder()
                .email(MEMBER_EMAIL)
                .build();
    }

    public static CreateHouseholdInvitationRequest createInvitationRequest() {
        return CreateHouseholdInvitationRequest.builder()
                .email(MEMBER_EMAIL)
                .build();
    }

    public static CreateHouseholdShoppingItemRequest createShoppingItemRequest() {
        return CreateHouseholdShoppingItemRequest.builder()
                .name("Milk")
                .quantity(new BigDecimal("2.00"))
                .unit("L")
                .note("Semi-skimmed")
                .build();
    }

    public static CreateHouseholdMessageRequest createMessageRequest() {
        return CreateHouseholdMessageRequest.builder()
                .message("Buy bread too")
                .build();
    }

    public static Household household() {
        return Household.builder()
                .id(HOUSEHOLD_ID)
                .name("Family")
                .createdByUserId(USER_ID)
                .createdByEmail(EMAIL)
                .createdAt(LocalDateTime.of(2026, 3, 28, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 28, 12, 5))
                .build();
    }

    public static HouseholdMember ownerMember() {
        return HouseholdMember.builder()
                .id(HOUSEHOLD_MEMBER_ROW_ID)
                .householdId(HOUSEHOLD_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .displayName("Owner")
                .role(HouseholdRole.OWNER)
                .joinedAt(LocalDateTime.of(2026, 3, 28, 12, 0))
                .build();
    }

    public static HouseholdMember member() {
        return HouseholdMember.builder()
                .id(UUID.fromString("77777777-7777-7777-7777-777777777777"))
                .householdId(HOUSEHOLD_ID)
                .userId(MEMBER_ID)
                .email(MEMBER_EMAIL)
                .displayName("Member")
                .role(HouseholdRole.MEMBER)
                .joinedAt(LocalDateTime.of(2026, 3, 28, 12, 10))
                .build();
    }

    public static HouseholdShoppingItem shoppingItem() {
        return HouseholdShoppingItem.builder()
                .id(SHOPPING_ITEM_ID)
                .householdId(HOUSEHOLD_ID)
                .name("Milk")
                .quantity(new BigDecimal("2.00"))
                .unit("L")
                .note("Semi-skimmed")
                .checked(false)
                .addedByUserId(USER_ID)
                .addedByName("Owner")
                .createdAt(LocalDateTime.of(2026, 3, 28, 12, 20))
                .updatedAt(LocalDateTime.of(2026, 3, 28, 12, 20))
                .build();
    }

    public static HouseholdInvitation invitation() {
        return HouseholdInvitation.builder()
                .id(INVITATION_ID)
                .householdId(HOUSEHOLD_ID)
                .invitedUserId(MEMBER_ID)
                .invitedEmail(MEMBER_EMAIL)
                .invitedDisplayName("Member")
                .invitedByUserId(USER_ID)
                .invitedByName("Owner")
                .status(HouseholdInvitationStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 3, 28, 12, 7))
                .build();
    }

    public static HouseholdMessage message() {
        return HouseholdMessage.builder()
                .id(MESSAGE_ID)
                .householdId(HOUSEHOLD_ID)
                .authorUserId(USER_ID)
                .authorName("Owner")
                .message("Buy bread too")
                .type(HouseholdMessageType.USER)
                .createdAt(LocalDateTime.of(2026, 3, 28, 12, 30))
                .build();
    }
}
