package recipes.recipesfromdbservice.dtos.responseDtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConstraintDto {
    private String key;
    private String type;
    private String status;
    private String reason;
    private String source;
    private BigDecimal confidence;

    public static ConstraintDto fromRow(recipes.recipesfromdbservice.repositories.projections.ConstraintRow row) {
        if (row == null) return null;
        return ConstraintDto.builder()
                .key(row.getKey())
                .type(row.getType())
                .status(row.getStatus())
                .reason(row.getReason())
                .source(row.getSource())
                .confidence(row.getConfidence())
                .build();
    }
}
