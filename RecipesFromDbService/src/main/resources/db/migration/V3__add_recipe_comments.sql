CREATE TABLE IF NOT EXISTS cookbook_wh.recipe_comments (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES cookbook_wh.recipes(id) ON DELETE CASCADE,
    author_email VARCHAR(255) NOT NULL,
    author_name VARCHAR(120) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recipe_comments_recipe_created_at
    ON cookbook_wh.recipe_comments (recipe_id, created_at, id);
