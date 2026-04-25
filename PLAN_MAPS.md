# Maps & Offline Geodata — Implementation Plan

**Owner:** Alex
**Slice:** Maps + offline geodata for the offline-first Android emergency app
**Stack:** Native Kotlin (Android), MapLibre Android SDK, MediaPipe LLM Inference (Gemma), BRouter, Compose UI
**Target:** Netherlands, Amsterdam-centric demo
**Timeline:** 24 hours

---

## 1. Storyline Scenarios

The demo narrates three situations where there is no internet but there is an emergency:

1. **Flooding** — dike breach, rising water, evacuation. User needs to know: where is high ground, which roads are passable, where is a shelter.
2. **King's Day** — mass event in Amsterdam, cell network saturated, medical/safety incidents, lost people. User needs: nearest first-aid, AED, water, toilets, exits.
3. **Bunkers / shelters** — generic "find shelter" situation (could be storm, civil unrest, exercise). User needs: nearest reinforced/underground space.

The map slice provides the geospatial substrate. The Gemma agent provides intent + procedural knowledge.

---

## 2. Data Sources

### Map data (visual basemap + layers)

| Source                              | Role                                       | Why                                                                                                            |
|-------------------------------------|--------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| **PDOK BRT Achtergrondkaart**       | Primary rendered basemap (raster WMTS)     | Dutch government's official basemap. Polished, distinctively Dutch labels/styling. Better demo than OSM Carto. |
| **PDOK Luchtfoto** (aerial)         | Toggleable satellite layer                 | Flooding scenario becomes dramatic with aerial imagery. "Show me my actual street."                            |
| **AHN (PDOK)** elevation            | Hillshade overlay + numeric "go uphill"    | Flooding context. Shows high vs. low ground at a glance.                                                       |
| **LIWO flood scenarios (PDOK WMS)** | Flood-depth overlay                        | Official Dutch dike-breach inundation maps. Powers "if X breaks, here's the water."                            |
| **OSM** (Geofabrik NL extract)      | POI database + routing graph (BRouter `.rd5`) | Most complete dataset for the agent's spatial queries.                                                         |

**Rejected:**
- **Klimaateffectatlas** — long-horizon climate planning tool, not actionable in real-time. Useful for a city planner, not for someone trapped on the ground floor.
- **Mapbox / MapTiler / Stadia** — beautiful but require API keys and online tile delivery. Defeats the offline narrative.
- **Sentinel-2 / Copernicus** — overkill global tooling when PDOK Luchtfoto is NL-specific and better.
- **NWB Wegen** (national road database) — OSM is good enough for NL roads; integration cost not justified.

**Stretch additions if time permits:**
- **PDOK BAG** — official building footprints + addresses. Enriches hospital/shelter records with proper Dutch addresses.
- **Wikidata** — link OSM POIs to Wikidata IDs for richer descriptions in the agent's responses.
- **3DBAG** — 3D building models. Visual wow factor; integration time is non-trivial.

### POI categories (from a single OSM Overpass query, NL bbox `50.7,3.3,53.6,7.3`)

Must-ship:
1. Hospitals & clinics — `amenity=hospital|clinic`, `healthcare=hospital`
2. AEDs — `emergency=defibrillator`
3. Pharmacies — `amenity=pharmacy`
4. Police & fire — `amenity=police|fire_station`
5. Drinking water — `amenity=drinking_water`
6. Toilets — `amenity=toilets`
7. Shelters / assembly — `amenity=shelter` (filter `shelter_type`), `emergency=assembly_point`, `amenity=community_centre`
8. **Bunkers** — `building=bunker`, `military=bunker`
9. **De-facto shelters** — `amenity=parking + parking=underground`, metro stations (`station=subway`), large public buildings

Free additions (same query):
10. Fuel — `amenity=fuel`
11. Supermarkets / convenience — `shop=supermarket|convenience`
12. ATMs — `amenity=atm`
13. Fire hydrants — `emergency=fire_hydrant` (only render at z≥16)
14. Emergency phones — `emergency=phone`

Per-POI attributes preserved for the LLM and personalization filters: `name`, `phone`, `opening_hours`, `wheelchair`, `description`, `access`, `defibrillator:location`, plus `addr:*` if BAG enrichment lands.

---

## 3. Architecture

### A) POI pipeline — `data-pipeline/`

- `overpass-query.ql` — single QL query for all categories, NL bbox parameterized.
- `extract.py` — Overpass → GeoJSON → split by category → write `pois-nl.geojson` with `category` field per feature.
- Expected size: 10–25 MB for whole NL. Bundle as JSON; no MBTiles needed for POIs.

### B) Basemap tile pack — `data-pipeline/`

- `build-basemap.sh` — downloads PDOK BRT WMTS tiles for NL bbox z8–z14 → single `nl-basemap.mbtiles` (~200–300 MB).
- `build-aerial.sh` — same for PDOK Luchtfoto, lower zoom range (z10–z14) to keep size sane (~150–250 MB).
- Decision gate at H+2: if PDOK rate-limits or tooling fights us, fall back to planetiler against the NL Geofabrik PBF (vector, ~150–250 MB).

### C) Offline routing — `data-pipeline/` + Android app

- **Engine: BRouter** (pure Java, Android-friendly, compact `.rd5` segment files).
- **Why BRouter over GraphHopper:** NL routing data ≈ 50 MB in `.rd5` vs ≈ 600+ MB for a GraphHopper CH graph. Difference between shippable and not.
- `data-pipeline/fetch-brouter-segments.sh` — downloads NL `.rd5` from `brouter.de/brouter/segments4/` (~5 files, ~10 MB each).
- Kotlin: directly include BRouter as a library dependency. No JNI bridge, no native module wrapping. Wrap `btools.router.RoutingEngine` in a small Kotlin class exposing `route(from, to, profile)` returning a `Route(polyline, distanceM, durationS)`.
- Profiles in v0: `walking` and `car`.
- **Risk gate (H+14):** if BRouter isn't returning routes by H+14, fall back to straight-line + bearing arrow for v0 and pre-bake one OSRM polyline for the demo route.

### D) Android integration — `app/src/main/java/.../maps/`

- **Library:** MapLibre Android SDK (`org.maplibre.gl:android-sdk`).
- `MapScreen.kt` (Compose) — `MapView` reading `nl-basemap.mbtiles` from `filesDir`; renders `GeoJsonSource` + `SymbolLayer` per category with distinct icons.
- **Tile loading:** Kotlin-native is *much* simpler than RN here. MapLibre Android supports `mbtiles://` directly via a `MBTilesSource` extension or by serving from a tiny in-process `NanoHTTPD` instance — pick whichever the SDK version supports cleanly.
- `PoiIndex.kt` — loads `pois-nl.geojson` once at startup into a `STRTree` (JTS) spatial index; exposes:
  - `findNearest(category, lat, lon, k=5): List<Poi>`
  - `findNearestAccessible(category, lat, lon, k=5)` — filters on `wheelchair=yes`
  - `getPoiDetails(id): Poi?`
- `GpsTracker.kt` — `FusedLocationProviderClient` wrapper, last-known position; works offline as long as GPS has any fix.
- `Routing.kt` — calls BRouter wrapper, renders polyline as a `LineLayer` over the map.

### E) Pack manager — `app/src/main/java/.../packs/`

- `Manifest` data class: `id, name, bbox, version, basemapPath, poisPath, brouterSegments[], sizeBytes`.
- `PackManager.kt`:
  - On launch, check connectivity → if online, fetch remote manifest, compare versions, download deltas to `filesDir`.
  - If offline, use whatever's on disk.
- Ship the NL pack bundled in `assets/packs/nl/` so a fresh install + airplane mode works immediately. On first launch, copy from `assets/` to `filesDir/packs/nl/`.

### F) LLM agent tool surface (for Marom)

The Gemma agent calls these from a single Kotlin interface, exposed via MediaPipe's function-calling shim:

```kotlin
interface MapTools {
    fun findNearest(category: String, lat: Double, lon: Double, k: Int = 5): List<Poi>
    fun getPoiDetails(id: String): Poi?
    fun getCurrentLocation(): Location?
    fun routeTo(toLat: Double, toLon: Double, profile: String): Route?
    fun elevationAt(lat: Double, lon: Double): Double?       // for flooding
    fun floodDepthAt(lat: Double, lon: Double, scenario: String): Double?  // for flooding
}
```

Document in repo README so Marom can stub on day one.

---

## 4. Beyond-the-Map Interactions

To make the demo memorable beyond "look, a map":

1. **Voice loop (must-ship)** — Vosk offline STT → Gemma → Android TTS. Hands-free is the killer feature for "I'm bleeding / it's dark / smoke in the room."
2. **Compass-guided arrow overlay** — magnetometer + heading; giant arrow + distance to nearest hospital. Visually punchier than a polyline.
3. **SOS composer** — Gemma drafts a structured ≤160-char SMS (location + status + needs) queued to send when signal returns.
4. **Camera + vision (stretch)** — point at injury / medication label / hazard symbol; Gemma multimodal or ML Kit OCR offline.
5. **Haptic nav (stretch)** — vibrate-left/right as you walk. Tiny code, surprisingly memorable.

For 24h, ship **voice + compass arrow + SOS composer**. Map + agent + these three is a complete, distinctive story.

---

## 5. Files to Create

```
data-pipeline/
  verify_sources.py            ← FIRST STEP — see §7
  overpass-query.ql
  extract.py
  build-basemap.sh
  build-aerial.sh
  fetch-brouter-segments.sh
  fetch-ahn-tiles.sh
  fetch-liwo-scenarios.sh
  README.md

app/src/main/java/.../maps/
  MapScreen.kt
  PoiIndex.kt
  GpsTracker.kt
  Routing.kt
  CompassArrow.kt              ← beyond-map interaction
  ElevationLookup.kt           ← reads AHN GeoTIFF
  FloodOverlay.kt              ← reads LIWO scenarios

app/src/main/java/.../packs/
  PackManager.kt
  Manifest.kt

app/src/main/java/.../voice/
  VoiceLoop.kt                 ← Vosk + TTS
  SosComposer.kt

app/src/main/java/.../routing/
  BRouterWrapper.kt

app/src/main/assets/packs/nl/
  pois-nl.geojson
  nl-basemap.mbtiles
  nl-aerial.mbtiles
  ahn-nl.tif                   ← elevation, downsampled
  liwo/<scenario>.tif          ← flood depth rasters
  brouter-segments/*.rd5
  manifest.json
```

---

## 6. Build Sequence (24-Hour Timeline)

| Window     | Task                                                                                                                  |
|------------|-----------------------------------------------------------------------------------------------------------------------|
| **H+0–1**  | **Verification spike** (`verify_sources.py`). Prove every data source is reachable before sinking hours into Kotlin.  |
| H+1–3      | Run Overpass NL, validate POI counts. Commit `pois-nl.geojson`. Sascha + Marom can stub against it.                   |
| H+3–7      | Build `nl-basemap.mbtiles` (PDOK BRT). In parallel: scaffold `MapScreen.kt`, render bundled GeoJSON.                  |
| H+7–10     | `PoiIndex.kt` + agent tool interface; integrate against Marom's stub. Document tool surface in README.                |
| H+10–14    | BRouter integration: download `.rd5`, write `BRouterWrapper.kt`, verify a single route returns a polyline.            |
| **H+14**   | **Risk gate.** If routing isn't returning a polyline, switch to straight-line fallback for v0.                        |
| H+14–17    | Pack manager + bundled-asset copy on first launch. Airplane-mode test on real device.                                 |
| H+17–20    | Flooding layer (AHN hillshade + LIWO depth toggle). Voice loop. Compass arrow.                                        |
| H+20–22    | Personalization filters (wheelchair, mobility), category icons, polish.                                               |
| H+22–24    | Demo rehearsal on the demo phone, airplane mode.                                                                      |

---

## 7. Verification Spike (FIRST STEP — H+0–1)

Before any Kotlin code, run `data-pipeline/verify_sources.py` against the Amsterdam bbox. The script must succeed end-to-end before committing to the rest of the plan.

The script proves:

1. **Overpass** reachable; count POIs per category for Amsterdam; print 3 sample features.
2. **PDOK BRT WMTS** reachable; download tile `(z=12, x=2106, y=1336)`; save as PNG; assert non-empty.
3. **PDOK Luchtfoto** reachable; same.
4. **PDOK AHN WCS** reachable; pull a 1×1 km elevation tile around Dam Square; print min/max/mean.
5. **PDOK LIWO WMS** reachable; pull one inundation scenario tile; print extent + max depth.
6. **BRouter segments** reachable; download `E5_N50.rd5`; print file size.

If any step fails, change strategy *before* writing app code. Single-file Python, deps: `requests`, `rasterio`.

---

## 8. Scope Risk Callout

Whole-NL pack + real offline routing + flooding-specific layers + voice is the most ambitious version of this slice.

| Component         | Risk      | Mitigation                                                                            |
|-------------------|-----------|---------------------------------------------------------------------------------------|
| POIs (whole NL)   | Low       | Overpass scales fine, output is small.                                                |
| Basemap (NL)      | Medium    | 200–300 MB tile bake takes hours; PDOK rate limits possible. Fallback: planetiler.    |
| BRouter           | High      | First-time integration. H+14 risk gate → straight-line fallback if not working.       |
| AHN/LIWO layers   | Medium    | Raster handling is fiddly. Demo-only is OK — bake one Amsterdam-area tile.            |
| Voice loop        | Medium    | Vosk model is large; bundle smallest NL/EN model.                                     |

---

## 9. End-to-End Verification

1. `python data-pipeline/extract.py --bbox 50.7,3.3,53.6,7.3` → `pois-nl.geojson` with ≥50k features.
2. `bash data-pipeline/build-basemap.sh` → `nl-basemap.mbtiles` <400 MB. Open in QGIS — Amsterdam and Rotterdam render at z12.
3. `bash data-pipeline/fetch-brouter-segments.sh` → ~5 `.rd5` files in `brouter-segments/`.
4. Real Android device, airplane mode ON, location services ON: map loads tiles, POI pins appear, GPS dot moves.
5. Debug log: `findNearest("hospital", 52.3676, 4.9041, 3)` returns ≥3 hospitals with sane distances.
6. Debug log: `routeTo(52.3676, 4.9041, "walking")` returns a polyline that hugs Amsterdam streets, distance and ETA non-zero.
7. Toggle "wheelchair-only" filter → POIs with `wheelchair=no` disappear.
8. Toggle "flood overlay" → blue depth raster appears over the map.
9. End-to-end with Marom's LLM: ask "where's the nearest defibrillator?" out loud → agent calls `findNearest("defibrillator", …)` → narrates answer → optionally calls `routeTo(…)` and renders the path.
10. Voice + map + flooding flow: "the dike just broke, what do I do?" → agent reads elevation + nearest shelter → narrates evacuation route → renders polyline.
