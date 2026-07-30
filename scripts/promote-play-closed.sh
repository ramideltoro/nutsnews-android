#!/usr/bin/env bash

set -euo pipefail

service_account_json="${1:-}"
version_name="${2:-}"
version_code="${3:-}"
release_notes="${4:-}"
repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
configuration="$repository_root/config/play/closed-testing.json"

fail() {
  echo "Google Play closed promotion failed: $*" >&2
  exit 1
}

[[ -f "$service_account_json" ]] || fail "service-account JSON file is required"
[[ -f "$configuration" ]] || fail "missing $configuration"
[[ -n "$release_notes" ]] || fail "release notes are required"
[[ "$version_code" =~ ^[0-9]+$ ]] || fail "version code must be numeric"
for command_name in curl jq; do
  command -v "$command_name" >/dev/null 2>&1 ||
    fail "$command_name is required"
done

package_name="$(jq -er '.packageName' "$configuration")"
source_track="$(jq -er '.sourceTrack' "$configuration")"
target_track="$(jq -er '.targetTrack' "$configuration")"
release_status="$(jq -er '.releaseStatus' "$configuration")"
review_behavior="$(jq -er '.reviewBehavior' "$configuration")"
[[ "$package_name" == "com.nutsnews.app" ]] || fail "unexpected package"
[[ "$source_track" == "internal" ]] || fail "source track must be internal"
[[ "$target_track" == "alpha" ]] || fail "target track must be alpha"
[[ "$release_status" == "completed" ]] || fail "closed promotion must be a full rollout"
[[ "$review_behavior" == "CANCEL_IN_REVIEW_AND_SUBMIT" ]] ||
  fail "review replacement must be explicit"

read -r derived_name derived_code < <(
  "$repository_root/scripts/release-version.sh" "android-v$version_name"
)
[[ "$derived_name" == "$version_name" && "$derived_code" == "$version_code" ]] ||
  fail "version name and deterministic version code do not match"

access_token="$(
  "$repository_root/scripts/mint-google-play-access-token.sh" \
    "$service_account_json"
)"
[[ -n "$access_token" ]] || fail "access token minting returned no token"

work_dir="$(mktemp -d)"
edit_id=""
cleanup() {
  if [[ -n "$edit_id" ]]; then
    curl --silent --request DELETE \
      --header "Authorization: Bearer $access_token" \
      "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}" \
      >/dev/null 2>&1 || true
  fi
  rm -rf "$work_dir"
}
trap cleanup EXIT
umask 077

create_edit() {
  local response_path="$1"
  curl --silent --show-error --fail \
    --request POST \
    --header "Authorization: Bearer $access_token" \
    --header "Content-Type: application/json" \
    --data '{}' \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits" \
    >"$response_path" || fail "could not create a controlled Play edit"
  edit_id="$(jq -er '.id' "$response_path")"
}

delete_edit() {
  curl --silent --show-error --fail \
    --request DELETE \
    --header "Authorization: Bearer $access_token" \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}" \
    >/dev/null || fail "controlled Play edit cleanup failed"
  edit_id=""
}

edit_response="$work_dir/edit-response.json"
source_response="$work_dir/source-track.json"
target_response="$work_dir/target-track.json"
create_edit "$edit_response"

curl --silent --show-error --fail \
  --header "Authorization: Bearer $access_token" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${source_track}" \
  >"$source_response" || fail "could not query the internal track"
jq -e --arg track "$source_track" --arg name "$version_name" --arg code "$version_code" '
  .track == $track and
  any(.releases[]?; .name == $name and any(.versionCodes[]?; . == $code))
' "$source_response" >/dev/null ||
  fail "$version_name ($version_code) is not verified on the internal track"

curl --silent --show-error --fail \
  --header "Authorization: Bearer $access_token" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${target_track}" \
  >"$target_response" || fail "could not query the Alpha closed-testing track"
[[ "$(jq -er '.track' "$target_response")" == "$target_track" ]] ||
  fail "Play returned an unexpected target track"

if jq -e --arg name "$version_name" --arg code "$version_code" '
  any(.releases[]?; .name == $name and any(.versionCodes[]?; . == $code))
' "$target_response" >/dev/null; then
  delete_edit
  jq -nc \
    --arg package "$package_name" \
    --arg track "$target_track" \
    --arg name "$version_name" \
    --argjson code "$version_code" \
    --arg behavior "$review_behavior" \
    '{
      packageName:$package,
      track:$track,
      versionName:$name,
      versionCode:$code,
      reviewBehavior:$behavior,
      status:"already-present"
    }'
  exit 0
fi

published_code="$(
  jq -r '[.releases[]?.versionCodes[]? | tonumber] | max // 0' \
    "$target_response"
)"
(( version_code > published_code )) ||
  fail "version code $version_code must exceed current Alpha code $published_code"

track_update="$work_dir/track-update.json"
jq -nc \
  --arg track "$target_track" \
  --arg name "$version_name" \
  --arg status "$release_status" \
  --arg code "$version_code" \
  --arg notes "$release_notes" \
  '{
    track:$track,
    releases:[{
      name:$name,
      status:$status,
      versionCodes:[$code],
      releaseNotes:[{language:"en-US",text:$notes}]
    }]
  }' >"$track_update"

curl --silent --show-error --fail \
  --request PUT \
  --header "Authorization: Bearer $access_token" \
  --header "Content-Type: application/json" \
  --data-binary "@$track_update" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${target_track}" \
  >/dev/null || fail "could not assign the verified bundle to Alpha"

curl --silent --show-error --fail \
  --request POST \
  --header "Authorization: Bearer $access_token" \
  --header "Content-Type: application/json" \
  --data '{}' \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}:commit?changesInReviewBehavior=${review_behavior}" \
  >/dev/null || fail "could not replace and resubmit the current Play review"
edit_id=""

verify_edit_response="$work_dir/verify-edit-response.json"
verify_track_response="$work_dir/verify-track-response.json"
create_edit "$verify_edit_response"
curl --silent --show-error --fail \
  --header "Authorization: Bearer $access_token" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${target_track}" \
  >"$verify_track_response" || fail "could not verify Alpha after promotion"
jq -e --arg track "$target_track" --arg name "$version_name" --arg code "$version_code" '
  .track == $track and
  any(.releases[]?; .name == $name and any(.versionCodes[]?; . == $code))
' "$verify_track_response" >/dev/null ||
  fail "post-promotion query did not return the expected Alpha release"
delete_edit

jq -nc \
  --arg package "$package_name" \
  --arg track "$target_track" \
  --arg name "$version_name" \
  --argjson code "$version_code" \
  --arg behavior "$review_behavior" \
  '{
    packageName:$package,
    track:$track,
    versionName:$name,
    versionCode:$code,
    reviewBehavior:$behavior,
    status:"submitted"
  }'
