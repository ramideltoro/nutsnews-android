#!/usr/bin/env bash

set -euo pipefail

gradle_file="app/build.gradle.kts"
workflow=".github/workflows/security.yml"
gitignore=".gitignore"
certificate="config/signing/nutsnews-upload-certificate.pem"
verification_script="scripts/verify-release-bundle.sh"

fail() {
  echo "Release signing contract validation failed: $*" >&2
  exit 1
}

for required_file in \
  "$gradle_file" \
  "$workflow" \
  "$gitignore" \
  "$certificate" \
  "$verification_script"; do
  [[ -f "$required_file" ]] || fail "missing $required_file"
done

[[ -x "$verification_script" ]] || fail "$verification_script is not executable"

required_gradle_fragments=(
  "NUTSNEWS_UPLOAD_KEYSTORE_PATH"
  "NUTSNEWS_UPLOAD_KEYSTORE_PASSWORD"
  "NUTSNEWS_UPLOAD_KEY_ALIAS"
  "NUTSNEWS_UPLOAD_KEY_PASSWORD"
  'signingConfig = signingConfigs.getByName("release")'
  "notCompatibleWithConfigurationCache"
)

for fragment in "${required_gradle_fragments[@]}"; do
  grep -Fq -- "$fragment" "$gradle_file" ||
    fail "Gradle signing configuration is missing: $fragment"
done

if grep -Fq -- 'signingConfigs.getByName("debug")' "$gradle_file"; then
  fail "release configuration must never use debug signing"
fi

required_workflow_fragments=(
  "name: Release signing contract"
  "name: Protected release signing"
  "environment: release-signing"
  'secrets.NUTSNEWS_UPLOAD_KEYSTORE_BASE64'
  'secrets.NUTSNEWS_UPLOAD_KEYSTORE_PASSWORD'
  'secrets.NUTSNEWS_UPLOAD_KEY_ALIAS'
  'secrets.NUTSNEWS_UPLOAD_KEY_PASSWORD'
  "--no-configuration-cache"
  "bundleRelease"
  "verify-release-bundle.sh"
  "config/signing/nutsnews-upload-certificate.pem"
)

for fragment in "${required_workflow_fragments[@]}"; do
  grep -Fq -- "$fragment" "$workflow" ||
    fail "release signing workflow is missing: $fragment"
done

for ignored_pattern in '*.jks' '*.keystore' 'keystore.properties'; do
  grep -Fqx -- "$ignored_pattern" "$gitignore" ||
    fail ".gitignore must exclude $ignored_pattern"
done

keytool -printcert -file "$certificate" >/dev/null 2>&1 ||
  fail "pinned upload certificate is invalid"

echo "Release signing contract validation passed."
