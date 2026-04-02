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
public class HouseholdSummaryResponse {

    private UUID id;
    private String name;
    private long membersCount;
    private long uncheckedItemsCount;
    private LocalDateTime createdAt;
}
