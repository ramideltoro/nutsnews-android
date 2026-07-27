# T58 emulator-matrix CI

Android CI runs the deterministic T56 journeys on three native Android Virtual
Devices after the fast compile, lint, unit-test, screenshot, and APK job passes.

| Matrix cell | System image | Hardware profile | Checks |
| --- | --- | --- | --- |
| `phone-api26` | Google APIs API 26 x86_64 | Pixel 2 | `connectedDebugAndroidTest` |
| `phone-api36` | Google APIs API 36 x86_64 | Pixel 2 | `connectedDebugAndroidTest` |
| `tablet-api36` | Google APIs API 36 x86_64 | Pixel C | screenshot goldens, then `connectedDebugAndroidTest` |

The tablet cell runs the deterministic T57 catalog, including all approved
phone/tablet/large-text/widget goldens, before exercising the production
activity at tablet dimensions. The device tests continue to use in-memory
fixtures and make no production network calls.

## Caching

Each cell restores an exact AVD snapshot keyed by runner OS, API/profile cell,
system-image target, architecture, and a manual cache generation. Pull requests
may restore the main-branch snapshot but cannot save new AVD caches. Main and
manual runs save a missing snapshot. Only `~/.android/avd` is cached; generated
ADB credentials and repository outputs are excluded.

Gradle uses the shared read-only-on-PR cache configured by
`gradle/actions/setup-gradle`.

## Failure artifacts

`run-emulator-tests.sh` records the screen while instrumentation runs. A test
failure retains:

- the exact Gradle console log and exit code;
- instrumentation XML/HTML reports and additional device output;
- logcat plus activity/window dumps;
- a final emulator screenshot;
- the MP4 screen recording;
- any expected/actual/diff images emitted by the T57 comparator.

The per-cell bundle is uploaded for 14 days as
`emulator-<cell>-failure-<run>-<attempt>`. Successful device runs stop and
delete the temporary recording.

## Controlled failure

The manual `exercise_emulator_failure` workflow input runs only the API 36
phone cell. It installs and opens the real debug app, deliberately returns a
non-zero test status, captures the same failure bundle, validates every
required file is non-empty, and uploads it. The probe action is
`continue-on-error`, but the validation step fails the job if the expected
failure or any diagnostic is missing.

Normal pull-request and main runs always execute all three real matrix cells.
