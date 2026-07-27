# T60 branch protection

`main` is protected by the versioned policy in
`.github/branch-protection.json`. Apply the policy from an authenticated
administrator checkout without placing a token in a command, file, log, or
artifact:

```sh
gh api \
  --method PUT \
  repos/ramideltoro/nutsnews-android/branches/main/protection \
  --input .github/branch-protection.json
./scripts/validate-branch-protection.sh --remote
```

## Enforced controls

- Every change to `main` must arrive through a pull request.
- The pull request branch must be current with `main`.
- Every review conversation must be resolved.
- Stale reviews are dismissed after new commits. No approval count is imposed
  because this repository currently has one maintainer; the pull-request gate
  still applies.
- Force pushes and branch deletion are disabled.
- Administrators are subject to the same controls (`enforce_admins: true`).
  This user-owned repository has no organization user, team, or app bypass
  allowances.

## Required checks

GitHub required-check contexts are job display names, so a workflow rename is a
policy change. `scripts/validate-branch-protection.sh` fails when this list and
the workflow definitions diverge:

- `Validate Gradle wrapper`
- `Compile, lint, test, and assemble`
- `Emulator (phone API 26)`
- `Emulator (phone API 36)`
- `Emulator (tablet API 36)`
- `Dependency review`
- `CodeQL (Java/Kotlin)`
- `Branch protection policy`
- `Release signing contract`
- `Google Play provisioning contract`

All contexts are restricted to the GitHub Actions app (`app_id: 15368`) so a
similarly named status from another integration cannot satisfy protection.

## Validation procedure

1. Open the focused implementation pull request and let every required workflow
   job report once.
2. Apply the policy and run the local plus remote validator.
3. Temporarily add a uniquely named required probe context to the live policy,
   report that context as failed on the pull-request head, and verify GitHub
   reports the pull request as blocked.
4. Reapply the versioned policy to remove the probe.
5. Verify the green, current pull request reports a clean merge state, merge it,
   and rerun the remote validator against `main`.

The probe must never be added to the versioned policy, and the final remote
policy must byte-for-byte preserve the controls and contexts represented by the
versioned JSON object.
