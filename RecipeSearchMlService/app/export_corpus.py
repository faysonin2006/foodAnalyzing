from __future__ import annotations

import argparse
import json
from pathlib import Path

import psycopg

EXPORT_SQL = """
SELECT
    recipe_id,
    title,
    category,
    ingredients::text AS ingredients_json
FROM cookbook_wh.card_search_mv
ORDER BY recipe_id
"""


def main() -> None:
    parser = argparse.ArgumentParser(description="Export recipe corpus for TensorFlow search training")
    parser.add_argument("--dsn", required=True, help="PostgreSQL DSN")
    parser.add_argument("--schema", default="cookbook_wh", help="Schema name")
    parser.add_argument("--output", required=True, help="Output JSONL file")
    args = parser.parse_args()

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    sql = EXPORT_SQL.replace("cookbook_wh", args.schema)

    with psycopg.connect(args.dsn) as connection, connection.cursor() as cursor, output_path.open("w", encoding="utf-8") as handle:
        cursor.execute(sql)
        for recipe_id, title, category, ingredients_json in cursor.fetchall():
            ingredients = []
            try:
                raw_ingredients = json.loads(ingredients_json or "[]")
                for item in raw_ingredients:
                    value = item.get("ingredient") or item.get("raw_text") or item.get("note")
                    if value:
                        ingredients.append(str(value))
            except Exception:
                ingredients = []
            payload = {
                "recipe_id": recipe_id,
                "title": title or "",
                "category": category or "",
                "ingredients": ingredients,
            }
            handle.write(json.dumps(payload, ensure_ascii=False) + "\n")

    print(f"Exported corpus to {output_path.resolve()}")


if __name__ == "__main__":
    main()
