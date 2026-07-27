# T57 Screenshot Parity Coverage

T57 freezes the approved Android rendering of the iOS parity baseline
`972dda3a0208bd97ddcdc2cd660bbd4360fc6898`. The suite renders with
Robolectric API 35 native graphics, a fixed English locale, mdpi density,
disabled motion, local fixture data, and no network or device dependency.

## Commands

- Verify approved PNGs: `./scripts/verify-screenshot-goldens.sh`
- Record an intentionally reviewed change:
  `./scripts/record-screenshot-goldens.sh`
- Run the full unit suite: `./gradlew testDebugUnitTest`

Recording is an explicit operation. Normal Gradle and CI runs only compare.
The committed PNGs live in `app/src/test/goldens`.

## Screen and state matrix

| Baseline surface | Golden coverage |
| --- | --- |
| S01 Startup | Fully visible branded splash |
| S02 Personalize | Loading, populated reminder-on, and 150% text |
| S03 Feed shell | Phone loading/empty/error/populated, tablet populated, and 150% text |
| S04 Feed states | First load, category empty, blocking error, and populated/paged content |
| S05 Dashboard | Loading, empty recommendations, populated metrics/actions, phone and tablet |
| S06 Article cards | Populated phone, tablet, and 150% text feed compositions |
| S07–S09 Detail and tools | Populated liked/note/reflection state, listening toolbar, share failure, unavailable story, phone/tablet/150% text |
| S10 Original story | Owned source action and recoverable browser-unavailable detail state; Chrome Custom Tab chrome remains Android-owned |
| S11 Listen Mode | Active on-device voice toolbar and expanded reading sheet |
| S12 Share | Deterministic 1080×1350 share card, share action, creating-failure state; chooser chrome remains Android-owned |
| S13 Saved | Loading, empty, and populated local library |
| S14 Search | Initial, loading, no-results, error/retry, and populated/saved result |
| S15 Good Mood | Empty and populated recommendations |
| S16 Today’s Picks | Empty and populated digest |
| S17 Stats | Loading, zero, and populated seven-day data |
| S18 Settings | Loading and populated phone plus populated tablet |
| S19 Themes | Amber, Sakura, SaaS, Foxy, Friday, and Bambi |
| S20 Haptics | Enabled preference screen |
| S21 Widget settings | Large-widget stats enabled |
| S22 Help | Hero, guide, and first feature actions |
| S23 Notification | Reminder enabled plus permission-denied guidance; permission dialog and delivered notification chrome remain Android-owned |
| S24–S26 Widgets | Small, medium, and large responsive launcher previews; production Glance structure/actions remain covered by `NutsNewsWidgetContentTest` |

The catalog contains phone 393×852 and tablet 800×1280 profiles. Large-text
goldens use a 1.5 font scale. Widget previews use the production size
classification, theme tokens, and data model at 140×140, 280×140, and 280×280
dp inside a stable launcher canvas.

## Failure artifacts

The comparator permits a one-value RGBA channel rounding difference and fails
when more than 0.01% of pixels exceed it. A mismatch writes:

- `expected.png`
- `actual.png`
- `diff.png`, with changed pixels highlighted in magenta

Files are written to
`app/build/reports/screenshot-parity/<golden-name>/`. The existing Android CI
failure upload includes `app/build/reports`, so these three images are retained
with the Gradle log and test reports. `ScreenshotComparisonArtifactTest`
exercises that artifact contract without changing an approved baseline.

Native Custom Tabs, the Android share chooser, notification permission dialog,
and notification shade are intentionally not treated as app-owned pixels.
Their NutsNews entry/fallback states are frozen here and their behavior remains
covered by the focused integration tests.
