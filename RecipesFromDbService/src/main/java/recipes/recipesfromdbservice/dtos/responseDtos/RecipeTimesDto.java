package recipes.recipesfromdbservice.dtos.responseDtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecipeTimesDto {
    @JsonAlias({"prepTime", "prep_time"})
    private String prepTime;

    @JsonAlias({"cookTime", "cook_time"})
    private String cookTime;

    @JsonAlias({"totalTime", "total_time"})
    private String totalTime;
}
