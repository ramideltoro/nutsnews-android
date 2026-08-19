#!/usr/bin/env bash

set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
validator="$repository_root/scripts/validate-play-production-promotion.sh"
fixture="$(mktemp -d)"
trap 'rm -rf "$fixture"' EXIT

fail() {
  echo "Google Play Production promotion test failed: $*" >&2
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
  cp "$repository_root/.github/workflows/play-production-promotion.yml" \
    "$fixture/.github/workflows/"
  cp "$repository_root/config/play/production.json" "$fixture/config/play/"
  cp \
    "$repository_root/scripts/mint-google-play-access-token.sh" \
    "$repository_root/scripts/promote-play-production.sh" \
    "$repository_root/scripts/validate-play-production-promotion.sh" \
    "$fixture/scripts/"
  chmod +x "$fixture/scripts/"*.sh
}

prepare_fixture
jq '.sourceTrack = "internal"' \
  "$fixture/config/play/production.json" >"$fixture/config.tmp"
mv "$fixture/config.tmp" "$fixture/config/play/production.json"
assert_failure \
  "non-Alpha source track" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

prepare_fixture
jq '.reviewBehavior = "CANCEL_IN_REVIEW_AND_SUBMIT"' \
  "$fixture/config/play/production.json" >"$fixture/config.tmp"
mv "$fixture/config.tmp" "$fixture/config/play/production.json"
assert_failure \
  "review-cancelling behavior" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

prepare_fixture
sed -i.bak '/PROMOTE_TO_PRODUCTION/d' \
  "$fixture/.github/workflows/play-production-promotion.yml"
rm "$fixture/.github/workflows/play-production-promotion.yml.bak"
assert_failure \
  "missing Production confirmation" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

prepare_fixture
sed -i.bak 's/RELEASE_LIFECYCLE_STATE_PUBLISHED/RELEASE_LIFECYCLE_STATE_IN_REVIEW/g' \
  "$fixture/scripts/promote-play-production.sh"
rm "$fixture/scripts/promote-play-production.sh.bak"
assert_failure \
  "missing published Alpha lifecycle gate" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

prepare_fixture
sed -i.bak 's/changesNotSentForReview=true/changesNotSentForReview=false/' \
  "$fixture/scripts/promote-play-production.sh"
rm "$fixture/scripts/promote-play-production.sh.bak"
assert_failure \
  "missing Console-review fallback" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

prepare_fixture
sed -i.bak 's,countryAvailability/${target_track},countryAvailability/alpha,' \
  "$fixture/scripts/promote-play-production.sh"
rm "$fixture/scripts/promote-play-production.sh.bak"
assert_failure \
  "missing Production country-availability preflight" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

prepare_fixture
sed -i.bak \
  '/if ! rest_of_world="$(/,/)"; then/ s/jq -r /jq -er /' \
  "$fixture/scripts/promote-play-production.sh"
rm "$fixture/scripts/promote-play-production.sh.bak"
assert_failure \
  "false rest-of-world parser" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

prepare_fixture
sed -i.bak \
  '/Play create-edit response did not include an edit id/d' \
  "$fixture/scripts/promote-play-production.sh"
rm "$fixture/scripts/promote-play-production.sh.bak"
assert_failure \
  "missing create-edit response diagnostic" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

prepare_fixture
sed -i.bak \
  's/fieldTypes:with_entries(.value |= type)/fieldValues:./' \
  "$fixture/scripts/promote-play-production.sh"
rm "$fixture/scripts/promote-play-production.sh.bak"
assert_failure \
  "country response values exposed by diagnostic" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" \
  "$fixture/scripts/validate-play-production-promotion.sh"

echo "Google Play Production promotion tests passed."
