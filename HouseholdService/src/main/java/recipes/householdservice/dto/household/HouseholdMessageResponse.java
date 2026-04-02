package recipes.householdservice.dto.household;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import recipes.householdservice.household.model.modelenums.HouseholdMessageType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdMessageResponse {

    private UUID id;
    private UUID authorUserId;
    private String authorName;
    private String message;
    private HouseholdMessageType type;
    private LocalDateTime createdAt;
}
