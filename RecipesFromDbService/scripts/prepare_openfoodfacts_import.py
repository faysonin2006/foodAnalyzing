#!/usr/bin/env python3
import csv
import math
import re
import sys
from pathlib import Path

csv.field_size_limit(sys.maxsize)

OUTPUT_HEADER = [
    "code",
    "product_name",
    "generic_name",
    "brand_name",
    "quantity",
    "serving_size",
    "categories_text",
    "countries_text",
    "stores_text",
    "ingredients_text",
    "image_url",
    "energy_kj_100g",
    "calories_kcal_100g",
    "fats_100g",
    "saturated_fat_100g",
    "carbohydrates_100g",
    "proteins_100g",
    "fiber_100g",
    "sugars_100g",
    "salt_100g",
    "sodium_100g",
    "search_text",
    "name_search_text",
    "brand_search_text",
    "country_search_text",
]


def cleaned(value: str | None) -> str:
    if not value:
        return ""
    value = value.replace("\t", " ").replace("\r", " ").replace("\n", " ")
    return re.sub(r"\s+", " ", value).strip()


def normalized(value: str | None) -> str:
    return cleaned(value).lower()


def as_decimal(value: str | None, *, max_abs: float | None = None) -> str:
    raw = cleaned(value)
    if not raw:
        return ""
    raw = raw.replace(",", ".")
    try:
        number = float(raw)
    except ValueError:
        return ""
    if math.isnan(number) or math.isinf(number):
        return ""
    if max_abs is not None and abs(number) > max_abs:
        return ""
    return f"{number:.4f}".rstrip("0").rstrip(".")


def main() -> int:
    if len(sys.argv) != 3:
        print(
            "Usage: prepare_openfoodfacts_import.py <input_tsv> <output_tsv>",
            file=sys.stderr,
        )
        return 1

    input_path = Path(sys.argv[1]).expanduser().resolve()
    output_path = Path(sys.argv[2]).expanduser().resolve()

    seen_codes: set[str] = set()
    total = 0
    kept = 0

    with input_path.open("r", encoding="utf-8", errors="replace", newline="") as src, output_path.open(
        "w", encoding="utf-8", newline=""
    ) as dst:
        reader = csv.DictReader(src, delimiter="\t")
        writer = csv.DictWriter(dst, fieldnames=OUTPUT_HEADER, delimiter="\t")
        writer.writeheader()

        for row in reader:
            total += 1
            code = cleaned(row.get("code"))
            product_name = cleaned(row.get("product_name")) or cleaned(row.get("generic_name"))
            if not code or not product_name or code in seen_codes:
                continue

            energy_kj = as_decimal(row.get("energy_100g"), max_abs=10000)
            fats = as_decimal(row.get("fat_100g"), max_abs=100)
            saturated_fat = as_decimal(row.get("saturated-fat_100g"), max_abs=100)
            carbs = as_decimal(row.get("carbohydrates_100g"), max_abs=100)
            proteins = as_decimal(row.get("proteins_100g"), max_abs=100)
            fiber = as_decimal(row.get("fiber_100g"), max_abs=100)
            sugars = as_decimal(row.get("sugars_100g"), max_abs=100)
            salt = as_decimal(row.get("salt_100g"), max_abs=100)
            sodium = as_decimal(row.get("sodium_100g"), max_abs=100)

            if not any([energy_kj, fats, carbs, proteins]):
                continue

            calories = ""
            if energy_kj:
                calories = f"{(float(energy_kj) / 4.184):.4f}".rstrip("0").rstrip(".")

            generic_name = cleaned(row.get("generic_name"))
            brand_name = cleaned(row.get("brands"))
            quantity = cleaned(row.get("quantity"))
            serving_size = cleaned(row.get("serving_size"))
            categories = cleaned(row.get("categories_en"))
            countries = cleaned(row.get("countries_en"))
            stores = cleaned(row.get("stores"))
            ingredients_text = cleaned(row.get("ingredients_text"))
            image_url = cleaned(row.get("image_url") or row.get("image_small_url"))

            name_search = normalized(f"{product_name} {generic_name}")
            brand_search = normalized(brand_name)
            country_search = normalized(countries)
            search_text = normalized(
                " ".join(
                    part
                    for part in [
                        product_name,
                        generic_name,
                        brand_name,
                        categories,
                        countries,
                        stores,
                        ingredients_text,
                    ]
                    if part
                )
            )

            writer.writerow(
                {
                    "code": code,
                    "product_name": product_name,
                    "generic_name": generic_name,
                    "brand_name": brand_name,
                    "quantity": quantity,
                    "serving_size": serving_size,
                    "categories_text": categories,
                    "countries_text": countries,
                    "stores_text": stores,
                    "ingredients_text": ingredients_text,
                    "image_url": image_url,
                    "energy_kj_100g": energy_kj,
                    "calories_kcal_100g": calories,
                    "fats_100g": fats,
                    "saturated_fat_100g": saturated_fat,
                    "carbohydrates_100g": carbs,
                    "proteins_100g": proteins,
                    "fiber_100g": fiber,
                    "sugars_100g": sugars,
                    "salt_100g": salt,
                    "sodium_100g": sodium,
                    "search_text": search_text,
                    "name_search_text": name_search,
                    "brand_search_text": brand_search,
                    "country_search_text": country_search,
                }
            )
            seen_codes.add(code)
            kept += 1

    print(f"Prepared {kept} products from {total} rows into {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
