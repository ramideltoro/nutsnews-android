#!/usr/bin/env bash

set -euo pipefail

service_account_json="${1:-}"
configuration="${2:-config/play/internal-testing.json}"

fail() {
  echo "Google Play internal access verification failed: $*" >&2
  exit 1
}

[[ -f "$service_account_json" ]] || fail "service-account JSON file is required"
[[ -f "$configuration" ]] || fail "missing configuration: $configuration"

for command_name in curl jq openssl; do
  command -v "$command_name" >/dev/null 2>&1 ||
    fail "$command_name is required"
done

package_name="$(jq -er '.packageName' "$configuration")"
track_name="$(jq -er '.track' "$configuration")"
[[ "$package_name" == "com.nutsnews.app" ]] ||
  fail "package must be com.nutsnews.app"
[[ "$track_name" == "internal" ]] || fail "track must be internal"

client_email="$(jq -er '.client_email' "$service_account_json")"
token_uri="$(jq -er '.token_uri' "$service_account_json")"
jq -e '.type == "service_account" and (.private_key | type == "string")' \
  "$service_account_json" >/dev/null ||
  fail "credential is not a service-account key"
[[ "$token_uri" == "https://oauth2.googleapis.com/token" ]] ||
  fail "unexpected OAuth token endpoint"

work_dir="$(mktemp -d)"
access_token=""
edit_id=""

cleanup() {
  if [[ -n "$access_token" && -n "$edit_id" ]]; then
    curl --silent --request DELETE \
      --header "Authorization: Bearer $access_token" \
      "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}" \
      >/dev/null 2>&1 || true
  fi
  rm -rf "$work_dir"
}
trap cleanup EXIT
umask 077

private_key_path="$work_dir/private-key.pem"
token_response="$work_dir/token-response.json"
edit_response="$work_dir/edit-response.json"
track_response="$work_dir/track-response.json"
jq -er '.private_key' "$service_account_json" >"$private_key_path"

base64_url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

issued_at="$(date +%s)"
expires_at=$((issued_at + 3600))
jwt_header="$(
  jq -nc '{alg:"RS256",typ:"JWT"}' | base64_url
)"
jwt_claims="$(
  jq -nc \
    --arg issuer "$client_email" \
    --arg audience "$token_uri" \
    --arg scope "https://www.googleapis.com/auth/androidpublisher" \
    --argjson issued_at "$issued_at" \
    --argjson expires_at "$expires_at" \
    '{
      iss: $issuer,
      scope: $scope,
      aud: $audience,
      iat: $issued_at,
      exp: $expires_at
    }' |
    base64_url
)"
unsigned_jwt="${jwt_header}.${jwt_claims}"
jwt_signature="$(
  printf '%s' "$unsigned_jwt" |
    openssl dgst -sha256 -sign "$private_key_path" |
    base64_url
)"
jwt_assertion="${unsigned_jwt}.${jwt_signature}"

curl --silent --show-error --fail \
  --request POST \
  --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" \
  --data-urlencode "assertion=$jwt_assertion" \
  "$token_uri" \
  >"$token_response" ||
  fail "OAuth token exchange was rejected"

access_token="$(jq -er '.access_token' "$token_response")"

curl --silent --show-error --fail \
  --request POST \
  --header "Authorization: Bearer $access_token" \
  --header "Content-Type: application/json" \
  --data '{}' \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits" \
  >"$edit_response" ||
  fail "service account cannot create a controlled edit for $package_name"

edit_id="$(jq -er '.id' "$edit_response")"

curl --silent --show-error --fail \
  --header "Authorization: Bearer $access_token" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${track_name}" \
  >"$track_response" ||
  fail "service account cannot read the internal track"

[[ "$(jq -er '.track' "$track_response")" == "$track_name" ]] ||
  fail "API returned the wrong track"

curl --silent --show-error --fail \
  --request DELETE \
  --header "Authorization: Bearer $access_token" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}" \
  >/dev/null ||
  fail "controlled edit cleanup failed"
edit_id=""

echo "Google Play access verified for com.nutsnews.app on the internal track."
