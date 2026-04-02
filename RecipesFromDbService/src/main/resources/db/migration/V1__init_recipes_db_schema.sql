create schema if not exists cookbook_wh;

create table if not exists cookbook_wh.card_search_mv
(
    recipe_id           bigint primary key,
    lang                varchar(8),
    title               varchar(255),
    image               varchar(1024),
    category            varchar(255),
    ingredients_count   integer,
    instructions_count  integer,
    ingredients         jsonb,
    instruction_steps   jsonb,
    nutritions          jsonb,
    times               jsonb,
    block_diet_keys     text[],
    block_allergy_keys  text[],
    block_health_keys   text[],
    caution_health_keys text[],
    constraints         jsonb
);

create or replace view cookbook_wh.card_view as
select *
from cookbook_wh.card_search_mv;

create index if not exists idx_card_search_mv_lang on cookbook_wh.card_search_mv (lang);
create index if not exists idx_card_search_mv_category on cookbook_wh.card_search_mv (category);
