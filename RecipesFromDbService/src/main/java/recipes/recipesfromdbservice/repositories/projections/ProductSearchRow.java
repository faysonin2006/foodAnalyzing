package recipes.recipesfromdbservice.repositories.projections;

import java.math.BigDecimal;

public interface ProductSearchRow {
    String getCode();
    String getProductName();
    String getBrandName();
    String getQuantity();
    String getServingSize();
    String getImageUrl();
    String getCountriesText();
    BigDecimal getCaloriesKcal100g();
    BigDecimal getProteins100g();
    BigDecimal getFats100g();
    BigDecimal getCarbohydrates100g();
}
