package recipes.recipesfromdbservice.searchml;

public record SemanticSearchScore(
        Long recipeId,
        Double score
) {
}
