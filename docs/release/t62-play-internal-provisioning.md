# T62 Google Play Internal Testing provisioning

The versioned destination contract is
`config/play/internal-testing.json`: package `com.nutsnews.app`, track
`internal`, and GitHub Environment `play-internal`.

## Manual Play Console setup

Google Play does not provide an API for creating the application record. A Play
Console administrator must:

1. Create or select the `com.nutsnews.app` application.
2. Complete the initial app setup and opt into Play App Signing.
3. Confirm the registered upload certificate matches
   `config/signing/nutsnews-upload-certificate.pem`.
4. Create an internal tester Google Group, add the intended testers, and attach
   that group to the Internal testing track.
5. Link a dedicated Google Cloud project, enable the Google Play Android
   Developer API, and invite its service account in Play Console.

Grant the service account access only to `com.nutsnews.app` with:

- View app information and download bulk reports (read-only).
- Release apps to testing tracks.

Do not grant production release, user administration, financial, order, or
subscription permissions.

## Protected GitHub environment

`play-internal` uses the explicit deployment policies versioned by T63: the
`main` branch for controlled Security probes and `android-v*.*.*` tags for
release delivery. Validate the exact remote policy with
`./scripts/configure-release-environments.sh --check`. Add the complete service
account JSON key as the environment secret
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. Do not split, print, commit, upload as an
artifact, or place that JSON in repository/environment files.

The credential-free contract runs on every pull request. The protected API
probe runs only through an explicit Security workflow dispatch:

```sh
gh workflow run security.yml \
  --ref main \
  -f run_protected_signing=false \
  -f run_play_verification=true
```

The probe exchanges a signed service-account JWT for an OAuth token, creates a
temporary Android Publisher edit, reads the `internal` track for exactly
`com.nutsnews.app`, and deletes the edit without committing changes.

## Verified provisioning state

T62 is complete. `com.nutsnews.app` is active on Google Play Internal Testing,
Play App Signing recognizes the pinned upload certificate, internal tester
access is configured, and the dedicated service account has only the scoped
app-information and testing-release permissions described above.

The `play-internal` environment contains
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. Security run `30352051909` passed the
controlled package and Internal-track API probe. Tagged release run
`30355469046` later uploaded and queried version `1.1.2` (`1001002`) on the
`internal` track without exposing the credential.

Revalidate the non-secret contract and controlled access after any Play role,
service-account key, environment policy, package, or track change:

```sh
./scripts/validate-play-internal-provisioning.sh --remote
gh workflow run security.yml \
  --ref main \
  -f run_protected_signing=false \
  -f run_play_verification=true
```
