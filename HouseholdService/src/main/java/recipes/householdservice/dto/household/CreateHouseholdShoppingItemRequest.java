package recipes.householdservice.dto.household;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHouseholdShoppingItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(max = 160, message = "Item name must not exceed 160 characters")
    private String name;

    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @Size(max = 30, message = "Unit must not exceed 30 characters")
    private String unit;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
