#!/usr/bin/env bash

set -euo pipefail

artifact_dir="${1:?artifact directory is required}"

if [[ "${DEVICE_TEST_OUTCOME:-}" != "failure" ]]; then
  echo "Controlled emulator probe must report a failed device-test step." >&2
  exit 1
fi

required_files=(
  "exit-code.txt"
  "failure-screen.png"
  "failure-video.mp4"
  "logcat.txt"
  "activity.txt"
  "window.txt"
)

for file_name in "${required_files[@]}"; do
  file_path="$artifact_dir/$file_name"
  if [[ ! -s "$file_path" ]]; then
    echo "Controlled emulator probe is missing a non-empty artifact: $file_path" >&2
    exit 1
  fi
done

if [[ "$(tr -d '[:space:]' < "$artifact_dir/exit-code.txt")" == "0" ]]; then
  echo "Controlled emulator probe unexpectedly recorded a zero exit code." >&2
  exit 1
fi

echo "Controlled emulator failure artifact validation passed."
