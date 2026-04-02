ALTER TABLE food_analysis
    ADD COLUMN IF NOT EXISTS is_food boolean,
    ADD COLUMN IF NOT EXISTS health_score integer;

UPDATE food_analysis
SET is_food = true
WHERE is_food IS NULL
  AND status = 'COMPLETED'
  AND calories IS NOT NULL;
