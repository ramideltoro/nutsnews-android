#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
METADATA="$ROOT/fastlane/metadata/android/en-US"
LISTING="$ROOT/play-store/listing.json"
POLICY="$ROOT/play-store/policy-declarations.json"
WORKFLOW="$ROOT/.github/workflows/play-store-metadata.yml"
TOKEN_MINTER="$ROOT/scripts/mint-google-play-access-token.sh"
PUBLISHER="$ROOT/scripts/publish-play-store-metadata.sh"
EXPECTED_PRIVACY_URL="https://www.nutsnews.com/privacy/android"

fail() {
  echo "Play Store metadata validation failed: $*" >&2
  exit 1
}

require_file() {
  [[ -s "$1" ]] || fail "missing or empty file ${1#"$ROOT/"}"
}

character_count() {
  LC_ALL=C tr -d '\n' < "$1" | wc -c | tr -d ' '
}

png_dimensions() {
  local file="$1"
  local signature width_hex height_hex
  signature="$(xxd -p -l 8 "$file")"
  [[ "$signature" == "89504e470d0a1a0a" ]] || fail "${file#"$ROOT/"} is not a PNG"
  width_hex="$(xxd -p -s 16 -l 4 "$file")"
  height_hex="$(xxd -p -s 20 -l 4 "$file")"
  printf '%d %d\n' "$((16#$width_hex))" "$((16#$height_hex))"
}

assert_dimensions() {
  local file="$1" expected_width="$2" expected_height="$3"
  local actual_width actual_height
  read -r actual_width actual_height < <(png_dimensions "$file")
  [[ "$actual_width" == "$expected_width" && "$actual_height" == "$expected_height" ]] ||
    fail "${file#"$ROOT/"} is ${actual_width}x${actual_height}, expected ${expected_width}x${expected_height}"
}

require_file "$METADATA/title.txt"
require_file "$METADATA/short_description.txt"
require_file "$METADATA/full_description.txt"
require_file "$METADATA/changelogs/1001002.txt"
require_file "$LISTING"
require_file "$POLICY"
require_file "$WORKFLOW"
require_file "$TOKEN_MINTER"
require_file "$PUBLISHER"
require_file "$METADATA/assets.sha256"

[[ "$(character_count "$METADATA/title.txt")" -le 30 ]] || fail "title exceeds 30 characters"
[[ "$(character_count "$METADATA/short_description.txt")" -le 80 ]] ||
  fail "short description exceeds 80 characters"
[[ "$(character_count "$METADATA/full_description.txt")" -le 4000 ]] ||
  fail "full description exceeds 4000 characters"
[[ "$(character_count "$METADATA/changelogs/1001002.txt")" -le 500 ]] ||
  fail "release notes exceed 500 characters"

jq -e \
  --arg package "com.nutsnews.app" \
  --arg locale "en-US" \
  --arg url "$EXPECTED_PRIVACY_URL" \
  '.schemaVersion == 1 and
   .packageName == $package and
   .locale == $locale and
   .category == "NEWS_AND_MAGAZINES" and
   .privacyPolicyUrl == $url and
   .release.track == "internal" and
   .release.versionName == "1.1.2" and
   .release.versionCode == 1001002' \
  "$LISTING" >/dev/null || fail "listing.json contract mismatch"

jq -e \
  --arg package "com.nutsnews.app" \
  --arg url "$EXPECTED_PRIVACY_URL" \
  '.schemaVersion == 1 and
   .packageName == $package and
   .privacyPolicyUrl == $url and
   .appAccess.requiresLogin == false and
   .ads.containsAds == false and
   .dataSafety.encryptedInTransit == true and
   (.dataSafety.dataShared | length) == 0 and
   (.dataSafety.dataCollected | length) == 2 and
   .contentRating.dynamicNewsContent == true and
   .contentRating.externalPublisherLinks == true and
   .contentRating.userGeneratedContent == false and
   .notifications.optional == true and
   .notifications.remotePush == false and
   .notifications.requestedAfterUserOptIn == true and
   .targetAudience.designedForChildren == false and
   .targetAudience.ageGroups == ["13-15", "16-17", "18+"]' \
  "$POLICY" >/dev/null || fail "policy declaration contract mismatch"

grep -Fq "$EXPECTED_PRIVACY_URL" "$METADATA/full_description.txt" ||
  fail "full description does not include the privacy URL"
grep -Fq "$EXPECTED_PRIVACY_URL" "$ROOT/app/src/main/kotlin/com/nutsnews/app/MainActivity.kt" ||
  fail "the app does not expose the Android privacy URL"
grep -Fq \
  "A local Android notification brings you back to Today’s Picks." \
  "$ROOT/app/src/main/kotlin/com/nutsnews/app/feature/personalization/PersonalizationScreen.kt" ||
  fail "notification disclosure no longer matches the implemented UI"

assert_dimensions "$METADATA/images/icon.png" 512 512
assert_dimensions "$METADATA/images/featureGraphic.png" 1024 500

IFS=$'\n' read -r -d '' -a phone_screenshots \
  < <(jq -r '.assets.phoneScreenshots[]' "$LISTING" && printf '\0')
IFS=$'\n' read -r -d '' -a tablet_screenshots \
  < <(jq -r '.assets.tenInchScreenshots[]' "$LISTING" && printf '\0')
[[ "${#phone_screenshots[@]}" -ge 4 ]] || fail "at least four phone screenshots are required"
[[ "${#tablet_screenshots[@]}" -ge 3 ]] || fail "at least three tablet screenshots are required"
for relative_path in "${phone_screenshots[@]}" "${tablet_screenshots[@]}"; do
  require_file "$ROOT/$relative_path"
  assert_dimensions "$ROOT/$relative_path" 1080 1920
  [[ "$(xxd -p -s 25 -l 1 "$ROOT/$relative_path")" == "02" ]] ||
    fail "$relative_path must be an opaque RGB PNG"
done
[[ "$(xxd -p -s 25 -l 1 "$METADATA/images/featureGraphic.png")" == "02" ]] ||
  fail "feature graphic must be an opaque RGB PNG"

(cd "$ROOT" && shasum -a 256 -c fastlane/metadata/android/en-US/assets.sha256 >/dev/null) ||
  fail "asset digest verification failed"

grep -Fq "workflow_dispatch:" "$WORKFLOW" || fail "metadata publishing must be manual"
grep -Fq "github.ref == 'refs/heads/main'" "$WORKFLOW" ||
  fail "metadata publishing is not restricted to main"
grep -Fq "environment: play-internal" "$WORKFLOW" ||
  fail "metadata publishing does not use play-internal"
[[ "$(grep -Fc 'secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON' "$WORKFLOW")" == "1" ]] ||
  fail "metadata publishing does not use the Play credential contract"
grep -Fq 'credential_dir="$(mktemp -d)"' "$WORKFLOW" ||
  fail "Play credentials are not isolated in a temporary directory"
grep -Fq 'rm -rf "$credential_dir"' "$WORKFLOW" ||
  fail "temporary Play credentials are not deleted"
grep -Fq "mint-google-play-access-token.sh" "$WORKFLOW" ||
  fail "metadata publishing does not mint a short-lived Play token"
grep -Fq 'GOOGLE_PLAY_ACCESS_TOKEN="$access_token"' "$WORKFLOW" ||
  fail "metadata publishing does not confine the token to the publishing process"
if grep -Fq "google-github-actions/auth" "$WORKFLOW"; then
  fail "metadata publishing must not depend on the IAM Credentials API"
fi
if grep -Fq "GITHUB_OUTPUT" "$WORKFLOW"; then
  fail "Play tokens must not cross steps through workflow outputs"
fi
if grep -Eq 'environment:[[:space:]]*release-signing|tracks/(production|beta|alpha)' "$WORKFLOW"; then
  fail "metadata publishing crosses a protected environment or track boundary"
fi
grep -Fq \
  'UPLOAD_API="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PACKAGE/edits"' \
  "$PUBLISHER" ||
  fail "Play images are not sent through the media upload endpoint"
grep -Fq \
  '$UPLOAD_API/$EDIT_ID/listings/$LOCALE/$image_type?uploadType=media' \
  "$PUBLISHER" ||
  fail "Play images do not use the simple media upload protocol"
grep -Fq -- '--header "Content-Type: image/png"' "$PUBLISHER" ||
  fail "Play image uploads do not declare the PNG media type"
grep -Fq -- '--data-binary "@$ROOT/$image_path"' "$PUBLISHER" ||
  fail "Play image uploads do not send raw media bytes"
if grep -Fq -- "--form" "$PUBLISHER"; then
  fail "Play simple media uploads must not use multipart form encoding"
fi
grep -Fq \
  'changesInReviewBehavior=ERROR_IF_IN_REVIEW' \
  "$PUBLISHER" ||
  fail "metadata commits could disrupt changes in review"
if grep -Fq 'changesNotSentForReview=' "$PUBLISHER"; then
  fail "metadata commits use unsupported review submission behavior"
fi

echo "Play Store listing, policy declarations, assets, and workflow contracts are valid."
