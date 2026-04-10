package recipes.recipesfromdbservice.repositories.projections;

public interface RecipeCardListRow {
    Long getRecipeId();
    String getTitle();
    String getImage();
    String getCategory();
    Integer getSearchScore();
    Integer getIngredientsCount();
    Integer getInstructionsCount();

    String getIngredientsJson();
    String getNutritionsJson();
    String getTimesJson();
    String getSearchDocument();

    String getBlockDietKeysJson();
    String getBlockAllergyKeysJson();
    String getBlockHealthKeysJson();
    String getCautionHealthKeysJson();
}
