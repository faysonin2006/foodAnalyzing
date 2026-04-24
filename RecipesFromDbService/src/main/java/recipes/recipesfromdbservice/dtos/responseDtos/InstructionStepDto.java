package recipes.recipesfromdbservice.dtos.responseDtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstructionStepDto {
    private Integer position;
    private String text;
    private String durationHint;
    private String temperatureHint;

    public static InstructionStepDto fromRow(recipes.recipesfromdbservice.repositories.projections.InstructionRow row) {
        if (row == null) return null;
        return InstructionStepDto.builder()
                .position(row.getPosition())
                .text(row.getText())
                .durationHint(row.getDurationHint())
                .temperatureHint(row.getTemperatureHint())
                .build();
    }
}
