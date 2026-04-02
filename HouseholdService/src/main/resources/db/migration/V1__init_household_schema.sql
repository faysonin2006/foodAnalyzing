create table if not exists households
(
    id                 uuid primary key,
    name               varchar(120) not null,
    created_by_user_id uuid         not null,
    created_by_email   varchar(255) not null,
    created_at         timestamp,
    updated_at         timestamp
);

create table if not exists household_members
(
    id           uuid primary key,
    household_id uuid         not null,
    user_id      uuid         not null,
    email        varchar(255) not null,
    display_name varchar(255),
    role         varchar(32)  not null,
    joined_at    timestamp,
    constraint fk_household_members_household foreign key (household_id) references households (id) on delete cascade,
    constraint uk_household_members_household_user unique (household_id, user_id)
);

create table if not exists household_shopping_items
(
    id                uuid primary key,
    household_id      uuid         not null,
    name              varchar(160) not null,
    quantity          numeric(19, 2),
    unit              varchar(30),
    note              varchar(500),
    checked           boolean      not null,
    added_by_user_id  uuid         not null,
    added_by_name     varchar(255) not null,
    checked_by_user_id uuid,
    checked_by_name   varchar(255),
    created_at        timestamp,
    updated_at        timestamp,
    constraint fk_household_shopping_items_household foreign key (household_id) references households (id) on delete cascade
);

create table if not exists household_messages
(
    id             uuid primary key,
    household_id   uuid         not null,
    author_user_id uuid,
    author_name    varchar(255) not null,
    message        varchar(1000) not null,
    type           varchar(32)  not null,
    created_at     timestamp,
    constraint fk_household_messages_household foreign key (household_id) references households (id) on delete cascade
);

create index if not exists idx_household_members_user_id on household_members (user_id);
create index if not exists idx_household_members_household_id on household_members (household_id);
create index if not exists idx_household_shopping_items_household on household_shopping_items (household_id, checked, created_at desc);
create index if not exists idx_household_messages_household on household_messages (household_id, created_at desc);
