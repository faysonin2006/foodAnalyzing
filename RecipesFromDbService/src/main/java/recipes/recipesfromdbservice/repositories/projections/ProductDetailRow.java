package recipes.recipesfromdbservice.repositories.projections;

import java.math.BigDecimal;

public interface ProductDetailRow {
    String getCode();
    String getProductName();
    String getGenericName();
    String getBrandName();
    String getQuantity();
    String getServingSize();
    String getCategoriesText();
    String getCountriesText();
    String getStoresText();
    String getIngredientsText();
    String getImageUrl();
    BigDecimal getEnergyKj100g();
    BigDecimal getCaloriesKcal100g();
    BigDecimal getProteins100g();
    BigDecimal getFats100g();
    BigDecimal getSaturatedFat100g();
    BigDecimal getCarbohydrates100g();
    BigDecimal getFiber100g();
    BigDecimal getSugars100g();
    BigDecimal getSalt100g();
    BigDecimal getSodium100g();
    String getSource();
}
