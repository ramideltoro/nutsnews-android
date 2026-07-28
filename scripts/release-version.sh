#!/usr/bin/env bash

set -euo pipefail

repository_root="${NUTSNEWS_REPOSITORY_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
contract="$repository_root/config/release/tagged-release.json"
tag_name="${1:-}"

fail() {
  echo "Release version validation failed: $*" >&2
  exit 1
}

[[ -f "$contract" ]] || fail "missing $contract"
command -v jq >/dev/null 2>&1 || fail "jq is required"
[[ -n "$tag_name" ]] || fail "an android-vX.Y.Z tag is required"

tag_regex='^android-v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'
[[ "$tag_name" =~ $tag_regex ]] ||
  fail "tag must match android-vX.Y.Z with stable, canonical SemVer integers"

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"
minor_maximum="$(jq -er '.versionCode.minorMaximum' "$contract")"
patch_maximum="$(jq -er '.versionCode.patchMaximum' "$contract")"
maximum_code="$(jq -er '.versionCode.maximumInclusive' "$contract")"
minimum_code="${2:-$(jq -er '.versionCode.minimumExclusive' "$contract")}"

[[ "$minimum_code" =~ ^[0-9]+$ ]] || fail "published version code must be numeric"
(( minor <= minor_maximum )) ||
  fail "minor version exceeds the base-1000 strategy bound"
(( patch <= patch_maximum )) ||
  fail "patch version exceeds the base-1000 strategy bound"

version_code=$((major * 1000000 + minor * 1000 + patch))
(( version_code <= maximum_code )) || fail "generated version code exceeds Android's maximum"
(( version_code > minimum_code )) ||
  fail "generated version code $version_code must be greater than published code $minimum_code"

printf '%s\t%s\n' "${major}.${minor}.${patch}" "$version_code"
