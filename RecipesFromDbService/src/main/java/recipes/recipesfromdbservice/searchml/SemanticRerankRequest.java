package recipes.recipesfromdbservice.searchml;

import java.util.List;

public record SemanticRerankRequest(
        String query,
        List<SemanticSearchCandidate> candidates,
        Integer topK
) {
}
