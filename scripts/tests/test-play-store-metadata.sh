#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VALIDATOR="$ROOT/scripts/validate-play-store-metadata.sh"

"$VALIDATOR" "$ROOT" >/dev/null
"$ROOT/scripts/publish-play-store-metadata.sh" --validate-only >/dev/null

if env -u GOOGLE_PLAY_ACCESS_TOKEN "$ROOT/scripts/publish-play-store-metadata.sh" >/dev/null 2>&1; then
  echo "Expected publishing without a Play access token to fail." >&2
  exit 1
fi

TEMP_ROOT="$(mktemp -d)"
trap 'rm -rf "$TEMP_ROOT"' EXIT
mkdir -p "$TEMP_ROOT/fastlane/metadata/android/en-US"
cp -R "$ROOT/fastlane/metadata/android/en-US/." \
  "$TEMP_ROOT/fastlane/metadata/android/en-US/"
mkdir -p "$TEMP_ROOT/play-store" "$TEMP_ROOT/.github/workflows" \
  "$TEMP_ROOT/app/src/main/kotlin/com/nutsnews/app/feature/personalization" \
  "$TEMP_ROOT/app/src/main/kotlin/com/nutsnews/app"
cp "$ROOT/play-store/"*.json "$TEMP_ROOT/play-store/"
cp "$ROOT/.github/workflows/play-store-metadata.yml" "$TEMP_ROOT/.github/workflows/"
cp "$ROOT/app/src/main/kotlin/com/nutsnews/app/MainActivity.kt" \
  "$TEMP_ROOT/app/src/main/kotlin/com/nutsnews/app/"
cp "$ROOT/app/src/main/kotlin/com/nutsnews/app/feature/personalization/PersonalizationScreen.kt" \
  "$TEMP_ROOT/app/src/main/kotlin/com/nutsnews/app/feature/personalization/"

printf '%090d\n' 0 > "$TEMP_ROOT/fastlane/metadata/android/en-US/short_description.txt"
if "$VALIDATOR" "$TEMP_ROOT" >/dev/null 2>&1; then
  echo "Expected an overlong short description to fail." >&2
  exit 1
fi

cp "$ROOT/fastlane/metadata/android/en-US/short_description.txt" \
  "$TEMP_ROOT/fastlane/metadata/android/en-US/short_description.txt"
jq '.targetAudience.designedForChildren = true' \
  "$ROOT/play-store/policy-declarations.json" \
  > "$TEMP_ROOT/play-store/policy-declarations.json"
if "$VALIDATOR" "$TEMP_ROOT" >/dev/null 2>&1; then
  echo "Expected a contradictory target-audience declaration to fail." >&2
  exit 1
fi

echo "Play Store metadata failure-path tests passed."
