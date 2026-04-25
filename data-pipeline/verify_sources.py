"""
Verify Tier-1 data sources for the offline emergency app are reachable.

Run:
    pip install requests
    python verify_sources.py

Exits 0 if all sources work, 1 otherwise.
"""

import math
import sys
import time
import requests

# Tiny bbox in central Amsterdam — keeps Overpass fast for a smoke test.
# Order: south, west, north, east
AMSTERDAM_BBOX = (52.36, 4.88, 52.38, 4.92)
DAM_SQUARE = (52.3731, 4.8926)  # for tile coord lookup
TIMEOUT = 30

# OSM etiquette: identify yourself so the server doesn't 406/429 you.
USER_AGENT = "anthropic-whale-hackathon/0.1 (alexleotik@gmail.com)"


def deg2tile(lat, lon, z):
    """Slippy-map tile coords (web mercator) for a lat/lon at zoom z."""
    lat_rad = math.radians(lat)
    n = 2 ** z
    x = int((lon + 180.0) / 360.0 * n)
    y = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return x, y

results = []


def check(label, fn):
    print(f"  {label} ...", end=" ", flush=True)
    t0 = time.time()
    try:
        msg = fn()
        dt = time.time() - t0
        print(f"OK  ({dt:4.1f}s) — {msg}")
        results.append(True)
    except Exception as e:
        dt = time.time() - t0
        print(f"FAIL ({dt:4.1f}s) — {type(e).__name__}: {e}")
        results.append(False)


# 1. OSM Overpass — confirms the POI database is queryable.
def overpass_hospitals():
    s, w, n, e = AMSTERDAM_BBOX
    query = f"""
    [out:json][timeout:25];
    (
      node["amenity"="hospital"]({s},{w},{n},{e});
      way["amenity"="hospital"]({s},{w},{n},{e});
      node["amenity"="pharmacy"]({s},{w},{n},{e});
      node["emergency"="defibrillator"]({s},{w},{n},{e});
    );
    out center 20;
    """
    r = requests.post(
        "https://overpass-api.de/api/interpreter",
        data={"data": query},
        headers={"User-Agent": USER_AGENT},
        timeout=TIMEOUT,
    )
    r.raise_for_status()
    data = r.json()
    n_features = len(data.get("elements", []))
    if n_features == 0:
        raise RuntimeError("query returned 0 features")
    return f"{n_features} POIs (hospitals/pharmacies/AEDs) in central Amsterdam"


# 2. PDOK BRT — confirms we can pull a basemap tile.
def pdok_brt_tile():
    z = 12
    x, y = deg2tile(*DAM_SQUARE, z)
    url = (
        f"https://service.pdok.nl/brt/achtergrondkaart/wmts/v2_0"
        f"/standaard/EPSG:3857/{z}/{x}/{y}.png"
    )
    r = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=TIMEOUT)
    r.raise_for_status()
    ct = r.headers.get("content-type", "")
    if "image" not in ct:
        raise RuntimeError(f"unexpected content-type: {ct}")
    if len(r.content) < 5000:
        raise RuntimeError(f"tile only {len(r.content)} bytes — likely blank")
    return f"{len(r.content)/1024:.1f} KB PNG (z={z} tile {x},{y})"


# 3. BRouter — confirms the NL routing segment is downloadable.
def brouter_segment():
    # E5_N50.rd5 covers 5–10°E, 50–55°N, which includes most of NL.
    url = "https://brouter.de/brouter/segments4/E5_N50.rd5"
    r = requests.head(url, headers={"User-Agent": USER_AGENT}, timeout=TIMEOUT, allow_redirects=True)
    r.raise_for_status()
    size = int(r.headers.get("content-length", 0))
    if size == 0:
        raise RuntimeError("empty or missing file")
    return f"{size/1024/1024:.1f} MB segment available"


print("Verifying Tier-1 data sources for the offline emergency app (NL)\n")
check("OSM Overpass (POIs)     ", overpass_hospitals)
check("PDOK BRT     (basemap)  ", pdok_brt_tile)
check("BRouter      (routing)  ", brouter_segment)

ok = sum(results)
total = len(results)
print(f"\n{ok}/{total} sources reachable")
sys.exit(0 if ok == total else 1)
