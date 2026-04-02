create table if not exists pantry_barcode_cache
(
    id             uuid primary key,
    user_id        uuid           not null,
    barcode        varchar(64)    not null,
    name           varchar(255)   not null,
    brand          varchar(255),
    category       varchar(255)   not null,
    image_url      varchar(1024),
    quantity       numeric(19, 2),
    unit           varchar(32),
    raw_quantity   varchar(255),
    created_source varchar(64),
    created_at     timestamp,
    updated_at     timestamp,
    constraint fk_pantry_barcode_cache_user foreign key (user_id) references user_profiles (id) on delete cascade,
    constraint uk_pantry_barcode_cache_user_barcode unique (user_id, barcode)
);

create index if not exists idx_pantry_barcode_cache_user_updated
    on pantry_barcode_cache (user_id, updated_at desc);
