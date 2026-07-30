#!/usr/bin/env bash

set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
validator="$repository_root/scripts/validate-play-closed-promotion.sh"
fixture="$(mktemp -d)"
trap 'rm -rf "$fixture"' EXIT

fail() {
  echo "Google Play closed promotion test failed: $*" >&2
  exit 1
}

assert_failure() {
  local description="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    fail "$description unexpectedly succeeded"
  fi
}

"$validator" >/dev/null

prepare_fixture() {
  rm -rf "$fixture"
  mkdir -p \
    "$fixture/.github/workflows" \
    "$fixture/config/play" \
    "$fixture/scripts"
  cp "$repository_root/.github/workflows/play-closed-promotion.yml" \
    "$fixture/.github/workflows/"
  cp "$repository_root/config/play/closed-testing.json" "$fixture/config/play/"
  cp \
    "$repository_root/scripts/mint-google-play-access-token.sh" \
    "$repository_root/scripts/promote-play-closed.sh" \
    "$repository_root/scripts/validate-play-closed-promotion.sh" \
    "$fixture/scripts/"
  chmod +x "$fixture/scripts/"*.sh
}

prepare_fixture
jq '.reviewBehavior = "ERROR_IF_IN_REVIEW"' \
  "$fixture/config/play/closed-testing.json" >"$fixture/config.tmp"
mv "$fixture/config.tmp" "$fixture/config/play/closed-testing.json"
assert_failure \
  "non-replacing review behavior" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-closed-promotion.sh"

prepare_fixture
sed -i.bak '/REPLACE_ALPHA_REVIEW/d' \
  "$fixture/.github/workflows/play-closed-promotion.yml"
rm "$fixture/.github/workflows/play-closed-promotion.yml.bak"
assert_failure \
  "missing review replacement confirmation" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-closed-promotion.sh"

prepare_fixture
sed -i.bak \
  's/Play rejected the Alpha release commit/Play rejected the release commit/' \
  "$fixture/scripts/promote-play-closed.sh"
rm "$fixture/scripts/promote-play-closed.sh.bak"
assert_failure \
  "missing structured Play commit error reporting" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-closed-promotion.sh"

prepare_fixture
sed -i.bak 's/changesNotSentForReview=true/changesNotSentForReview=false/' \
  "$fixture/scripts/promote-play-closed.sh"
rm "$fixture/scripts/promote-play-closed.sh.bak"
assert_failure \
  "missing Console-review fallback" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-closed-promotion.sh"

echo "Google Play closed promotion tests passed."
