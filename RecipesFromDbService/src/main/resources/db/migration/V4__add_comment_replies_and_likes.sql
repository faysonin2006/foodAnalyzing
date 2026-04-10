ALTER TABLE cookbook_wh.recipe_comments
    ADD COLUMN IF NOT EXISTS parent_comment_id BIGINT,
    ADD COLUMN IF NOT EXISTS author_user_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = 'cookbook_wh'
          AND table_name = 'recipe_comments'
          AND constraint_name = 'fk_recipe_comments_parent_comment'
    ) THEN
        ALTER TABLE cookbook_wh.recipe_comments
            ADD CONSTRAINT fk_recipe_comments_parent_comment
                FOREIGN KEY (parent_comment_id)
                REFERENCES cookbook_wh.recipe_comments(id)
                ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_recipe_comments_parent_created_at
    ON cookbook_wh.recipe_comments (parent_comment_id, created_at, id);

CREATE TABLE IF NOT EXISTS cookbook_wh.recipe_comment_likes (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES cookbook_wh.recipe_comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_recipe_comment_likes_comment_user UNIQUE (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_recipe_comment_likes_comment
    ON cookbook_wh.recipe_comment_likes (comment_id);
