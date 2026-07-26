#!/usr/bin/env bash

set -euo pipefail

workflow=".github/workflows/android-ci.yml"

if [[ ! -f "$workflow" ]]; then
  echo "Missing Android CI workflow: $workflow" >&2
  exit 1
fi

action_revisions="$(
  sed -nE 's/^[[:space:]]*uses:[[:space:]]+[^@]+@([^[:space:]#]+).*$/\1/p' "$workflow"
)"

if [[ -z "$action_revisions" ]]; then
  echo "Android CI must use at least one external action." >&2
  exit 1
fi

action_count=0
while IFS= read -r revision; do
  if [[ ! "$revision" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Action revision is not pinned to a full commit SHA: $revision" >&2
    exit 1
  fi
  action_count=$((action_count + 1))
done <<< "$action_revisions"

required_fragments=(
  "pull_request:"
  "push:"
  "branches:"
  "- main"
  "cancel-in-progress: true"
  "gradle/actions/wrapper-validation@"
  "java-version: \"17\""
  "gradle/actions/setup-gradle@"
  "cache-read-only:"
  "compileDebugKotlin"
  "lintDebug"
  "testDebugUnitTest"
  "assembleDebug"
  "if: failure()"
  "actions/upload-artifact@"
  "build-logs/android-ci.log"
)

for fragment in "${required_fragments[@]}"; do
  if ! grep -Fq -- "$fragment" "$workflow"; then
    echo "Android CI workflow is missing required fragment: $fragment" >&2
    exit 1
  fi
done

echo "Android CI contract validation passed: $action_count pinned action uses."
