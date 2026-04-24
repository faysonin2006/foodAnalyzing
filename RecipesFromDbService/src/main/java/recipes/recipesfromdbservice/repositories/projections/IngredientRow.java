package recipes.recipesfromdbservice.repositories.projections;

public interface IngredientRow {
    Integer getPosition();
    String getQuantityText();
    Double getQuantityValue();
    String getUnit();
    String getIngredient();
    String getNote();
    String getRawText();
}
