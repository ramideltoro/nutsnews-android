# NutsNews Android

Native Kotlin and Jetpack Compose implementation of NutsNews for phones,
tablets, foldables, landscape layouts, and Android home-screen widgets. The
application ID is `com.nutsnews.app`; the supported runtime range starts at
API 26 and the project compiles and targets API 36.

## Start here

1. Install Android Studio Quail 2 (`2026.1.2`) or newer, JDK 17, and the API
   36 Android SDK.
2. Read [the developer guide](docs/development.md) for checkout, Android SDK,
   build, lint, unit-test, screenshot, and emulator commands.
3. Read [the architecture](docs/architecture/android-application-architecture.md)
   before changing package boundaries, state ownership, persistence, or
   navigation.
4. Read [the release runbook](docs/release/release-operations.md) before
   creating any `android-vX.Y.Z` tag.

## Fast host validation

```sh
./scripts/validate-documentation.sh
./gradlew --dependency-verification=strict --stacktrace --console=plain \
  compileDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

Release delivery is automated only to Google Play Internal Testing. Production
promotion is intentionally manual and is not implemented by repository
automation.
