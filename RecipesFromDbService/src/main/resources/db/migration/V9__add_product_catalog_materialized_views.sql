DROP MATERIALIZED VIEW IF EXISTS cookbook_wh.product_search_mv;

CREATE MATERIALIZED VIEW cookbook_wh.product_search_mv AS
SELECT
    p.code,
    p.product_name,
    p.brand_name,
    p.quantity,
    p.serving_size,
    p.image_url,
    p.countries_text,
    p.calories_kcal_100g,
    p.proteins_100g,
    p.fats_100g,
    p.carbohydrates_100g,
    p.search_text,
    p.name_search_text,
    p.brand_search_text,
    p.country_search_text,
    p.updated_at
FROM cookbook_wh.product_catalog p
WITH DATA;

CREATE UNIQUE INDEX idx_product_search_mv_code
    ON cookbook_wh.product_search_mv (code);

CREATE INDEX idx_product_search_mv_name_trgm
    ON cookbook_wh.product_search_mv USING gin (name_search_text cookbook_wh.gin_trgm_ops);

CREATE INDEX idx_product_search_mv_brand_trgm
    ON cookbook_wh.product_search_mv USING gin (brand_search_text cookbook_wh.gin_trgm_ops);

CREATE INDEX idx_product_search_mv_search_trgm
    ON cookbook_wh.product_search_mv USING gin (search_text cookbook_wh.gin_trgm_ops);

CREATE INDEX idx_product_search_mv_country
    ON cookbook_wh.product_search_mv (country_search_text);

DROP MATERIALIZED VIEW IF EXISTS cookbook_wh.product_detail_mv;

CREATE MATERIALIZED VIEW cookbook_wh.product_detail_mv AS
SELECT
    p.code,
    p.product_name,
    p.generic_name,
    p.brand_name,
    p.quantity,
    p.serving_size,
    p.categories_text,
    p.countries_text,
    p.stores_text,
    p.ingredients_text,
    p.image_url,
    p.energy_kj_100g,
    p.calories_kcal_100g,
    p.proteins_100g,
    p.fats_100g,
    p.saturated_fat_100g,
    p.carbohydrates_100g,
    p.fiber_100g,
    p.sugars_100g,
    p.salt_100g,
    p.sodium_100g,
    p.source,
    p.updated_at
FROM cookbook_wh.product_catalog p
WITH DATA;

CREATE UNIQUE INDEX idx_product_detail_mv_code
    ON cookbook_wh.product_detail_mv (code);

CREATE OR REPLACE FUNCTION cookbook_wh.refresh_product_catalog_cache()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    REFRESH MATERIALIZED VIEW cookbook_wh.product_detail_mv;
    REFRESH MATERIALIZED VIEW cookbook_wh.product_search_mv;
    ANALYZE cookbook_wh.product_detail_mv;
    ANALYZE cookbook_wh.product_search_mv;
END;
$$;

ANALYZE cookbook_wh.product_detail_mv;
ANALYZE cookbook_wh.product_search_mv;
