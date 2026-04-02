create table if not exists food_analysis
(
    id            uuid primary key,
    user_id       varchar(255)  not null,
    image_url     varchar(1024) not null,
    status        varchar(32)   not null,
    dish_name     varchar(255),
    calories      integer,
    protein       double precision,
    carbs         double precision,
    fats          double precision,
    extra_info    text,
    error_message varchar(1000),
    saved_meal_id uuid,
    saved_at      timestamp,
    created_at    timestamp
);

create index if not exists idx_food_analysis_user_created on food_analysis (user_id, created_at desc);
create index if not exists idx_food_analysis_status on food_analysis (status);
create index if not exists idx_food_analysis_saved_meal_id on food_analysis (saved_meal_id);
