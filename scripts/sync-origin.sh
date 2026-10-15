#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REF='861be0473847d5c020102191f2c2891acda5d628'
BASE="https://raw.githubusercontent.com/ReChronoRain/HyperCeiler/${REF}"
DEST="$ROOT/app/src/main"

fetch() {
    local source="$1"
    local target="$2"
    local path="$DEST/$target"
    mkdir -p "$(dirname "$path")"
    curl -fsSL --retry 3 --retry-delay 2 "$BASE/$source" -o "$path.tmp"
    mv "$path.tmp" "$path"
}

rm -f "$DEST/res/raw/glow.glsl"
fetch 'library/provision/src/main/res/raw/glow.glsl' 'res/raw/glow.agsl'
fetch 'library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/GlowController.java' 'java/com/sevtinge/hyperceiler/provision/renderengine/GlowController.java'
fetch 'library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java' 'java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java'
fetch 'library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/RenderViewLayout.java' 'java/com/sevtinge/hyperceiler/provision/renderengine/RenderViewLayout.java'

sed -i 's/import com.sevtinge.hyperceiler.provision.R;/import me.rain.kernel.R;/' \
    "$DEST/java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java"

curl -fsSL --retry 3 --retry-delay 2 "$BASE/LICENSE" -o "$ROOT/LICENSE"
