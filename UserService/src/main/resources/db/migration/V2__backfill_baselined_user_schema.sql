-- Forward-only patch for databases that were baselined at version 1
-- before pantry, meals, and shopping list tables were introduced.

create table if not exists pantry_items
(
    id           uuid primary key,
    user_id      uuid           not null,
    name         varchar(255)   not null,
    brand        varchar(255),
    category     varchar(255)   not null,
    quantity     numeric(19, 2) not null,
    unit         varchar(32)    not null,
    purchased_at date           not null,
    opened_at    date,
    expires_at   date,
    status       varchar(32),
    image_url    varchar(1024),
    barcode      varchar(255),
    created_at   timestamp,
    updated_at   timestamp,
    constraint fk_pantry_items_user foreign key (user_id) references user_profiles (id) on delete cascade
);

create table if not exists meal_entries
(
    id            uuid primary key,
    user_id       uuid         not null,
    title         varchar(255) not null,
    calories      integer      not null,
    proteins      double precision,
    fats          double precision,
    carbohydrates double precision,
    eaten_at      timestamp    not null,
    source        varchar(32)  not null,
    notes         varchar(1000),
    image_url     varchar(1024),
    created_at    timestamp,
    constraint fk_meal_entries_user foreign key (user_id) references user_profiles (id) on delete cascade
);

create table if not exists shopping_list_items
(
    id         uuid primary key,
    user_id    uuid         not null,
    name       varchar(160) not null,
    quantity   numeric(19, 2),
    unit       varchar(30),
    checked    boolean      not null,
    created_at timestamp,
    updated_at timestamp,
    constraint fk_shopping_list_items_user foreign key (user_id) references user_profiles (id) on delete cascade
);

create index if not exists idx_pantry_items_user_status_created on pantry_items (user_id, status, created_at desc);
create index if not exists idx_pantry_items_user_expires on pantry_items (user_id, expires_at);
create index if not exists idx_meal_entries_user_eaten_at on meal_entries (user_id, eaten_at desc);
create index if not exists idx_shopping_list_items_user_checked_created on shopping_list_items (user_id, checked, created_at desc);

do
$$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'user_profiles'
          and column_name = 'gender'
          and data_type = 'smallint'
    ) then
        alter table user_profiles
            drop constraint if exists user_profiles_gender_check;

        alter table user_profiles
            alter column gender type varchar(32)
            using case gender
                when 0 then 'MALE'
                when 1 then 'FEMALE'
                else null
            end;
    end if;
end
$$;
