-- Cleanup migration for redundant legacy objects.
-- Removes duplicate normalization tables created by mistake and drops the
-- redundant materialized view recipe_constraints_search_mv.
-- The search refresh function is updated so the database stays consistent
-- after the materialized view is removed.

-- 1. Drop duplicate legacy tables that are no longer used anywhere in code.
DROP TABLE IF EXISTS cookbook_wh.recipe_ingredients CASCADE;
DROP TABLE IF EXISTS cookbook_wh.recipe_instructions CASCADE;
DROP TABLE IF EXISTS cookbook_wh.recipe_nutritions CASCADE;
DROP TABLE IF EXISTS cookbook_wh.recipe_times CASCADE;

-- 2. Drop the redundant materialized view. Java code reads from card_search_mv,
-- and the remaining constraints data stays available in recipe_constraints.
DROP MATERIALIZED VIEW IF EXISTS cookbook_wh.recipe_constraints_search_mv CASCADE;

-- 3. Keep the search refresh function valid after removing the redundant view.
CREATE OR REPLACE FUNCTION cookbook_wh.refresh_recipe_search_cache()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM cookbook_wh.refresh_recipe_constraints();
    REFRESH MATERIALIZED VIEW cookbook_wh.card_search_mv;
    ANALYZE cookbook_wh.recipe_constraints;
    ANALYZE cookbook_wh.card_search_mv;
END;
$$;
