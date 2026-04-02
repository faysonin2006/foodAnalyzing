create table if not exists household_invitations
(
    id                   uuid primary key,
    household_id         uuid         not null,
    invited_user_id      uuid         not null,
    invited_email        varchar(255) not null,
    invited_display_name varchar(255),
    invited_by_user_id   uuid         not null,
    invited_by_name      varchar(255) not null,
    status               varchar(32)  not null,
    created_at           timestamp,
    responded_at         timestamp,
    constraint fk_household_invitations_household foreign key (household_id) references households (id) on delete cascade
);

create index if not exists idx_household_invitations_invited on household_invitations (invited_user_id, status, created_at desc);
create index if not exists idx_household_invitations_household on household_invitations (household_id, created_at desc);
create unique index if not exists uk_household_invitations_pending
    on household_invitations (household_id, invited_user_id)
    where status = 'PENDING';
