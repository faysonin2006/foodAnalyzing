package recipes.householdservice.dto.household;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdShoppingItemResponse {

    private UUID id;
    private String name;
    private BigDecimal quantity;
    private String unit;
    private String note;
    private boolean checked;
    private UUID addedByUserId;
    private String addedByName;
    private UUID checkedByUserId;
    private String checkedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
