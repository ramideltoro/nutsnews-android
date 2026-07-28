#!/usr/bin/env bash

set -euo pipefail

repository_root="${NUTSNEWS_REPOSITORY_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
readme="$repository_root/README.md"
development="$repository_root/docs/development.md"
release_runbook="$repository_root/docs/release/release-operations.md"
architecture="$repository_root/docs/architecture/android-application-architecture.md"
play_provisioning="$repository_root/docs/release/t62-play-internal-provisioning.md"

fail() {
  echo "Documentation validation failed: $*" >&2
  exit 1
}

documentation_files=(
  "$readme"
  "$development"
  "$release_runbook"
  "$architecture"
  "$play_provisioning"
)
for required_file in "${documentation_files[@]}"; do
  [[ -f "$required_file" ]] || fail "missing $required_file"
done

documentation="$(cat "${documentation_files[@]}")"
required_fragments=(
  "Android Studio Quail 2"
  "JDK 17"
  "Gradle"
  "9.5.0"
  "9.3.0"
  "git clone https://github.com/ramideltoro/nutsnews-android.git"
  "compileDebugKotlin"
  "lintDebug"
  "testDebugUnitTest"
  "assembleDebug"
  "connectedDebugAndroidTest"
  "verify-screenshot-goldens.sh"
  "run-emulator-tests.sh"
  "core.model"
  "data.*"
  "feature.*"
  "android-vX.Y.Z"
  "NUTSNEWS_UPLOAD_KEYSTORE_BASE64"
  "NUTSNEWS_UPLOAD_KEYSTORE_PASSWORD"
  "NUTSNEWS_UPLOAD_KEY_ALIAS"
  "NUTSNEWS_UPLOAD_KEY_PASSWORD"
  "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"
  "play-internal"
  "release-signing"
  "Internal Testing"
  "Rollback"
  "troubleshooting"
  "1001002"
  "30355469046"
)
for fragment in "${required_fragments[@]}"; do
  grep -Fq -- "$fragment" <<<"$documentation" ||
    fail "documentation is missing required content: $fragment"
done

secret_patterns=(
  'gh[opsu]_[A-Za-z0-9_]{20,}'
  'github_pat_[A-Za-z0-9_]{20,}'
  'AIza[0-9A-Za-z_-]{30,}'
  '-----BEGIN (RSA |EC )?PRIVATE KEY-----'
  '"private_key"[[:space:]]*:'
)
for pattern in "${secret_patterns[@]}"; do
  if grep -Eiq -- "$pattern" "${documentation_files[@]}"; then
    fail "documentation contains secret-like material matching a forbidden pattern"
  fi
done

if find "$repository_root/docs" -type f \
  \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pem' \) \
  -print -quit | grep -q .; then
  fail "documentation tree must not contain signing material"
fi

grep -Fq 'promotion is intentionally manual' "$readme" ||
  fail "README must state the production boundary"
grep -Fq 'No workflow job deploys or promotes to production.' "$release_runbook" ||
  fail "release runbook must forbid automated production promotion"
grep -Fq 'Deleting a GitHub Release or tag does not remove a Play release' "$release_runbook" ||
  fail "release rollback must describe immutable Play delivery"
grep -Fq 'T62 is complete.' "$play_provisioning" ||
  fail "Play provisioning documentation still reports the resolved blocker"

echo "Developer and release documentation validation passed."
