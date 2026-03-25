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
            c.recipe_id AS recipeId,
            c.title AS title,
            c.image AS image,
            c.category AS category,
            c.ingredients_count AS ingredientsCount,
            c.instructions_count AS instructionsCount,
            c.nutritions::text AS nutritionsJson,
            c.times::text AS timesJson,
            to_json(c.block_diet_keys)::text AS blockDietKeysJson,
            to_json(c.block_allergy_keys)::text AS blockAllergyKeysJson,
            to_json(c.block_health_keys)::text AS blockHealthKeysJson,
            to_json(c.caution_health_keys)::text AS cautionHealthKeysJson
        FROM cookbook_wh.card_search_mv c
        WHERE (:lang IS NULL OR c.lang = :lang)
          AND (:title IS NULL OR c.title ILIKE CONCAT('%', :title, '%'))
          AND (:category IS NULL OR c.category ILIKE CONCAT('%', :category, '%'))
          AND (
                COALESCE(cardinality(CAST(:requiredDietKeys AS text[])), 0) = 0
                OR NOT (c.block_diet_keys && CAST(:requiredDietKeys AS text[]))
              )
          AND (
                COALESCE(cardinality(CAST(:allergyKeys AS text[])), 0) = 0
                OR NOT (c.block_allergy_keys && CAST(:allergyKeys AS text[]))
              )
          AND (
                COALESCE(cardinality(CAST(:healthConditionKeys AS text[])), 0) = 0
                OR NOT (c.block_health_keys && CAST(:healthConditionKeys AS text[]))
              )
        ORDER BY
          (
            SELECT COUNT(*)
            FROM unnest(c.caution_health_keys) k
            WHERE k = ANY(CAST(:preferredHealthKeys AS text[]))
          ) DESC,
          CASE WHEN :sortBy = 'recipe_id'          AND :sortDir = 'asc'  THEN c.recipe_id END ASC,
          CASE WHEN :sortBy = 'recipe_id'          AND :sortDir = 'desc' THEN c.recipe_id END DESC,
          CASE WHEN :sortBy = 'title'              AND :sortDir = 'asc'  THEN c.title END ASC,
          CASE WHEN :sortBy = 'title'              AND :sortDir = 'desc' THEN c.title END DESC,
          CASE WHEN :sortBy = 'category'           AND :sortDir = 'asc'  THEN c.category END ASC,
          CASE WHEN :sortBy = 'category'           AND :sortDir = 'desc' THEN c.category END DESC,
          CASE WHEN :sortBy = 'ingredients_count'  AND :sortDir = 'asc'  THEN c.ingredients_count END ASC,
          CASE WHEN :sortBy = 'ingredients_count'  AND :sortDir = 'desc' THEN c.ingredients_count END DESC,
          CASE WHEN :sortBy = 'instructions_count' AND :sortDir = 'asc'  THEN c.instructions_count END ASC,
          CASE WHEN :sortBy = 'instructions_count' AND :sortDir = 'desc' THEN c.instructions_count END DESC,
          c.recipe_id DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<RecipeCardListRow> findRecipes(
            @Param("lang") String lang,
            @Param("title") String title,
            @Param("category") String category,
            @Param("requiredDietKeys") String[] requiredDietKeys,
            @Param("preferredHealthKeys") String[] preferredHealthKeys,
            @Param("allergyKeys") String[] allergyKeys,
            @Param("healthConditionKeys") String[] healthConditionKeys,
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
