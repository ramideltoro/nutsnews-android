# NutsNews Android release operations

This runbook covers automated delivery to Google Play Internal Testing and
separate, explicitly confirmed promotions of an already verified bundle to the
Alpha closed-testing and Production tracks. The Production workflow reuses the
published Alpha artifact; it does not rebuild, upload, or receive signing
credentials.

## Release contract

A release starts only when a new immutable tag matching exactly
`android-vX.Y.Z` is pushed and the tagged commit is contained by `main`.
`X.Y.Z` is stable canonical SemVer: no leading zeroes, prerelease suffix, or
build metadata. The tag version becomes the AAB `versionName`.

The version code is deterministic:

```text
major * 1,000,000 + minor * 1,000 + patch
```

Minor and patch are limited to 999. The result must be no greater than
`2,100,000,000` and strictly greater than the highest code already published
to the Internal track. Version code `2` and every later published code are
never reusable.

## Protected GitHub contracts

The `release-signing` environment contains exactly these secret names:

- `NUTSNEWS_UPLOAD_KEYSTORE_BASE64`
- `NUTSNEWS_UPLOAD_KEYSTORE_PASSWORD`
- `NUTSNEWS_UPLOAD_KEY_ALIAS`
- `NUTSNEWS_UPLOAD_KEY_PASSWORD`

The `play-internal` environment contains exactly:

- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`

Never print, split, download, copy into the repository, pass between jobs, or
upload any secret value or signing material as an artifact. The signing job
receives no Play credential. The Play job receives no signing credential. The
GitHub Release job receives neither.

Both environments allow the protected `main` branch for controlled Security
workflow probes and `android-v*.*.*` tags for tagged delivery. Audit the
non-secret policy and secret-name contract before a release:

```sh
./scripts/configure-release-environments.sh --check
./scripts/validate-play-internal-provisioning.sh --remote
gh secret list --repo ramideltoro/nutsnews-android --env release-signing
gh secret list --repo ramideltoro/nutsnews-android --env play-internal
```

Only names and update timestamps should be inspected. Never request secret
values.

## Preflight

1. Confirm the T64 documentation and all intended product changes are merged.
2. Confirm Android CI and Security passed on the current `main` commit.
3. Confirm the previous Internal version and select the next SemVer.
4. Confirm there is no existing tag or GitHub Release for that version.
5. Run the credential-free and remote contracts from the developer guide.

For an illustrative next patch after `1.1.2`, validate the identity before
tagging:

```sh
git fetch origin main --tags
tag=android-v1.1.3
./scripts/release-version.sh "$tag" 1001002
git merge-base --is-ancestor origin/main origin/main
git ls-remote --tags origin "refs/tags/$tag"
```

The final command must return no tag. Replace the example version and prior
code with the actual next release values.

## Create and monitor the release

Create the tag only from the verified remote `main` commit:

```sh
git tag "$tag" origin/main
git push origin "$tag"
```

Do not move, force-push, or delete a tag after a protected job starts. Find the
run and poll no more frequently than every 60 seconds:

```sh
gh run list --repo ramideltoro/nutsnews-android \
  --workflow tagged-release.yml --limit 1
gh run view RUN_ID --repo ramideltoro/nutsnews-android
```

The tagged workflow performs these gates in order:

1. Validate exact tag SemVer, deterministic version code, and `main` ancestry.
2. Run the complete Android build, lint, unit-test, screenshot, and three-cell emulator workflow.
3. Run wrapper, dependency-integrity, CodeQL, branch, signing, and Play contracts.
4. Enter `release-signing`, build the AAB, verify its build identity and pinned upload certificate, and retain the verified AAB as a 30-day Actions artifact.
5. Enter `play-internal`, re-verify the AAB, upload and commit only to `internal` using Play's automatic review-submission behavior, then query package, track, version name, and version code.
6. Create the GitHub Release and attach that same verified AAB only after Play verification succeeds.

The tagged workflow never deploys or promotes to Production. Production is a
separate manual workflow with its own exact confirmation and lifecycle gates.

## Replace an Alpha release that is in review

First wait for the tagged release to finish successfully. The desired version
must already be verified on the Internal track; the Alpha promotion workflow
does not upload or sign an artifact.

Google Play normally cancels the current review when a new edit is committed
while changes are in review. This workflow makes that behavior explicit and
requires the exact `REPLACE_ALPHA_REVIEW` confirmation. It then assigns the
verified Internal bundle to the `alpha` track, performs a full closed-testing
rollout, cancels the superseded review, submits the updated changes, and
queries Alpha to verify the new version code.

```sh
gh workflow run play-closed-promotion.yml \
  --repo ramideltoro/nutsnews-android \
  --ref main \
  -f version_name=1.1.3 \
  -f version_code=1001003 \
  -f release_notes='Improves the Read Story button shape on article cards.' \
  -f review_replacement=REPLACE_ALPHA_REVIEW
```

Run this workflow only when replacing the existing Alpha review is intended.
Submitting another app change can restart Google Play's review wait time. The
workflow has no production path and receives no release-signing credential.

## Promote a published Alpha release to Production

Production promotion is allowed only after all of the following are true:

1. Android developer verification reports every distributed package as
   registered.
2. Play Console reports no blocking app-content, policy, or store-listing task.
3. If the developer account is subject to new-personal-account testing rules,
   the required closed test is complete and Play has approved the production
   access application.
4. The exact version is `RELEASE_LIFECYCLE_STATE_PUBLISHED` on Alpha.
5. The tagged release, Alpha promotion, Android CI, and Security runs for the
   candidate are successful.

The Production workflow first queries the release-lifecycle API. It refuses an
Alpha candidate that is merely drafted, staged, in review, or rejected. It also
uses `ERROR_IF_IN_REVIEW`, so it never cancels or replaces an unrelated Play
review. Before mutation it also confirms that Production has at least one
selected country or region. The release is a full rollout (`completed`) because
the workflow is for an explicit Production launch; later staged update policy
can be added as a separate reviewed change.

For version `1.1.9` (`1001009`), run:

```sh
gh workflow run play-production-promotion.yml \
  --repo ramideltoro/nutsnews-android \
  --ref main \
  -f version_name=1.1.9 \
  -f version_code=1001009 \
  -f release_notes='Improves navigation spacing, Favorites discovery and removal confirmation, and adds a first-launch feature walkthrough.' \
  -f production_confirmation=PROMOTE_TO_PRODUCTION
```

The safe outcomes are:

- `published`: the release is available on Production.
- `in-review`: Play accepted and is reviewing the Production release.
- `submitted`: the edit was committed but the lifecycle endpoint has not yet
  reported the release.
- `pending-console-review`: Play required the committed change to be sent from
  Publishing overview.
- `approved-not-published`: managed publishing is enabled and the approved
  release must be published from Play Console.

Any missing Production access, unmet testing requirement, active review,
service-account permission problem, or Play validation error fails the workflow
with the structured Play message. Do not weaken the guard or cancel a review to
bypass the failure.

## Post-release verification

The workflow must be successful, and the Play job log must contain only the
safe verified identity. Confirm the GitHub artifact and Release asset:

```sh
gh run view RUN_ID --repo ramideltoro/nutsnews-android
gh api repos/ramideltoro/nutsnews-android/actions/runs/RUN_ID/artifacts
gh release view "$tag" --repo ramideltoro/nutsnews-android
```

Record the run ID, tag, commit, package `com.nutsnews.app`, track `internal`,
version name, version code, artifact name, Release URL, and Release asset
SHA-256 on the release issue. Do not record credentials or tokens.

The first controlled tagged release was `android-v1.1.2`: tagged workflow run
`30355469046` delivered `com.nutsnews.app` version `1.1.2` (`1001002`) to
`internal` and created the matching GitHub Release.

## Rollback and recovery

Android version codes and published tag identities are immutable. Rollback is
therefore a roll-forward:

1. Identify the last known-good source commit without moving its old tag.
2. Create a focused fix or revert PR from current `main`; do not bypass branch protection.
3. Merge only after required checks pass on the restored source state.
4. Choose a new SemVer whose generated version code is higher than every code Play has seen.
5. Create a new tag on the repaired `main` and let the normal Internal workflow run.
6. Verify tester delivery before running the separately confirmed Production
   promotion workflow.

Deleting a GitHub Release or tag does not remove a Play release and is not a
rollback. Never attempt to replace an AAB under an existing version code.

If signing credentials may be compromised, stop release work, rotate the Play
upload key through the process in `t61-secure-release-signing.md`, update the
pinned public certificate and all four environment secrets through a reviewed
change, then run protected signing before resuming. Play service-account
credential compromise requires key revocation, a new least-privilege key, and
replacement of only `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.

## Release troubleshooting

### Tag validation fails

Do not rename or move the tag after any protected work starts. If the run
failed before protected signing and no external release exists, leave clear
issue evidence, correct the source through a PR, and use a higher version tag.

### Environment job is skipped or rejected

Run `./scripts/configure-release-environments.sh --check`. The tag policy must
be `android-v*.*.*`, and the pushed tag must still pass the stricter workflow
SemVer validator.

### Signing job fails

Use the job name and non-secret error only. Confirm all four secret names exist
in `release-signing` and dispatch the controlled protected-signing probe. Do
not download or echo the keystore or passwords.

### Play rejects the version code

The code was already used or a higher code exists. Do not reuse the tag or
lower the code. Correct any source issue through a PR, choose a higher SemVer,
and create a new tag.

### Play rejects the edit commit review parameter

Internal releases commit without `changesNotSentForReview`; current Play apps
whose changes are submitted for review automatically reject that legacy query
parameter. If a bundle was uploaded before the commit failed, treat its version
code as consumed, fix the automation through a PR, and create a higher SemVer
tag rather than reusing the failed release identity.

### Play succeeds but GitHub Release publication fails

Rerun failed jobs for the same workflow. The Play step treats the exact
existing package, `internal` track, release name, and version code as an
idempotent verified success; the final job can then publish the Release.

### Upload fails before Play commit

Preserve the failed run and error. A bundle version code may have been consumed
even if it is not active on the track. Fix the cause through a PR and use a
higher SemVer/version code rather than attempting reuse.
