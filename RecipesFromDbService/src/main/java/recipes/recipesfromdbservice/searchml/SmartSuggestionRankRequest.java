package recipes.recipesfromdbservice.searchml;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SmartSuggestionRankRequest(
        @NotBlank(message = "query must not be blank")
        String query,
        List<SmartSuggestionCandidate> candidates,
        @Min(value = 1, message = "limit must be at least 1")
        @Max(value = 20, message = "limit must be at most 20")
        Integer limit
) {
}
