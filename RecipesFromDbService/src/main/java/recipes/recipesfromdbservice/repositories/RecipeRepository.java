package recipes.recipesfromdbservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import recipes.recipesfromdbservice.models.RecipeCard;
import recipes.recipesfromdbservice.repositories.projections.CardFullRecipeRow;
import recipes.recipesfromdbservice.repositories.projections.RecipeCardListRow;

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
            q.ingredientsJson,
            q.nutritionsJson,
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
                c.ingredients::text AS ingredientsJson,
                c.nutritions::text AS nutritionsJson,
                c.times::text AS timesJson,
                c.search_document AS searchDocument,
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
            c.ingredients::text AS ingredientsJson,
            c.instruction_steps::text AS instructionStepsJson,
            c.nutritions::text AS nutritionsJson,
            c.times::text AS timesJson,
            to_json(c.block_diet_keys)::text AS blockDietKeysJson,
            to_json(c.block_allergy_keys)::text AS blockAllergyKeysJson,
            to_json(c.block_health_keys)::text AS blockHealthKeysJson,
            to_json(c.caution_health_keys)::text AS cautionHealthKeysJson,
            c.constraints::text AS constraintsJson
        FROM cookbook_wh.card_search_mv c
        WHERE c.recipe_id = :recipeId
        """, nativeQuery = true)
    Optional<CardFullRecipeRow> getFullRecipeInfo(@Param("recipeId") Long id);
}
