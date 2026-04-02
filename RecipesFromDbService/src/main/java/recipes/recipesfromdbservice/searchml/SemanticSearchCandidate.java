package recipes.recipesfromdbservice.searchml;

import java.util.List;

public record SemanticSearchCandidate(
        Long recipeId,
        String title,
        String category,
        List<String> ingredients
) {
}
