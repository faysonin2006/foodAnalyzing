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
                  CASE WHEN :title IS NOT NULL AND lower(c.title) = lower(:title) THEN 100 ELSE 0 END
                  + CASE WHEN :title IS NOT NULL AND c.title ILIKE CONCAT('%', :title, '%') THEN 60 ELSE 0 END
                  + CASE WHEN :title IS NOT NULL AND c.ingredients::text ILIKE CONCAT('%', :title, '%') THEN 40 ELSE 0 END
                  + CASE WHEN :title IS NOT NULL AND c.category ILIKE CONCAT('%', :title, '%') THEN 24 ELSE 0 END
                  + COALESCE((
                        SELECT SUM(
                            CASE
                                WHEN c.ingredients::text ILIKE CONCAT('%', token, '%') THEN 30
                                WHEN c.title ILIKE CONCAT('%', token, '%') THEN 24
                                WHEN c.category ILIKE CONCAT('%', token, '%') THEN 18
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
                    OR c.title ILIKE CONCAT('%', :title, '%')
                    OR c.category ILIKE CONCAT('%', :title, '%')
                    OR c.ingredients::text ILIKE CONCAT('%', :title, '%')
                    OR EXISTS (
                        SELECT 1
                        FROM unnest(CAST(:includeIngredients AS text[])) token
                        WHERE c.title ILIKE CONCAT('%', token, '%')
                           OR c.category ILIKE CONCAT('%', token, '%')
                           OR c.ingredients::text ILIKE CONCAT('%', token, '%')
                    )
                  )
              AND (
                    :category IS NULL
                    OR c.category ILIKE CONCAT('%', :category, '%')
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
