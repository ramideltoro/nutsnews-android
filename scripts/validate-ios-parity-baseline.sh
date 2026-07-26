#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "${script_dir}/.." && pwd)"
baseline="${repo_root}/docs/parity/ios-parity-baseline.md"
checklist="${repo_root}/docs/parity/ios-parity-checklist.md"
capture_dir="${repo_root}/docs/parity/reference/ios"
checksum_file="${capture_dir}/captures.sha256"
frozen_commit="972dda3a0208bd97ddcdc2cd660bbd4360fc6898"

fail() {
  printf 'parity baseline validation failed: %s\n' "$1" >&2
  exit 1
}

require_file() {
  local path="$1"
  [[ -s "${path}" ]] || fail "missing or empty file ${path#${repo_root}/}"
}

require_identifier() {
  local identifier="$1"
  local path="$2"
  grep -Eq "(^|[^[:alnum:]])${identifier}([^[:alnum:]]|$)" "${path}" ||
    fail "${identifier} is not represented in ${path#${repo_root}/}"
}

require_file "${baseline}"
require_file "${checklist}"
require_file "${checksum_file}"

grep -Fq "${frozen_commit}" "${baseline}" ||
  fail "frozen commit is absent from baseline document"
grep -Fq "${frozen_commit}" "${checklist}" ||
  fail "frozen commit is absent from parity checklist"

for task_number in $(seq 1 66); do
  task_id="$(printf 'T%02d' "${task_number}")"
  require_identifier "${task_id}" "${checklist}"
done

for screen_number in $(seq 1 26); do
  screen_id="$(printf 'S%02d' "${screen_number}")"
  require_identifier "${screen_id}" "${baseline}"
done

for flow_number in $(seq 1 20); do
  flow_id="$(printf 'F%02d' "${flow_number}")"
  require_identifier "${flow_id}" "${baseline}"
done

for motion_number in $(seq 1 13); do
  motion_id="$(printf 'M%02d' "${motion_number}")"
  require_identifier "${motion_id}" "${baseline}"
done

requirement_ids="$(
  grep -Eo 'P-T[0-9]{2}-[0-9]{2}' "${checklist}" || true
)"
requirement_count="$(printf '%s\n' "${requirement_ids}" | sed '/^$/d' | wc -l | tr -d ' ')"
unique_requirement_count="$(
  printf '%s\n' "${requirement_ids}" | sed '/^$/d' | sort -u | wc -l | tr -d ' '
)"
[[ "${requirement_count}" -eq "${unique_requirement_count}" ]] ||
  fail "parity requirement identifiers are not unique"

png_files=(
  "ipad-feed.png"
  "ipad-onboarding.png"
  "iphone-feed.png"
  "iphone-onboarding.png"
  "iphone-splash.png"
  "iphone-theme-creativePremium-feed.png"
  "iphone-theme-modernSaaS-feed.png"
  "iphone-theme-moodyCyberpunk-feed.png"
  "iphone-theme-sakura-feed.png"
  "iphone-theme-sanJuan-feed.png"
)

video_files=(
  "ipad-startup.mp4"
  "iphone-startup.mp4"
)

for file_name in "${png_files[@]}"; do
  capture_path="${capture_dir}/${file_name}"
  require_file "${capture_path}"

  signature="$(od -An -tx1 -N8 "${capture_path}" | tr -d ' \n')"
  [[ "${signature}" == "89504e470d0a1a0a" ]] ||
    fail "${file_name} does not have a PNG signature"

  file_description="$(file -b "${capture_path}")"
  if [[ "${file_name}" == ipad-* ]]; then
    [[ "${file_description}" == *"1668 x 2420"* ]] ||
      fail "${file_name} is not the frozen tablet resolution"
  else
    [[ "${file_description}" == *"1206 x 2622"* ]] ||
      fail "${file_name} is not the frozen phone resolution"
  fi
done

for file_name in "${video_files[@]}"; do
  capture_path="${capture_dir}/${file_name}"
  require_file "${capture_path}"
  LC_ALL=C head -c 32 "${capture_path}" | grep -aq 'ftyp' ||
    fail "${file_name} does not have an ISO media header"
done

if command -v sha256sum >/dev/null 2>&1; then
  (
    cd "${capture_dir}"
    sha256sum --check captures.sha256 >/dev/null
  ) || fail "capture checksum mismatch"
elif command -v shasum >/dev/null 2>&1; then
  (
    cd "${capture_dir}"
    shasum -a 256 --check captures.sha256 >/dev/null
  ) || fail "capture checksum mismatch"
else
  fail "sha256sum or shasum is required"
fi

printf 'iOS parity baseline validation passed: 66 tasks, 26 screens, 20 flows, 13 motions, 12 captures.\n'
