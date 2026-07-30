#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

png_dimension() {
  local file="$1"
  local offset="$2"
  local hex
  hex="$(xxd -p -s "$offset" -l 4 "$file")"
  printf '%d' "$((16#$hex))"
}

assert_png() {
  local file="$1"
  local expected_width="$2"
  local expected_height="$3"
  local signature
  local color_type

  [[ -f "$file" ]] || {
    echo "Missing PNG asset: $file" >&2
    exit 1
  }

  signature="$(xxd -p -l 8 "$file")"
  [[ "$signature" == "89504e470d0a1a0a" ]] || {
    echo "Invalid PNG signature: $file" >&2
    exit 1
  }

  [[ "$(png_dimension "$file" 16)" == "$expected_width" ]] || {
    echo "Unexpected PNG width: $file" >&2
    exit 1
  }
  [[ "$(png_dimension "$file" 20)" == "$expected_height" ]] || {
    echo "Unexpected PNG height: $file" >&2
    exit 1
  }

  color_type="$(xxd -p -s 25 -l 1 "$file")"
  [[ "$color_type" == "06" ]] || {
    echo "PNG no longer preserves an alpha channel: $file" >&2
    exit 1
  }
}

assert_contains() {
  local file="$1"
  local fragment="$2"

  grep -Fq -- "$fragment" "$file" || {
    echo "$file is missing required content: $fragment" >&2
    exit 1
  }
}

brand_icon="app/src/main/res/drawable-nodpi/brand_icon.png"
brand_splash="app/src/main/res/drawable-nodpi/brand_splash.png"

assert_png "$brand_icon" 1024 1024
assert_png "$brand_splash" 1254 1254

[[ "$(sha256 "$brand_icon")" == "00a812d26633fd2db1e6941d8e64912dc4de321e32b482329250eb75912b7be2" ]] || {
  echo "Approved app icon pixels differ from the frozen iOS source." >&2
  exit 1
}
[[ "$(sha256 "$brand_splash")" == "3ba7557550ccab3720f451cfa8db6c7de3d2eac1d634b4192df395a03bc087f6" ]] || {
  echo "Approved splash pixels differ from the frozen iOS source." >&2
  exit 1
}

for density_spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  density="${density_spec%%:*}"
  size="${density_spec##*:}"
  assert_png "app/src/main/res/mipmap-$density/ic_launcher.png" "$size" "$size"
  assert_png "app/src/main/res/mipmap-$density/ic_launcher_round.png" "$size" "$size"
done

for icon_xml in \
  app/src/main/res/mipmap-anydpi/ic_launcher.xml \
  app/src/main/res/mipmap-anydpi/ic_launcher_round.xml; do
  assert_contains "$icon_xml" "<adaptive-icon"
  assert_contains "$icon_xml" "@drawable/ic_launcher_foreground"
done

for inset_edge in insetLeft insetTop insetRight insetBottom; do
  assert_contains \
    "app/src/main/res/drawable/ic_launcher_foreground.xml" \
    "android:${inset_edge}=\"19.4444%\""
done
assert_contains \
  "app/src/main/res/drawable/ic_launcher_foreground.xml" \
  "android:src=\"@drawable/brand_icon\""

for icon_xml in \
  app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml \
  app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml; do
  assert_contains "$icon_xml" "<monochrome"
  assert_contains "$icon_xml" "@drawable/ic_launcher_monochrome"
done

assert_contains "app/src/main/AndroidManifest.xml" "android:icon=\"@mipmap/ic_launcher\""
assert_contains "app/src/main/AndroidManifest.xml" "android:roundIcon=\"@mipmap/ic_launcher_round\""
assert_contains "app/src/main/AndroidManifest.xml" "android:theme=\"@style/Theme.NutsNews.Starting\""
assert_contains "app/src/main/res/values/themes.xml" "@drawable/splash_screen"
assert_contains "app/src/main/res/values-v31/themes.xml" "android:windowSplashScreenAnimatedIcon"
assert_contains "app/src/main/res/values/colors.xml" "#F28A0F"
assert_contains "app/src/main/res/values-night/colors.xml" "#F28A0F"

echo "Brand asset validation passed: approved iOS sources, 10 legacy icons, safe-zone adaptive/round/monochrome launchers, and light/dark splash resources."
