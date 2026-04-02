DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename = 'food_analysis'
    ) THEN
        EXECUTE format('ALTER TABLE public.food_analysis OWNER TO %I', current_user);
        EXECUTE format(
            'GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES, TRIGGER ON TABLE public.food_analysis TO %I',
            current_user
        );
    END IF;
END $$;
