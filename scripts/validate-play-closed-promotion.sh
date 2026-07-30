#!/usr/bin/env bash

set -euo pipefail

repository_root="${NUTSNEWS_REPOSITORY_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
configuration="$repository_root/config/play/closed-testing.json"
workflow="$repository_root/.github/workflows/play-closed-promotion.yml"
promotion_script="$repository_root/scripts/promote-play-closed.sh"
token_script="$repository_root/scripts/mint-google-play-access-token.sh"

fail() {
  echo "Google Play closed promotion validation failed: $*" >&2
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
  .sourceTrack == "internal" and
  .targetTrack == "alpha" and
  .releaseStatus == "completed" and
  .reviewBehavior == "CANCEL_IN_REVIEW_AND_SUBMIT" and
  .githubEnvironment == "play-internal" and
  .serviceAccountSecret == "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"
' "$configuration" >/dev/null ||
  fail "closed-testing configuration is incomplete"

required_workflow_fragments=(
  "workflow_dispatch:"
  "environment: play-internal"
  'secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON'
  "REPLACE_ALPHA_REVIEW"
  'refs/heads/main'
  "promote-play-closed.sh"
  "CANCEL_IN_REVIEW_AND_SUBMIT"
  "cancel-in-progress: false"
)
for fragment in "${required_workflow_fragments[@]}"; do
  grep -Fq -- "$fragment" "$workflow" ||
    fail "workflow is missing: $fragment"
done
[[ "$(grep -Fc 'secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON' "$workflow")" == "1" ]] ||
  fail "Play service-account secret must be referenced exactly once"

if grep -Eiq 'production|release-signing|NUTSNEWS_UPLOAD_KEY' "$workflow" "$promotion_script"; then
  fail "closed promotion must not contain production or signing access"
fi
grep -Fq \
  "changesInReviewBehavior=\${review_behavior}" \
  "$promotion_script" ||
  fail "promotion does not explicitly control the existing review"
for fragment in \
  '--output "$commit_response"' \
  "--write-out '%{http_code}'" \
  "Play rejected the Alpha release commit (HTTP"; do
  grep -Fq -- "$fragment" "$promotion_script" ||
    fail "promotion does not report structured Play commit errors: $fragment"
done
grep -Fq \
  '.reviewBehavior == "CANCEL_IN_REVIEW_AND_SUBMIT"' \
  "$workflow" ||
  fail "workflow does not verify review-replacement behavior"

action_revisions="$(
  sed -nE 's/^[[:space:]]*uses:[[:space:]]+[^@]+@([^[:space:]#]+).*$/\1/p' \
    "$workflow"
)"
[[ -n "$action_revisions" ]] || fail "workflow must use a pinned checkout action"
while IFS= read -r revision; do
  [[ "$revision" =~ ^[0-9a-f]{40}$ ]] ||
    fail "action is not pinned to a full commit SHA: $revision"
done <<<"$action_revisions"

echo "Google Play closed promotion validation passed."
