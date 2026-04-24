DO $$
DECLARE
    target_owner text;
    role_name text;
BEGIN
    SELECT pg_get_userbyid(c.relowner)
    INTO target_owner
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'cookbook_wh'
      AND c.relname = 'product_catalog'
      AND c.relkind = 'r'
    LIMIT 1;

    IF target_owner IS NULL OR target_owner = '' THEN
        target_owner := current_user;
    END IF;

    EXECUTE format(
        'ALTER MATERIALIZED VIEW cookbook_wh.product_search_mv OWNER TO %I',
        target_owner
    );
    EXECUTE format(
        'ALTER MATERIALIZED VIEW cookbook_wh.product_detail_mv OWNER TO %I',
        target_owner
    );
    EXECUTE format(
        'ALTER FUNCTION cookbook_wh.refresh_product_catalog_cache() OWNER TO %I',
        target_owner
    );

    FOR role_name IN
        SELECT DISTINCT grantee
        FROM information_schema.role_table_grants
        WHERE table_schema = 'cookbook_wh'
          AND table_name = 'product_catalog'
          AND privilege_type = 'SELECT'
    LOOP
        EXECUTE format(
            'GRANT SELECT ON TABLE cookbook_wh.product_search_mv TO %I',
            role_name
        );
        EXECUTE format(
            'GRANT SELECT ON TABLE cookbook_wh.product_detail_mv TO %I',
            role_name
        );
        EXECUTE format(
            'GRANT EXECUTE ON FUNCTION cookbook_wh.refresh_product_catalog_cache() TO %I',
            role_name
        );
    END LOOP;
END
$$;
