CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS cookbook_wh.product_catalog (
    code TEXT PRIMARY KEY,
    product_name TEXT NOT NULL,
    generic_name TEXT,
    brand_name TEXT,
    quantity TEXT,
    serving_size TEXT,
    categories_text TEXT,
    countries_text TEXT,
    stores_text TEXT,
    ingredients_text TEXT,
    image_url TEXT,
    energy_kj_100g NUMERIC(12, 2),
    calories_kcal_100g NUMERIC(12, 2),
    fats_100g NUMERIC(12, 2),
    saturated_fat_100g NUMERIC(12, 2),
    carbohydrates_100g NUMERIC(12, 2),
    proteins_100g NUMERIC(12, 2),
    fiber_100g NUMERIC(12, 2),
    sugars_100g NUMERIC(12, 2),
    salt_100g NUMERIC(12, 4),
    sodium_100g NUMERIC(12, 4),
    search_text TEXT NOT NULL,
    name_search_text TEXT NOT NULL,
    brand_search_text TEXT NOT NULL DEFAULT '',
    country_search_text TEXT NOT NULL DEFAULT '',
    source TEXT NOT NULL DEFAULT 'OPEN_FOOD_FACTS',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_catalog_name_trgm
    ON cookbook_wh.product_catalog USING gin (name_search_text cookbook_wh.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_product_catalog_brand_trgm
    ON cookbook_wh.product_catalog USING gin (brand_search_text cookbook_wh.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_product_catalog_search_trgm
    ON cookbook_wh.product_catalog USING gin (search_text cookbook_wh.gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_product_catalog_country
    ON cookbook_wh.product_catalog (country_search_text);
