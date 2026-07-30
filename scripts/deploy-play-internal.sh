#!/usr/bin/env bash

set -euo pipefail

service_account_json="${1:-}"
bundle_path="${2:-}"
version_name="${3:-}"
version_code="${4:-}"
repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
play_configuration="$repository_root/config/play/internal-testing.json"
release_contract="$repository_root/config/release/tagged-release.json"

fail() {
  echo "Google Play Internal deployment failed: $*" >&2
  exit 1
}

[[ -f "$service_account_json" ]] || fail "service-account JSON file is required"
[[ -f "$bundle_path" ]] || fail "verified AAB file is required"
[[ -f "$play_configuration" ]] || fail "missing $play_configuration"
[[ -f "$release_contract" ]] || fail "missing $release_contract"
for command_name in curl jq openssl; do
  command -v "$command_name" >/dev/null 2>&1 || fail "$command_name is required"
done

package_name="$(jq -er '.packageName' "$play_configuration")"
track_name="$(jq -er '.track' "$play_configuration")"
release_status="$(jq -er '.releaseStatus' "$play_configuration")"
[[ "$package_name" == "$(jq -er '.packageName' "$release_contract")" ]] ||
  fail "release and Play package contracts differ"
[[ "$track_name" == "internal" && "$track_name" == "$(jq -er '.track' "$release_contract")" ]] ||
  fail "deployment is restricted to the internal track"
[[ "$version_code" =~ ^[0-9]+$ ]] || fail "version code must be numeric"
read -r derived_name derived_code < <(
  "$repository_root/scripts/release-version.sh" "android-v$version_name"
)
[[ "$derived_name" == "$version_name" && "$derived_code" == "$version_code" ]] ||
  fail "version name and deterministic version code do not match"
"$repository_root/scripts/verify-release-bundle.sh" \
  "$bundle_path" \
  "$repository_root/config/signing/nutsnews-upload-certificate.pem" >/dev/null

client_email="$(jq -er '.client_email' "$service_account_json")"
token_uri="$(jq -er '.token_uri' "$service_account_json")"
jq -e '.type == "service_account" and (.private_key | type == "string")' \
  "$service_account_json" >/dev/null || fail "credential is not a service-account key"
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
jq -er '.private_key' "$service_account_json" >"$private_key_path"

base64_url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

issued_at="$(date +%s)"
expires_at=$((issued_at + 3600))
jwt_header="$(jq -nc '{alg:"RS256",typ:"JWT"}' | base64_url)"
jwt_claims="$(
  jq -nc \
    --arg issuer "$client_email" \
    --arg audience "$token_uri" \
    --arg scope "https://www.googleapis.com/auth/androidpublisher" \
    --argjson issued_at "$issued_at" \
    --argjson expires_at "$expires_at" \
    '{iss:$issuer,scope:$scope,aud:$audience,iat:$issued_at,exp:$expires_at}' |
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
  "$token_uri" >"$token_response" || fail "OAuth token exchange was rejected"
access_token="$(jq -er '.access_token' "$token_response")"

create_edit() {
  local response_path="$1"
  curl --silent --show-error --fail \
    --request POST \
    --header "Authorization: Bearer $access_token" \
    --header "Content-Type: application/json" \
    --data '{}' \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits" \
    >"$response_path" || fail "could not create a controlled Play edit"
  edit_id="$(jq -er '.id' "$response_path")"
}

delete_edit() {
  curl --silent --show-error --fail \
    --request DELETE \
    --header "Authorization: Bearer $access_token" \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}" \
    >/dev/null || fail "controlled Play edit cleanup failed"
  edit_id=""
}

edit_response="$work_dir/edit-response.json"
track_response="$work_dir/track-response.json"
create_edit "$edit_response"
curl --silent --show-error --fail \
  --header "Authorization: Bearer $access_token" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${track_name}" \
  >"$track_response" || fail "could not query the internal track before deployment"
[[ "$(jq -er '.track' "$track_response")" == "$track_name" ]] ||
  fail "Play returned an unexpected track"

if jq -e --arg name "$version_name" --arg code "$version_code" '
  any(.releases[]?; .name == $name and any(.versionCodes[]?; . == $code))
' "$track_response" >/dev/null; then
  delete_edit
  jq -nc \
    --arg package "$package_name" \
    --arg track "$track_name" \
    --arg name "$version_name" \
    --argjson code "$version_code" \
    '{packageName:$package,track:$track,versionName:$name,versionCode:$code,status:"already-present"}'
  exit 0
fi

published_code="$(
  jq -r '[.releases[]?.versionCodes[]? | tonumber] | max // 0' "$track_response"
)"
(( version_code > published_code )) ||
  fail "version code $version_code must be greater than current internal code $published_code"

upload_response="$work_dir/upload-response.json"
curl --silent --show-error --fail \
  --request POST \
  --header "Authorization: Bearer $access_token" \
  --header "Content-Type: application/octet-stream" \
  --data-binary "@$bundle_path" \
  "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/bundles" \
  >"$upload_response" || fail "Play rejected the verified AAB upload"
[[ "$(jq -er '.versionCode' "$upload_response")" == "$version_code" ]] ||
  fail "uploaded bundle returned an unexpected version code"

track_update="$work_dir/track-update.json"
jq -nc \
  --arg track "$track_name" \
  --arg name "$version_name" \
  --arg status "$release_status" \
  --arg code "$version_code" \
  '{track:$track,releases:[{name:$name,status:$status,versionCodes:[$code]}]}' \
  >"$track_update"
curl --silent --show-error --fail \
  --request PUT \
  --header "Authorization: Bearer $access_token" \
  --header "Content-Type: application/json" \
  --data-binary "@$track_update" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${track_name}" \
  >/dev/null || fail "could not assign the bundle to the internal track"

commit_response="$work_dir/commit-response.json"
if ! commit_http_status="$(
  curl --silent --show-error \
    --output "$commit_response" \
    --write-out '%{http_code}' \
    --request POST \
    --header "Authorization: Bearer $access_token" \
    --header "Content-Type: application/json" \
    --data '{}' \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}:commit?changesNotSentForReview=true"
)"; then
  fail "could not reach Play while committing the internal release"
fi
if [[ ! "$commit_http_status" =~ ^2[0-9][0-9]$ ]]; then
  commit_error_message="$(
    jq -r '.error.message // empty' "$commit_response" 2>/dev/null || true
  )"
  [[ -n "$commit_error_message" ]] ||
    commit_error_message="Play returned no structured error message."
  fail "Play rejected the internal release commit (HTTP $commit_http_status): $commit_error_message"
fi
edit_id=""

verify_edit_response="$work_dir/verify-edit-response.json"
verify_track_response="$work_dir/verify-track-response.json"
create_edit "$verify_edit_response"
curl --silent --show-error --fail \
  --header "Authorization: Bearer $access_token" \
  "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${package_name}/edits/${edit_id}/tracks/${track_name}" \
  >"$verify_track_response" || fail "could not query the internal track after deployment"
jq -e --arg track "$track_name" --arg name "$version_name" --arg code "$version_code" '
  .track == $track and
  any(.releases[]?; .name == $name and any(.versionCodes[]?; . == $code))
' "$verify_track_response" >/dev/null ||
  fail "post-deployment query did not return the expected package release"
delete_edit

jq -nc \
  --arg package "$package_name" \
  --arg track "$track_name" \
  --arg name "$version_name" \
  --argjson code "$version_code" \
  '{packageName:$package,track:$track,versionName:$name,versionCode:$code,status:"deployed"}'
