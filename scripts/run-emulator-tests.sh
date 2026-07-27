#!/usr/bin/env bash

set -euo pipefail

matrix_id="${1:?matrix id is required}"
form_factor="${2:?form factor is required}"
video_size="${3:?video size is required}"
artifact_dir="artifacts/emulator/$matrix_id"
log_dir="build-logs"
recording_path="/sdcard/nutsnews-$matrix_id.mp4"
recording_pid=""

mkdir -p "$artifact_dir" "$log_dir"
adb logcat -c >/dev/null 2>&1 || true
adb shell rm -f "$recording_path"

stop_recording() {
  if [[ -z "$recording_pid" ]]; then
    return
  fi
  recorder_process="$(
    adb shell pidof screenrecord 2>/dev/null | tr -d '\r' || true
  )"
  if [[ "$recorder_process" =~ ^[0-9]+$ ]]; then
    adb shell kill -2 "$recorder_process" >/dev/null 2>&1 || true
  else
    kill "$recording_pid" >/dev/null 2>&1 || true
  fi
  wait "$recording_pid" >/dev/null 2>&1 || true
  recording_pid=""
}

capture_failure_artifacts() {
  local status="$1"

  printf '%s\n' "$status" > "$artifact_dir/exit-code.txt"
  adb exec-out screencap -p > "$artifact_dir/failure-screen.png" || true
  adb logcat -d -v threadtime > "$artifact_dir/logcat.txt" || true
  adb shell dumpsys activity activities > "$artifact_dir/activity.txt" || true
  adb shell dumpsys window windows > "$artifact_dir/window.txt" || true
  stop_recording
  adb pull "$recording_path" "$artifact_dir/failure-video.mp4" >/dev/null 2>&1 || true
}

trap stop_recording EXIT

adb shell screenrecord \
  --size "$video_size" \
  --bit-rate 2000000 \
  --time-limit 180 \
  "$recording_path" \
  >/dev/null 2>&1 &
recording_pid="$!"

if [[ "$form_factor" == "tablet" ]]; then
  set +e
  ./scripts/verify-screenshot-goldens.sh \
    2>&1 | tee "$log_dir/$matrix_id-screenshot.log"
  screenshot_status="${PIPESTATUS[0]}"
  set -e
  if [[ "$screenshot_status" -ne 0 ]]; then
    capture_failure_artifacts "$screenshot_status"
    exit "$screenshot_status"
  fi
fi

if [[ "${NUTSNEWS_EMULATOR_CONTROLLED_FAILURE:-false}" == "true" ]]; then
  set +e
  ./gradlew --stacktrace --console=plain installDebug \
    2>&1 | tee "$log_dir/$matrix_id-controlled-failure.log"
  install_status="${PIPESTATUS[0]}"
  set -e
  if [[ "$install_status" -eq 0 ]]; then
    adb shell input keyevent KEYCODE_WAKEUP
    adb shell am start -W -n com.nutsnews.app/.MainActivity >/dev/null
    install_status=86
  fi
  capture_failure_artifacts "$install_status"
  exit "$install_status"
fi

set +e
./gradlew \
  --stacktrace \
  --console=plain \
  connectedDebugAndroidTest \
  2>&1 | tee "$log_dir/$matrix_id-instrumentation.log"
test_status="${PIPESTATUS[0]}"
set -e

if [[ "$test_status" -ne 0 ]]; then
  capture_failure_artifacts "$test_status"
  exit "$test_status"
fi

stop_recording
adb shell rm -f "$recording_path"
echo "Emulator checks passed for $matrix_id."
