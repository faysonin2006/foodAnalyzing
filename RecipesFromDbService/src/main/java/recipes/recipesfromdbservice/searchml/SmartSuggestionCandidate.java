package recipes.recipesfromdbservice.searchml;

import java.util.List;

public record SmartSuggestionCandidate(
        String id,
        String primaryText,
        String secondaryText,
        String category,
        String brand,
        List<String> searchTerms
) {
}
