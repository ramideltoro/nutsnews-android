#!/usr/bin/env bash

set -euo pipefail

dependabot=".github/dependabot.yml"
security_workflow=".github/workflows/security.yml"
verification_metadata="gradle/verification-metadata.xml"

for required_file in "$dependabot" "$security_workflow" "$verification_metadata"; do
  if [[ ! -f "$required_file" ]]; then
    echo "Missing security automation file: $required_file" >&2
    exit 1
  fi
done

required_dependabot_fragments=(
  "package-ecosystem: gradle"
  "package-ecosystem: github-actions"
  "interval: weekly"
  "open-pull-requests-limit:"
)

for fragment in "${required_dependabot_fragments[@]}"; do
  if ! grep -Fq -- "$fragment" "$dependabot"; then
    echo "Dependabot configuration is missing: $fragment" >&2
    exit 1
  fi
done

required_workflow_fragments=(
  "pull_request:"
  "push:"
  "dependency-review-action@"
  "fail-on-severity: moderate"
  "codeql-action/init@"
  "languages: java-kotlin"
  "build-mode: manual"
  "--no-configuration-cache"
  "--dependency-verification=strict"
  "codeql-action/analyze@"
  "security-events: write"
)

for fragment in "${required_workflow_fragments[@]}"; do
  if ! grep -Fq -- "$fragment" "$security_workflow"; then
    echo "Security workflow is missing: $fragment" >&2
    exit 1
  fi
done

action_revisions="$(
  sed -nE \
    's/^[[:space:]]*uses:[[:space:]]+[^@]+@([^[:space:]#]+).*$/\1/p' \
    .github/workflows/*.yml
)"

action_count=0
while IFS= read -r revision; do
  if [[ ! "$revision" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Action revision is not pinned to a full commit SHA: $revision" >&2
    exit 1
  fi
  action_count=$((action_count + 1))
done <<< "$action_revisions"

artifact_paths="$(
  awk '
    function indentation(line, copy) {
      copy = line
      sub(/[^ ].*$/, "", copy)
      return length(copy)
    }
    /uses:[[:space:]]+actions\/upload-artifact@/ {
      upload_indent = indentation($0)
      in_upload = 1
      next
    }
    in_upload && /^[[:space:]]*path:[[:space:]]*\|[[:space:]]*$/ {
      path_indent = indentation($0)
      in_paths = 1
      next
    }
    in_paths {
      current_indent = indentation($0)
      if ($0 !~ /^[[:space:]]*$/ && current_indent <= path_indent) {
        in_paths = 0
      } else if ($0 !~ /^[[:space:]]*$/) {
        value = $0
        sub(/^[[:space:]]+/, "", value)
        print value
        next
      }
    }
    in_upload && $0 !~ /^[[:space:]]*$/ &&
      indentation($0) < upload_indent {
      in_upload = 0
    }
  ' .github/workflows/*.yml
)"

if [[ -z "$artifact_paths" ]]; then
  echo "Security validation expected at least one uploaded artifact path." >&2
  exit 1
fi

while IFS= read -r artifact_path; do
  case "$artifact_path" in
    build-logs | build-logs/* | \
    app/build/reports | app/build/reports/* | \
    app/build/test-results | app/build/test-results/* | \
    app/build/outputs | app/build/outputs/* | \
    artifacts/emulator | artifacts/emulator/*)
      ;;
    *)
      echo "Artifact path is outside the build-output allowlist: $artifact_path" >&2
      exit 1
      ;;
  esac
done <<< "$artifact_paths"

if ! grep -Fq '<verify-metadata>true</verify-metadata>' "$verification_metadata"; then
  echo "Gradle dependency metadata verification is not enabled." >&2
  exit 1
fi

if ! grep -Fq '<sha256 value=' "$verification_metadata"; then
  echo "Gradle dependency verification has no SHA-256 checksums." >&2
  exit 1
fi

echo "Security automation validation passed: $action_count pinned action uses and allowlisted artifacts."
