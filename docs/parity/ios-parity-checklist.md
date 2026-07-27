# NutsNews Android parity checklist

This checklist is owned by the ordered GitHub backlog. Requirement identifiers
are stable and should be cited in implementation pull requests, automated
tests, screenshot names, and T66 audit notes.

The frozen reference is
`ramideltoro/nutsnews-ios@972dda3a0208bd97ddcdc2cd660bbd4360fc6898`.
Screen (`Sxx`), flow (`Fxx`), and motion (`Mxx`) definitions live in
[`ios-parity-baseline.md`](ios-parity-baseline.md).

## Foundation

### T01 — Freeze the iOS parity baseline

- [x] P-T01-01 Record the immutable iOS commit, build environment, bundle IDs,
  app group, and project constraints.
- [x] P-T01-02 Inventory every screen, state, theme, motion, permission,
  widget family, native surface, persistence rule, and user flow.
- [x] P-T01-03 Check in representative phone/tablet screenshots, six-theme
  captures, startup recordings, and their checksums.
- [x] P-T01-04 Add a repeatable validation script for the baseline package.

### T02 — Bootstrap the Android project

- [ ] P-T02-01 Native Kotlin/Compose `:app` launches as `com.nutsnews.app`.
- [ ] P-T02-02 Min SDK is 26 and compile/target SDK is 36.
- [ ] P-T02-03 Kotlin DSL, version catalog, Gradle wrapper, JDK 17, and
  debug/release variants build from a clean checkout.
- [ ] P-T02-04 No website embed or cross-platform runtime is present.

### T03 — Baseline pull-request CI

- [ ] P-T03-01 Wrapper validation and pinned GitHub Actions run for PRs.
- [ ] P-T03-02 Compilation, Lint, unit tests, and debug APK assembly run with
  Gradle caching and cancellation of superseded runs.
- [ ] P-T03-03 Failures upload actionable reports/logs.

### T04 — Application architecture

- [ ] P-T04-01 Core/model, network, data, design-system, navigation, and
  feature packages have explicit boundaries.
- [ ] P-T04-02 UI state is immutable; ViewModels own coroutines; repositories
  are interfaces at feature boundaries.
- [ ] P-T04-03 A manual dependency container wires production dependencies.

### T05 — Branding and launcher assets

- [ ] P-T05-01 Import the frozen asset masters with documented SHA-256 values
  and no distortion.
- [ ] P-T05-02 Generate adaptive, round, legacy, and monochrome launcher icons.
- [ ] P-T05-03 Android splash assets match S01 in light/dark system contexts on
  API 26 and 36.

### T06 — Six-theme design system

- [ ] P-T06-01 Amber, Sakura, SaaS, Foxy, Friday, and Bambi match the frozen
  raw-value mapping, scheme, gradients, and colors.
- [ ] P-T06-02 Shared spacing, radii, type, surface, border, shadow, badge,
  chip, and button tokens match the baseline.
- [ ] P-T06-03 Every theme produces deterministic previews and widget tokens.

### T07 — Article models and decoding

- [ ] P-T07-01 Decode string/numeric IDs and snake_case/camelCase variants.
- [ ] P-T07-02 Apply summary, URL, image, date, source, and category fallbacks,
  including `Uplifting`.
- [ ] P-T07-03 Stable identity is original URL → API ID → normalized title.
- [ ] P-T07-04 Date and category normalization matches the frozen source.

### T08 — Article-feed API

- [ ] P-T08-01 Request `/api/articles` with page and optional encoded category.
- [ ] P-T08-02 Enforce 20-second timeout, 2xx validation, and structured errors.
- [ ] P-T08-03 Centralize production URLs and inject transport for tests.

### T09 — Archive-search API

- [ ] P-T09-01 Request `/api/search` with normalized `q`, page, and limit.
- [ ] P-T09-02 Enforce two-character minimum, page ≥ 0, and limit 1–50.
- [ ] P-T09-03 Expose loading, empty, paging, and structured failure outcomes.

### T10 — Response cache and stale fallback

- [ ] P-T10-01 Feed freshness is 15 minutes and search freshness is 5 minutes.
- [ ] P-T10-02 Cache keys normalize feed category and search query/page/limit.
- [ ] P-T10-03 Corrupt entries are evicted; forced refresh bypasses freshness;
  network failure can return decodable stale data.

### T11 — DataStore preferences

- [ ] P-T11-01 Persist onboarding, topics, mood, goal, reminder, theme, haptics,
  and large-widget stats preference.
- [ ] P-T11-02 Apply S02 defaults and sanitize malformed/invalid values.
- [ ] P-T11-03 Preference updates are observable and restart-safe.

### T12 — Room database and migrations

- [ ] P-T12-01 Define entity/DAO boundaries, timestamps, indexes, and stable IDs.
- [ ] P-T12-02 Export schemas and establish explicit forward migrations.
- [ ] P-T12-03 All access is off-main and observable.

### T13 — Liked and saved stories

- [ ] P-T13-01 Liking stores the full story; unliking removes it.
- [ ] P-T13-02 Deduplicate by stable ID and order newest saved first.
- [ ] P-T13-03 Expose observable save/remove/query/count behavior to S06, S07,
  S13, S14, S15, and S16.

### T14 — Story notes

- [ ] P-T14-01 Create, update, clear, lookup, and count normalized notes.
- [ ] P-T14-02 Preserve timestamps and legacy API-ID lookup.
- [ ] P-T14-03 The same note resolves from every entry route in F07.

### T15 — Story reflections

- [ ] P-T15-01 Persist exactly one Smile/Hope/Revisit reaction per stable ID.
- [ ] P-T15-02 Replacement updates metadata/timestamp without duplication.
- [ ] P-T15-03 Legacy lookup and aggregate counts work.

### T16 — Reading statistics

- [ ] P-T16-01 Track unique stable article IDs by local day and every original
  source open.
- [ ] P-T16-02 Calculate total unique stories, current streak, and bounded
  seven-day activity.
- [ ] P-T16-03 Duplicate same-day detail opens do not increment unique count.

### T17 — For You ranking

- [ ] P-T17-01 Port every topic/mood keyword and default.
- [ ] P-T17-02 Match category bonus, keyword weights, title tie-break,
  fallback fill, and result limits.
- [ ] P-T17-03 Match the user-facing personalization summary.

### T18 — Good Mood scoring

- [ ] P-T18-01 Port Calm, Hopeful, Inspired, and Curious labels/icons/keywords.
- [ ] P-T18-02 Produce deterministic featured and remaining ordering.
- [ ] P-T18-03 Fall back to safe feed ordering when no score is positive.

### T19 — Daily Digest selection

- [ ] P-T19-01 Calculate story/source/saved/category metrics.
- [ ] P-T19-02 Select featured, quick-read, and worth-saving deterministically.
- [ ] P-T19-03 Remaining rows contain no duplicates and handle empty/one/small
  feeds.

## Application shell and feed

### T20 — Application shell and navigation

- [ ] P-T20-01 Route first-run users through F01 and returning users through F02.
- [ ] P-T20-02 Define S03 and every feature/detail destination.
- [ ] P-T20-03 Android back, modal equivalents, restoration, and F18 linked
  return behavior are correct.

### T21 — Startup splash

- [ ] P-T21-01 S01 visual composition and M01 stage order/timing match captures.
- [ ] P-T21-02 Background, fade, scale, icon, title, and subtitle match.
- [ ] P-T21-03 Splash does not replay on ordinary configuration changes.

### T22 — Onboarding and personalization

- [ ] P-T22-01 S02 topics, moods, goal 1–5, reminders, defaults, and copy match.
- [ ] P-T22-02 Empty topic selection is impossible.
- [ ] P-T22-03 First completion and later edit persist and reopen correctly.

### T23 — Daily notifications

- [ ] P-T23-01 Create channel and handle notification permission granted/denied.
- [ ] P-T23-02 Schedule/cancel morning, afternoon, or evening without exact
  alarm permission.
- [ ] P-T23-03 Restore after reboot, package replacement, and timezone change.
- [ ] P-T23-04 Notification content/tap opens the intended app destination.

### T24 — Feed ViewModel

- [ ] P-T24-01 Initial load, category, force refresh, pagination, and retry are
  race-safe.
- [ ] P-T24-02 Deduplicate articles and merge category labels
  case-insensitively.
- [ ] P-T24-03 Expose all S04 loading/refreshing/paging/empty/stale/error state.

### T25 — Feed header, menu, and chips

- [ ] P-T25-01 S03 wordmark, hamburger, divider, and chip styling match.
- [ ] P-T25-02 Menu destinations and order match S03 exactly.
- [ ] P-T25-03 Horizontal category selection is stateful and accessible.

### T26 — Home Dashboard

- [ ] P-T26-01 S05 goal, streak, saved, notes, mood, and personalization match.
- [ ] P-T26-02 Six action cards and For You rows navigate correctly.
- [ ] P-T26-03 Loading, empty recommendation, refresh, and article-open states
  are covered.

### T27 — Feed non-card states

- [ ] P-T27-01 Implement every S04 first-load/empty/error/retry/refresh/page state.
- [ ] P-T27-02 Stale content remains visible after network failure.
- [ ] P-T27-03 Duplicate pagination is prevented and scroll position survives
  updates.

### T28 — Article-card visuals

- [ ] P-T28-01 S06 image, badges, title, summary, source, and date match.
- [ ] P-T28-02 Padding, width, controls, borders, shadows, and line limits match.
- [ ] P-T28-03 Regular and compact source layouts are supported.

### T29 — Article-card like/save

- [ ] P-T29-01 Heart state stays synchronized with saved persistence.
- [ ] P-T29-02 M08 glow/particles and M09 unlike behavior match and restart safely.
- [ ] P-T29-03 Haptics preference and no-vibrator behavior are honored.

## Article and feature screens

### T30 — Article-detail shell and hero

- [ ] P-T30-01 S07 navigation, close, background, and presentation semantics match.
- [ ] P-T30-02 Hero loading/crop/fallback/categories/title cover wide, tall,
  missing, and failed images.
- [ ] P-T30-03 Phone scroll and compact tablet-landscape layouts work.

### T31 — Article brief, summary, and source

- [ ] P-T31-01 Read time and mood derivation match S08.
- [ ] P-T31-02 What happened, Why it's good, and Takeaway derivations match.
- [ ] P-T31-03 Summary and source/date cards work in regular and compact layouts.

### T32 — Detail persistence and reading tracking

- [ ] P-T32-01 Detail heart/save state is persistent.
- [ ] P-T32-02 Detail appearance records one unique daily story open.
- [ ] P-T32-03 Original source opens increment every time and widget stats sync.

### T33 — Original-story browser

- [ ] P-T33-01 Valid URLs open in themed Android Custom Tabs.
- [ ] P-T33-02 Missing, invalid, unavailable-browser, and failed cases recover.
- [ ] P-T33-03 Return and larger-device presentation preserve F08 intent.

### T34 — Article notes

- [ ] P-T34-01 S09 loads draft and supports save/edit/clear/status states.
- [ ] P-T34-02 Keyboard and regular/compact layouts remain usable.
- [ ] P-T34-03 Cross-entry consistency in F07 is tested.

### T35 — Article reflections

- [ ] P-T35-01 All three choices, icons, labels, and selected style match S09.
- [ ] P-T35-02 Replacement, confirmation title/subtitle/date, and persistence work.
- [ ] P-T35-03 Regular and compact layouts never duplicate records.

### T36 — Listen Mode

- [ ] P-T36-01 Structured script and spoken ordering match S11.
- [ ] P-T36-02 Play/pause/resume/stop/status/progress/waveform states match.
- [ ] P-T36-03 Lifecycle cleanup and missing/failed TTS engine recovery work.

### T37 — Positive share card

- [ ] P-T37-01 S12 typography, theme, branding, article fields, and thumbnail match.
- [ ] P-T37-02 Bitmap size/content are deterministic.
- [ ] P-T37-03 Loading/failure states and URI-safe Android Sharesheet work.

### T38 — Saved Stories

- [ ] P-T38-01 S13 empty/populated/count/search/date/image/metadata states match.
- [ ] P-T38-02 Local search covers title, summary, source, and category.
- [ ] P-T38-03 Remove, persistence, and native detail return work.

### T39 — Full archive search

- [ ] P-T39-01 S14 debounce, minimum length, paging, retry, and cache behavior match.
- [ ] P-T39-02 Results render all required metadata and state surfaces.
- [ ] P-T39-03 Save and native detail return work for every result.

### T40 — Good Mood UI

- [ ] P-T40-01 S15 four-choice styling and M06 transition match.
- [ ] P-T40-02 Featured/remaining cards and image states match.
- [ ] P-T40-03 Save, haptics, empty, close, and detail return work.

### T41 — Today's Picks UI

- [ ] P-T41-01 S16 metrics, category mix, and empty/small/full states match.
- [ ] P-T41-02 Featured, quick-read, worth-saving, and remaining cards match.
- [ ] P-T41-03 Save, haptics, close, and detail return work.

### T42 — Reading Stats UI

- [ ] P-T42-01 S17 goal, streak, total, saved, note, original, and seven-day
  values match.
- [ ] P-T42-02 Zero/partial/complete visual states match.
- [ ] P-T42-03 Private/on-device messaging and chart accessibility are preserved.

### T43 — Settings hub

- [ ] P-T43-01 S18 cards, row order, subtitles, icons, and close/back/Home match.
- [ ] P-T43-02 Theme, Haptics, and Widget destinations link correctly.
- [ ] P-T43-03 Current values update immediately in subtitles.

### T44 — Theme selection

- [ ] P-T44-01 S19 names, descriptions, icons, swatches, and selected state match.
- [ ] P-T44-02 Selection persists and updates app/system bars immediately.
- [ ] P-T44-03 M03 transition has no stale recomposition.

### T45 — Haptics and widget preferences

- [ ] P-T45-01 S20 haptics preference reaches all relevant interactions.
- [ ] P-T45-02 S21 large-widget stats preference persists.
- [ ] P-T45-03 Widget refresh is immediate and unsupported haptics are safe.

### T46 — Help and F.A.Q.

- [ ] P-T46-01 S22 guide/checklist/FAQ copy is ported with Android nouns.
- [ ] P-T46-02 Every listed feature action navigates correctly.
- [ ] P-T46-03 F18 restores Help after each linked destination closes.

### T47 — Widget data pipeline

- [ ] P-T47-01 Fetch/cache current article with placeholder and stale fallback.
- [ ] P-T47-02 Project theme, goal, streak, totals, and large-stats preference.
- [ ] P-T47-03 Relevant app-state changes trigger explicit refresh.

### T48 — Home-screen widget

- [ ] P-T48-01 S24/S25/S26 render responsive small/medium/large launcher sizes.
- [ ] P-T48-02 Live/placeholder/fallback, themes, optional stats, and app launch
  match.
- [ ] P-T48-03 Picker preview, scheduled update, and manual refresh work.

## Cross-cutting parity

### T49 — Responsive layouts

- [ ] P-T49-01 Every screen uses window constraints/size classes, not model names.
- [ ] P-T49-02 Compact landscape cards/detail and full-screen tablet intent work.
- [ ] P-T49-03 Phone/tablet/foldable portrait/landscape avoid clipping,
  unreachable controls, and excessive whitespace.

### T50 — Accessibility

- [ ] P-T50-01 Semantics, descriptions, order, state announcements, and headings
  are complete.
- [ ] P-T50-02 Scalable text, touch targets, contrast, keyboard, and TalkBack work.
- [ ] P-T50-03 Reduced motion preserves essential feedback.

### T51 — Lifecycle and offline recovery

- [ ] P-T51-01 Navigation, query, drafts, and scroll-relevant state restore.
- [ ] P-T51-02 TTS/fetch/reminder/stats work avoids duplicate side effects.
- [ ] P-T51-03 Saved data and stale feed/search remain usable offline and after
  process recreation.

### T52 — Animation and visual polish

- [x] P-T52-01 Spacing, type, crops, borders, gradients, shadows, and glows match.
- [x] P-T52-02 M01–M13 timing is tuned side-by-side.
- [x] P-T52-03 Only unavoidable Android-owned differences are documented.

## Automated verification

### T53 — Domain and persistence unit tests

- [x] P-T53-01 Cover decoding, identity, preferences, saves, notes, reflections,
  and stats with deterministic clocks.
- [x] P-T53-02 Cover personalization, mood, digest, dates, defaults, and malformed
  stored data.

### T54 — API and cache integration tests

- [x] P-T54-01 Controlled server covers paging, encoding, errors, timeout,
  malformed data, and cancellation.
- [x] P-T54-02 Fresh/stale/corrupt/force/offline cache paths pass without internet.

### T55 — Room and DataStore migration tests

- [x] P-T55-01 Create/migrate from every committed schema version.
- [x] P-T55-02 Defaults, invalid/future values, identity fallback,
  deduplication, timestamps, and all user data survive.

### T56 — Compose end-to-end tests

- [x] P-T56-01 F01–F18 have deterministic fake-backed UI coverage.
- [x] P-T56-02 Stable semantics selectors cover all primary actions/back paths.
- [x] P-T56-03 `connectedDebugAndroidTest` passes on the primary emulator.

### T57 — Screenshot parity tests

- [x] P-T57-01 Goldens cover every major S01–S26 loading/empty/error/populated state.
- [x] P-T57-02 All themes, phone/tablet widths, large text, and widget sizes are
  represented.
- [x] P-T57-03 Comparison failures retain actionable diff artifacts.

### T58 — Emulator-matrix CI

- [x] P-T58-01 Instrumentation passes on API 26 and API 36 phones.
- [x] P-T58-02 Responsive/screenshot checks pass on a tablet.
- [x] P-T58-03 Failure artifacts include reports, logs, screenshots, and videos.

## Repository security and controls

### T59 — Dependency and security automation

- [x] P-T59-01 Dependabot and dependency review are configured.
- [x] P-T59-02 CodeQL Java/Kotlin and Gradle dependency verification pass.
- [x] P-T59-03 Actions are pinned and secrets cannot enter artifacts.

### T60 — Branch protection

- [ ] P-T60-01 Main requires PRs, current branches, resolved conversations, and
  all CI/security checks.
- [ ] P-T60-02 Force pushes and branch deletion are blocked.
- [ ] P-T60-03 Required-check names and any admin bypass are documented/tested.

## Release and delivery

### T61 — Secure release signing

- [ ] P-T61-01 Upload key generation/backup/rotation/recovery is documented.
- [ ] P-T61-02 Keystore, alias, and passwords load only from protected
  environment secrets.
- [ ] P-T61-03 Release cannot use debug signing; AAB signature is verified.

### T62 — Google Play Internal Testing

- [ ] P-T62-01 Correct `com.nutsnews.app` Play app and Play App Signing exist.
- [ ] P-T62-02 Least-privilege publishing account and testers are configured.
- [ ] P-T62-03 Protected `play-internal` environment and required secrets exist.

### T63 — Tagged-release CD

- [ ] P-T63-01 Only `android-vX.Y.Z` tags trigger and version consistency is checked.
- [ ] P-T63-02 Full tests/security, monotonic version code, signed AAB, and
  artifacts complete before upload.
- [ ] P-T63-03 Internal track deployment, GitHub Release, and Play version are
  verified.

### T64 — Developer and release documentation

- [ ] P-T64-01 Clean-checkout setup/build/test/lint/emulator instructions work.
- [ ] P-T64-02 Architecture, tags, secret names, Play flow, rollback, and
  troubleshooting are documented without secret values.

### T65 — Play Store metadata and policy

- [ ] P-T65-01 Phone/tablet images, icon, feature graphic, descriptions, and
  release notes match the real app.
- [ ] P-T65-02 Privacy URL, Data Safety, content rating, notification disclosure,
  and audience are accurate.
- [ ] P-T65-03 Internal Testing has no blocking metadata/policy errors.

### T66 — Final parity and release-candidate audit

- [ ] P-T66-01 Re-run S01–S26, F01–F20, M01–M13, and every P-T02–P-T65 item on
  real/emulated devices.
- [ ] P-T66-02 Close all critical/major parity, accessibility, reliability,
  security, and performance gaps.
- [ ] P-T66-03 Verify the signed release version on Internal Testing and record
  only unavoidable Android-native differences.
- [ ] P-T66-04 Tracker #1 is fully checked and all required/main/release checks
  are green.
