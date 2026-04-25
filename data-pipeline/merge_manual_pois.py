#!/usr/bin/env python3
"""Merge hand-curated POIs into the bundled `pois-nl.geojson`.

Source: `data-pipeline/manual_pois.geojson` — anything you add there ends up
on the map. Re-running this script is idempotent: every feature whose `id`
starts with `manual/` is removed first, then the file is rewritten with the
latest manual set.

Each manual feature only needs `geometry` + `properties.category` and
`properties.name`; missing optional fields (phone, opening_hours, …) are
filled in with empty strings so the GeoJSON schema matches the rest of the
bundle.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

OPTIONAL_PROPS = ("phone", "opening_hours", "wheelchair", "operator", "addr_full")


def normalize(feat: dict, idx: int) -> dict:
    """Return a feature copy with a stable `manual/...` id and full property set."""
    props = dict(feat.get("properties") or {})
    if "category" not in props:
        raise ValueError(f"manual feature #{idx} is missing 'category'")
    props.setdefault("name", "")
    props["id"] = props.get("id") or f"manual/{props['category']}/{idx}"
    if not str(props["id"]).startswith("manual/"):
        props["id"] = f"manual/{props['id']}"
    for key in OPTIONAL_PROPS:
        props.setdefault(key, "")
    return {
        "type": "Feature",
        "geometry": feat["geometry"],
        "properties": props,
    }


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    bundle = root / "app" / "src" / "main" / "assets" / "pois-nl.geojson"
    manual_path = root / "data-pipeline" / "manual_pois.geojson"
    bundle_data = json.loads(bundle.read_text())
    manual_data = json.loads(manual_path.read_text())
    manual_feats = [
        normalize(f, i) for i, f in enumerate(manual_data.get("features") or [])
    ]
    keep = [
        f
        for f in (bundle_data.get("features") or [])
        if not str(((f.get("properties") or {}).get("id") or "")).startswith("manual/")
    ]
    bundle_data["features"] = keep + manual_feats
    bundle.write_text(json.dumps(bundle_data, separators=(",", ":")), encoding="utf-8")
    print(
        f"merged {len(manual_feats)} manual features; bundle now "
        f"{len(bundle_data['features'])} features total"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
