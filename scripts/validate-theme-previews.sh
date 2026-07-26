#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

png_dimension() {
  local file="$1"
  local offset="$2"
  local hex
  hex="$(xxd -p -s "$offset" -l 4 "$file")"
  printf '%d' "$((16#$hex))"
}

manifest="docs/design-system/previews.sha256"
[[ -f "$manifest" ]] || {
  echo "Missing design-system preview manifest: $manifest" >&2
  exit 1
}

[[ "$(wc -l < "$manifest" | tr -d ' ')" == "6" ]] || {
  echo "Design-system preview manifest must contain exactly six themes." >&2
  exit 1
}

while read -r expected_hash file; do
  [[ -f "$file" ]] || {
    echo "Missing design-system preview: $file" >&2
    exit 1
  }

  [[ "$(xxd -p -l 8 "$file")" == "89504e470d0a1a0a" ]] || {
    echo "Invalid PNG signature: $file" >&2
    exit 1
  }
  [[ "$(png_dimension "$file" 16)" == "1080" ]] || {
    echo "Unexpected preview width: $file" >&2
    exit 1
  }
  [[ "$(png_dimension "$file" 20)" == "2400" ]] || {
    echo "Unexpected preview height: $file" >&2
    exit 1
  }
  [[ "$(xxd -p -s 25 -l 1 "$file")" == "06" ]] || {
    echo "Preview no longer uses RGBA pixels: $file" >&2
    exit 1
  }
  [[ "$(sha256 "$file")" == "$expected_hash" ]] || {
    echo "Design-system preview pixels changed: $file" >&2
    exit 1
  }
done < "$manifest"

for theme in amber sakura saas foxy friday bambi; do
  grep -Fq "docs/design-system/previews/$theme.png" "$manifest" || {
    echo "Design-system preview manifest is missing $theme." >&2
    exit 1
  }
done

echo "Design-system preview validation passed: six deterministic 1080x2400 RGBA renders."
