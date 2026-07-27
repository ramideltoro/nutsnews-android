#!/usr/bin/env bash

set -euo pipefail

bundle_path="${1:-}"
expected_certificate="${2:-}"

fail() {
  echo "Release bundle verification failed: $*" >&2
  exit 1
}

[[ -n "$bundle_path" ]] || fail "provide the release AAB path"
[[ -f "$bundle_path" ]] || fail "bundle does not exist: $bundle_path"

command -v jarsigner >/dev/null 2>&1 || fail "jarsigner is required"
command -v keytool >/dev/null 2>&1 || fail "keytool is required"
command -v openssl >/dev/null 2>&1 || fail "openssl is required"

if ! verification_output="$(jarsigner -verify -verbose "$bundle_path" 2>&1)"; then
  fail "jarsigner rejected the bundle"
fi

grep -Fq 'jar verified.' <<<"$verification_output" ||
  fail "bundle does not contain a verified JAR signature"

signer_details="$(keytool -printcert -jarfile "$bundle_path" 2>/dev/null)"
if grep -Fq 'CN=Android Debug' <<<"$signer_details"; then
  fail "release bundle uses an Android debug certificate"
fi

if [[ -n "$expected_certificate" ]]; then
  [[ -f "$expected_certificate" ]] ||
    fail "expected certificate does not exist: $expected_certificate"

  actual_fingerprint="$(
    keytool -printcert -jarfile "$bundle_path" -rfc 2>/dev/null |
      openssl x509 -outform DER 2>/dev/null |
      openssl dgst -sha256 -r |
      awk '{ print toupper($1) }'
  )"
  expected_fingerprint="$(
    openssl x509 -in "$expected_certificate" -outform DER 2>/dev/null |
      openssl dgst -sha256 -r |
      awk '{ print toupper($1) }'
  )"
  [[ -n "$actual_fingerprint" ]] || fail "could not read the bundle signer"
  [[ "$actual_fingerprint" == "$expected_fingerprint" ]] ||
    fail "bundle signer does not match the pinned upload certificate"
fi

echo "Release bundle signature verified."
