package recipes.recipesfromdbservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import recipes.recipesfromdbservice.models.ProductCatalog;
import recipes.recipesfromdbservice.repositories.projections.ProductDetailRow;
import recipes.recipesfromdbservice.repositories.projections.ProductSearchRow;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductCatalog, String> {

    @Query(value = """
        WITH candidate_codes AS (
            (
                SELECT p.code, 220 AS base_rank
                FROM cookbook_wh.product_search_mv p
                WHERE p.name_search_text = :query
                  AND (:country IS NULL OR p.country_search_text LIKE CONCAT('%', :country, '%'))
                LIMIT 40
            )
            UNION ALL
            (
                SELECT p.code, 150 AS base_rank
                FROM cookbook_wh.product_search_mv p
                WHERE p.name_search_text LIKE CONCAT(:query, '%')
                  AND (:country IS NULL OR p.country_search_text LIKE CONCAT('%', :country, '%'))
                LIMIT 120
            )
            UNION ALL
            (
                SELECT p.code, 110 AS base_rank
                FROM cookbook_wh.product_search_mv p
                WHERE p.name_search_text LIKE CONCAT('%', :query, '%')
                  AND (:country IS NULL OR p.country_search_text LIKE CONCAT('%', :country, '%'))
                LIMIT 120
            )
            UNION ALL
            (
                SELECT p.code, 70 AS base_rank
                FROM cookbook_wh.product_search_mv p
                WHERE p.brand_search_text LIKE CONCAT('%', :query, '%')
                  AND (:country IS NULL OR p.country_search_text LIKE CONCAT('%', :country, '%'))
                LIMIT 80
            )
            UNION ALL
            (
                SELECT p.code, 90 AS base_rank
                FROM cookbook_wh.product_search_mv p
                WHERE :fullTextEnabled IS TRUE
                  AND to_tsvector(
                        'simple',
                        coalesce(p.name_search_text, '')
                            || ' '
                            || coalesce(p.brand_search_text, '')
                            || ' '
                            || coalesce(p.search_text, '')
                    ) @@ websearch_to_tsquery('simple', CAST(:query AS text))
                  AND (:country IS NULL OR p.country_search_text LIKE CONCAT('%', :country, '%'))
                LIMIT 160
            )
        ),
        ranked_codes AS (
            SELECT DISTINCT ON (code) code, base_rank
            FROM candidate_codes
            ORDER BY code, base_rank DESC
        )
        SELECT
            p.code AS code,
            p.product_name AS productName,
            p.brand_name AS brandName,
            p.quantity AS quantity,
            p.serving_size AS servingSize,
            p.image_url AS imageUrl,
            p.countries_text AS countriesText,
            p.calories_kcal_100g AS caloriesKcal100g,
            p.proteins_100g AS proteins100g,
            p.fats_100g AS fats100g,
            p.carbohydrates_100g AS carbohydrates100g
        FROM ranked_codes r
        JOIN cookbook_wh.product_search_mv p ON p.code = r.code
        ORDER BY
            (
                r.base_rank
                + CAST(GREATEST(cookbook_wh.similarity(p.name_search_text, CAST(:query AS text)), 0) * 96 AS integer)
                + CAST(GREATEST(cookbook_wh.similarity(p.brand_search_text, CAST(:query AS text)), 0) * 42 AS integer)
            ) DESC,
            p.product_name ASC,
            p.code ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<ProductSearchRow> searchProducts(
            @Param("query") String query,
            @Param("country") String country,
            @Param("fullTextEnabled") boolean fullTextEnabled,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT
            p.code AS code,
            p.product_name AS productName,
            p.generic_name AS genericName,
            p.brand_name AS brandName,
            p.quantity AS quantity,
            p.serving_size AS servingSize,
            p.categories_text AS categoriesText,
            p.countries_text AS countriesText,
            p.stores_text AS storesText,
            p.ingredients_text AS ingredientsText,
            p.image_url AS imageUrl,
            p.energy_kj_100g AS energyKj100g,
            p.calories_kcal_100g AS caloriesKcal100g,
            p.proteins_100g AS proteins100g,
            p.fats_100g AS fats100g,
            p.saturated_fat_100g AS saturatedFat100g,
            p.carbohydrates_100g AS carbohydrates100g,
            p.fiber_100g AS fiber100g,
            p.sugars_100g AS sugars100g,
            p.salt_100g AS salt100g,
            p.sodium_100g AS sodium100g,
            p.source AS source
        FROM cookbook_wh.product_catalog p
        WHERE p.code = :code
        LIMIT 1
        """, nativeQuery = true)
    Optional<ProductDetailRow> findProductDetailByCode(@Param("code") String code);
}
