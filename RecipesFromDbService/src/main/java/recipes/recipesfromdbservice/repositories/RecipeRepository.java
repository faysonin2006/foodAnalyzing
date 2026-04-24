package recipes.recipesfromdbservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import recipes.recipesfromdbservice.models.RecipeCard;
import recipes.recipesfromdbservice.repositories.projections.*;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<RecipeCard, Long> {

    @Query(value = """
        SELECT
            q.recipeId,
            q.title,
            q.image,
            q.category,
            q.searchScore,
            q.ingredientsCount,
            q.instructionsCount,
            q.timesJson,
            q.searchDocument,
            q.blockDietKeysJson,
            q.blockAllergyKeysJson,
            q.blockHealthKeysJson,
            q.cautionHealthKeysJson
        FROM (
            SELECT
                c.recipe_id AS recipeId,
                c.title AS title,
                c.image AS image,
                c.category AS category,
                (
                  CASE WHEN :title IS NOT NULL AND c.title_search_text = :title THEN 220 ELSE 0 END
                  + CASE WHEN :title IS NOT NULL AND c.title_search_text LIKE CONCAT(:title, '%') THEN 150 ELSE 0 END
                  + CASE WHEN :title IS NOT NULL AND c.title_search_text LIKE CONCAT('%', :title, '%') THEN 120 ELSE 0 END
                  + CASE WHEN :title IS NOT NULL AND c.search_document LIKE CONCAT('%', :title, '%') THEN 72 ELSE 0 END
                  + CASE WHEN :title IS NOT NULL THEN CAST(GREATEST(cookbook_wh.similarity(c.title_search_text, CAST(:title AS text)), 0) * 92 AS integer) ELSE 0 END
                  + CASE WHEN :title IS NOT NULL THEN CAST(GREATEST(cookbook_wh.similarity(c.ingredient_search_text, CAST(:title AS text)), 0) * 44 AS integer) ELSE 0 END
                  + CASE WHEN :category IS NOT NULL AND c.category_search_text LIKE CONCAT('%', :category, '%') THEN 54 ELSE 0 END
                  + COALESCE((
                        SELECT SUM(
                            CASE
                                WHEN c.title_search_text LIKE CONCAT('%', token, '%') THEN 48
                                WHEN c.ingredient_search_text LIKE CONCAT('%', token, '%') THEN 42
                                WHEN c.category_search_text LIKE CONCAT('%', token, '%') THEN 28
                                WHEN c.search_document LIKE CONCAT('%', token, '%') THEN 16
                                ELSE 0
                            END
                        )
                        FROM unnest(CAST(:includeIngredients AS text[])) token
                    ), 0)
                ) AS searchScore,
                c.ingredients_count AS ingredientsCount,
                c.instructions_count AS instructionsCount,
                c.times::text AS timesJson,
                c.search_document::text AS searchDocument,
                to_json(c.block_diet_keys)::text AS blockDietKeysJson,
                to_json(c.block_allergy_keys)::text AS blockAllergyKeysJson,
                to_json(c.block_health_keys)::text AS blockHealthKeysJson,
                to_json(c.caution_health_keys)::text AS cautionHealthKeysJson,
                c.recipe_id AS sortRecipeId
            FROM cookbook_wh.card_search_mv c
            WHERE (:lang IS NULL OR c.lang = :lang)
              AND (
                    (
                        :title IS NULL
                        AND COALESCE(cardinality(CAST(:includeIngredients AS text[])), 0) = 0
                    )
                    OR c.title_search_text LIKE CONCAT('%', :title, '%')
                    OR c.category_search_text LIKE CONCAT('%', :title, '%')
                    OR c.ingredient_search_text LIKE CONCAT('%', :title, '%')
                    OR c.search_document LIKE CONCAT('%', :title, '%')
                    OR (:title IS NOT NULL AND cookbook_wh.similarity(c.title_search_text, CAST(:title AS text)) >= 0.28)
                    OR (:title IS NOT NULL AND cookbook_wh.similarity(c.ingredient_search_text, CAST(:title AS text)) >= 0.22)
                    OR EXISTS (
                        SELECT 1
                        FROM unnest(CAST(:includeIngredients AS text[])) token
                        WHERE c.title_search_text LIKE CONCAT('%', token, '%')
                           OR c.category_search_text LIKE CONCAT('%', token, '%')
                           OR c.ingredient_search_text LIKE CONCAT('%', token, '%')
                           OR c.search_document LIKE CONCAT('%', token, '%')
                    )
                  )
              AND (
                    :category IS NULL
                    OR c.category_search_text LIKE CONCAT('%', :category, '%')
                  )
        ) q
        ORDER BY
          CASE WHEN :sortBy = 'search_score' THEN q.searchScore END DESC,
          CASE WHEN :sortBy = 'recipe_id'          AND :sortDir = 'asc'  THEN q.sortRecipeId END ASC,
          CASE WHEN :sortBy = 'recipe_id'          AND :sortDir = 'desc' THEN q.sortRecipeId END DESC,
          CASE WHEN :sortBy = 'title'              AND :sortDir = 'asc'  THEN q.title END ASC,
          CASE WHEN :sortBy = 'title'              AND :sortDir = 'desc' THEN q.title END DESC,
          CASE WHEN :sortBy = 'category'           AND :sortDir = 'asc'  THEN q.category END ASC,
          CASE WHEN :sortBy = 'category'           AND :sortDir = 'desc' THEN q.category END DESC,
          CASE WHEN :sortBy = 'ingredients_count'  AND :sortDir = 'asc'  THEN q.ingredientsCount END ASC,
          CASE WHEN :sortBy = 'ingredients_count'  AND :sortDir = 'desc' THEN q.ingredientsCount END DESC,
          CASE WHEN :sortBy = 'instructions_count' AND :sortDir = 'asc'  THEN q.instructionsCount END ASC,
          CASE WHEN :sortBy = 'instructions_count' AND :sortDir = 'desc' THEN q.instructionsCount END DESC,
          q.sortRecipeId DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<RecipeCardListRow> findRecipes(
            @Param("lang") String lang,
            @Param("title") String title,
            @Param("category") String category,
            @Param("includeIngredients") String[] includeIngredients,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT
            c.recipe_id AS recipeId,
            c.title AS title,
            c.image AS image,
            c.category AS category,
            c.ingredients_count AS ingredientsCount,
            c.instructions_count AS instructionsCount,
            c.times::text AS timesJson,
            to_json(c.block_diet_keys)::text AS blockDietKeysJson,
            to_json(c.block_allergy_keys)::text AS blockAllergyKeysJson,
            to_json(c.block_health_keys)::text AS blockHealthKeysJson,
            to_json(c.caution_health_keys)::text AS cautionHealthKeysJson
        FROM cookbook_wh.card_search_mv c
        WHERE c.recipe_id = :recipeId
        """, nativeQuery = true)
    Optional<CardFullRecipeRow> getFullRecipeInfo(@Param("recipeId") Long id);

    @Query(value = """
        SELECT
            c.recipe_id AS recipeId,
            c.title AS title,
            c.image AS image,
            c.category AS category,
            1 AS searchScore,
            c.ingredients_count AS ingredientsCount,
            c.instructions_count AS instructionsCount,
            c.times::text AS timesJson,
            c.search_document::text AS searchDocument,
            to_json(c.block_diet_keys)::text AS blockDietKeysJson,
            to_json(c.block_allergy_keys)::text AS blockAllergyKeysJson,
            to_json(c.block_health_keys)::text AS blockHealthKeysJson,
            to_json(c.caution_health_keys)::text AS cautionHealthKeysJson,
            c.recipe_id AS sortRecipeId
        FROM cookbook_wh.card_search_mv c
        JOIN cookbook_wh.nutrition_items n ON n.recipe_id = c.recipe_id
        WHERE (:lang IS NULL OR c.lang = :lang)
          AND (:maxCalories IS NULL OR (n.nutrient = 'calories' AND n.amount_value <= :maxCalories))
          AND (:maxProteins IS NULL OR (n.nutrient = 'protein' AND n.amount_value <= :maxProteins))
          AND (:maxFats IS NULL OR (n.nutrient = 'fat' AND n.amount_value <= :maxFats))
          AND (:maxCarbohydrates IS NULL OR (n.nutrient = 'carbohydrates' AND n.amount_value <= :maxCarbohydrates))
        GROUP BY c.recipe_id
        LIMIT 50
        """, nativeQuery = true)
    List<RecipeCardListRow> findRecipesByNutrition(
            @Param("lang") String lang,
            @Param("maxCalories") Double maxCalories,
            @Param("maxProteins") Double maxProteins,
            @Param("maxFats") Double maxFats,
            @Param("maxCarbohydrates") Double maxCarbohydrates
    );

    @Query(value = """
        SELECT
            rif.position AS position,
            rif.quantity_text AS quantityText,
            rif.quantity_value AS quantityValue,
            u.display_name AS unit,
            ic.display_name AS ingredient,
            rif.note AS note,
            rif.raw_text AS rawText
        FROM cookbook_wh.recipe_ingredient_facts rif
        LEFT JOIN cookbook_wh.units u ON u.id = rif.unit_id
        LEFT JOIN cookbook_wh.ingredients_catalog ic ON ic.id = rif.ingredient_id
        WHERE rif.recipe_id = :recipeId
        ORDER BY rif.position
        """, nativeQuery = true)
    List<IngredientRow> findIngredientsByRecipeId(@Param("recipeId") Long recipeId);

    @Query(value = """
        SELECT
            s.position AS position,
            s.raw_text AS text,
            s.duration_hint AS durationHint,
            s.temperature_hint AS temperatureHint
        FROM cookbook_wh.instruction_steps s
        WHERE s.recipe_id = :recipeId
        ORDER BY s.position
        """, nativeQuery = true)
    List<InstructionRow> findInstructionsByRecipeId(@Param("recipeId") Long recipeId);

    @Query(value = """
        SELECT
            n.nutrient AS nutrient,
            n.amount AS amount
        FROM cookbook_wh.nutrition_items n
        WHERE n.recipe_id = :recipeId
        ORDER BY n.nutrient
        """, nativeQuery = true)
    List<NutritionRow> findNutritionsByRecipeId(@Param("recipeId") Long recipeId);

    @Query(value = """
        SELECT
            rc.constraint_key AS key,
            rc.constraint_type AS type,
            rc.status AS status,
            rc.reason AS reason,
            rc.source AS source,
            rc.confidence AS confidence
        FROM cookbook_wh.recipe_constraints rc
        WHERE rc.recipe_id = :recipeId
        """, nativeQuery = true)
    List<ConstraintRow> findConstraintsByRecipeId(@Param("recipeId") Long recipeId);
}
