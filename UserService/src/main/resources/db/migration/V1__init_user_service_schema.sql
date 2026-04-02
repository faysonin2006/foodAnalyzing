create table if not exists user_profiles
(
    id                      uuid primary key,
    name                    varchar(255),
    email                   varchar(255) not null unique,
    date_of_birth           date,
    gender                  varchar(32),
    height                  integer,
    weight                  double precision,
    activity_level          varchar(64),
    goal_type               varchar(64),
    target_calories_per_day integer,
    created_at              date,
    updated_at              date
);

create table if not exists allergies
(
    id          varchar(64) primary key,
    description varchar(255)
);

create table if not exists diet_preferences
(
    id          varchar(64) primary key,
    description varchar(255)
);

create table if not exists health_conditions
(
    id          varchar(64) primary key,
    description varchar(255)
);

create table if not exists user_diet_preferences
(
    user_id uuid        not null,
    diet_id varchar(64) not null,
    primary key (user_id, diet_id),
    constraint fk_user_diet_preferences_user foreign key (user_id) references user_profiles (id) on delete cascade,
    constraint fk_user_diet_preferences_diet foreign key (diet_id) references diet_preferences (id)
);

create table if not exists user_allergies
(
    user_id    uuid        not null,
    allergy_id varchar(64) not null,
    primary key (user_id, allergy_id),
    constraint fk_user_allergies_user foreign key (user_id) references user_profiles (id) on delete cascade,
    constraint fk_user_allergies_allergy foreign key (allergy_id) references allergies (id)
);

create table if not exists user_health_conditions
(
    user_id             uuid        not null,
    health_condition_id varchar(64) not null,
    primary key (user_id, health_condition_id),
    constraint fk_user_health_conditions_user foreign key (user_id) references user_profiles (id) on delete cascade,
    constraint fk_user_health_conditions_health foreign key (health_condition_id) references health_conditions (id)
);

create table if not exists user_likes
(
    id         uuid primary key,
    user_id    uuid   not null,
    recipe_id  bigint not null,
    created_at timestamp,
    constraint uk_user_likes_user_recipe unique (user_id, recipe_id),
    constraint fk_user_likes_user foreign key (user_id) references user_profiles (id) on delete cascade
);

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
create index if not exists idx_user_likes_user_id on user_likes (user_id);
