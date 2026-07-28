#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LISTING="$ROOT/play-store/listing.json"
METADATA="$ROOT/fastlane/metadata/android/en-US"

if [[ "${1:-}" == "--validate-only" ]]; then
  exec "$ROOT/scripts/validate-play-store-metadata.sh" "$ROOT"
fi

"$ROOT/scripts/validate-play-store-metadata.sh" "$ROOT"

TOKEN="${GOOGLE_PLAY_ACCESS_TOKEN:-}"
[[ -n "$TOKEN" ]] || {
  echo "GOOGLE_PLAY_ACCESS_TOKEN is required." >&2
  exit 1
}

PACKAGE="$(jq -er '.packageName' "$LISTING")"
LOCALE="$(jq -er '.locale' "$LISTING")"
API="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE/edits"
AUTH_HEADER="Authorization: Bearer $TOKEN"
EDIT_ID=""

cleanup() {
  if [[ -n "$EDIT_ID" ]]; then
    curl --silent --output /dev/null --request DELETE \
      --header "$AUTH_HEADER" "$API/$EDIT_ID" || true
  fi
}
trap cleanup EXIT

create_response="$(
  curl --silent --show-error --fail-with-body \
    --request POST \
    --header "$AUTH_HEADER" \
    --header "Content-Type: application/json" \
    --data '{}' \
    "$API"
)"
EDIT_ID="$(jq -er '.id' <<<"$create_response")"

listing_payload="$(
  jq -n \
    --rawfile title "$METADATA/title.txt" \
    --rawfile short "$METADATA/short_description.txt" \
    --rawfile full "$METADATA/full_description.txt" \
    '{
      title: ($title | sub("\\n$"; "")),
      shortDescription: ($short | sub("\\n$"; "")),
      fullDescription: ($full | sub("\\n$"; ""))
    }'
)"

curl --silent --show-error --fail-with-body \
  --request PUT \
  --header "$AUTH_HEADER" \
  --header "Content-Type: application/json" \
  --data "$listing_payload" \
  "$API/$EDIT_ID/listings/$LOCALE" >/dev/null

upload_images() {
  local image_type="$1"
  shift
  curl --silent --show-error --fail-with-body \
    --request DELETE \
    --header "$AUTH_HEADER" \
    "$API/$EDIT_ID/listings/$LOCALE/$image_type" >/dev/null
  local image_path
  for image_path in "$@"; do
    curl --silent --show-error --fail-with-body \
      --request POST \
      --header "$AUTH_HEADER" \
      --form "image=@$ROOT/$image_path;type=image/png" \
      "$API/$EDIT_ID/listings/$LOCALE/$image_type" >/dev/null
  done
}

upload_images icon "$(jq -er '.assets.icon' "$LISTING")"
upload_images featureGraphic "$(jq -er '.assets.featureGraphic' "$LISTING")"
IFS=$'\n' read -r -d '' -a phone_screenshots \
  < <(jq -r '.assets.phoneScreenshots[]' "$LISTING" && printf '\0')
IFS=$'\n' read -r -d '' -a tablet_screenshots \
  < <(jq -r '.assets.tenInchScreenshots[]' "$LISTING" && printf '\0')
upload_images phoneScreenshots "${phone_screenshots[@]}"
upload_images tenInchScreenshots "${tablet_screenshots[@]}"

curl --silent --show-error --fail-with-body \
  --request POST \
  --header "$AUTH_HEADER" \
  "$API/$EDIT_ID:commit" >/dev/null
EDIT_ID=""

verify_response="$(
  curl --silent --show-error --fail-with-body \
    --request POST \
    --header "$AUTH_HEADER" \
    --header "Content-Type: application/json" \
    --data '{}' \
    "$API"
)"
EDIT_ID="$(jq -er '.id' <<<"$verify_response")"

remote_listing="$(
  curl --silent --show-error --fail-with-body \
    --header "$AUTH_HEADER" \
    "$API/$EDIT_ID/listings/$LOCALE"
)"
jq -e \
  --argjson expected "$listing_payload" \
  '.title == $expected.title and
   .shortDescription == $expected.shortDescription and
   .fullDescription == $expected.fullDescription' \
  <<<"$remote_listing" >/dev/null

verify_image_count() {
  local image_type="$1"
  local expected_count="$2"
  local remote_images
  remote_images="$(
    curl --silent --show-error --fail-with-body \
      --header "$AUTH_HEADER" \
      "$API/$EDIT_ID/listings/$LOCALE/$image_type"
  )"
  [[ "$(jq '.images | length' <<<"$remote_images")" == "$expected_count" ]]
}

verify_image_count icon 1
verify_image_count featureGraphic 1
verify_image_count phoneScreenshots "${#phone_screenshots[@]}"
verify_image_count tenInchScreenshots "${#tablet_screenshots[@]}"

echo "Verified Google Play listing and graphics for $PACKAGE ($LOCALE)."
