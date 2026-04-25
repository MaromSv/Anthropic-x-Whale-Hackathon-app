"""
Extract all emergency-relevant POIs in the Netherlands from OSM Overpass.
Writes a GeoJSON FeatureCollection to app/src/main/assets/pois-nl.geojson.

Run:
    pip install requests
    python extract_pois.py
"""
import json
import os
import sys
import time
import requests

# Whole NL bbox (south, west, north, east)
NL_BBOX = (50.7, 3.3, 53.6, 7.3)
USER_AGENT = "anthropic-whale-hackathon/0.1 (alexleotik@gmail.com)"
OVERPASS = "https://overpass-api.de/api/interpreter"
OUT = os.path.join(
    os.path.dirname(__file__),
    "..",
    "app",
    "src",
    "main",
    "assets",
    "pois-nl.geojson",
)

# All categories worth showing in an emergency app — must-haves + nice-to-haves.
# Tuple is (overpass selector, _ignored).
CATEGORIES = {
    # --- Must-haves: emergency-critical infrastructure ---
    "hospital": [
        ('node["amenity"="hospital"]',           True),
        ('way["amenity"="hospital"]',            True),
    ],
    "doctor": [
        ('node["amenity"="doctors"]',            True),
        ('node["amenity"="clinic"]',             True),
        ('way["amenity"="clinic"]',              True),
    ],
    "first_aid": [
        ('node["emergency"="first_aid_kit"]',    True),
        ('node["emergency"="first_aid_post"]',   True),
    ],
    "aed": [
        ('node["emergency"="defibrillator"]',    True),
    ],
    "pharmacy": [
        ('node["amenity"="pharmacy"]',           True),
    ],
    "police": [
        ('node["amenity"="police"]',             True),
        ('way["amenity"="police"]',              True),
    ],
    "fire": [
        ('node["amenity"="fire_station"]',       True),
        ('way["amenity"="fire_station"]',        True),
    ],
    "shelter": [
        # Exclude bus-stop shelters explicitly.
        ('node["amenity"="shelter"]["shelter_type"!="public_transport"]', True),
        ('way["amenity"="shelter"]["shelter_type"!="public_transport"]',  True),
        ('node["emergency"="assembly_point"]',   True),
    ],

    # --- Nice-to-haves: scenario-specific ---
    "water": [  # King's Day / flooding survival
        ('node["amenity"="drinking_water"]',     True),
    ],
    "toilet": [  # King's Day
        ('node["amenity"="toilets"]',            True),
    ],
    "metro": [  # Bunker scenario — de-facto underground shelter
        ('node["railway"="station"]["station"="subway"]', True),
        ('node["railway"="subway_entrance"]',    True),
    ],
    "parking_underground": [  # Bunker — reinforced indoor
        ('node["amenity"="parking"]["parking"="underground"]', True),
        ('way["amenity"="parking"]["parking"="underground"]',  True),
    ],
    "bunker": [  # Bunker scenario — actual bunkers
        ('node["building"="bunker"]',            True),
        ('way["building"="bunker"]',             True),
        ('node["military"="bunker"]',            True),
        ('way["military"="bunker"]',             True),
    ],
    "fuel": [  # Evacuation
        ('node["amenity"="fuel"]',               True),
    ],
    "supermarket": [  # Supplies
        ('node["shop"="supermarket"]',           True),
        ('way["shop"="supermarket"]',            True),
        ('node["shop"="convenience"]',           True),
    ],
    "atm": [  # When card systems fail
        ('node["amenity"="atm"]',                True),
    ],
    "phone": [  # When mobile is dead
        ('node["emergency"="phone"]',            True),
        ('node["amenity"="telephone"]',          True),
    ],
    "school": [  # Designated NL evacuation centers
        ('node["amenity"="school"]',             True),
        ('way["amenity"="school"]',              True),
    ],
    "community": [  # Coordination during major incidents
        ('node["amenity"="community_centre"]',   True),
        ('way["amenity"="community_centre"]',    True),
        ('node["amenity"="townhall"]',           True),
        ('way["amenity"="townhall"]',            True),
    ],
    "worship": [  # Large reinforced buildings; traditional gathering points
        ('node["amenity"="place_of_worship"]',   True),
        ('way["amenity"="place_of_worship"]',    True),
    ],
}


def fetch_category(category, selectors):
    s, w, n, e = NL_BBOX
    statements = "\n  ".join(f"{sel}({s},{w},{n},{e});" for sel, _ in selectors)
    query = f"""
    [out:json][timeout:300];
    (
      {statements}
    );
    out center;
    """
    print(f"  → querying {category} ...", end=" ", flush=True)
    t0 = time.time()
    for attempt in range(6):
        r = requests.post(
            OVERPASS,
            data={"data": query},
            headers={"User-Agent": USER_AGENT},
            timeout=360,
        )
        if r.status_code == 200:
            elements = r.json().get("elements", [])
            print(f"{len(elements)} ({time.time() - t0:.0f}s)")
            time.sleep(20)  # gentle pause to stay in Overpass quota
            return elements
        if r.status_code in (429, 504, 503):
            wait = 60 * (attempt + 1)
            print(f"\n    {r.status_code} — waiting {wait}s before retry {attempt + 2}/6", end="", flush=True)
            time.sleep(wait)
            continue
        r.raise_for_status()
    raise RuntimeError(f"{category}: gave up after 6 attempts")


def to_features(elements, category):
    out = []
    for el in elements:
        tags = el.get("tags", {})
        if "lat" in el:
            lat, lon = el["lat"], el["lon"]
        elif "center" in el:
            lat, lon = el["center"]["lat"], el["center"]["lon"]
        else:
            continue
        out.append({
            "type": "Feature",
            "geometry": {"type": "Point", "coordinates": [lon, lat]},
            "properties": {
                "id": f"{el['type']}/{el['id']}",
                "category": category,
                "name": tags.get("name", ""),
                "phone": tags.get("phone", ""),
                "opening_hours": tags.get("opening_hours", ""),
                "wheelchair": tags.get("wheelchair", ""),
                "operator": tags.get("operator", ""),
                "addr_full": _addr(tags),
            },
        })
    return out


def _addr(tags):
    street = tags.get("addr:street", "")
    house = tags.get("addr:housenumber", "")
    city = tags.get("addr:city", "")
    if street or city:
        s = f"{street} {house}".strip()
        return f"{s}, {city}".strip(", ")
    return ""


def main():
    print(f"Extracting NL POIs to {os.path.abspath(OUT)}\n")
    all_features = []
    for category, selectors in CATEGORIES.items():
        elements = fetch_category(category, selectors)
        all_features.extend(to_features(elements, category))

    fc = {"type": "FeatureCollection", "features": all_features}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(fc, f, ensure_ascii=False, separators=(",", ":"))

    size_mb = os.path.getsize(OUT) / 1024 / 1024
    print(f"\n✓ Wrote {len(all_features)} features → {os.path.abspath(OUT)} ({size_mb:.1f} MB)")
    by_cat = {}
    for f in all_features:
        c = f["properties"]["category"]
        by_cat[c] = by_cat.get(c, 0) + 1
    for c, n in sorted(by_cat.items()):
        print(f"   {c}: {n}")


if __name__ == "__main__":
    main()
