#!/usr/bin/env bash
set -euo pipefail

service_account_json="${1:-}"

fail() {
  echo "Google Play token minting failed: $*" >&2
  exit 1
}

[[ -s "$service_account_json" ]] ||
  fail "service-account JSON file is required"

for command_name in curl jq openssl; do
  command -v "$command_name" >/dev/null 2>&1 ||
    fail "$command_name is required"
done

client_email="$(jq -er '.client_email' "$service_account_json")"
token_uri="$(jq -er '.token_uri' "$service_account_json")"
jq -e '
  .type == "service_account" and
  (.private_key | type == "string") and
  (.private_key | length) > 0
' "$service_account_json" >/dev/null ||
  fail "credential is not a service-account key"
[[ "$token_uri" == "https://oauth2.googleapis.com/token" ]] ||
  fail "unexpected OAuth token endpoint"

work_dir="$(mktemp -d)"
cleanup() {
  rm -rf "$work_dir"
}
trap cleanup EXIT
umask 077

private_key_path="$work_dir/private-key.pem"
token_response="$work_dir/token-response.json"
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

curl --silent --show-error --fail \
  --request POST \
  --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" \
  --data-urlencode "assertion=${unsigned_jwt}.${jwt_signature}" \
  "$token_uri" >"$token_response" ||
  fail "OAuth token exchange was rejected"

access_token="$(jq -er '
  select(.access_token | type == "string" and length > 0) |
  .access_token
' "$token_response")" ||
  fail "OAuth token response did not include an access token"

printf '%s\n' "$access_token"
