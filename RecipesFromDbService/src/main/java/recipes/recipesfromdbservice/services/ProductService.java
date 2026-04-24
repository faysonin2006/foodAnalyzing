package recipes.recipesfromdbservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.BadRequestException;
import recipes.recipesfromdbservice.configs.exceptionhandler.exceptions.RecipeNotFoundException;
import recipes.recipesfromdbservice.constants.AppMessages;
import recipes.recipesfromdbservice.dtos.ProductCardResponse;
import recipes.recipesfromdbservice.dtos.ProductDetailResponse;
import recipes.recipesfromdbservice.dtos.ProductSearchPageResponse;
import recipes.recipesfromdbservice.repositories.ProductRepository;
import recipes.recipesfromdbservice.repositories.projections.ProductDetailRow;
import recipes.recipesfromdbservice.repositories.projections.ProductSearchRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 40;

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductCardResponse> searchProducts(String query, String country, Integer page, Integer size) {
        String normalizedQuery = normalizeSearchPhrase(query);
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            throw new BadRequestException("Product query must not be blank");
        }

        String normalizedCountry = normalizeSearchPhrase(country);
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int resolvedPage = page == null ? 1 : Math.max(1, page);
        int offset = (resolvedPage - 1) * resolvedSize;
        boolean fullTextEnabled = isFullTextEnabled(normalizedQuery);

        return productRepository.searchProducts(
                        normalizedQuery,
                        normalizedCountry,
                        fullTextEnabled,
                        resolvedSize,
                        offset
                )
                .stream()
                .map(this::toCardResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductSearchPageResponse searchProductsPage(String query, String country, Integer page, Integer size) {
        String normalizedQuery = normalizeSearchPhrase(query);
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            throw new BadRequestException("Product query must not be blank");
        }

        String normalizedCountry = normalizeSearchPhrase(country);
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int resolvedPage = page == null ? 1 : Math.max(1, page);
        int offset = (resolvedPage - 1) * resolvedSize;
        boolean fullTextEnabled = isFullTextEnabled(normalizedQuery);

        List<ProductCardResponse> rows = productRepository
                .searchProducts(
                        normalizedQuery,
                        normalizedCountry,
                        fullTextEnabled,
                        resolvedSize + 1,
                        offset
                )
                .stream()
                .map(this::toCardResponse)
                .toList();

        boolean hasNext = rows.size() > resolvedSize;
        List<ProductCardResponse> items = hasNext ? rows.subList(0, resolvedSize) : rows;

        return ProductSearchPageResponse.builder()
                .items(items)
                .page(resolvedPage)
                .size(resolvedSize)
                .hasNext(hasNext)
                .build();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductByCode(String code) {
        String normalizedCode = code == null ? null : code.trim();
        if (normalizedCode == null || normalizedCode.isBlank()) {
            throw new BadRequestException("Product code must not be blank");
        }

        ProductDetailRow product = productRepository.findProductDetailByCode(normalizedCode)
                .orElseThrow(() -> new RecipeNotFoundException(AppMessages.PRODUCT_NOT_FOUND_PREFIX + normalizedCode));
        return toDetailResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductByBarcode(String barcode) {
        return getProductByCode(barcode);
    }

    private ProductCardResponse toCardResponse(ProductSearchRow row) {
        return ProductCardResponse.builder()
                .code(row.getCode())
                .productName(row.getProductName())
                .brandName(row.getBrandName())
                .quantity(row.getQuantity())
                .servingSize(row.getServingSize())
                .countriesText(row.getCountriesText())
                .imageUrl(row.getImageUrl())
                .caloriesKcal100g(toDouble(row.getCaloriesKcal100g()))
                .proteins100g(toDouble(row.getProteins100g()))
                .fats100g(toDouble(row.getFats100g()))
                .carbohydrates100g(toDouble(row.getCarbohydrates100g()))
                .build();
    }

    private ProductDetailResponse toDetailResponse(ProductDetailRow product) {
        return ProductDetailResponse.builder()
                .code(product.getCode())
                .productName(product.getProductName())
                .genericName(product.getGenericName())
                .brandName(product.getBrandName())
                .quantity(product.getQuantity())
                .servingSize(product.getServingSize())
                .categoriesText(product.getCategoriesText())
                .countriesText(product.getCountriesText())
                .storesText(product.getStoresText())
                .ingredientsText(product.getIngredientsText())
                .imageUrl(product.getImageUrl())
                .energyKj100g(toDouble(product.getEnergyKj100g()))
                .caloriesKcal100g(toDouble(product.getCaloriesKcal100g()))
                .proteins100g(toDouble(product.getProteins100g()))
                .fats100g(toDouble(product.getFats100g()))
                .saturatedFat100g(toDouble(product.getSaturatedFat100g()))
                .carbohydrates100g(toDouble(product.getCarbohydrates100g()))
                .fiber100g(toDouble(product.getFiber100g()))
                .sugars100g(toDouble(product.getSugars100g()))
                .salt100g(toDouble(product.getSalt100g()))
                .sodium100g(toDouble(product.getSodium100g()))
                .source(product.getSource())
                .build();
    }

    private String normalizeSearchPhrase(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.replaceAll("\\s+", " ");
    }

    private boolean isFullTextEnabled(String normalizedQuery) {
        return normalizedQuery != null
                && (normalizedQuery.length() >= 5 || normalizedQuery.contains(" "));
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
