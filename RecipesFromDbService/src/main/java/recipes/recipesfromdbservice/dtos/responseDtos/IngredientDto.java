package recipes.recipesfromdbservice.dtos.responseDtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngredientDto {
    private Integer position;

    @JsonAlias({"quantityText", "quantity_text", "quantity", "qty"})
    private String quantityText;

    @JsonAlias({"quantityValue", "quantity_value", "amountValue", "amount_value"})
    private Double quantityValue;

    @JsonAlias({"unit", "measure", "measurement"})
    private String unit;

    @JsonAlias({"ingredient", "name", "item", "label"})
    private String ingredient;

    @JsonAlias({"note", "comment", "description"})
    private String note;

    @JsonAlias({"rawText", "raw_text", "raw", "raw_line", "rawLine"})
    private String rawText;
}
