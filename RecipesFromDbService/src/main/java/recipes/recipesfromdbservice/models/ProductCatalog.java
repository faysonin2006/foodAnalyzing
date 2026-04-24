package recipes.recipesfromdbservice.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "product_catalog", schema = "cookbook_wh")
@Getter
@Setter
public class ProductCatalog {

    @Id
    @Column(name = "code", nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(name = "product_name", nullable = false, columnDefinition = "TEXT")
    private String productName;

    @Column(name = "generic_name", columnDefinition = "TEXT")
    private String genericName;

    @Column(name = "brand_name", columnDefinition = "TEXT")
    private String brandName;

    @Column(name = "quantity", columnDefinition = "TEXT")
    private String quantity;

    @Column(name = "serving_size", columnDefinition = "TEXT")
    private String servingSize;

    @Column(name = "categories_text", columnDefinition = "TEXT")
    private String categoriesText;

    @Column(name = "countries_text", columnDefinition = "TEXT")
    private String countriesText;

    @Column(name = "stores_text", columnDefinition = "TEXT")
    private String storesText;

    @Column(name = "ingredients_text", columnDefinition = "TEXT")
    private String ingredientsText;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "energy_kj_100g")
    private BigDecimal energyKj100g;

    @Column(name = "calories_kcal_100g")
    private BigDecimal caloriesKcal100g;

    @Column(name = "fats_100g")
    private BigDecimal fats100g;

    @Column(name = "saturated_fat_100g")
    private BigDecimal saturatedFat100g;

    @Column(name = "carbohydrates_100g")
    private BigDecimal carbohydrates100g;

    @Column(name = "proteins_100g")
    private BigDecimal proteins100g;

    @Column(name = "fiber_100g")
    private BigDecimal fiber100g;

    @Column(name = "sugars_100g")
    private BigDecimal sugars100g;

    @Column(name = "salt_100g")
    private BigDecimal salt100g;

    @Column(name = "sodium_100g")
    private BigDecimal sodium100g;

    @Column(name = "search_text", nullable = false, columnDefinition = "TEXT")
    private String searchText;

    @Column(name = "name_search_text", nullable = false, columnDefinition = "TEXT")
    private String nameSearchText;

    @Column(name = "brand_search_text", nullable = false, columnDefinition = "TEXT")
    private String brandSearchText;

    @Column(name = "country_search_text", nullable = false, columnDefinition = "TEXT")
    private String countrySearchText;

    @Column(name = "source", nullable = false, columnDefinition = "TEXT")
    private String source;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
