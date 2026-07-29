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
UPLOAD_API="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PACKAGE/edits"
AUTH_HEADER="Authorization: Bearer $TOKEN"
EDIT_ID=""

fail() {
  echo "Play Store metadata publishing failed: $*" >&2
  exit 1
}

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
)" || fail "could not create a Play edit"
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
  "$API/$EDIT_ID/listings/$LOCALE" >/dev/null ||
  fail "could not update the $LOCALE listing"

upload_images() {
  local image_type="$1"
  shift
  curl --silent --show-error --fail-with-body \
    --request DELETE \
    --header "$AUTH_HEADER" \
    "$API/$EDIT_ID/listings/$LOCALE/$image_type" >/dev/null ||
    fail "could not clear existing $image_type images"
  local image_path
  for image_path in "$@"; do
    curl --silent --show-error --fail-with-body \
      --request POST \
      --header "$AUTH_HEADER" \
      --header "Content-Type: image/png" \
      --data-binary "@$ROOT/$image_path" \
      "$UPLOAD_API/$EDIT_ID/listings/$LOCALE/$image_type?uploadType=media" \
      >/dev/null ||
      fail "could not upload $image_type image"
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

commit_response=""
if ! commit_response="$(
  curl --silent --show-error --fail-with-body \
    --request POST \
    --header "$AUTH_HEADER" \
    "$API/$EDIT_ID:commit?changesNotSentForReview=true&changesInReviewBehavior=ERROR_IF_IN_REVIEW"
)"; then
  commit_error_message="$(
    jq -er '.error.message | strings | select(length > 0)' \
      <<<"$commit_response" 2>/dev/null || true
  )"
  [[ -n "$commit_error_message" ]] ||
    commit_error_message="Play returned no structured error message."
  fail "Play rejected metadata commit: $commit_error_message"
fi
EDIT_ID=""

verify_response="$(
  curl --silent --show-error --fail-with-body \
    --request POST \
    --header "$AUTH_HEADER" \
    --header "Content-Type: application/json" \
    --data '{}' \
    "$API"
)" || fail "could not create a verification edit"
EDIT_ID="$(jq -er '.id' <<<"$verify_response")"

remote_listing="$(
  curl --silent --show-error --fail-with-body \
    --header "$AUTH_HEADER" \
    "$API/$EDIT_ID/listings/$LOCALE"
)" || fail "could not query the committed $LOCALE listing"
jq -e \
  --argjson expected "$listing_payload" \
  '.title == $expected.title and
   .shortDescription == $expected.shortDescription and
   .fullDescription == $expected.fullDescription' \
  <<<"$remote_listing" >/dev/null ||
  fail "committed listing text does not match the repository"

verify_image_count() {
  local image_type="$1"
  local expected_count="$2"
  local remote_images
  remote_images="$(
    curl --silent --show-error --fail-with-body \
      --header "$AUTH_HEADER" \
      "$API/$EDIT_ID/listings/$LOCALE/$image_type"
  )" || fail "could not query committed $image_type images"
  [[ "$(jq '.images | length' <<<"$remote_images")" == "$expected_count" ]] ||
    fail "committed $image_type image count does not match the repository"
}

verify_image_count icon 1
verify_image_count featureGraphic 1
verify_image_count phoneScreenshots "${#phone_screenshots[@]}"
verify_image_count tenInchScreenshots "${#tablet_screenshots[@]}"

echo "Verified Google Play listing and graphics for $PACKAGE ($LOCALE)."
