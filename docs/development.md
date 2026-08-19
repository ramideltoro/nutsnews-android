# NutsNews Android development

## Supported toolchain

Use the repository wrapper and version catalog rather than installing a global
Gradle or overriding dependency versions.

| Component | Required version or contract |
| --- | --- |
| Android Studio | Quail 2 (`2026.1.2`) or newer; this is the first release listed as supporting AGP 9.3 |
| JDK | 17 for both Android Studio's Gradle JDK and `JAVA_HOME` |
| Gradle | Wrapper `9.5.0`; run `./gradlew`, never a global `gradle` |
| Android Gradle Plugin | `9.3.0` |
| Kotlin | `2.3.21` |
| Android SDK | Platform 36 and Build Tools 36; compile/target 36, minimum runtime API 26 |

Android Studio compatibility and JDK requirements are maintained in the
official Android documentation:
<https://developer.android.com/build/releases/about-agp> and
<https://developer.android.com/build/jdks>.

## Clean checkout

```sh
git clone https://github.com/ramideltoro/nutsnews-android.git
cd nutsnews-android
git switch main
git pull --ff-only origin main
```

Set the command-line JDK to 17 and expose the SDK without committing a local
path. On macOS the normal SDK location is:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

On Linux, set `ANDROID_HOME` and `ANDROID_SDK_ROOT` to the SDK installed by
Android Studio. A developer-specific `local.properties` containing `sdk.dir`
is also supported and ignored by Git, but environment variables are preferred
for reproducible command logs. Confirm the wrapper and Java runtime:

```sh
./gradlew --version
adb version
```

## Build, lint, and host tests

The pull-request compile gate is reproduced by:

```sh
./gradlew \
  --dependency-verification=strict \
  --stacktrace \
  --console=plain \
  compileDebugKotlin \
  lintDebug \
  testDebugUnitTest \
  assembleDebug
```

The debug APK is written under `app/build/outputs/apk/debug/`. Lint reports are
under `app/build/reports/`; JVM test reports are under
`app/build/reports/tests/`. Do not commit generated build output.

Run the screenshot catalog separately when changing UI, tokens, layout, text
scale, or widget rendering:

```sh
./scripts/verify-screenshot-goldens.sh
```

Recording new goldens is an intentional review operation, not a way to make a
failed comparison pass:

```sh
./scripts/record-screenshot-goldens.sh
git diff -- app/src/test/goldens
```

Review every changed image and checksum before committing it.

## Device and emulator tests

Create AVDs in Android Studio Device Manager using Google APIs images. The CI
matrix is authoritative:

| Cell | API | Profile | Form factor |
| --- | --- | --- | --- |
| `phone-api26` | 26 | Pixel 2 | phone |
| `phone-api36` | 36 | Pixel 2 | phone |
| `tablet-api36` | 36 | Pixel C | tablet |

With one device or emulator booted and visible in `adb devices`, run:

```sh
adb wait-for-device
./gradlew --stacktrace --console=plain connectedDebugAndroidTest
```

To reproduce the CI wrapper, including screen recording and failure evidence,
use the dimensions for the active device:

```sh
./scripts/run-emulator-tests.sh local-phone phone 720x1280
./scripts/run-emulator-tests.sh local-tablet tablet 1280x800
```

Run only one command for the connected form factor. Failure evidence is stored
under `artifacts/emulator/<matrix-id>/`. GitHub executes all three real cells
on every pull request and push to `main`.

## Repository contract validation

Run these credential-free checks after changing automation, delivery, or
documentation:

```sh
./scripts/validate-android-ci.sh
./scripts/validate-security-automation.sh
./scripts/validate-branch-protection.sh
./scripts/validate-release-signing.sh
./scripts/validate-play-internal-provisioning.sh
./scripts/validate-tagged-release.sh
./scripts/tests/test-tagged-release.sh
./scripts/validate-play-closed-promotion.sh
./scripts/tests/test-play-closed-promotion.sh
./scripts/validate-play-production-promotion.sh
./scripts/tests/test-play-production-promotion.sh
./scripts/validate-documentation.sh
```

Remote checks require the authenticated GitHub CLI but never a token on the
command line:

```sh
./scripts/validate-branch-protection.sh --remote
./scripts/validate-play-internal-provisioning.sh --remote
./scripts/configure-release-environments.sh --check
```

## Architecture overview

The application is a single `:app` Gradle module with module-safe package
boundaries. Platform-neutral models are under `core.model`; networking and
connectivity contracts are under `core.network`; repositories and local or
remote implementations are under `data.*`; branded Compose primitives are in
`designsystem`; typed routes and back-stack ownership are in `navigation`;
screen state and UI are in `feature.*`; and `di.DefaultAppContainer` is the
manual application composition root.

Features depend on repository interfaces and never instantiate storage,
network clients, or other feature ViewModels. ViewModels own durable screen
work and expose immutable `StateFlow`; Compose emits intent and owns only UI
effects. Room, DataStore, disk cache, API clients, reminders, TTS, share-card
generation, and Glance widgets remain behind explicit boundaries. Read the
complete [architecture contract](architecture/android-application-architecture.md)
before moving responsibilities.

## Development troubleshooting

### SDK location not found

Set `ANDROID_HOME` and `ANDROID_SDK_ROOT`, or create an ignored
`local.properties` with an absolute `sdk.dir`. Do not commit a workstation
path.

### Wrong Java runtime

`./gradlew --version` must report JVM 17. Align Android Studio's Gradle JDK and
the shell's `JAVA_HOME`; do not change source compatibility to accommodate a
different local JDK.

### Dependency verification failure

Treat a checksum or missing-metadata failure as a supply-chain review event.
Do not disable strict verification. For an intentional dependency update,
follow `docs/security/t59-dependency-security-automation.md` and review only
the expected coordinates and checksums.

### Emulator is offline or instrumentation is stuck

Confirm exactly one intended device with `adb devices`, cold-boot the AVD,
then rerun the device command. Preserve `artifacts/emulator` and Gradle reports
when reporting a reproducible failure.

### Screenshot comparison fails

Open the expected, actual, and diff artifacts. Fix unintended rendering drift.
Only use the recording script when the product change intentionally updates
the approved baseline.
