#!/usr/bin/env python3
import csv
import sys
from pathlib import Path

csv.field_size_limit(sys.maxsize)


def load_rows(path: Path):
    with path.open("r", encoding="utf-8", newline="") as src:
        reader = csv.DictReader(src, delimiter="\t")
        header = reader.fieldnames
        if not header:
            raise ValueError(f"No header found in {path}")
        for row in reader:
            yield header, row


def main() -> int:
    if len(sys.argv) != 4:
        print(
            "Usage: merge_clean_openfoodfacts_imports.py <primary_tsv> <secondary_tsv> <output_tsv>",
            file=sys.stderr,
        )
        return 1

    primary_path = Path(sys.argv[1]).expanduser().resolve()
    secondary_path = Path(sys.argv[2]).expanduser().resolve()
    output_path = Path(sys.argv[3]).expanduser().resolve()

    seen_codes: set[str] = set()
    primary_kept = 0
    secondary_added = 0
    header: list[str] | None = None

    with output_path.open("w", encoding="utf-8", newline="") as dst:
        writer = None

        for source_index, source_path in enumerate([primary_path, secondary_path]):
            for source_header, row in load_rows(source_path):
                if header is None:
                    header = source_header
                    writer = csv.DictWriter(dst, fieldnames=header, delimiter="\t")
                    writer.writeheader()
                elif source_header != header:
                    raise ValueError(f"Header mismatch for {source_path}")

                code = (row.get("code") or "").strip()
                if not code or code in seen_codes:
                    continue

                writer.writerow({key: row.get(key, "") for key in header})
                seen_codes.add(code)
                if source_index == 0:
                    primary_kept += 1
                else:
                    secondary_added += 1

    print(
        f"Merged {primary_kept} primary rows and added {secondary_added} secondary-only rows into {output_path}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
