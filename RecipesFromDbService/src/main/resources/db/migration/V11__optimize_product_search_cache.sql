DROP MATERIALIZED VIEW IF EXISTS cookbook_wh.product_detail_mv;

CREATE INDEX IF NOT EXISTS idx_product_search_mv_name_prefix
    ON cookbook_wh.product_search_mv (name_search_text text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_product_search_mv_brand_prefix
    ON cookbook_wh.product_search_mv (brand_search_text text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_product_search_mv_search_tsv
    ON cookbook_wh.product_search_mv
    USING gin (
        to_tsvector(
            'simple',
            coalesce(name_search_text, '')
                || ' '
                || coalesce(brand_search_text, '')
                || ' '
                || coalesce(search_text, '')
        )
    );

CREATE INDEX IF NOT EXISTS idx_product_search_mv_product_name_code
    ON cookbook_wh.product_search_mv (product_name, code);

CREATE INDEX IF NOT EXISTS idx_product_search_mv_country_product_name_code
    ON cookbook_wh.product_search_mv (country_search_text, product_name, code);

CREATE OR REPLACE FUNCTION cookbook_wh.refresh_product_catalog_cache()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    REFRESH MATERIALIZED VIEW cookbook_wh.product_search_mv;
    ANALYZE cookbook_wh.product_search_mv;
END;
$$;

DO $$
DECLARE
    target_owner TEXT;
    grantee_name TEXT;
BEGIN
    SELECT pg_get_userbyid(c.relowner)
    INTO target_owner
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'cookbook_wh'
      AND c.relname = 'product_catalog'
      AND c.relkind IN ('r', 'p')
    LIMIT 1;

    IF target_owner IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'cookbook_wh'
              AND c.relname = 'product_search_mv'
              AND c.relkind = 'm'
        ) THEN
            EXECUTE format(
                'ALTER MATERIALIZED VIEW cookbook_wh.product_search_mv OWNER TO %I',
                target_owner
            );
        END IF;

        IF EXISTS (
            SELECT 1
            FROM pg_proc p
            JOIN pg_namespace n ON n.oid = p.pronamespace
            WHERE n.nspname = 'cookbook_wh'
              AND p.proname = 'refresh_product_catalog_cache'
              AND pg_get_function_identity_arguments(p.oid) = ''
        ) THEN
            EXECUTE format(
                'ALTER FUNCTION cookbook_wh.refresh_product_catalog_cache() OWNER TO %I',
                target_owner
            );
        END IF;
    END IF;

    FOR grantee_name IN
        SELECT DISTINCT grantee
        FROM information_schema.role_table_grants
        WHERE table_schema = 'cookbook_wh'
          AND table_name = 'product_catalog'
    LOOP
        IF EXISTS (
            SELECT 1
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'cookbook_wh'
              AND c.relname = 'product_search_mv'
              AND c.relkind = 'm'
        ) THEN
            EXECUTE format(
                'GRANT SELECT ON cookbook_wh.product_search_mv TO %I',
                grantee_name
            );
        END IF;

        IF EXISTS (
            SELECT 1
            FROM pg_proc p
            JOIN pg_namespace n ON n.oid = p.pronamespace
            WHERE n.nspname = 'cookbook_wh'
              AND p.proname = 'refresh_product_catalog_cache'
              AND pg_get_function_identity_arguments(p.oid) = ''
        ) THEN
            EXECUTE format(
                'GRANT EXECUTE ON FUNCTION cookbook_wh.refresh_product_catalog_cache() TO %I',
                grantee_name
            );
        END IF;
    END LOOP;
END
$$;

ANALYZE cookbook_wh.product_search_mv;
