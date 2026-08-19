# T63 tagged Android release CD

NutsNews releases are initiated only by a tag whose complete name is
`android-vX.Y.Z`, where each component is a canonical non-negative integer.
Prerelease identifiers, build metadata, leading zeroes, manual workflow
dispatches, and tags whose commit is not contained by `main` fail before any
protected environment is entered.

## Version identity

`scripts/release-version.sh` maps the tag SemVer to an Android version code:

```text
major * 1,000,000 + minor * 1,000 + patch
```

Minor and patch components are bounded at 999 and the result is bounded by
Android's `2,100,000,000` maximum. This makes the mapping deterministic and
strictly monotonic for every accepted SemVer. The generated value must exceed
both the versioned baseline code `2` and the highest code already present on
the Internal track. Gradle receives the name and code as explicit properties,
writes the configured build identity, and the workflow compares that identity
with the tag before accepting the AAB.

## Validation and credential boundaries

The tag workflow calls the complete Android CI and Security workflows. The
release cannot be signed until the strict compile, lint, unit test, debug APK,
three-cell emulator matrix, screenshot catalog, wrapper validation, signing
contract, Play contract, branch protection contract, dependency integrity,
and CodeQL analysis have passed.

The `release-signing` job receives only the four `NUTSNEWS_UPLOAD_*` secrets.
It builds the release AAB without configuration-cache credential retention,
verifies the JAR signature against the pinned public upload certificate, and
uploads the renamed AAB as a 30-day GitHub Actions artifact.

The later `play-internal` job receives only
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. It downloads and re-verifies the AAB,
rejects a non-monotonic version code, uploads only to `internal`, commits the
edit, then creates a new read edit and verifies package, track, release name,
and version code. An exact existing release is treated as a verified
idempotent retry. The tagged workflow has no Alpha or Production deployment
path; those promotions are independently confirmed manual workflows.

Only after the Play query succeeds does a job with `contents: write` create or
update the GitHub Release and attach the same verified AAB.

## Protected environment policy

Both environments retain `main` access for the existing controlled Security
workflow and allow candidate release tags through an explicit
`android-v*.*.*` tag deployment policy. Apply or audit that non-secret
configuration with:

```sh
./scripts/configure-release-environments.sh --apply
./scripts/configure-release-environments.sh --check
```

## Controlled release procedure

After the implementation PR and all `main` checks pass, create the next patch
tag from the current `main` commit and monitor at 60-second intervals:

```sh
git fetch origin main --tags
git tag android-v1.1.2 origin/main
git push origin android-v1.1.2
gh run list --workflow tagged-release.yml --limit 1
```

Do not move or reuse a release tag. If validation fails before upload, delete
the failed tag only when no Play version or GitHub Release was created, fix
the problem through a new PR, and create a higher patch tag. If Play contains
the version, keep the immutable tag and rerun the failed workflow; the Play
step verifies the existing release and the GitHub Release step completes.
