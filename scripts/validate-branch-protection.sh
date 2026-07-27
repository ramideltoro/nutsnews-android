#!/usr/bin/env bash

set -euo pipefail

policy=".github/branch-protection.json"
android_workflow=".github/workflows/android-ci.yml"
security_workflow=".github/workflows/security.yml"

fail() {
  echo "Branch protection validation failed: $*" >&2
  exit 1
}

for required_file in "$policy" "$android_workflow" "$security_workflow"; do
  [[ -f "$required_file" ]] || fail "missing $required_file"
done

command -v jq >/dev/null 2>&1 || fail "jq is required"

expected_checks=(
  "Validate Gradle wrapper"
  "Compile, lint, test, and assemble"
  "Emulator (phone API 26)"
  "Emulator (phone API 36)"
  "Emulator (tablet API 36)"
  "Dependency review"
  "CodeQL (Java/Kotlin)"
  "Branch protection policy"
  "Release signing contract"
)

configured_checks=()
while IFS= read -r check_name; do
  configured_checks+=("$check_name")
done < <(
  jq -er '.required_status_checks.checks | sort_by(.context)[] | .context' "$policy"
)

sorted_expected_checks=()
while IFS= read -r check_name; do
  sorted_expected_checks+=("$check_name")
done < <(printf '%s\n' "${expected_checks[@]}" | sort)

if [[ "${configured_checks[*]}" != "${sorted_expected_checks[*]}" ]]; then
  fail "required checks do not match the repository policy contract"
fi

jq -e '
  .required_status_checks.strict == true and
  ([.required_status_checks.checks[].app_id] | all(. == 15368)) and
  .enforce_admins == true and
  .required_pull_request_reviews != null and
  .required_pull_request_reviews.dismiss_stale_reviews == true and
  .required_pull_request_reviews.required_approving_review_count == 0 and
  .required_conversation_resolution == true and
  .allow_force_pushes == false and
  .allow_deletions == false
' "$policy" >/dev/null || fail "local policy does not enforce every control"

literal_workflow_names=(
  "Validate Gradle wrapper"
  "Compile, lint, test, and assemble"
  "Dependency review"
  "CodeQL (Java/Kotlin)"
  "Branch protection policy"
  "Release signing contract"
)

for check_name in "${literal_workflow_names[@]}"; do
  if ! grep -Fq -- "name: $check_name" .github/workflows/*.yml; then
    fail "required check is not a workflow job name: $check_name"
  fi
done

grep -Fq -- "name: Emulator (\${{ matrix.label }})" "$android_workflow" ||
  fail "emulator job no longer derives its check name from matrix.label"

for label in "phone API 26" "phone API 36" "tablet API 36"; do
  grep -Fq -- "label: $label" "$android_workflow" ||
    fail "emulator matrix is missing required label: $label"
done

if [[ "${1:-}" == "--remote" ]]; then
  command -v gh >/dev/null 2>&1 || fail "gh is required for remote validation"
  repository="${GITHUB_REPOSITORY:-ramideltoro/nutsnews-android}"
  protection_json="$(gh api "repos/${repository}/branches/main/protection")"

  remote_checks=()
  while IFS= read -r check_name; do
    remote_checks+=("$check_name")
  done < <(
    jq -er '.required_status_checks.checks | sort_by(.context)[] | .context' \
      <<<"$protection_json"
  )
  if [[ "${remote_checks[*]}" != "${sorted_expected_checks[*]}" ]]; then
    fail "remote required checks differ from .github/branch-protection.json"
  fi

  jq -e '
    .required_status_checks.strict == true and
    ([.required_status_checks.checks[].app_id] | all(. == 15368)) and
    .enforce_admins.enabled == true and
    .required_pull_request_reviews != null and
    .required_pull_request_reviews.dismiss_stale_reviews == true and
    .required_pull_request_reviews.required_approving_review_count == 0 and
    .required_conversation_resolution.enabled == true and
    .allow_force_pushes.enabled == false and
    .allow_deletions.enabled == false
  ' <<<"$protection_json" >/dev/null ||
    fail "remote main protection does not enforce every control"
fi

echo "Branch protection validation passed: ${#expected_checks[@]} synchronized required checks."
