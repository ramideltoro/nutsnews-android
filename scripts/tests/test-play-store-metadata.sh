#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VALIDATOR="$ROOT/scripts/validate-play-store-metadata.sh"
TOKEN_MINTER="$ROOT/scripts/mint-google-play-access-token.sh"

"$VALIDATOR" "$ROOT" >/dev/null
"$ROOT/scripts/publish-play-store-metadata.sh" --validate-only >/dev/null

if "$TOKEN_MINTER" >/dev/null 2>&1; then
  echo "Expected token minting without a service-account file to fail." >&2
  exit 1
fi

if env -u GOOGLE_PLAY_ACCESS_TOKEN "$ROOT/scripts/publish-play-store-metadata.sh" >/dev/null 2>&1; then
  echo "Expected publishing without a Play access token to fail." >&2
  exit 1
fi

TEMP_ROOT="$(mktemp -d)"
trap 'rm -rf "$TEMP_ROOT"' EXIT
mkdir -p "$TEMP_ROOT/fastlane/metadata/android/en-US"
cp -R "$ROOT/fastlane/metadata/android/en-US/." \
  "$TEMP_ROOT/fastlane/metadata/android/en-US/"
mkdir -p "$TEMP_ROOT/play-store" "$TEMP_ROOT/.github/workflows" \
  "$TEMP_ROOT/scripts" \
  "$TEMP_ROOT/app/src/main/kotlin/com/nutsnews/app/feature/personalization" \
  "$TEMP_ROOT/app/src/main/kotlin/com/nutsnews/app"
cp "$ROOT/play-store/"*.json "$TEMP_ROOT/play-store/"
cp "$ROOT/.github/workflows/play-store-metadata.yml" "$TEMP_ROOT/.github/workflows/"
cp "$TOKEN_MINTER" "$TEMP_ROOT/scripts/"
cp "$ROOT/scripts/publish-play-store-metadata.sh" "$TEMP_ROOT/scripts/"
cp "$ROOT/app/src/main/kotlin/com/nutsnews/app/MainActivity.kt" \
  "$TEMP_ROOT/app/src/main/kotlin/com/nutsnews/app/"
cp "$ROOT/app/src/main/kotlin/com/nutsnews/app/feature/personalization/PersonalizationScreen.kt" \
  "$TEMP_ROOT/app/src/main/kotlin/com/nutsnews/app/feature/personalization/"

jq -nc '{
  type: "service_account",
  client_email: "invalid@example.invalid",
  private_key: "not-a-private-key",
  token_uri: "https://example.invalid/token"
}' >"$TEMP_ROOT/invalid-service-account.json"
if "$TOKEN_MINTER" "$TEMP_ROOT/invalid-service-account.json" >/dev/null 2>&1; then
  echo "Expected token minting with an untrusted token endpoint to fail." >&2
  exit 1
fi

mkdir -p "$TEMP_ROOT/bin" "$TEMP_ROOT/curl-state"
cat >"$TEMP_ROOT/bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

state_dir="${FAKE_CURL_STATE_DIR:?}"
method="GET"
url=""
data=""
content_type=""

while (($#)); do
  case "$1" in
    --request)
      method="$2"
      shift 2
      ;;
    --data | --data-binary)
      data="$2"
      shift 2
      ;;
    --header)
      if [[ "$2" == "Content-Type: "* ]]; then
        content_type="${2#Content-Type: }"
      fi
      shift 2
      ;;
    --silent | --show-error | --fail-with-body)
      shift
      ;;
    http*)
      url="$1"
      shift
      ;;
    *)
      shift
      ;;
  esac
done

api="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.nutsnews.app/edits"
upload_api="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/com.nutsnews.app/edits"

if [[ "$method" == "POST" && "$url" == "$api" ]]; then
  printf '{"id":"metadata-edit"}\n'
elif [[ "$method" == "PUT" && "$url" == "$api/metadata-edit/listings/en-US" ]]; then
  printf '%s\n' "$data" >"$state_dir/listing.json"
  printf '{}\n'
elif [[ "$method" == "DELETE" && "$url" == "$api/metadata-edit/listings/en-US/"* ]]; then
  image_type="${url##*/}"
  printf '0\n' >"$state_dir/$image_type.count"
  printf '{}\n'
elif [[ "$method" == "POST" &&
  "$url" == "$upload_api/metadata-edit/listings/en-US/"*"?uploadType=media" ]]; then
  [[ "$content_type" == "image/png" ]]
  [[ "$data" == @* && -f "${data#@}" ]]
  image_type="${url%%\?*}"
  image_type="${image_type##*/}"
  count_file="$state_dir/$image_type.count"
  count="$(cat "$count_file")"
  printf '%s\n' "$((count + 1))" >"$count_file"
  printf '{"image":{"id":"uploaded-image"}}\n'
elif [[ "$method" == "POST" &&
  "$url" == "$api/metadata-edit:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW" ]]; then
  if [[ "${FAKE_PLAY_COMMIT_ERROR:-}" == "true" ]]; then
    printf '{"error":{"code":400,"message":"Simulated Play commit rejection."}}\n'
    exit 22
  elif [[ "${FAKE_PLAY_COMMIT_ERROR:-}" == "console-review" ]]; then
    printf '{"error":{"code":400,"message":"Changes cannot be sent for review automatically. Please set the query parameter changesNotSentForReview to true. Once committed, the changes in this edit can be sent for review from the Google Play Console UI."}}\n'
    exit 22
  fi
  printf '{"id":"metadata-edit"}\n'
elif [[ "$method" == "POST" &&
  "$url" == "$api/metadata-edit:commit?changesNotSentForReview=true" ]]; then
  printf 'true\n' >"$state_dir/deferred-commit"
  printf '{"id":"metadata-edit"}\n'
elif [[ "$method" == "GET" && "$url" == "$api/metadata-edit/listings/en-US" ]]; then
  cat "$state_dir/listing.json"
elif [[ "$method" == "GET" && "$url" == "$api/metadata-edit/listings/en-US/"* ]]; then
  image_type="${url##*/}"
  count="$(cat "$state_dir/$image_type.count")"
  jq -nc --argjson count "$count" '{images: [range(0; $count) | {}]}'
elif [[ "$method" == "DELETE" && "$url" == "$api/metadata-edit" ]]; then
  printf '{}\n'
else
  echo "Unexpected fake Play request: $method $url" >&2
  exit 64
fi
EOF
chmod 0755 "$TEMP_ROOT/bin/curl"

FAKE_CURL_STATE_DIR="$TEMP_ROOT/curl-state" \
  PATH="$TEMP_ROOT/bin:$PATH" \
  GOOGLE_PLAY_ACCESS_TOKEN="test-access-token" \
  "$ROOT/scripts/publish-play-store-metadata.sh" >/dev/null

FAKE_PLAY_COMMIT_ERROR=console-review \
  FAKE_CURL_STATE_DIR="$TEMP_ROOT/curl-state" \
  PATH="$TEMP_ROOT/bin:$PATH" \
  GOOGLE_PLAY_ACCESS_TOKEN="test-access-token" \
  "$ROOT/scripts/publish-play-store-metadata.sh" >/dev/null
if [[ "$(cat "$TEMP_ROOT/curl-state/deferred-commit")" != "true" ]]; then
  echo "Expected Console-review metadata to be committed without submission." >&2
  exit 1
fi

commit_error_log="$TEMP_ROOT/commit-error.log"
if FAKE_PLAY_COMMIT_ERROR=true \
  FAKE_CURL_STATE_DIR="$TEMP_ROOT/curl-state" \
  PATH="$TEMP_ROOT/bin:$PATH" \
  GOOGLE_PLAY_ACCESS_TOKEN="test-access-token" \
  "$ROOT/scripts/publish-play-store-metadata.sh" \
  >/dev/null 2>"$commit_error_log"; then
  echo "Expected a rejected Play commit to fail." >&2
  exit 1
fi
if ! grep -Fq \
  "Play Store metadata publishing failed: Play rejected metadata commit: Simulated Play commit rejection." \
  "$commit_error_log"; then
  echo "Expected the structured Play commit error to be reported." >&2
  exit 1
fi

printf '%090d\n' 0 > "$TEMP_ROOT/fastlane/metadata/android/en-US/short_description.txt"
if "$VALIDATOR" "$TEMP_ROOT" >/dev/null 2>&1; then
  echo "Expected an overlong short description to fail." >&2
  exit 1
fi

cp "$ROOT/fastlane/metadata/android/en-US/short_description.txt" \
  "$TEMP_ROOT/fastlane/metadata/android/en-US/short_description.txt"
jq '.targetAudience.designedForChildren = true' \
  "$ROOT/play-store/policy-declarations.json" \
  > "$TEMP_ROOT/play-store/policy-declarations.json"
if "$VALIDATOR" "$TEMP_ROOT" >/dev/null 2>&1; then
  echo "Expected a contradictory target-audience declaration to fail." >&2
  exit 1
fi

echo "Play Store metadata failure-path tests passed."
