#!/usr/bin/env bash
# Fetch and assemble the offline map bundle that ships inside the APK:
#   * BRouter routing engine JAR  → app/libs/brouter.jar
#   * BRouter routing segments    → app/src/main/assets/brouter/segments/*.rd5
#   * NL raster tile pyramid      → app/src/main/assets/tiles/nl.mbtiles
#
# These files are too large for git (~470 MB total) so the repo only stores
# the small profile files; everything else is rebuilt from upstream sources
# by this script.
#
# Idempotent — already-present files are skipped. Re-run after pulling a
# branch that bumps BRouter version or changes the tile coverage.
#
# Usage: bash scripts/setup_offline_data.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LIBS_DIR="$ROOT/app/libs"
PROFILES_DIR="$ROOT/app/src/main/assets/brouter/profiles"
SEGMENTS_DIR="$ROOT/app/src/main/assets/brouter/segments"
TILES_DIR="$ROOT/app/src/main/assets/tiles"

BROUTER_VERSION="1.7.9"
BROUTER_ZIP_URL="https://github.com/abrensch/brouter/releases/download/v${BROUTER_VERSION}/brouter-${BROUTER_VERSION}.zip"

# Segments covering NL. Each tile is 5°×5°; together these cover the
# full Netherlands (and unavoidably also bits of BE/DE/UK).
SEGMENTS=("E0_N50.rd5" "E5_N50.rd5")
SEGMENT_BASE="https://brouter.de/brouter/segments4"

mkdir -p "$LIBS_DIR" "$PROFILES_DIR" "$SEGMENTS_DIR" "$TILES_DIR"

need() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "ERROR: required tool '$1' not on PATH" >&2
        exit 1
    }
}
need curl
need python3

echo "==> 1/4 BRouter JAR + profiles"
if [ ! -f "$LIBS_DIR/brouter.jar" ] || \
   [ ! -f "$PROFILES_DIR/trekking.brf" ] || \
   [ ! -f "$PROFILES_DIR/lookups.dat" ]; then
    tmp="$(mktemp -d)"
    trap 'rm -rf "$tmp"' EXIT
    echo "    downloading BRouter $BROUTER_VERSION…"
    curl -sL --fail --max-time 120 -o "$tmp/brouter.zip" "$BROUTER_ZIP_URL"
    python3 - "$tmp" <<'PY'
import sys, zipfile
src = sys.argv[1] + "/brouter.zip"
zipfile.ZipFile(src).extractall(sys.argv[1])
PY
    cp "$tmp/brouter-${BROUTER_VERSION}/brouter-${BROUTER_VERSION}-all.jar" \
       "$LIBS_DIR/brouter.jar"
    for f in trekking.brf fastbike.brf car-fast.brf lookups.dat; do
        cp "$tmp/brouter-${BROUTER_VERSION}/profiles2/$f" "$PROFILES_DIR/$f"
    done
    rm -rf "$tmp"
    trap - EXIT
    echo "    done."
else
    echo "    already present — skipping."
fi

echo "==> 2/4 NL routing segments"
for seg in "${SEGMENTS[@]}"; do
    out="$SEGMENTS_DIR/$seg"
    if [ -s "$out" ]; then
        echo "    $seg already present ($(stat -c%s "$out" 2>/dev/null || stat -f%z "$out") bytes) — skipping."
        continue
    fi
    echo "    downloading $seg …"
    curl -L --fail --max-time 1200 --progress-bar \
        -o "$out.partial" "$SEGMENT_BASE/$seg"
    mv "$out.partial" "$out"
done

echo "==> 3/4 NL MBTiles tile pack"
if [ -s "$TILES_DIR/nl.mbtiles" ]; then
    echo "    nl.mbtiles already present — skipping. Delete it to rebuild."
else
    echo "    building from PDOK BRT (this takes ~5-15 min on a good link)…"
    python3 "$ROOT/scripts/build_nl_mbtiles.py"
fi

echo "==> 4/4 Summary"
ls -lh "$LIBS_DIR/brouter.jar" "$PROFILES_DIR"/*.brf "$PROFILES_DIR/lookups.dat" \
       "$SEGMENTS_DIR"/*.rd5 "$TILES_DIR"/*.mbtiles 2>/dev/null

echo
echo "Offline bundle ready. Build and install the APK as usual."
