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

rm -rf "$DEST/java/me/rain/motiondemo"
rm -f "$DEST/res/layout/page_overview.xml" \
      "$DEST/res/layout/page_profiles.xml" \
      "$DEST/res/layout/page_settings.xml" \
      "$DEST/res/values/widgets.xml" \
      "$DEST/res/drawable/bg_card.xml" \
      "$DEST/res/drawable/bg_profile.xml" \
      "$DEST/res/drawable/ic_chevron.xml" \
      "$DEST/res/drawable/ic_drivers.xml" \
      "$DEST/res/drawable/ic_overview.xml" \
      "$DEST/res/drawable/ic_profiles.xml" \
      "$DEST/res/drawable/ic_settings.xml"
rm -f "$DEST/res/raw/glow.glsl"
rm -f "$DEST/res/drawable/provision_logo_image.xml" \
      "$DEST/res/drawable/provision_logo_image_lite.xml" \
      "$DEST/res/drawable/provision_text_logo_image.xml" \
      "$DEST/res/drawable/provision_text_logo_image_lite.xml" \
      "$DEST/res/drawable/provision_icon_arrow.xml" \
      "$DEST/res/drawable/provision_next.xml" \
      "$DEST/res/drawable/provision_next_lite.xml" \
      "$DEST/res/drawable/provision_logo_image_bg.webp" \
      "$DEST/res/values/compat_colors.xml" \
      "$DEST/res/values/dimens.xml"
fetch 'library/provision/src/main/res/raw/glow.glsl' 'res/raw/glow.agsl'
fetch 'library/provision/src/main/res/drawable/provision_service_state.xml' 'res/drawable/provision_service_state.xml'
fetch 'library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java' 'java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java'
fetch 'library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/RenderViewLayout.java' 'java/com/sevtinge/hyperceiler/provision/renderengine/RenderViewLayout.java'

sed -i 's/import com.sevtinge.hyperceiler.provision.R;/import me.rain.kernel.R;/' \
    "$DEST/java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java"

curl -fsSL --retry 3 --retry-delay 2 "$BASE/LICENSE" -o "$ROOT/LICENSE"
