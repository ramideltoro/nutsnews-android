#!/usr/bin/env bash

set -euo pipefail

workflow=".github/workflows/android-ci.yml"
emulator_script="scripts/run-emulator-tests.sh"

if [[ ! -f "$workflow" ]]; then
  echo "Missing Android CI workflow: $workflow" >&2
  exit 1
fi

if [[ ! -x "$emulator_script" ]]; then
  echo "Missing executable emulator runner: $emulator_script" >&2
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
  "phone API 26"
  "phone API 36"
  "tablet API 36"
  "api-level: 26"
  "api-level: 36"
  "profile: pixel_c"
  "actions/cache/restore@"
  "actions/cache/save@"
  "run-emulator-tests.sh"
  "exercise_emulator_failure"
  "validate-emulator-failure-artifacts.sh"
  "if: failure()"
  "actions/upload-artifact@"
  "build-logs/android-ci.log"
  "app/build/reports/androidTests"
  "app/build/reports/screenshot-parity"
  "app/build/outputs/androidTest-results"
  "app/build/outputs/connected_android_test_additional_output"
)

for fragment in "${required_fragments[@]}"; do
  if ! grep -Fq -- "$fragment" "$workflow"; then
    echo "Android CI workflow is missing required fragment: $fragment" >&2
    exit 1
  fi
done

required_emulator_fragments=(
  "connectedDebugAndroidTest"
  "verify-screenshot-goldens.sh"
  "screenrecord"
  "failure-screen.png"
  "failure-video.mp4"
  "logcat.txt"
  "activity.txt"
  "window.txt"
)

for fragment in "${required_emulator_fragments[@]}"; do
  if ! grep -Fq -- "$fragment" "$emulator_script"; then
    echo "Emulator runner is missing required fragment: $fragment" >&2
    exit 1
  fi
done

echo "Android CI contract validation passed: $action_count pinned action uses."
