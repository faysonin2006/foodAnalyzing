create table if not exists users
(
    id            uuid primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    role          varchar(32),
    created_at    date,
    updated_at    date
);

create table if not exists refresh_tokens
(
    id          bigserial primary key,
    token       varchar(255) not null unique,
    expiry_date timestamp    not null,
    user_id     uuid         not null,
    constraint fk_refresh_tokens_user foreign key (user_id) references users (id) on delete cascade
);

create index if not exists idx_refresh_tokens_user_id on refresh_tokens (user_id);
