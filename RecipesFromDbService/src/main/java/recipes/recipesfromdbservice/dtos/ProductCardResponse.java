package recipes.recipesfromdbservice.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCardResponse {
    private String code;
    private String productName;
    private String brandName;
    private String quantity;
    private String servingSize;
    private String countriesText;
    private String imageUrl;
    private Double caloriesKcal100g;
    private Double proteins100g;
    private Double fats100g;
    private Double carbohydrates100g;
}
