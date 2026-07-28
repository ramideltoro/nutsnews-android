#!/usr/bin/env bash

set -euo pipefail

repository_root="${NUTSNEWS_REPOSITORY_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
contract="$repository_root/config/release/tagged-release.json"
mode="${1:---check}"

fail() {
  echo "Release environment validation failed: $*" >&2
  exit 1
}

[[ "$mode" == "--check" || "$mode" == "--apply" ]] ||
  fail "use --check or --apply"
[[ -f "$contract" ]] || fail "missing $contract"
for command_name in gh jq; do
  command -v "$command_name" >/dev/null 2>&1 || fail "$command_name is required"
done
repository="${GITHUB_REPOSITORY:-$(jq -er '.repository' "$contract")}"

while IFS= read -r environment_name; do
  policies_path="repos/${repository}/environments/${environment_name}/deployment-branch-policies"
  if [[ "$mode" == "--apply" ]]; then
    jq -nc '{deployment_branch_policy:{protected_branches:false,custom_branch_policies:true}}' |
      gh api --method PUT "repos/${repository}/environments/${environment_name}" --input - \
        >/dev/null
    current_policies="$(gh api "$policies_path")"
    while IFS= read -r policy_id; do
      gh api --method DELETE "${policies_path}/${policy_id}" >/dev/null
    done < <(jq -r '.branch_policies[]?.id' <<<"$current_policies")
    while IFS= read -r policy; do
      gh api --method POST "$policies_path" --input - <<<"$policy" >/dev/null
    done < <(
      jq -c --arg environment "$environment_name" \
        '.githubEnvironments[$environment].deploymentPolicies[]' "$contract"
    )
  fi

  environment_json="$(gh api "repos/${repository}/environments/${environment_name}")"
  jq -e '
    .deployment_branch_policy.protected_branches == false and
    .deployment_branch_policy.custom_branch_policies == true
  ' <<<"$environment_json" >/dev/null ||
    fail "$environment_name does not use explicit protected deployment policies"

  expected_policies="$(
    jq -c --arg environment "$environment_name" \
      '[.githubEnvironments[$environment].deploymentPolicies[] | {name,type}] | sort_by(.type,.name)' \
      "$contract"
  )"
  actual_policies="$(
    gh api "$policies_path" |
      jq -c '[.branch_policies[] | {name,type}] | sort_by(.type,.name)'
  )"
  [[ "$actual_policies" == "$expected_policies" ]] ||
    fail "$environment_name deployment policies differ from the release contract"
done < <(jq -r '.githubEnvironments | keys[]' "$contract")

echo "Release environment contract validated."
