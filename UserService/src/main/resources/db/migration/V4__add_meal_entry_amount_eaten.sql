alter table if exists meal_entries
    add column if not exists amount_eaten varchar(80);
