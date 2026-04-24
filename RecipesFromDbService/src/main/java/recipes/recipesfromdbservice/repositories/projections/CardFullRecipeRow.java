package recipes.recipesfromdbservice.repositories.projections;

public interface CardFullRecipeRow {
    Long getRecipeId();
    String getTitle();
    String getImage();
    String getCategory();
    Integer getIngredientsCount();
    Integer getInstructionsCount();

    String getTimesJson();

    String getBlockDietKeysJson();
    String getBlockAllergyKeysJson();
    String getBlockHealthKeysJson();
    String getCautionHealthKeysJson();
}
