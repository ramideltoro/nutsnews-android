#!/usr/bin/env bash

set -euo pipefail

configuration="config/play/internal-testing.json"
workflow=".github/workflows/security.yml"
gradle_file="app/build.gradle.kts"
api_verifier="scripts/verify-play-internal-access.sh"

fail() {
  echo "Google Play provisioning contract validation failed: $*" >&2
  exit 1
}

for required_file in \
  "$configuration" \
  "$workflow" \
  "$gradle_file" \
  "$api_verifier"; do
  [[ -f "$required_file" ]] || fail "missing $required_file"
done

[[ -x "$api_verifier" ]] || fail "$api_verifier is not executable"

jq -e '
  .packageName == "com.nutsnews.app" and
  .track == "internal" and
  .releaseStatus == "completed" and
  .githubEnvironment == "play-internal" and
  .serviceAccountSecret == "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON" and
  (.requiredAppPermissions | length == 2) and
  (.forbiddenPermissions | length == 4)
' "$configuration" >/dev/null ||
  fail "internal testing configuration is incomplete"

grep -Fq 'applicationId = "com.nutsnews.app"' "$gradle_file" ||
  fail "Gradle application ID differs from Play configuration"

required_workflow_fragments=(
  "name: Google Play provisioning contract"
  "name: Google Play internal access"
  "environment: play-internal"
  'secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON'
  "verify-play-internal-access.sh"
)

for fragment in "${required_workflow_fragments[@]}"; do
  grep -Fq -- "$fragment" "$workflow" ||
    fail "Security workflow is missing: $fragment"
done

if [[ "${1:-}" == "--remote" ]]; then
  command -v gh >/dev/null 2>&1 || fail "gh is required for remote validation"
  repository="${GITHUB_REPOSITORY:-ramideltoro/nutsnews-android}"
  environment_json="$(
    gh api "repos/${repository}/environments/play-internal"
  )"
  jq -e '
    .deployment_branch_policy.protected_branches == false and
    .deployment_branch_policy.custom_branch_policies == true
  ' <<<"$environment_json" >/dev/null ||
    fail "play-internal must use explicit branch and tag deployment policies"

  ./scripts/configure-release-environments.sh --check >/dev/null ||
    fail "release environment deployment policies differ from the tagged-release contract"

  secret_names="$(
    gh secret list \
      --repo "$repository" \
      --env play-internal \
      --json name \
      --jq '.[].name'
  )"
  grep -Fxq 'GOOGLE_PLAY_SERVICE_ACCOUNT_JSON' <<<"$secret_names" ||
    fail "play-internal is missing GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"
fi

echo "Google Play provisioning contract validation passed."
