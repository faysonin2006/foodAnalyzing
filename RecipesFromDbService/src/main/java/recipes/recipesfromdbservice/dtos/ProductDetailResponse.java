package recipes.recipesfromdbservice.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDetailResponse {
    private String code;
    private String productName;
    private String genericName;
    private String brandName;
    private String quantity;
    private String servingSize;
    private String categoriesText;
    private String countriesText;
    private String storesText;
    private String ingredientsText;
    private String imageUrl;
    private Double energyKj100g;
    private Double caloriesKcal100g;
    private Double proteins100g;
    private Double fats100g;
    private Double saturatedFat100g;
    private Double carbohydrates100g;
    private Double fiber100g;
    private Double sugars100g;
    private Double salt100g;
    private Double sodium100g;
    private String source;
}
