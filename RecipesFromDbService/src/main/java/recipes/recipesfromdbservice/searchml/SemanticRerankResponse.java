package recipes.recipesfromdbservice.searchml;

import java.util.List;

public record SemanticRerankResponse(
        List<SemanticSearchScore> scores
) {
}
