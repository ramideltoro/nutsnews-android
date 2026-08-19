#!/usr/bin/env bash

set -euo pipefail

service_account_json="${1:-}"
version_name="${2:-}"
version_code="${3:-}"
release_notes="${4:-}"
repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
configuration="$repository_root/config/play/production.json"

fail() {
  echo "Google Play Production promotion failed: $*" >&2
  exit 1
}

[[ -f "$service_account_json" ]] || fail "service-account JSON file is required"
[[ -f "$configuration" ]] || fail "missing $configuration"
[[ -n "$release_notes" ]] || fail "release notes are required"
(( ${#release_notes} <= 500 )) || fail "release notes must not exceed 500 characters"
[[ "$version_code" =~ ^[0-9]+$ ]] || fail "version code must be numeric"
for command_name in curl jq; do
  command -v "$command_name" >/dev/null 2>&1 ||
    fail "$command_name is required"
done

if ! package_name="$(jq -er '.packageName' "$configuration")"; then
  fail "Production configuration is missing packageName"
fi
if ! source_track="$(jq -er '.sourceTrack' "$configuration")"; then
  fail "Production configuration is missing sourceTrack"
fi
if ! target_track="$(jq -er '.targetTrack' "$configuration")"; then
  fail "Production configuration is missing targetTrack"
fi
if ! release_status="$(jq -er '.releaseStatus' "$configuration")"; then
  fail "Production configuration is missing releaseStatus"
fi
if ! review_behavior="$(jq -er '.reviewBehavior' "$configuration")"; then
  fail "Production configuration is missing reviewBehavior"
fi
[[ "$package_name" == "com.nutsnews.app" ]] || fail "unexpected package"
[[ "$source_track" == "alpha" ]] || fail "source track must be Alpha"
[[ "$target_track" == "production" ]] || fail "target track must be Production"
[[ "$release_status" == "completed" ]] || fail "Production promotion must be a full rollout"
[[ "$review_behavior" == "ERROR_IF_IN_REVIEW" ]] ||
  fail "Production promotion must not cancel an existing review"

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

structured_error() {
  local response_path="$1"
  local fallback="$2"
  local detail_summary
  local message
  local status
  message="$(jq -r '.error.message // empty' "$response_path" 2>/dev/null || true)"
  status="$(jq -r '.error.status // empty' "$response_path" 2>/dev/null || true)"
  detail_summary="$(
    jq -r '
      [
        .error.details[]? |
        objects |
        (.reason? // empty),
        (.violations[]? |
          objects |
          [(.type // empty), (.subject // empty), (.description // empty)] |
          map(select(type == "string" and length > 0)) |
          join(": ")),
        (.fieldViolations[]? |
          objects |
          [(.field // empty), (.description // empty)] |
          map(select(type == "string" and length > 0)) |
          join(": "))
      ] |
      map(select(type == "string" and length > 0)) |
      unique |
      join("; ")
    ' "$response_path" 2>/dev/null || true
  )"
  [[ -n "$message" ]] || message="$fallback"
  if [[ -n "$status" && -n "$detail_summary" ]]; then
    printf '%s [status: %s; details: %s]' "$message" "$status" "$detail_summary"
  elif [[ -n "$status" ]]; then
    printf '%s [status: %s]' "$message" "$status"
  elif [[ -n "$detail_summary" ]]; then
    printf '%s [details: %s]' "$message" "$detail_summary"
  else
    printf '%s' "$message"
  fi
}

json_shape() {
  local response_path="$1"
  if [[ ! -s "$response_path" ]]; then
    printf '{"topLevelType":"empty","fieldTypes":{}}'
    return
  fi
  jq -c '
    if type == "object" then
      {topLevelType:type, fieldTypes:with_entries(.value |= type)}
    else
      {topLevelType:type, fieldTypes:{}}
    end
  ' "$response_path" 2>/dev/null ||
    printf '{"topLevelType":"invalid-json","fieldTypes":{}}'
}

list_track_releases() {
  local track_name="$1"
  local response_path="$2"
  local operation="$3"
  local http_status
  if ! http_status="$(
    curl --silent --show-error \
      --output "$response_path" \
      --write-out '%{http_code}' \
      --header "Authorization: Bearer $access_token" \
      "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/tracks/${track_name}/releases"
  )"; then
    fail "could not reach Play while $operation"
  fi
  if [[ ! "$http_status" =~ ^2[0-9][0-9]$ ]]; then
    fail "$operation failed (HTTP $http_status): $(
      structured_error "$response_path" "Play returned no structured error message."
    )"
  fi
}

release_lifecycle() {
  local response_path="$1"
  jq -r \
    --arg name "$version_name" \
    --argjson code "$version_code" \
    '[.releases[]? |
      select(.releaseName == $name) |
      select(any(.activeArtifacts[]?; .versionCode == $code))][0]
      .releaseLifecycleState // empty' \
    "$response_path"
}

emit_result() {
  local status="$1"
  local lifecycle="$2"
  jq -nc \
    --arg package "$package_name" \
    --arg source "$source_track" \
    --arg track "$target_track" \
    --arg name "$version_name" \
    --argjson code "$version_code" \
    --arg behavior "$review_behavior" \
    --arg status "$status" \
    --arg lifecycle "$lifecycle" \
    '{
      packageName:$package,
      sourceTrack:$source,
      track:$track,
      versionName:$name,
      versionCode:$code,
      reviewBehavior:$behavior,
      status:$status,
      releaseLifecycleState:$lifecycle
    }'
}

source_releases="$work_dir/source-releases.json"
list_track_releases \
  "$source_track" \
  "$source_releases" \
  "querying the Alpha release lifecycle"
source_lifecycle="$(release_lifecycle "$source_releases")"
[[ "$source_lifecycle" == "RELEASE_LIFECYCLE_STATE_PUBLISHED" ]] ||
  fail "$version_name ($version_code) must be published on Alpha before Production promotion; lifecycle was ${source_lifecycle:-not-found}"

target_releases="$work_dir/target-releases.json"
list_track_releases \
  "$target_track" \
  "$target_releases" \
  "querying Production access and release lifecycle"
target_lifecycle="$(release_lifecycle "$target_releases")"
case "$target_lifecycle" in
  RELEASE_LIFECYCLE_STATE_PUBLISHED)
    emit_result "published" "$target_lifecycle"
    exit 0
    ;;
  RELEASE_LIFECYCLE_STATE_IN_REVIEW)
    emit_result "in-review" "$target_lifecycle"
    exit 0
    ;;
  RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW)
    emit_result "pending-console-review" "$target_lifecycle"
    exit 0
    ;;
  RELEASE_LIFECYCLE_STATE_APPROVED_NOT_PUBLISHED)
    emit_result "approved-not-published" "$target_lifecycle"
    exit 0
    ;;
  RELEASE_LIFECYCLE_STATE_NOT_APPROVED)
    fail "Production release $version_name ($version_code) was not approved"
    ;;
  RELEASE_LIFECYCLE_STATE_DRAFT|RELEASE_LIFECYCLE_STATE_UNSPECIFIED)
    fail "Production release $version_name ($version_code) has unexpected lifecycle $target_lifecycle"
    ;;
  "")
    ;;
  *)
    fail "Production release $version_name ($version_code) returned unknown lifecycle $target_lifecycle"
    ;;
esac

create_edit() {
  local response_path="$1"
  curl --silent --show-error --fail \
    --request POST \
    --header "Authorization: Bearer $access_token" \
    --header "Content-Type: application/json" \
    --data '{}' \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits" \
    >"$response_path" || fail "could not create a controlled Play edit"
  if ! edit_id="$(jq -er '.id | strings | select(length > 0)' "$response_path")"; then
    fail "Play create-edit response did not include an edit id"
  fi
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
target_track_response="$work_dir/target-track.json"
create_edit "$edit_response"
if ! target_track_http_status="$(
  curl --silent --show-error \
    --output "$target_track_response" \
    --write-out '%{http_code}' \
    --header "Authorization: Bearer $access_token" \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${target_track}"
)"; then
  fail "could not reach Play while querying the Production track"
fi
if [[ ! "$target_track_http_status" =~ ^2[0-9][0-9]$ ]]; then
  fail "Play rejected Production-track access (HTTP $target_track_http_status): $(
    structured_error "$target_track_response" "Play returned no structured error message."
  )"
fi
if ! returned_target_track="$(
  jq -er '.track | strings | select(length > 0)' "$target_track_response"
)"; then
  fail "Production-track response did not include a track name"
fi
[[ "$returned_target_track" == "$target_track" ]] ||
  fail "Play returned an unexpected target track"

country_availability_response="$work_dir/country-availability.json"
if ! country_availability_http_status="$(
  curl --silent --show-error \
    --output "$country_availability_response" \
    --write-out '%{http_code}' \
    --header "Authorization: Bearer $access_token" \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/countryAvailability/${target_track}"
)"; then
  fail "could not reach Play while querying Production country availability"
fi
if [[ ! "$country_availability_http_status" =~ ^2[0-9][0-9]$ ]]; then
  fail "Play rejected the Production country-availability query (HTTP $country_availability_http_status): $(
    structured_error "$country_availability_response" "Play returned no structured error message."
  )"
fi
country_availability_bytes="$(
  wc -c <"$country_availability_response" | tr -d '[:space:]'
)"
country_availability_shape="$(json_shape "$country_availability_response")"
if ! country_count="$(
  jq -er '(.countries // []) | arrays | length' "$country_availability_response"
)"; then
  fail "Production country-availability response did not include a valid countries list (HTTP $country_availability_http_status; bytes $country_availability_bytes; shape $country_availability_shape)"
fi
if ! rest_of_world="$(
  jq -r '
    (.restOfWorld // false) as $value |
    if ($value | type) == "boolean" then $value else error("invalid boolean") end
  ' "$country_availability_response"
)"; then
  fail "Production country-availability response did not include a valid restOfWorld state (HTTP $country_availability_http_status; bytes $country_availability_bytes; shape $country_availability_shape)"
fi
if (( country_count == 0 )) && [[ "$rest_of_world" != "true" ]]; then
  fail "Production has no selected countries or regions; configure App availability in Play Console before promotion"
fi

if jq -e --arg name "$version_name" --arg code "$version_code" '
  any(.releases[]?; .name == $name and any(.versionCodes[]?; . == $code))
' "$target_track_response" >/dev/null; then
  fail "Production track contains $version_name ($version_code), but the lifecycle API did not return a review or publishing state"
fi

published_code="$(
  jq -r '[.releases[]?.versionCodes[]? | tonumber] | max // 0' \
    "$target_track_response"
)"
(( version_code > published_code )) ||
  fail "version code $version_code must exceed current Production code $published_code"

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

track_update_response="$work_dir/track-update-response.json"
if ! track_update_http_status="$(
  curl --silent --show-error \
    --output "$track_update_response" \
    --write-out '%{http_code}' \
    --request PUT \
    --header "Authorization: Bearer $access_token" \
    --header "Content-Type: application/json" \
    --data-binary "@$track_update" \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${target_track}"
)"; then
  fail "could not reach Play while assigning the Alpha bundle to Production"
fi
if [[ ! "$track_update_http_status" =~ ^2[0-9][0-9]$ ]]; then
  fail "Play rejected the Production track update (HTTP $track_update_http_status): $(
    structured_error "$track_update_response" "Play returned no structured error message."
  )"
fi

commit_response="$work_dir/commit-response.json"
promotion_status="submitted"
release_lifecycle_state=""
if ! commit_http_status="$(
  curl --silent --show-error \
    --output "$commit_response" \
    --write-out '%{http_code}' \
    --request POST \
    --header "Authorization: Bearer $access_token" \
    --header "Content-Type: application/json" \
    --data '{}' \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}:commit?changesInReviewBehavior=${review_behavior}"
)"; then
  fail "could not reach Play while committing the Production release"
fi
if [[ ! "$commit_http_status" =~ ^2[0-9][0-9]$ ]]; then
  commit_error_message="$(
    structured_error "$commit_response" "Play returned no structured error message."
  )"
  console_review_message="Changes cannot be sent for review automatically. Please set the query parameter changesNotSentForReview to true. Once committed, the changes in this edit can be sent for review from the Google Play Console UI."
  if [[ "$commit_http_status" == "400" &&
    "$commit_error_message" == "$console_review_message" ]]; then
    deferred_commit_response="$work_dir/deferred-commit-response.json"
    if ! deferred_commit_http_status="$(
      curl --silent --show-error \
        --output "$deferred_commit_response" \
        --write-out '%{http_code}' \
        --request POST \
        --header "Authorization: Bearer $access_token" \
        --header "Content-Type: application/json" \
        --data '{}' \
        "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}:commit?changesNotSentForReview=true"
    )"; then
      fail "could not reach Play while staging Production for Console review"
    fi
    if [[ ! "$deferred_commit_http_status" =~ ^2[0-9][0-9]$ ]]; then
      fail "Play rejected the deferred Production commit (HTTP $deferred_commit_http_status): $(
        structured_error "$deferred_commit_response" "Play returned no structured error message."
      )"
    fi
    promotion_status="pending-console-review"
    release_lifecycle_state="RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW"
  else
    fail "Play rejected the Production release commit (HTTP $commit_http_status): $commit_error_message"
  fi
fi
edit_id=""

post_commit_releases="$work_dir/post-commit-releases.json"
list_track_releases \
  "$target_track" \
  "$post_commit_releases" \
  "verifying the Production release lifecycle"
post_commit_lifecycle="$(release_lifecycle "$post_commit_releases")"
case "$post_commit_lifecycle" in
  RELEASE_LIFECYCLE_STATE_PUBLISHED)
    promotion_status="published"
    release_lifecycle_state="$post_commit_lifecycle"
    ;;
  RELEASE_LIFECYCLE_STATE_IN_REVIEW)
    promotion_status="in-review"
    release_lifecycle_state="$post_commit_lifecycle"
    ;;
  RELEASE_LIFECYCLE_STATE_NOT_SENT_FOR_REVIEW)
    promotion_status="pending-console-review"
    release_lifecycle_state="$post_commit_lifecycle"
    ;;
  RELEASE_LIFECYCLE_STATE_APPROVED_NOT_PUBLISHED)
    promotion_status="approved-not-published"
    release_lifecycle_state="$post_commit_lifecycle"
    ;;
  RELEASE_LIFECYCLE_STATE_NOT_APPROVED)
    fail "Production release $version_name ($version_code) was not approved"
    ;;
  RELEASE_LIFECYCLE_STATE_DRAFT|RELEASE_LIFECYCLE_STATE_UNSPECIFIED)
    fail "Production release $version_name ($version_code) has unexpected lifecycle $post_commit_lifecycle"
    ;;
  "")
    ;;
  *)
    fail "Production release $version_name ($version_code) returned unknown lifecycle $post_commit_lifecycle"
    ;;
esac

emit_result "$promotion_status" "$release_lifecycle_state"
