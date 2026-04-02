package recipes.recipesfromdbservice.searchml;

import java.util.List;

public record SmartSuggestionRankResponse(
        List<SmartSuggestionRankItem> items
) {
}
