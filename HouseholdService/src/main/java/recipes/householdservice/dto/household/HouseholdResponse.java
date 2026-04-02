package recipes.householdservice.dto.household;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdResponse {

    private UUID id;
    private String name;
    private UUID createdByUserId;
    private String createdByEmail;
    private long uncheckedItemsCount;
    private List<HouseholdMemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
