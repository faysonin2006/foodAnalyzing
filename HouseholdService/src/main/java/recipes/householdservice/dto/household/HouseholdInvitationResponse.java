package recipes.householdservice.dto.household;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdInvitationResponse {

    private UUID id;
    private UUID householdId;
    private String householdName;
    private UUID invitedUserId;
    private String invitedEmail;
    private String invitedDisplayName;
    private UUID invitedByUserId;
    private String invitedByName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
