package recipes.householdservice.dto.household;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHouseholdRequest {

    @NotBlank(message = "Household name is required")
    @Size(max = 120, message = "Household name must not exceed 120 characters")
    private String name;
}
