package recipes.householdservice.household.mapper;

import org.springframework.stereotype.Component;
import recipes.householdservice.dto.household.HouseholdInvitationResponse;
import recipes.householdservice.household.model.HouseholdInvitation;

@Component
public class HouseholdInvitationMapper {

    public HouseholdInvitationResponse toResponse(HouseholdInvitation entity, String householdName) {
        return HouseholdInvitationResponse.builder()
                .id(entity.getId())
                .householdId(entity.getHouseholdId())
                .householdName(householdName)
                .invitedUserId(entity.getInvitedUserId())
                .invitedEmail(entity.getInvitedEmail())
                .invitedDisplayName(entity.getInvitedDisplayName())
                .invitedByUserId(entity.getInvitedByUserId())
                .invitedByName(entity.getInvitedByName())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .respondedAt(entity.getRespondedAt())
                .build();
    }
}
