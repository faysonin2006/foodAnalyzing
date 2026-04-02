package recipes.householdservice.dto.household;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import recipes.householdservice.household.model.modelenums.HouseholdRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdMemberResponse {

    private UUID id;
    private UUID userId;
    private String email;
    private String displayName;
    private HouseholdRole role;
    private LocalDateTime joinedAt;
}
