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

fetch 'library/provision/src/main/res/raw/glow.glsl' 'res/raw/glow.glsl'
fetch 'library/provision/src/main/res/layout/provision_startup_layout.xml' 'res/layout/provision_startup_layout.xml'
fetch 'library/provision/src/main/res/drawable/provision_logo_image.xml' 'res/drawable/provision_logo_image.xml'
fetch 'library/provision/src/main/res/drawable/provision_logo_image_lite.xml' 'res/drawable/provision_logo_image_lite.xml'
fetch 'library/provision/src/main/res/drawable/provision_text_logo_image.xml' 'res/drawable/provision_text_logo_image.xml'
fetch 'library/provision/src/main/res/drawable/provision_text_logo_image_lite.xml' 'res/drawable/provision_text_logo_image_lite.xml'
fetch 'library/provision/src/main/res/drawable/provision_icon_arrow.xml' 'res/drawable/provision_icon_arrow.xml'
fetch 'library/provision/src/main/res/drawable/provision_next.xml' 'res/drawable/provision_next.xml'
fetch 'library/provision/src/main/res/drawable/provision_next_lite.xml' 'res/drawable/provision_next_lite.xml'
fetch 'library/provision/src/main/res/drawable/provision_logo_image_bg.webp' 'res/drawable/provision_logo_image_bg.webp'
fetch 'library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/GlowController.java' 'java/com/sevtinge/hyperceiler/provision/renderengine/GlowController.java'
fetch 'library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java' 'java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java'
fetch 'library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/RenderViewLayout.java' 'java/com/sevtinge/hyperceiler/provision/renderengine/RenderViewLayout.java'

curl -fsSL --retry 3 --retry-delay 2 "$BASE/LICENSE" -o "$ROOT/LICENSE"
