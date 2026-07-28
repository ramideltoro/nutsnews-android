# T65 Play Store metadata and policy readiness

Issue: [#66](https://github.com/ramideltoro/nutsnews-android/issues/66)

This record is the source-controlled contract for the Google Play Internal
Testing listing and policy answers for `com.nutsnews.app`. It contains no
credentials or secret values.

## Public listing

- App name: `NutsNews`
- Default language: English (United States), `en-US`
- App category: News & Magazines
- Privacy policy: <https://www.nutsnews.com/privacy/android>
- Developer website: <https://www.nutsnews.com>
- Internal release represented by the initial release notes: `1.1.2`
  (`1001002`)
- Listing text and changelog:
  `fastlane/metadata/android/en-US/`
- Machine-readable listing contract: `play-store/listing.json`

The copy describes only behavior implemented and covered by the Android
repository. It does not claim awards, medical outcomes, exclusive reporting,
fact-check guarantees, real-time alerts, cloud synchronization, or other
unsupported capabilities.

## Asset inventory

The checked-in gallery is generated from approved Android brand art and
repository screenshot goldens:

| Play asset | Count | Dimensions | Source |
| --- | ---: | ---: | --- |
| App icon | 1 | 512 x 512 | `brand_icon.png` |
| Feature graphic | 1 | 1024 x 500 | Approved brand icon and factual tagline |
| Phone screenshots | 4 | 1080 x 1920 | Dashboard, story, Good Mood, stats goldens |
| 10-inch tablet screenshots | 3 | 1080 x 1920 | Dashboard, story, settings goldens |

Run `./scripts/generate-play-store-assets.swift` on macOS to regenerate the
assets. Refresh `assets.sha256` only after visually reviewing deliberate source
or generator changes. CI validates the committed dimensions, opacity, paths,
and digests without regenerating platform-specific artwork.

## Play Console policy answers

The complete reviewable declaration is
`play-store/policy-declarations.json`. Enter its answers in Play Console
without broadening or weakening them.

### App access and ads

- All functionality is available without a login or access instructions.
- The app contains no ads.
- The app has no purchases or subscriptions.

### Data Safety

- Data is encrypted in transit.
- No data is shared with third parties by the Android app.
- Declare optional `App activity > In-app search history` collection for
  Archive Search. Its purpose is App functionality.
- Declare required `Device or other IDs` collection for the IP address and
  user agent that can appear in standard service logs. Purposes are App
  functionality; Analytics for service performance and reliability; and Fraud
  prevention, security, and compliance.
- Saved and liked stories, private notes and reflections, reading stats,
  topics, mood, theme, haptics, widget and reminder preferences, and caches are
  local-only app data.
- There is no account-deletion requirement because accounts cannot be created.
  Users can delete local data in the app, clear Android app storage, or
  uninstall. Privacy help is available through
  <https://www.nutsnews.com/contact>.

Do not mark search queries or standard technical service logs as shared,
advertising data, or cross-app tracking. Do not mark local-only app state as
collected.

### Content rating

- Type: News and magazines.
- Disclose changing current-events content.
- Disclose links to independent publisher websites.
- No user-generated content, user interaction, location sharing, digital
  purchases, gambling, or ads.
- The questionnaire note in `policy-declarations.json` explains that external
  publisher pages can contain material NutsNews does not control.

Use the rating assigned by the completed IARC questionnaire; do not state a
rating in metadata before Play calculates it.

### Notifications

- Notification permission is optional.
- The permission prompt occurs only after the user enables the local daily
  reminder.
- The in-app disclosure is: "A local Android notification brings you back to
  Today's Picks."
- The app does not use a remote push-notification service.

### Target audience and news declaration

- Select ages `13-15`, `16-17`, and `18+`.
- Do not select an age group under 13.
- Mark the app as not designed for children.
- The rationale is a general-audience current-events newsreader with links to
  independent publisher sites.
- Identify NutsNews as a News & Magazines app that summarizes positive stories
  and attributes and links to original publishers.

## Publishing workflow

The `Play Store Metadata` workflow is manual, runs only from `main`, and uses
only the protected `play-internal` environment. It receives a short-lived
Android Publisher access token without writing a credential file into the
checkout. It atomically updates the `en-US` listing and graphics, commits the
Play edit, then queries Play to verify the text and image counts.

The Android Publisher API does not expose the Privacy policy, Data Safety,
Content rating, notification, or Target audience forms. Those exact
source-controlled answers must be entered and reviewed in the authorized Play
Console session. The workflow must not be used as evidence that console-only
forms are complete.

## Validation

Run:

```bash
./scripts/generate-play-store-assets.swift
find fastlane/metadata/android/en-US/images -type f -name '*.png' \
  | LC_ALL=C sort \
  | xargs shasum -a 256 \
  > fastlane/metadata/android/en-US/assets.sha256
./scripts/tests/test-play-store-metadata.sh
./gradlew --no-daemon --stacktrace \
  compileDebugKotlin lintDebug testDebugUnitTest
```

After merge, dispatch `Play Store Metadata` from `main`. Confirm its safe log
states that the listing and graphics for `com.nutsnews.app` and `en-US` were
verified. Then review App content and Store presence in Play Console until no
blocking Internal Testing metadata or policy task remains.
