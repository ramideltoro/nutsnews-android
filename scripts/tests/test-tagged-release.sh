#!/usr/bin/env bash

set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
version_script="$repository_root/scripts/release-version.sh"
validator="$repository_root/scripts/validate-tagged-release.sh"

fail() {
  echo "Tagged release test failed: $*" >&2
  exit 1
}

assert_failure() {
  local description="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    fail "$description unexpectedly succeeded"
  fi
}

[[ "$("$version_script" android-v1.1.2)" == $'1.1.2\t1001002' ]] ||
  fail "1.1.2 did not map to deterministic code 1001002"
[[ "$("$version_script" android-v2.0.0 1999999)" == $'2.0.0\t2000000' ]] ||
  fail "major-version transition is not monotonic"
[[ "$("$version_script" android-v1.2.0 1001999)" == $'1.2.0\t1002000' ]] ||
  fail "minor-version transition is not monotonic"

for invalid_tag in \
  android-v1.1 \
  android-v1.1.2-rc.1 \
  android-v01.1.2 \
  android-v1.01.2 \
  android-v1.1.02 \
  v1.1.2 \
  android-v1.1000.0 \
  android-v1.1.1000; do
  assert_failure "invalid tag $invalid_tag" "$version_script" "$invalid_tag"
done
assert_failure "published version-code reuse" "$version_script" android-v0.0.2 2
assert_failure "published version-code regression" "$version_script" android-v1.1.2 1001002
assert_failure "Android version-code overflow" "$version_script" android-v2100.0.1

"$validator" >/dev/null
fixture="$(mktemp -d)"
trap 'rm -rf "$fixture"' EXIT

prepare_fixture() {
  rm -rf "$fixture"
  mkdir -p \
    "$fixture/.github/workflows" \
    "$fixture/config/release" \
    "$fixture/scripts"
  cp "$repository_root/.github/workflows/tagged-release.yml" "$fixture/.github/workflows/"
  cp "$repository_root/config/release/tagged-release.json" "$fixture/config/release/"
  cp \
    "$repository_root/scripts/release-version.sh" \
    "$repository_root/scripts/deploy-play-internal.sh" \
    "$repository_root/scripts/configure-release-environments.sh" \
    "$repository_root/scripts/validate-tagged-release.sh" \
    "$fixture/scripts/"
  chmod +x "$fixture/scripts/"*.sh
}

prepare_fixture
awk '
  !changed && $0 == "  contents: read" { print "  contents: write"; changed = 1; next }
  { print }
' "$fixture/.github/workflows/tagged-release.yml" >"$fixture/workflow.tmp"
mv "$fixture/workflow.tmp" "$fixture/.github/workflows/tagged-release.yml"
assert_failure \
  "top-level contents write permission" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" "$fixture/scripts/validate-tagged-release.sh"

prepare_fixture
awk '
  $0 == "    environment: release-signing" { print "    environment: play-internal"; next }
  { print }
' "$fixture/.github/workflows/tagged-release.yml" >"$fixture/workflow.tmp"
mv "$fixture/workflow.tmp" "$fixture/.github/workflows/tagged-release.yml"
assert_failure \
  "signing credentials in the Play environment" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" "$fixture/scripts/validate-tagged-release.sh"

prepare_fixture
awk '
  $0 == "          NUTSNEWS_UPLOAD_KEY_ALIAS: ${{ secrets.NUTSNEWS_UPLOAD_KEY_ALIAS }}" {
    print "          NUTSNEWS_UPLOAD_KEY_ALIAS: missing"; next
  }
  { print }
' "$fixture/.github/workflows/tagged-release.yml" >"$fixture/workflow.tmp"
mv "$fixture/workflow.tmp" "$fixture/.github/workflows/tagged-release.yml"
assert_failure \
  "missing signing-secret reference" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" "$fixture/scripts/validate-tagged-release.sh"

prepare_fixture
sed -i.bak \
  's/:commit"/:commit?changesNotSentForReview=true"/' \
  "$fixture/scripts/deploy-play-internal.sh"
rm "$fixture/scripts/deploy-play-internal.sh.bak"
assert_failure \
  "obsolete deferred-review parameter" \
  env NUTSNEWS_REPOSITORY_ROOT="$fixture" "$fixture/scripts/validate-tagged-release.sh"

echo "Tagged release tests passed."
