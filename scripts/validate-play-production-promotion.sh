#!/usr/bin/env bash

set -euo pipefail

repository_root="${NUTSNEWS_REPOSITORY_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
configuration="$repository_root/config/play/production.json"
workflow="$repository_root/.github/workflows/play-production-promotion.yml"
promotion_script="$repository_root/scripts/promote-play-production.sh"
token_script="$repository_root/scripts/mint-google-play-access-token.sh"

fail() {
  echo "Google Play Production promotion validation failed: $*" >&2
  exit 1
}

for required_file in \
  "$configuration" \
  "$workflow" \
  "$promotion_script" \
  "$token_script"; do
  [[ -f "$required_file" ]] || fail "missing $required_file"
done
for required_script in "$promotion_script" "$token_script"; do
  [[ -x "$required_script" ]] || fail "$required_script is not executable"
done

jq -e '
  .packageName == "com.nutsnews.app" and
  .sourceTrack == "alpha" and
  .targetTrack == "production" and
  .releaseStatus == "completed" and
  .reviewBehavior == "ERROR_IF_IN_REVIEW" and
  .githubEnvironment == "play-internal" and
  .serviceAccountSecret == "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"
' "$configuration" >/dev/null ||
  fail "Production configuration is incomplete"

required_workflow_fragments=(
  "workflow_dispatch:"
  "environment: play-internal"
  'secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON'
  "PROMOTE_TO_PRODUCTION"
  'refs/heads/main'
  "promote-play-production.sh"
  "ERROR_IF_IN_REVIEW"
  "pending-console-review"
  "approved-not-published"
  "cancel-in-progress: false"
)
for fragment in "${required_workflow_fragments[@]}"; do
  grep -Fq -- "$fragment" "$workflow" || fail "workflow is missing: $fragment"
done
[[ "$(grep -Fc 'secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON' "$workflow")" == "1" ]] ||
  fail "Play service-account secret must be referenced exactly once"
if grep -Eiq 'release-signing|NUTSNEWS_UPLOAD_KEY' "$workflow" "$promotion_script"; then
  fail "Production promotion must not receive signing access"
fi

required_script_fragments=(
  'source_track" == "alpha"'
  'target_track" == "production"'
  'release_status" == "completed"'
  'review_behavior" == "ERROR_IF_IN_REVIEW"'
  '/tracks/${track_name}/releases'
  '/countryAvailability/${target_track}'
  'Production configuration is missing packageName'
  'Play create-edit response did not include an edit id'
  'Production-track response did not include a track name'
  'Production country-availability response did not include a valid countries list'
  'Production country-availability response did not include a valid restOfWorld state'
  'json_shape()'
  '"topLevelType":"empty"'
  'fieldTypes:with_entries(.value |= type)'
  'bytes $country_availability_bytes; shape $country_availability_shape'
  'rest_of_world'
  'RELEASE_LIFECYCLE_STATE_PUBLISHED'
  'changesInReviewBehavior=${review_behavior}'
  'changesNotSentForReview=true'
  'Production has no selected countries or regions'
  '.error.details[]?'
  'Play rejected Production-track access (HTTP'
  'Play rejected the Production country-availability query (HTTP'
  'Play rejected the Production track update (HTTP'
  'Play rejected the Production release commit (HTTP'
)
for fragment in "${required_script_fragments[@]}"; do
  grep -Fq -- "$fragment" "$promotion_script" ||
    fail "promotion script is missing: $fragment"
done
rest_of_world_parser="$(
  awk '
    /if ! rest_of_world="\$\(/ { capture = 1 }
    capture { print }
    capture && /\)"; then/ { exit }
  ' "$promotion_script"
)"
grep -Fq 'jq -r ' <<<"$rest_of_world_parser" ||
  fail "restOfWorld must be parsed without jq exit-status evaluation"
if grep -Fq 'jq -er ' <<<"$rest_of_world_parser"; then
  fail "restOfWorld false must not fail JSON extraction"
fi

action_revisions="$(
  sed -nE 's/^[[:space:]]*uses:[[:space:]]+[^@]+@([^[:space:]#]+).*$/\1/p' \
    "$workflow"
)"
[[ -n "$action_revisions" ]] || fail "workflow must use a pinned checkout action"
while IFS= read -r revision; do
  [[ "$revision" =~ ^[0-9a-f]{40}$ ]] ||
    fail "action is not pinned to a full commit SHA: $revision"
done <<<"$action_revisions"

echo "Google Play Production promotion validation passed."
