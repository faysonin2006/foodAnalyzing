alter table if exists meal_entries
    add column if not exists amount_mode varchar(32),
    add column if not exists eaten_ratio double precision,
    add column if not exists total_weight_grams double precision,
    add column if not exists eaten_weight_grams double precision,
    add column if not exists package_fraction_numerator integer,
    add column if not exists package_fraction_denominator integer,
    add column if not exists full_portion_calories integer,
    add column if not exists full_portion_proteins double precision,
    add column if not exists full_portion_fats double precision,
    add column if not exists full_portion_carbohydrates double precision;
