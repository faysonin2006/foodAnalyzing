CREATE SCHEMA IF NOT EXISTS cookbook_wh;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION cookbook_wh.normalize_free_text(input_text TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
RETURNS NULL ON NULL INPUT
AS $$
    SELECT NULLIF(
        trim(
            regexp_replace(
                regexp_replace(lower(input_text), '[[:punct:]]+', ' ', 'g'),
                '\s+',
                ' ',
                'g'
            )
        ),
        ''
    );
$$;

CREATE OR REPLACE FUNCTION cookbook_wh.parse_numeric_amount(value_text TEXT)
RETURNS NUMERIC
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    WITH cleaned AS (
        SELECT NULLIF(
                   replace(
                       regexp_replace(coalesce(value_text, ''), '[^0-9,.-]', '', 'g'),
                       ',',
                       '.'
                   ),
                   ''
               ) AS numeric_text
    )
    SELECT CASE
               WHEN numeric_text ~ '^-?[0-9]+(\.[0-9]+)?$' THEN numeric_text::numeric
               ELSE NULL
           END
    FROM cleaned;
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_matviews
        WHERE schemaname = 'cookbook_wh'
          AND matviewname = 'recipe_constraints_search_mv'
    ) THEN
        EXECUTE 'DROP MATERIALIZED VIEW cookbook_wh.recipe_constraints_search_mv';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_matviews
        WHERE schemaname = 'cookbook_wh'
          AND matviewname = 'card_search_mv'
    ) THEN
        EXECUTE 'DROP MATERIALIZED VIEW cookbook_wh.card_search_mv';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_tables
        WHERE schemaname = 'cookbook_wh'
          AND tablename = 'card_search_mv'
    ) THEN
        EXECUTE 'DROP TABLE cookbook_wh.card_search_mv';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'card_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.card_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_constraints_recipe_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_constraints_recipe_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_card_detail_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_card_detail_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_rule_source_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_rule_source_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_nutrition_metrics_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_nutrition_metrics_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_nutrition_json_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_nutrition_json_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_instruction_agg_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_instruction_agg_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_ingredient_agg_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_ingredient_agg_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_category_agg_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_category_agg_view';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_views
        WHERE schemaname = 'cookbook_wh'
          AND viewname = 'recipe_notes_agg_view'
    ) THEN
        EXECUTE 'DROP VIEW cookbook_wh.recipe_notes_agg_view';
    END IF;
END;
$$;

DROP TABLE IF EXISTS cookbook_wh.recipe_constraints CASCADE;
DROP TABLE IF EXISTS cookbook_wh.recipe_constraint_rule_catalog CASCADE;

DO $$
BEGIN
    IF to_regclass('cookbook_wh.recipe_texts') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_recipe_texts_recipe_type ON cookbook_wh.recipe_texts(recipe_id, text_type)';
    END IF;

    IF to_regclass('cookbook_wh.recipe_images') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_recipe_images_recipe_position ON cookbook_wh.recipe_images(recipe_id, position, image_id)';
    END IF;

    IF to_regclass('cookbook_wh.recipe_categories') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_recipe_categories_recipe_id ON cookbook_wh.recipe_categories(recipe_id, category_id)';
    END IF;

    IF to_regclass('cookbook_wh.recipe_ingredient_facts') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_recipe_ingredient_facts_recipe_position ON cookbook_wh.recipe_ingredient_facts(recipe_id, position, ingredient_id, unit_id)';
    END IF;

    IF to_regclass('cookbook_wh.instruction_steps') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_instruction_steps_recipe_position ON cookbook_wh.instruction_steps(recipe_id, position)';
    END IF;

    IF to_regclass('cookbook_wh.nutrition_items') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_nutrition_items_recipe_nutrient ON cookbook_wh.nutrition_items(recipe_id, nutrient)';
    END IF;
END;
$$;

CREATE TABLE cookbook_wh.recipe_constraint_rule_catalog (
    rule_code TEXT PRIMARY KEY,
    constraint_type TEXT NOT NULL CHECK (constraint_type IN ('ALLERGY', 'DIET', 'HEALTH')),
    constraint_key TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('BLOCK', 'CAUTION', 'ALLOW')),
    rule_kind TEXT NOT NULL CHECK (rule_kind IN ('TEXT', 'NUTRITION')),
    match_scope TEXT CHECK (match_scope IN ('INGREDIENT', 'CATEGORY', 'COMBINED')),
    regex_pattern TEXT,
    metric_key TEXT,
    min_value NUMERIC,
    max_value NUMERIC,
    confidence NUMERIC(3,2) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    source TEXT NOT NULL,
    reason TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (
        (rule_kind = 'TEXT' AND match_scope IS NOT NULL AND regex_pattern IS NOT NULL AND metric_key IS NULL)
        OR
        (rule_kind = 'NUTRITION' AND match_scope IS NULL AND regex_pattern IS NULL AND metric_key IS NOT NULL)
    )
);

CREATE INDEX idx_recipe_constraint_rule_catalog_active
    ON cookbook_wh.recipe_constraint_rule_catalog(is_active, rule_kind, constraint_type, constraint_key, status, priority);

INSERT INTO cookbook_wh.recipe_constraint_rule_catalog (
    rule_code,
    constraint_type,
    constraint_key,
    status,
    rule_kind,
    match_scope,
    regex_pattern,
    metric_key,
    min_value,
    max_value,
    confidence,
    source,
    reason,
    priority
)
VALUES
    ('allergy_gluten_ingredient', 'ALLERGY', 'GLUTEN', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(gluten[^ ]*|wheat[^ ]*|spelt[^ ]*|farro[^ ]*|bulgur[^ ]*|semolina[^ ]*|barley[^ ]*|rye[^ ]*|triticale[^ ]*|seitan[^ ]*|breadcrumb[^ ]*|breadcrumbs|пшениц[^ ]*|ячмен[^ ]*|рож[^ ]*|глютен[^ ]*|мук[^ ]*|манк[^ ]*)( |$)', NULL, NULL, NULL, 0.98, 'rule:text:ingredient', 'contains gluten-bearing ingredients', 10),
    ('allergy_lactose_ingredient', 'ALLERGY', 'LACTOSE', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(lactose[^ ]*|milk[^ ]*|cheese[^ ]*|butter[^ ]*|cream[^ ]*|yogurt[^ ]*|kefir[^ ]*|whey[^ ]*|casein[^ ]*|молок[^ ]*|сыр[^ ]*|масл[^ ]*|сливк[^ ]*|йогурт[^ ]*|кефир[^ ]*|лактоз[^ ]*)( |$)', NULL, NULL, NULL, 0.96, 'rule:text:ingredient', 'contains dairy or lactose sources', 10),
    ('allergy_tree_nuts_ingredient', 'ALLERGY', 'TREE_NUTS', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(almond[^ ]*|walnut[^ ]*|hazelnut[^ ]*|cashew[^ ]*|pecan[^ ]*|pistachio[^ ]*|macadamia[^ ]*|brazil nut[^ ]*|pine nut[^ ]*|миндал[^ ]*|грецк[^ ]*|фундук[^ ]*|кешью[^ ]*|пекан[^ ]*|фисташ[^ ]*)( |$)', NULL, NULL, NULL, 0.98, 'rule:text:ingredient', 'contains tree nuts', 10),
    ('allergy_peanuts_ingredient', 'ALLERGY', 'PEANUTS', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(peanut[^ ]*|groundnut[^ ]*|арахис[^ ]*)( |$)', NULL, NULL, NULL, 0.99, 'rule:text:ingredient', 'contains peanuts', 10),
    ('allergy_eggs_ingredient', 'ALLERGY', 'EGGS', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(egg[^ ]*|яйц[^ ]*)( |$)', NULL, NULL, NULL, 0.97, 'rule:text:ingredient', 'contains eggs', 10),
    ('allergy_soy_ingredient', 'ALLERGY', 'SOY', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(soy[^ ]*|soybean[^ ]*|tofu[^ ]*|edamame[^ ]*|miso[^ ]*|tamari[^ ]*|соев[^ ]*|тофу[^ ]*)( |$)', NULL, NULL, NULL, 0.97, 'rule:text:ingredient', 'contains soy', 10),
    ('allergy_fish_ingredient', 'ALLERGY', 'FISH', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(fish[^ ]*|salmon[^ ]*|tuna[^ ]*|cod[^ ]*|anchov[^ ]*|sardin[^ ]*|mackerel[^ ]*|herring[^ ]*|рыб[^ ]*|лосос[^ ]*|тунец[^ ]*|треск[^ ]*|анчоус[^ ]*|сардин[^ ]*)( |$)', NULL, NULL, NULL, 0.98, 'rule:text:ingredient', 'contains fish', 10),
    ('allergy_shellfish_ingredient', 'ALLERGY', 'SHELLFISH', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(shellfish[^ ]*|shrimp[^ ]*|prawn[^ ]*|crab[^ ]*|lobster[^ ]*|clam[^ ]*|mussel[^ ]*|oyster[^ ]*|scallop[^ ]*|кревет[^ ]*|краб[^ ]*|лобстер[^ ]*|мид[^ ]*|устриц[^ ]*)( |$)', NULL, NULL, NULL, 0.98, 'rule:text:ingredient', 'contains shellfish', 10),
    ('allergy_mustard_ingredient', 'ALLERGY', 'MUSTARD', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(mustard[^ ]*|горчиц[^ ]*)( |$)', NULL, NULL, NULL, 0.95, 'rule:text:ingredient', 'contains mustard', 10),
    ('allergy_sesame_ingredient', 'ALLERGY', 'SESAME', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(sesame[^ ]*|tahini[^ ]*|кунжут[^ ]*|тахин[^ ]*)( |$)', NULL, NULL, NULL, 0.96, 'rule:text:ingredient', 'contains sesame', 10),

    ('diet_vegetarian_block', 'DIET', 'VEGETARIAN', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(beef[^ ]*|pork[^ ]*|chicken[^ ]*|turkey[^ ]*|lamb[^ ]*|veal[^ ]*|ham[^ ]*|bacon[^ ]*|sausage[^ ]*|salami[^ ]*|prosciutto[^ ]*|fish[^ ]*|seafood[^ ]*|shrimp[^ ]*|crab[^ ]*|lobster[^ ]*|mussel[^ ]*|oyster[^ ]*|anchov[^ ]*|fish sauce|gelatin[^ ]*|stock[^ ]*|broth[^ ]*|говяд[^ ]*|свин[^ ]*|куриц[^ ]*|индейк[^ ]*|баран[^ ]*|ветчин[^ ]*|бекон[^ ]*|колбас[^ ]*|рыб[^ ]*|морепродукт[^ ]*|кревет[^ ]*|краб[^ ]*|лобстер[^ ]*|мид[^ ]*|устриц[^ ]*|желатин[^ ]*|бульон[^ ]*)( |$)', NULL, NULL, NULL, 0.98, 'rule:text:ingredient', 'contains meat, fish, seafood, gelatin, or stock', 20),
    ('diet_vegan_block', 'DIET', 'VEGAN', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(beef[^ ]*|pork[^ ]*|chicken[^ ]*|turkey[^ ]*|lamb[^ ]*|veal[^ ]*|ham[^ ]*|bacon[^ ]*|sausage[^ ]*|salami[^ ]*|prosciutto[^ ]*|fish[^ ]*|seafood[^ ]*|shrimp[^ ]*|crab[^ ]*|lobster[^ ]*|mussel[^ ]*|oyster[^ ]*|anchov[^ ]*|fish sauce|egg[^ ]*|milk[^ ]*|cheese[^ ]*|butter[^ ]*|cream[^ ]*|yogurt[^ ]*|kefir[^ ]*|whey[^ ]*|casein[^ ]*|honey[^ ]*|gelatin[^ ]*|говяд[^ ]*|свин[^ ]*|куриц[^ ]*|индейк[^ ]*|баран[^ ]*|рыб[^ ]*|морепродукт[^ ]*|яйц[^ ]*|молок[^ ]*|сыр[^ ]*|масл[^ ]*|сливк[^ ]*|йогурт[^ ]*|кефир[^ ]*|мед[^ ]*|желатин[^ ]*)( |$)', NULL, NULL, NULL, 0.98, 'rule:text:ingredient', 'contains animal-derived ingredients', 20),
    ('diet_pescatarian_block', 'DIET', 'PESCATARIAN', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(beef[^ ]*|pork[^ ]*|chicken[^ ]*|turkey[^ ]*|lamb[^ ]*|veal[^ ]*|ham[^ ]*|bacon[^ ]*|sausage[^ ]*|salami[^ ]*|prosciutto[^ ]*|gelatin[^ ]*|stock[^ ]*|broth[^ ]*|говяд[^ ]*|свин[^ ]*|куриц[^ ]*|индейк[^ ]*|баран[^ ]*|ветчин[^ ]*|бекон[^ ]*|колбас[^ ]*|желатин[^ ]*|бульон[^ ]*)( |$)', NULL, NULL, NULL, 0.97, 'rule:text:ingredient', 'contains land-animal meat or gelatin', 20),
    ('diet_paleo_block', 'DIET', 'PALEO', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(wheat[^ ]*|spelt[^ ]*|farro[^ ]*|bulgur[^ ]*|semolina[^ ]*|barley[^ ]*|rye[^ ]*|flour[^ ]*|rice[^ ]*|corn[^ ]*|bean[^ ]*|beans|lentil[^ ]*|pea[^ ]*|peas|chickpea[^ ]*|soy[^ ]*|tofu[^ ]*|milk[^ ]*|cheese[^ ]*|yogurt[^ ]*|sugar[^ ]*|pasta[^ ]*|noodle[^ ]*|oat[^ ]*|пшениц[^ ]*|ячмен[^ ]*|рож[^ ]*|мук[^ ]*|рис[^ ]*|кукуруз[^ ]*|боб[^ ]*|чечев[^ ]*|нут[^ ]*|соев[^ ]*|тофу[^ ]*|молок[^ ]*|сыр[^ ]*|йогурт[^ ]*|сахар[^ ]*|макарон[^ ]*|овсян[^ ]*)( |$)', NULL, NULL, NULL, 0.92, 'rule:text:ingredient', 'contains grains, legumes, dairy, or refined sugar', 20),
    ('diet_halal_block', 'DIET', 'HALAL', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(pork[^ ]*|ham[^ ]*|bacon[^ ]*|lard[^ ]*|wine[^ ]*|beer[^ ]*|vodka[^ ]*|rum[^ ]*|brandy[^ ]*|whisk(e)?y[^ ]*|свин[^ ]*|ветчин[^ ]*|бекон[^ ]*|сало[^ ]*|вино[^ ]*|пиво[^ ]*|водк[^ ]*|ром[^ ]*|бренди[^ ]*|виски[^ ]*)( |$)', NULL, NULL, NULL, 0.95, 'rule:text:ingredient', 'contains pork or alcohol', 20),
    ('diet_kosher_block', 'DIET', 'KOSHER', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(pork[^ ]*|ham[^ ]*|bacon[^ ]*|lard[^ ]*|shellfish[^ ]*|shrimp[^ ]*|prawn[^ ]*|crab[^ ]*|lobster[^ ]*|clam[^ ]*|mussel[^ ]*|oyster[^ ]*|scallop[^ ]*|свин[^ ]*|ветчин[^ ]*|бекон[^ ]*|сало[^ ]*|кревет[^ ]*|краб[^ ]*|лобстер[^ ]*|мид[^ ]*|устриц[^ ]*)( |$)', NULL, NULL, NULL, 0.95, 'rule:text:ingredient', 'contains non-kosher pork or shellfish ingredients', 20),
    ('diet_gluten_free_block', 'DIET', 'GLUTEN_FREE', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(gluten[^ ]*|wheat[^ ]*|spelt[^ ]*|farro[^ ]*|bulgur[^ ]*|semolina[^ ]*|barley[^ ]*|rye[^ ]*|triticale[^ ]*|seitan[^ ]*|breadcrumb[^ ]*|breadcrumbs|пшениц[^ ]*|ячмен[^ ]*|рож[^ ]*|глютен[^ ]*|мук[^ ]*|манк[^ ]*)( |$)', NULL, NULL, NULL, 0.98, 'rule:text:ingredient', 'contains gluten-bearing ingredients', 20),
    ('diet_lactose_free_block', 'DIET', 'LACTOSE_FREE', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(lactose[^ ]*|milk[^ ]*|cheese[^ ]*|butter[^ ]*|cream[^ ]*|yogurt[^ ]*|kefir[^ ]*|whey[^ ]*|casein[^ ]*|молок[^ ]*|сыр[^ ]*|масл[^ ]*|сливк[^ ]*|йогурт[^ ]*|кефир[^ ]*|лактоз[^ ]*)( |$)', NULL, NULL, NULL, 0.96, 'rule:text:ingredient', 'contains lactose or dairy ingredients', 20),

    ('diet_vegetarian_allow', 'DIET', 'VEGETARIAN', 'ALLOW', 'TEXT', 'COMBINED', '(^| )(vegetarian[^ ]*|вегетариан[^ ]*)( |$)', NULL, NULL, NULL, 0.85, 'rule:text:combined', 'recipe explicitly presented as vegetarian', 90),
    ('diet_vegan_allow', 'DIET', 'VEGAN', 'ALLOW', 'TEXT', 'COMBINED', '(^| )(vegan[^ ]*|plant based|веган[^ ]*)( |$)', NULL, NULL, NULL, 0.88, 'rule:text:combined', 'recipe explicitly presented as vegan', 90),
    ('diet_keto_allow', 'DIET', 'KETO', 'ALLOW', 'TEXT', 'COMBINED', '(^| )(keto[^ ]*|ketogenic[^ ]*|кето[^ ]*)( |$)', NULL, NULL, NULL, 0.88, 'rule:text:combined', 'recipe explicitly presented as keto', 90),
    ('diet_paleo_allow', 'DIET', 'PALEO', 'ALLOW', 'TEXT', 'COMBINED', '(^| )(paleo[^ ]*|палео[^ ]*)( |$)', NULL, NULL, NULL, 0.85, 'rule:text:combined', 'recipe explicitly presented as paleo', 90),
    ('diet_gluten_free_allow', 'DIET', 'GLUTEN_FREE', 'ALLOW', 'TEXT', 'COMBINED', '(^| )(gluten free|без глютен[^ ]*)( |$)', NULL, NULL, NULL, 0.90, 'rule:text:combined', 'recipe explicitly presented as gluten free', 90),
    ('diet_lactose_free_allow', 'DIET', 'LACTOSE_FREE', 'ALLOW', 'TEXT', 'COMBINED', '(^| )(lactose free|dairy free|без лактоз[^ ]*|без молоч[^ ]*)( |$)', NULL, NULL, NULL, 0.90, 'rule:text:combined', 'recipe explicitly presented as lactose or dairy free', 90),
    ('diet_halal_allow', 'DIET', 'HALAL', 'ALLOW', 'TEXT', 'COMBINED', '(^| )(halal[^ ]*|халяль[^ ]*)( |$)', NULL, NULL, NULL, 0.85, 'rule:text:combined', 'recipe explicitly presented as halal', 90),
    ('diet_kosher_allow', 'DIET', 'KOSHER', 'ALLOW', 'TEXT', 'COMBINED', '(^| )(kosher[^ ]*|кошер[^ ]*)( |$)', NULL, NULL, NULL, 0.85, 'rule:text:combined', 'recipe explicitly presented as kosher', 90),

    ('health_gastritis_caution', 'HEALTH', 'GASTRITIS', 'CAUTION', 'TEXT', 'COMBINED', '(^| )(spicy[^ ]*|chili[^ ]*|pepper[^ ]*|hot sauce|fried[^ ]*|deep fry[^ ]*|vinegar[^ ]*|citrus[^ ]*|lemon[^ ]*|lime[^ ]*|tomato[^ ]*|coffee[^ ]*|smoked[^ ]*|pickled[^ ]*|остр[^ ]*|жарен[^ ]*|уксус[^ ]*|цитрус[^ ]*|лимон[^ ]*|томат[^ ]*|кофе[^ ]*|копчен[^ ]*|марин[^ ]*)( |$)', NULL, NULL, NULL, 0.82, 'rule:text:combined', 'recipe contains spicy, acidic, fried, smoked, or pickled components', 30),
    ('health_pregnancy_caution', 'HEALTH', 'PREGNANCY', 'CAUTION', 'TEXT', 'COMBINED', '(^| )(raw egg|raw fish|sushi[^ ]*|unpasteur[^ ]*|alcohol[^ ]*|wine[^ ]*|beer[^ ]*|vodka[^ ]*|rum[^ ]*|soft cheese|runny egg|сырая рыб[^ ]*|сырое яйц[^ ]*|непастериз[^ ]*|алкогол[^ ]*|вино[^ ]*|пиво[^ ]*|мягк[^ ]* сыр[^ ]*)( |$)', NULL, NULL, NULL, 0.85, 'rule:text:combined', 'recipe includes raw, unpasteurized, or alcoholic components', 30),
    ('health_gout_block', 'HEALTH', 'GOUT', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(liver[^ ]*|kidney[^ ]*|anchov[^ ]*|sardin[^ ]*|beer[^ ]*|alcohol[^ ]*|печен[^ ]*|почки[^ ]*|анчоус[^ ]*|сардин[^ ]*|пиво[^ ]*|алкогол[^ ]*)( |$)', NULL, NULL, NULL, 0.88, 'rule:text:ingredient', 'contains high-purine organ meats, anchovies, sardines, or alcohol', 30),
    ('health_gout_caution', 'HEALTH', 'GOUT', 'CAUTION', 'TEXT', 'INGREDIENT', '(^| )(beef[^ ]*|lamb[^ ]*|shellfish[^ ]*|shrimp[^ ]*|crab[^ ]*|lobster[^ ]*|говяд[^ ]*|баран[^ ]*|кревет[^ ]*|краб[^ ]*|лобстер[^ ]*)( |$)', NULL, NULL, NULL, 0.72, 'rule:text:ingredient', 'contains moderate purine ingredients', 30),
    ('health_celiac_block', 'HEALTH', 'CELIAC_DISEASE', 'BLOCK', 'TEXT', 'INGREDIENT', '(^| )(gluten[^ ]*|wheat[^ ]*|spelt[^ ]*|farro[^ ]*|bulgur[^ ]*|semolina[^ ]*|barley[^ ]*|rye[^ ]*|triticale[^ ]*|seitan[^ ]*|breadcrumb[^ ]*|breadcrumbs|пшениц[^ ]*|ячмен[^ ]*|рож[^ ]*|глютен[^ ]*|мук[^ ]*|манк[^ ]*)( |$)', NULL, NULL, NULL, 0.99, 'rule:text:ingredient', 'contains gluten and is unsuitable for celiac disease', 30),

    ('diet_keto_block_carbs', 'DIET', 'KETO', 'BLOCK', 'NUTRITION', NULL, NULL, 'carbs_g', 20, NULL, 0.88, 'rule:nutrition', 'carbohydrates exceed a practical keto threshold', 40),
    ('diet_keto_block_sugar', 'DIET', 'KETO', 'BLOCK', 'NUTRITION', NULL, NULL, 'sugar_g', 10, NULL, 0.88, 'rule:nutrition', 'sugars exceed a practical keto threshold', 40),

    ('health_diabetes_type_1_block_sugar', 'HEALTH', 'DIABETES_TYPE_1', 'BLOCK', 'NUTRITION', NULL, NULL, 'sugar_g', 18, NULL, 0.82, 'rule:nutrition', 'sugar load is high for type 1 diabetes meal planning', 40),
    ('health_diabetes_type_1_block_carbs', 'HEALTH', 'DIABETES_TYPE_1', 'BLOCK', 'NUTRITION', NULL, NULL, 'carbs_g', 45, NULL, 0.80, 'rule:nutrition', 'carbohydrate load is high for type 1 diabetes meal planning', 40),
    ('health_diabetes_type_1_caution_sugar', 'HEALTH', 'DIABETES_TYPE_1', 'CAUTION', 'NUTRITION', NULL, NULL, 'sugar_g', 12, 17.99, 0.68, 'rule:nutrition', 'sugar load may need insulin or portion adjustment', 41),
    ('health_diabetes_type_1_caution_carbs', 'HEALTH', 'DIABETES_TYPE_1', 'CAUTION', 'NUTRITION', NULL, NULL, 'carbs_g', 30, 44.99, 0.66, 'rule:nutrition', 'carbohydrate load may need insulin or portion adjustment', 41),

    ('health_diabetes_type_2_block_sugar', 'HEALTH', 'DIABETES_TYPE_2', 'BLOCK', 'NUTRITION', NULL, NULL, 'sugar_g', 12, NULL, 0.88, 'rule:nutrition', 'sugar load is high for type 2 diabetes', 40),
    ('health_diabetes_type_2_block_carbs', 'HEALTH', 'DIABETES_TYPE_2', 'BLOCK', 'NUTRITION', NULL, NULL, 'carbs_g', 35, NULL, 0.86, 'rule:nutrition', 'carbohydrate load is high for type 2 diabetes', 40),
    ('health_diabetes_type_2_caution_sugar', 'HEALTH', 'DIABETES_TYPE_2', 'CAUTION', 'NUTRITION', NULL, NULL, 'sugar_g', 8, 11.99, 0.74, 'rule:nutrition', 'sugar load is moderate and should be portioned carefully', 41),
    ('health_diabetes_type_2_caution_carbs', 'HEALTH', 'DIABETES_TYPE_2', 'CAUTION', 'NUTRITION', NULL, NULL, 'carbs_g', 25, 34.99, 0.72, 'rule:nutrition', 'carbohydrate load is moderate and should be portioned carefully', 41),

    ('health_insulin_resistance_block_sugar', 'HEALTH', 'INSULIN_RESISTANCE', 'BLOCK', 'NUTRITION', NULL, NULL, 'sugar_g', 12, NULL, 0.88, 'rule:nutrition', 'sugar load is high for insulin resistance', 40),
    ('health_insulin_resistance_block_carbs', 'HEALTH', 'INSULIN_RESISTANCE', 'BLOCK', 'NUTRITION', NULL, NULL, 'carbs_g', 35, NULL, 0.86, 'rule:nutrition', 'carbohydrate load is high for insulin resistance', 40),
    ('health_insulin_resistance_caution_sugar', 'HEALTH', 'INSULIN_RESISTANCE', 'CAUTION', 'NUTRITION', NULL, NULL, 'sugar_g', 8, 11.99, 0.74, 'rule:nutrition', 'sugar load is moderate and should be portioned carefully', 41),
    ('health_insulin_resistance_caution_carbs', 'HEALTH', 'INSULIN_RESISTANCE', 'CAUTION', 'NUTRITION', NULL, NULL, 'carbs_g', 25, 34.99, 0.72, 'rule:nutrition', 'carbohydrate load is moderate and should be portioned carefully', 41),

    ('health_hypertension_block_sodium', 'HEALTH', 'HYPERTENSION', 'BLOCK', 'NUTRITION', NULL, NULL, 'sodium_mg', 600, NULL, 0.92, 'rule:nutrition', 'sodium is high for hypertension', 40),
    ('health_hypertension_caution_sodium', 'HEALTH', 'HYPERTENSION', 'CAUTION', 'NUTRITION', NULL, NULL, 'sodium_mg', 350, 599.99, 0.78, 'rule:nutrition', 'sodium is moderately high for hypertension', 41),

    ('health_cholesterol_block_cholesterol', 'HEALTH', 'HIGH_CHOLESTEROL', 'BLOCK', 'NUTRITION', NULL, NULL, 'cholesterol_mg', 100, NULL, 0.88, 'rule:nutrition', 'cholesterol is high', 40),
    ('health_cholesterol_block_satfat', 'HEALTH', 'HIGH_CHOLESTEROL', 'BLOCK', 'NUTRITION', NULL, NULL, 'saturated_fat_g', 8, NULL, 0.86, 'rule:nutrition', 'saturated fat is high', 40),
    ('health_cholesterol_caution_cholesterol', 'HEALTH', 'HIGH_CHOLESTEROL', 'CAUTION', 'NUTRITION', NULL, NULL, 'cholesterol_mg', 60, 99.99, 0.72, 'rule:nutrition', 'cholesterol is moderately high', 41),
    ('health_cholesterol_caution_satfat', 'HEALTH', 'HIGH_CHOLESTEROL', 'CAUTION', 'NUTRITION', NULL, NULL, 'saturated_fat_g', 5, 7.99, 0.70, 'rule:nutrition', 'saturated fat is moderately high', 41),

    ('health_kidney_block_sodium', 'HEALTH', 'KIDNEY_DISEASE', 'BLOCK', 'NUTRITION', NULL, NULL, 'sodium_mg', 600, NULL, 0.90, 'rule:nutrition', 'sodium is high for kidney disease', 40),
    ('health_kidney_block_protein', 'HEALTH', 'KIDNEY_DISEASE', 'BLOCK', 'NUTRITION', NULL, NULL, 'protein_g', 30, NULL, 0.78, 'rule:nutrition', 'protein is high for kidney disease', 40),
    ('health_kidney_block_potassium', 'HEALTH', 'KIDNEY_DISEASE', 'BLOCK', 'NUTRITION', NULL, NULL, 'potassium_mg', 700, NULL, 0.82, 'rule:nutrition', 'potassium is high for kidney disease', 40),
    ('health_kidney_block_phosphorus', 'HEALTH', 'KIDNEY_DISEASE', 'BLOCK', 'NUTRITION', NULL, NULL, 'phosphorus_mg', 350, NULL, 0.82, 'rule:nutrition', 'phosphorus is high for kidney disease', 40),
    ('health_kidney_caution_sodium', 'HEALTH', 'KIDNEY_DISEASE', 'CAUTION', 'NUTRITION', NULL, NULL, 'sodium_mg', 400, 599.99, 0.74, 'rule:nutrition', 'sodium is moderately high for kidney disease', 41),
    ('health_kidney_caution_protein', 'HEALTH', 'KIDNEY_DISEASE', 'CAUTION', 'NUTRITION', NULL, NULL, 'protein_g', 25, 29.99, 0.68, 'rule:nutrition', 'protein is moderately high for kidney disease', 41),
    ('health_kidney_caution_potassium', 'HEALTH', 'KIDNEY_DISEASE', 'CAUTION', 'NUTRITION', NULL, NULL, 'potassium_mg', 450, 699.99, 0.72, 'rule:nutrition', 'potassium is moderately high for kidney disease', 41),
    ('health_kidney_caution_phosphorus', 'HEALTH', 'KIDNEY_DISEASE', 'CAUTION', 'NUTRITION', NULL, NULL, 'phosphorus_mg', 250, 349.99, 0.72, 'rule:nutrition', 'phosphorus is moderately high for kidney disease', 41)
ON CONFLICT (rule_code) DO NOTHING;

CREATE TABLE cookbook_wh.recipe_constraints (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES cookbook_wh.recipes(id) ON DELETE CASCADE,
    rule_code TEXT REFERENCES cookbook_wh.recipe_constraint_rule_catalog(rule_code) ON DELETE SET NULL,
    constraint_type TEXT NOT NULL CHECK (constraint_type IN ('ALLERGY', 'DIET', 'HEALTH')),
    constraint_key TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('BLOCK', 'CAUTION', 'ALLOW')),
    confidence NUMERIC(3,2) NOT NULL DEFAULT 0.70 CHECK (confidence >= 0 AND confidence <= 1),
    source TEXT NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_recipe_constraints_rule_code_unique
    ON cookbook_wh.recipe_constraints(recipe_id, rule_code)
    WHERE rule_code IS NOT NULL;

CREATE UNIQUE INDEX idx_recipe_constraints_manual_unique
    ON cookbook_wh.recipe_constraints(recipe_id, constraint_type, constraint_key, status, source)
    WHERE rule_code IS NULL;

CREATE INDEX idx_recipe_constraints_lookup
    ON cookbook_wh.recipe_constraints(constraint_type, constraint_key, status, recipe_id);

CREATE INDEX idx_recipe_constraints_recipe
    ON cookbook_wh.recipe_constraints(recipe_id);

CREATE INDEX idx_recipe_constraints_source
    ON cookbook_wh.recipe_constraints(source, recipe_id);

CREATE OR REPLACE VIEW cookbook_wh.recipe_notes_agg_view AS
SELECT
    rt.recipe_id,
    string_agg(rt.content, '; ') AS notes_text
FROM cookbook_wh.recipe_texts rt
WHERE rt.text_type = 'notes'
  AND nullif(trim(coalesce(rt.content, '')), '') IS NOT NULL
GROUP BY rt.recipe_id;

CREATE OR REPLACE VIEW cookbook_wh.recipe_category_agg_view AS
SELECT
    rc.recipe_id,
    min(c.name) AS primary_category,
    string_agg(DISTINCT c.name, ' ') AS categories_text
FROM cookbook_wh.recipe_categories rc
JOIN cookbook_wh.categories c
  ON c.id = rc.category_id
GROUP BY rc.recipe_id;

CREATE OR REPLACE VIEW cookbook_wh.recipe_ingredient_agg_view AS
SELECT
    rif.recipe_id,
    jsonb_agg(
        jsonb_build_object(
            'position', rif.position,
            'quantity_text', rif.quantity_text,
            'quantity_value', rif.quantity_value,
            'unit', u.display_name,
            'ingredient', ic.display_name,
            'note', rif.note,
            'raw_text', rif.raw_text
        )
        ORDER BY rif.position
    ) AS ingredients_json,
    string_agg(coalesce(ic.display_name, rif.raw_text), ' ' ORDER BY rif.position) AS ingredients_text
FROM cookbook_wh.recipe_ingredient_facts rif
LEFT JOIN cookbook_wh.units u
  ON u.id = rif.unit_id
LEFT JOIN cookbook_wh.ingredients_catalog ic
  ON ic.id = rif.ingredient_id
GROUP BY rif.recipe_id;

CREATE OR REPLACE VIEW cookbook_wh.recipe_instruction_agg_view AS
SELECT
    s.recipe_id,
    jsonb_agg(
        jsonb_build_object(
            'position', s.position,
            'text', s.raw_text,
            'duration_hint', s.duration_hint,
            'temperature_hint', s.temperature_hint
        )
        ORDER BY s.position
    ) AS steps_json
FROM cookbook_wh.instruction_steps s
GROUP BY s.recipe_id;

CREATE OR REPLACE VIEW cookbook_wh.recipe_nutrition_json_view AS
SELECT
    n.recipe_id,
    jsonb_agg(
        jsonb_build_object(
            'nutrient', n.nutrient,
            'amount', n.amount,
            'unit', n.unit,
            'raw_text', n.raw_text
        )
        ORDER BY n.nutrient
    ) AS nutritions_json
FROM cookbook_wh.nutrition_items n
GROUP BY n.recipe_id;

CREATE OR REPLACE VIEW cookbook_wh.recipe_nutrition_metrics_view AS
WITH nutrition_norm AS (
    SELECT
        n.recipe_id,
        cookbook_wh.normalize_free_text(n.nutrient) AS nutrient_text,
        lower(replace(replace(coalesce(n.unit, ''), 'μ', 'u'), 'µ', 'u')) AS unit_text,
        cookbook_wh.parse_numeric_amount(n.amount) AS raw_value
    FROM cookbook_wh.nutrition_items n
),
converted AS (
    SELECT
        recipe_id,
        nutrient_text,
        CASE
            WHEN unit_text IN ('mg') THEN raw_value
            WHEN unit_text IN ('g', 'gram', 'grams') THEN raw_value * 1000
            WHEN unit_text IN ('kg') THEN raw_value * 1000000
            WHEN unit_text IN ('ug', 'mcg') THEN raw_value / 1000
            ELSE raw_value
        END AS value_mg,
        CASE
            WHEN unit_text IN ('mg') THEN raw_value / 1000
            WHEN unit_text IN ('g', 'gram', 'grams') THEN raw_value
            WHEN unit_text IN ('kg') THEN raw_value * 1000
            WHEN unit_text IN ('ug', 'mcg') THEN raw_value / 1000000
            ELSE raw_value
        END AS value_g
    FROM nutrition_norm
    WHERE raw_value IS NOT NULL
)
SELECT
    recipe_id,
    max(CASE WHEN nutrient_text ~ '(^| )(protein|белок)( |$)' THEN value_g END) AS protein_g,
    max(CASE WHEN nutrient_text ~ '(^| )(carbohydrate|carb[^ ]*|углевод[^ ]*)( |$)' THEN value_g END) AS carbs_g,
    max(CASE WHEN nutrient_text ~ '(^| )(sugar[^ ]*|сахар[^ ]*)( |$)' THEN value_g END) AS sugar_g,
    max(CASE WHEN nutrient_text ~ '(^| )(sodium|натрий)( |$)' THEN value_mg END) AS sodium_mg,
    max(CASE WHEN nutrient_text ~ '(^| )(cholesterol|холестерин)( |$)' THEN value_mg END) AS cholesterol_mg,
    max(CASE WHEN nutrient_text ~ '(^| )(saturated fat|saturatedfat|sat fat|насыщ[^ ]*)( |$)' THEN value_g END) AS saturated_fat_g,
    max(CASE WHEN nutrient_text ~ '(^| )(potassium|калий)( |$)' THEN value_mg END) AS potassium_mg,
    max(CASE WHEN nutrient_text ~ '(^| )(phosphorus|phosphate|фосфор[^ ]*)( |$)' THEN value_mg END) AS phosphorus_mg
FROM converted
GROUP BY recipe_id;

CREATE OR REPLACE VIEW cookbook_wh.recipe_rule_source_view AS
SELECT
    r.id AS recipe_id,
    r.lang,
    coalesce(r.title, '') AS title,
    coalesce(r.description, '') AS description,
    coalesce(n.notes_text, '') AS notes_text,
    coalesce(i.ingredients_text, '') AS ingredients_text,
    coalesce(c.categories_text, '') AS categories_text,
    coalesce(c.primary_category, '') AS primary_category,
    coalesce(cookbook_wh.normalize_free_text(r.title), '') AS title_search_text,
    coalesce(cookbook_wh.normalize_free_text(i.ingredients_text), '') AS ingredient_search_text,
    coalesce(cookbook_wh.normalize_free_text(c.categories_text), '') AS category_search_text,
    coalesce(
        cookbook_wh.normalize_free_text(
            concat_ws(
                ' ',
                coalesce(r.title, ''),
                coalesce(r.description, ''),
                coalesce(n.notes_text, ''),
                coalesce(i.ingredients_text, ''),
                coalesce(c.categories_text, '')
            )
        ),
        ''
    ) AS search_document
FROM cookbook_wh.recipes r
LEFT JOIN cookbook_wh.recipe_notes_agg_view n
  ON n.recipe_id = r.id
LEFT JOIN cookbook_wh.recipe_ingredient_agg_view i
  ON i.recipe_id = r.id
LEFT JOIN cookbook_wh.recipe_category_agg_view c
  ON c.recipe_id = r.id;

CREATE OR REPLACE VIEW cookbook_wh.recipe_card_detail_view AS
SELECT
    r.id AS id,
    r.lang,
    r.title,
    coalesce(img.file_url, '') AS image,
    coalesce(rs.ingredients_count, 0) AS ingredients_count,
    coalesce(rs.instructions_count, 0) AS instructions_count,
    coalesce(cat.primary_category, '') AS category,
    coalesce(ing.ingredients_json, '[]'::jsonb) AS ingredients,
    coalesce(steps.steps_json, '[]'::jsonb) AS instruction_steps,
    coalesce(nut.nutritions_json, '[]'::jsonb) AS nutritions,
    jsonb_strip_nulls(
        jsonb_build_object(
            'prep_time', NULLIF((regexp_match(coalesce(notes.notes_text, ''), '(?i)(?:^|[;][ ]*)prep[_ ]?time[ ]*:[ ]*([^;]+)'))[1], ''),
            'cook_time', NULLIF((regexp_match(coalesce(notes.notes_text, ''), '(?i)(?:^|[;][ ]*)cook[_ ]?time[ ]*:[ ]*([^;]+)'))[1], ''),
            'total_time', NULLIF((regexp_match(coalesce(notes.notes_text, ''), '(?i)(?:^|[;][ ]*)total[_ ]?time[ ]*:[ ]*([^;]+)'))[1], ''),
            'ready_in_minutes', CASE
                WHEN NULLIF((regexp_match(coalesce(notes.notes_text, ''), '(?i)(?:^|[;][ ]*)ready[_ ]?in[_ ]?minutes[ ]*:[ ]*([0-9]+)'))[1], '') IS NOT NULL
                    THEN ((regexp_match(coalesce(notes.notes_text, ''), '(?i)(?:^|[;][ ]*)ready[_ ]?in[_ ]?minutes[ ]*:[ ]*([0-9]+)'))[1])::INTEGER
                ELSE NULL
            END
        )
    ) AS times
FROM cookbook_wh.recipes r
LEFT JOIN cookbook_wh.recipe_stats rs
  ON rs.recipe_id = r.id
LEFT JOIN cookbook_wh.recipe_notes_agg_view notes
  ON notes.recipe_id = r.id
LEFT JOIN LATERAL (
    SELECT i.file_url
    FROM cookbook_wh.recipe_images ri
    JOIN cookbook_wh.images i
      ON i.id = ri.image_id
    WHERE ri.recipe_id = r.id
    ORDER BY ri.position
    LIMIT 1
) img ON TRUE
LEFT JOIN cookbook_wh.recipe_category_agg_view cat
  ON cat.recipe_id = r.id
LEFT JOIN cookbook_wh.recipe_ingredient_agg_view ing
  ON ing.recipe_id = r.id
LEFT JOIN cookbook_wh.recipe_instruction_agg_view steps
  ON steps.recipe_id = r.id
LEFT JOIN cookbook_wh.recipe_nutrition_json_view nut
  ON nut.recipe_id = r.id;

CREATE OR REPLACE FUNCTION cookbook_wh.refresh_recipe_constraints()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM cookbook_wh.recipe_constraints
    WHERE source LIKE 'rule:%';

    INSERT INTO cookbook_wh.recipe_constraints (
        recipe_id,
        rule_code,
        constraint_type,
        constraint_key,
        status,
        confidence,
        source,
        reason
    )
    SELECT
        src.recipe_id,
        rules.rule_code,
        rules.constraint_type,
        rules.constraint_key,
        rules.status,
        rules.confidence,
        rules.source,
        rules.reason
    FROM cookbook_wh.recipe_rule_source_view src
    JOIN cookbook_wh.recipe_constraint_rule_catalog rules
      ON rules.is_active
     AND rules.rule_kind = 'TEXT'
     AND (
        CASE rules.match_scope
            WHEN 'INGREDIENT' THEN src.ingredient_search_text
            WHEN 'CATEGORY' THEN src.category_search_text
            ELSE src.search_document
        END
     ) ~ rules.regex_pattern
    ON CONFLICT DO NOTHING;

    INSERT INTO cookbook_wh.recipe_constraints (
        recipe_id,
        rule_code,
        constraint_type,
        constraint_key,
        status,
        confidence,
        source,
        reason
    )
    SELECT
        metrics.recipe_id,
        rules.rule_code,
        rules.constraint_type,
        rules.constraint_key,
        rules.status,
        rules.confidence,
        rules.source,
        rules.reason
    FROM cookbook_wh.recipe_nutrition_metrics_view metrics
    CROSS JOIN LATERAL (
        VALUES
            ('protein_g', metrics.protein_g),
            ('carbs_g', metrics.carbs_g),
            ('sugar_g', metrics.sugar_g),
            ('sodium_mg', metrics.sodium_mg),
            ('cholesterol_mg', metrics.cholesterol_mg),
            ('saturated_fat_g', metrics.saturated_fat_g),
            ('potassium_mg', metrics.potassium_mg),
            ('phosphorus_mg', metrics.phosphorus_mg)
    ) AS measured(metric_key, metric_value)
    JOIN cookbook_wh.recipe_constraint_rule_catalog rules
      ON rules.is_active
     AND rules.rule_kind = 'NUTRITION'
     AND rules.metric_key = measured.metric_key
     AND measured.metric_value IS NOT NULL
     AND (rules.min_value IS NULL OR measured.metric_value >= rules.min_value)
     AND (rules.max_value IS NULL OR measured.metric_value <= rules.max_value)
    ON CONFLICT DO NOTHING;
END;
$$;

SELECT cookbook_wh.refresh_recipe_constraints();

CREATE OR REPLACE VIEW cookbook_wh.recipe_constraints_recipe_view AS
WITH constraint_agg AS (
    SELECT
        rc.recipe_id,
        array_agg(DISTINCT rc.constraint_key ORDER BY rc.constraint_key)
            FILTER (WHERE rc.constraint_type = 'DIET' AND rc.status = 'BLOCK') AS block_diet_keys,
        array_agg(DISTINCT rc.constraint_key ORDER BY rc.constraint_key)
            FILTER (WHERE rc.constraint_type = 'DIET' AND rc.status = 'ALLOW') AS allow_diet_keys,
        array_agg(DISTINCT rc.constraint_key ORDER BY rc.constraint_key)
            FILTER (WHERE rc.constraint_type = 'ALLERGY' AND rc.status = 'BLOCK') AS block_allergy_keys,
        array_agg(DISTINCT rc.constraint_key ORDER BY rc.constraint_key)
            FILTER (WHERE rc.constraint_type = 'HEALTH' AND rc.status = 'BLOCK') AS block_health_keys,
        array_agg(DISTINCT rc.constraint_key ORDER BY rc.constraint_key)
            FILTER (WHERE rc.constraint_type = 'HEALTH' AND rc.status = 'CAUTION') AS caution_health_keys,
        array_agg(DISTINCT rc.constraint_key ORDER BY rc.constraint_key)
            FILTER (WHERE rc.constraint_type = 'HEALTH' AND rc.status = 'ALLOW') AS allow_health_keys,
        jsonb_agg(
            jsonb_build_object(
                'type', rc.constraint_type,
                'key', rc.constraint_key,
                'status', rc.status,
                'reason', rc.reason,
                'confidence', rc.confidence,
                'source', rc.source,
                'rule_code', rc.rule_code
            )
            ORDER BY rc.constraint_type, rc.constraint_key, rc.status, rc.confidence DESC, rc.source, rc.rule_code
        ) AS constraints
    FROM cookbook_wh.recipe_constraints rc
    GROUP BY rc.recipe_id
)
SELECT
    d.id AS recipe_id,
    d.lang,
    d.title,
    d.image,
    d.category,
    d.ingredients_count,
    d.instructions_count,
    d.ingredients,
    d.instruction_steps,
    d.nutritions,
    d.times,
    src.title_search_text,
    src.ingredient_search_text,
    src.category_search_text,
    src.search_document,
    coalesce(nm.protein_g, 0)::numeric(10,2) AS protein_g,
    coalesce(ca.block_diet_keys, ARRAY[]::text[]) AS block_diet_keys,
    coalesce(ca.allow_diet_keys, ARRAY[]::text[]) AS allow_diet_keys,
    coalesce(ca.block_allergy_keys, ARRAY[]::text[]) AS block_allergy_keys,
    coalesce(ca.block_health_keys, ARRAY[]::text[]) AS block_health_keys,
    coalesce(ca.caution_health_keys, ARRAY[]::text[]) AS caution_health_keys,
    coalesce(ca.allow_health_keys, ARRAY[]::text[]) AS allow_health_keys,
    coalesce(ca.constraints, '[]'::jsonb) AS constraints
FROM cookbook_wh.recipe_card_detail_view d
LEFT JOIN cookbook_wh.recipe_rule_source_view src
  ON src.recipe_id = d.id
LEFT JOIN cookbook_wh.recipe_nutrition_metrics_view nm
  ON nm.recipe_id = d.id
LEFT JOIN constraint_agg ca
  ON ca.recipe_id = d.id;

CREATE MATERIALIZED VIEW cookbook_wh.recipe_constraints_search_mv AS
SELECT *
FROM cookbook_wh.recipe_constraints_recipe_view;

CREATE UNIQUE INDEX idx_rcs_mv_recipe_id
    ON cookbook_wh.recipe_constraints_search_mv(recipe_id);

CREATE INDEX idx_rcs_mv_lang
    ON cookbook_wh.recipe_constraints_search_mv(lang);

CREATE INDEX idx_rcs_mv_protein
    ON cookbook_wh.recipe_constraints_search_mv(protein_g DESC NULLS LAST);

CREATE INDEX idx_rcs_mv_block_diet_gin
    ON cookbook_wh.recipe_constraints_search_mv USING gin (block_diet_keys);

CREATE INDEX idx_rcs_mv_block_allergy_gin
    ON cookbook_wh.recipe_constraints_search_mv USING gin (block_allergy_keys);

CREATE INDEX idx_rcs_mv_block_health_gin
    ON cookbook_wh.recipe_constraints_search_mv USING gin (block_health_keys);

CREATE INDEX idx_rcs_mv_caution_health_gin
    ON cookbook_wh.recipe_constraints_search_mv USING gin (caution_health_keys);

CREATE INDEX idx_rcs_mv_allow_diet_gin
    ON cookbook_wh.recipe_constraints_search_mv USING gin (allow_diet_keys);

CREATE OR REPLACE VIEW cookbook_wh.card_view AS
SELECT
    recipe_id,
    lang,
    title,
    image,
    category,
    ingredients_count,
    instructions_count,
    ingredients,
    instruction_steps,
    nutritions,
    times,
    title_search_text,
    ingredient_search_text,
    category_search_text,
    search_document,
    block_diet_keys,
    allow_diet_keys,
    block_allergy_keys,
    block_health_keys,
    caution_health_keys,
    allow_health_keys,
    constraints
FROM cookbook_wh.recipe_constraints_recipe_view;

CREATE MATERIALIZED VIEW cookbook_wh.card_search_mv AS
SELECT *
FROM cookbook_wh.card_view;

CREATE UNIQUE INDEX idx_card_search_mv_recipe_id
    ON cookbook_wh.card_search_mv(recipe_id);

CREATE INDEX idx_card_search_mv_lang_recipe
    ON cookbook_wh.card_search_mv(lang, recipe_id DESC);

CREATE INDEX idx_card_search_mv_title_trgm
    ON cookbook_wh.card_search_mv USING gin (title gin_trgm_ops);

CREATE INDEX idx_card_search_mv_category_trgm
    ON cookbook_wh.card_search_mv USING gin (category gin_trgm_ops);

CREATE INDEX idx_card_search_mv_search_document_trgm
    ON cookbook_wh.card_search_mv USING gin (search_document gin_trgm_ops);

CREATE INDEX idx_card_search_mv_ingredient_search_trgm
    ON cookbook_wh.card_search_mv USING gin (ingredient_search_text gin_trgm_ops);

CREATE INDEX idx_card_search_mv_block_diet_gin
    ON cookbook_wh.card_search_mv USING gin (block_diet_keys);

CREATE INDEX idx_card_search_mv_block_allergy_gin
    ON cookbook_wh.card_search_mv USING gin (block_allergy_keys);

CREATE INDEX idx_card_search_mv_block_health_gin
    ON cookbook_wh.card_search_mv USING gin (block_health_keys);

CREATE INDEX idx_card_search_mv_caution_health_gin
    ON cookbook_wh.card_search_mv USING gin (caution_health_keys);

CREATE INDEX idx_card_search_mv_allow_diet_gin
    ON cookbook_wh.card_search_mv USING gin (allow_diet_keys);

CREATE OR REPLACE FUNCTION cookbook_wh.refresh_recipe_search_cache()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM cookbook_wh.refresh_recipe_constraints();
    REFRESH MATERIALIZED VIEW cookbook_wh.recipe_constraints_search_mv;
    REFRESH MATERIALIZED VIEW cookbook_wh.card_search_mv;
    ANALYZE cookbook_wh.recipe_constraints;
    ANALYZE cookbook_wh.recipe_constraints_search_mv;
    ANALYZE cookbook_wh.card_search_mv;
END;
$$;

ANALYZE cookbook_wh.recipe_constraint_rule_catalog;
ANALYZE cookbook_wh.recipe_constraints;
ANALYZE cookbook_wh.recipe_constraints_search_mv;
ANALYZE cookbook_wh.card_search_mv;
