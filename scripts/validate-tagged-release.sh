#!/usr/bin/env bash

set -euo pipefail

repository_root="${NUTSNEWS_REPOSITORY_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
workflow="$repository_root/.github/workflows/tagged-release.yml"
contract="$repository_root/config/release/tagged-release.json"
version_script="$repository_root/scripts/release-version.sh"
deploy_script="$repository_root/scripts/deploy-play-internal.sh"
environment_script="$repository_root/scripts/configure-release-environments.sh"

fail() {
  echo "Tagged release contract validation failed: $*" >&2
  exit 1
}

for required_file in \
  "$workflow" \
  "$contract" \
  "$version_script" \
  "$deploy_script" \
  "$environment_script"; do
  [[ -f "$required_file" ]] || fail "missing $required_file"
done
for required_script in "$version_script" "$deploy_script" "$environment_script"; do
  [[ -x "$required_script" ]] || fail "$required_script is not executable"
done
command -v jq >/dev/null 2>&1 || fail "jq is required"

jq -e '
  .repository == "ramideltoro/nutsnews-android" and
  .tagGlob == "android-v[0-9]*.[0-9]*.[0-9]*" and
  .packageName == "com.nutsnews.app" and
  .track == "internal" and
  .versionCode.strategy == "semver-base-1000" and
  .versionCode.minimumExclusive == 2 and
  .versionCode.maximumInclusive == 2100000000 and
  .versionCode.minorMaximum == 999 and
  .versionCode.patchMaximum == 999 and
  ([.githubEnvironments | keys[]] | sort) == ["play-internal","release-signing"] and
  ([.githubEnvironments[].deploymentPolicies[] | select(.type == "tag") | .name] | unique) == ["android-v*.*.*"] and
  ([.githubEnvironments[].deploymentPolicies[] | select(.type == "branch") | .name] | unique) == ["main"]
' "$contract" >/dev/null || fail "release configuration is incomplete"

grep -Fq -- '- "android-v[0-9]*.[0-9]*.[0-9]*"' "$workflow" ||
  fail "workflow tag filter differs from the versioned contract"
if grep -Eq '^[[:space:]]+workflow_dispatch:' "$workflow"; then
  fail "tagged releases must not support manual dispatch"
fi

job_block() {
  local job_name="$1"
  awk -v marker="  ${job_name}:" '
    $0 == marker { inside = 1; print; next }
    inside && /^  [A-Za-z0-9_-]+:/ { exit }
    inside { print }
  ' "$workflow"
}

metadata_block="$(job_block release-metadata)"
signing_block="$(job_block sign-release)"
play_block="$(job_block deploy-internal)"
github_release_block="$(job_block github-release)"
[[ -n "$metadata_block" && -n "$signing_block" && -n "$play_block" && -n "$github_release_block" ]] ||
  fail "workflow is missing a required release job"

top_permissions="$(
  awk '
    $0 == "permissions:" { inside = 1; next }
    inside && /^[^ ]/ { exit }
    inside { print }
  ' "$workflow"
)"
grep -Fq 'contents: read' <<<"$top_permissions" || fail "top-level contents permission must be read"
grep -Fq 'actions: read' <<<"$top_permissions" || fail "top-level actions permission must be read"
if grep -Fq 'write' <<<"$top_permissions"; then
  fail "top-level workflow permissions must not grant write access"
fi
grep -Fq 'contents: write' <<<"$github_release_block" ||
  fail "only the GitHub Release job may write repository contents"

signing_secrets=(
  NUTSNEWS_UPLOAD_KEYSTORE_BASE64
  NUTSNEWS_UPLOAD_KEYSTORE_PASSWORD
  NUTSNEWS_UPLOAD_KEY_ALIAS
  NUTSNEWS_UPLOAD_KEY_PASSWORD
)
for secret_name in "${signing_secrets[@]}"; do
  [[ "$(grep -Fc "secrets.${secret_name}" "$workflow")" == "1" ]] ||
    fail "$secret_name must be referenced exactly once"
  grep -Fq "secrets.${secret_name}" <<<"$signing_block" ||
    fail "$secret_name must remain confined to the signing job"
done
[[ "$(grep -Fc 'secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON' "$workflow")" == "1" ]] ||
  fail "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON must be referenced exactly once"
grep -Fq 'secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON' <<<"$play_block" ||
  fail "Play credentials must remain confined to the Play job"
if grep -Fq 'GOOGLE_PLAY_SERVICE_ACCOUNT_JSON' <<<"$signing_block"; then
  fail "signing job must not receive Play credentials"
fi
for secret_name in "${signing_secrets[@]}"; do
  if grep -Fq "$secret_name" <<<"$play_block"; then
    fail "Play job must not receive signing credentials"
  fi
done
grep -Fq 'environment: release-signing' <<<"$signing_block" ||
  fail "signing job must use release-signing"
grep -Fq 'environment: play-internal' <<<"$play_block" ||
  fail "Play job must use play-internal"

required_fragments=(
  "uses: ./.github/workflows/android-ci.yml"
  "uses: ./.github/workflows/security.yml"
  "- android-validation"
  "- security-validation"
  "verify-release-bundle.sh"
  "deploy-play-internal.sh"
  "actions/upload-artifact@"
  "actions/download-artifact@"
  "gh release create"
  "retention-days: 30"
  "cancel-in-progress: false"
)
for fragment in "${required_fragments[@]}"; do
  grep -Fq -- "$fragment" "$workflow" || fail "workflow is missing: $fragment"
done
if grep -Fiq 'production' "$workflow" "$deploy_script"; then
  fail "tagged release automation must not contain a production deployment path"
fi

action_revisions="$(
  sed -nE 's/^[[:space:]]*uses:[[:space:]]+[^@]+@([^[:space:]#]+).*$/\1/p' "$workflow"
)"
[[ -n "$action_revisions" ]] || fail "release workflow must use pinned actions"
while IFS= read -r revision; do
  [[ "$revision" =~ ^[0-9a-f]{40}$ ]] || fail "action is not pinned to a full commit SHA: $revision"
done <<<"$action_revisions"

echo "Tagged release contract validation passed."
