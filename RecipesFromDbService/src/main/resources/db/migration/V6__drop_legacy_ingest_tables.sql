-- Drop legacy crawl / ingest / raw parsing tables that are not used by the
-- current RecipesFromDbService code path.
-- These objects were backed up before cleanup.

-- Legacy views depending on obsolete quality/tag/crawl tables.
DROP VIEW IF EXISTS cookbook_wh.crawl_overview_view CASCADE;
DROP VIEW IF EXISTS cookbook_wh.recipe_card_view CASCADE;
DROP VIEW IF EXISTS cookbook_wh.recipe_filter_facets_view CASCADE;
DROP VIEW IF EXISTS cookbook_wh.recipe_full_view CASCADE;
DROP VIEW IF EXISTS cookbook_wh.recipe_quality_view CASCADE;
DROP VIEW IF EXISTS cookbook_wh.recipe_search_view CASCADE;

-- Raw crawl / ingest trace tables.
DROP TABLE IF EXISTS cookbook_wh.crawl_logs;
DROP TABLE IF EXISTS cookbook_wh.ingest_metrics;
DROP TABLE IF EXISTS cookbook_wh.parse_errors;
DROP TABLE IF EXISTS cookbook_wh.skipped_pages;
DROP TABLE IF EXISTS cookbook_wh.crawl_state;
DROP TABLE IF EXISTS cookbook_wh.crawl_runs;
DROP TABLE IF EXISTS cookbook_wh.page_snapshots;

-- Legacy recipe-side raw parsing / metadata tables not used by current code.
DROP TABLE IF EXISTS cookbook_wh.equipment_items;
DROP TABLE IF EXISTS cookbook_wh.ingredient_items;
DROP TABLE IF EXISTS cookbook_wh.recipe_links;
DROP TABLE IF EXISTS cookbook_wh.recipe_quality;
DROP TABLE IF EXISTS cookbook_wh.recipe_tags;
DROP TABLE IF EXISTS cookbook_wh.tags;
