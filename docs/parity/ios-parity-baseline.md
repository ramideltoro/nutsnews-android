# NutsNews iOS parity baseline

This document is the binding visual and behavioral reference for the native
Android replica. It freezes the iOS implementation, records every user-facing
surface and state found in that source tree, and gives later Android issues
stable identifiers to cite.

## Frozen source of truth

| Field | Value |
| --- | --- |
| Repository | `ramideltoro/nutsnews-ios` |
| Commit | `972dda3a0208bd97ddcdc2cd660bbd4360fc6898` |
| Commit date | 2026-07-04 17:39:27 -04:00 |
| Commit subject | `Merge pull request #2 from ramideltoro/codex/update-agents-pr-policy` |
| Xcode used for captures | Xcode 26.5 (17F42) |
| iOS deployment target at the frozen commit | 18.5 |
| App bundle identifier | `com.nutsnews.app` |
| Widget bundle identifier | `com.nutsnews.app.NutsNewsWidgetExtension` |
| Shared app group | `group.com.nutsnews.app` |

The Swift source at this exact commit is authoritative for copy, ordering,
derived behavior, persistence semantics, colors, spacing, motion, and feature
flows. The checked-in captures are observed rendering evidence. When an iOS
system surface has no direct Android equivalent, preserve the intent and use
the Android-native surface named in the mapping below.

## Reproducing the frozen build

Use a detached checkout. Do not build from a moving branch.

```bash
git clone https://github.com/ramideltoro/nutsnews-ios.git
cd nutsnews-ios
git switch --detach 972dda3a0208bd97ddcdc2cd660bbd4360fc6898

xcodebuild \
  -project NutsNews/NutsNews.xcodeproj \
  -scheme NutsNews \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.6' \
  -derivedDataPath /tmp/nutsnews-ios-parity-derived \
  build CODE_SIGNING_ALLOWED=NO
```

The frozen build was verified with `** BUILD SUCCEEDED **`.

### Capture profiles

| Profile | Logical profile | Captured pixels | Purpose |
| --- | --- | --- | --- |
| Phone | iPhone 16 Pro, iOS 18.6, portrait | 1206 × 2622 | Primary visual and motion reference |
| Tablet | iPad Pro 11-inch (M4), iPadOS 18.6, portrait | 1668 × 2420 | Tablet installation and presentation reference |

The frozen Xcode project declares `TARGETED_DEVICE_FAMILY = 1` for both app and
widget, so the iPad runs the iPhone app in compatibility mode. The source still
contains explicit iPad full-screen and iPad-landscape compact branches. Android
must preserve that source intent and the backlog's adaptive requirements rather
than reproducing the compatibility-mode black surround.

## Source-tree inventory

The frozen application contains 10,865 lines of Swift across the app and
widget. Every feature route is represented by one of the entries below.

| Area | Frozen source paths | Responsibility |
| --- | --- | --- |
| App entry and shell | `NutsNews/NutsNews/NutsNewsApp.swift`, `ContentView.swift` | Startup, splash gating, onboarding/feed routing |
| Design system | `Design/NutsNewsTheme.swift` | Six themes, tokens, gradients, borders, shadows, color scheme |
| API models | `Models/Article.swift`, `ArticlesResponse.swift` | Decoding, normalization, dates, category fallback |
| Preferences | `Models/NutsNewsUserPreferences.swift` | Topics, moods, goal, reminder choices, For You ranking |
| User-owned data | `LikedStoryStore.swift`, `SavedStoryStore.swift`, `StoryNoteStore.swift`, `NutsNewsReflectionStore.swift`, `ReadingStatsStore.swift` | Likes, saved stories, notes, reflections, activity |
| Network and cache | `Networking/NutsNewsAPIClient.swift`, `NutsNewsArticlesCache.swift` | Feed/search requests, freshness, stale fallback |
| Feed | `Features/Feed/FeedView.swift`, `ArticleFeedViewModel.swift`, `ArticleCardView.swift` | Header, menu, chips, dashboard, feed states, cards |
| Home dashboard | `Features/Home/HomeDashboardView.swift` | Goal, quick actions, For You |
| Onboarding | `Features/Onboarding/OnboardingView.swift` | First-run and later personalization |
| Article detail | `Features/Article/ArticleDetailView.swift` | Hero, brief, reflection, note, share, listen, source |
| Saved | `Features/Saved/SavedStoriesView.swift` | Local saved-story library and search |
| Archive search | `Features/Search/ArchiveSearchView.swift` | Full archive search and paging |
| Good Mood | `Features/Mood/GoodMoodView.swift` | Mood picker and deterministic recommendations |
| Daily digest | `Features/Digest/DailyDigestView.swift` | Metrics, positive mix, featured/quick/save picks |
| Reading stats | `Features/Stats/ReadingStatsView.swift` | Goal, seven-day chart, totals |
| Help | `Features/Review/HelpFAQView.swift` | Guide, FAQ, linked return flows |
| Splash | `Features/Splash/SplashView.swift` | Branded staged startup |
| Native support | `Support/SafariView.swift`, `NutsNewsListenController.swift`, `NutsNewsReminderCenter.swift`, `NutsNewsShareCard.swift`, `NutsNewsWidgetSettings.swift` | System integrations |
| Widget | `NutsNewsWidget/NutsNewsWidget.swift` | Small, medium, and large home-screen widgets |

## Screen and state inventory

The `Sxx` identifiers are stable. Android implementation, tests, screenshots,
and audit notes should cite them.

| ID | Surface | Required states and behavior | Primary later tasks |
| --- | --- | --- | --- |
| S01 | Startup splash | Empty gradient; icon fade-in; title fade-in; subtitle fade-in; staged fade-out; transition to onboarding or feed. Splash is process-start state and must not replay on ordinary recreation. | T05, T06, T21, T52, T57 |
| S02 | Onboarding / Personalize | First-run title `Welcome`; edit title `Personalize`; hero; eight topics; exactly one-or-more selected; four moods; daily goal 1–5; reminder off/on; morning/afternoon/evening; scheduling/saved/off status; finish disabled only when topic set is empty; first-run completion and edit close/save. Defaults are Animals, Science, Community; Calm; goal 3; reminder off at 8:00 AM. | T11, T22, T23, T24 |
| S03 | Feed shell | Static header, centered NutsNews wordmark, hamburger, divider, horizontal `All` plus discovered category chips. Menu order: Help & F.A.Q.; divider; Today’s Picks; Good Mood; Reading Stats; Saved; Search; Personalize; Settings. | T20, T24, T25 |
| S04 | Feed content states | First-load progress and `Loading good news...`; dashboard + articles; category-specific feed; pull-to-refresh; forced refresh; pagination spinner; stale last-known-good content; empty all/category; retry/load-more button; non-blocking error banner; safe rejection of malformed/unsafe cards; preserved scroll. | T08–T10, T24, T27, T51 |
| S05 | Home Dashboard | Zero/partial/complete goal; percent ring; mood/saved/note pills; six action cards; reminder-on variant; For You loading, populated, fewer-than-three, and no-section states; three-row paging/refresh spin; edit Personalize. | T13, T16, T17, T26 |
| S06 | Article card | Regular portrait and compact iPad-landscape source layouts; image loading/success/failure/missing; 3:2 crop; up to six category badges; full title/summary or compact line limits; date/source; Read Story; heart unliked/liked; persisted liked card border; glow; celebration particles; unlike; haptics on/off. | T05–T07, T28, T29 |
| S07 | Article detail shell | Modal/full-screen presentation; Story title bar; close, play/waveform, and heart actions; regular scrolling and compact iPad-landscape two-column layouts; wide-image 3:2 detection; standard 210-point crop; image loading/failure/missing fallback; up to eight categories; full title. | T20, T30 |
| S08 | Article brief and content | Read-time and primary mood metrics; What happened; Why it’s good news; Feel-good takeaway; summary; source/date intent; missing-summary fallback; regular and compact content limits. | T31 |
| S09 | Article persistent tools | Like/save persisted together; unique detail-open tracking; original-open count; My Note empty/existing/edit/saved/cleared; three Daily Reflection choices empty/selected/replaced; source button disabled when URL is absent; widget stats refresh. | T13–T16, T32, T34, T35 |
| S10 | Original story browser | Valid URL in `SFSafariViewController`; close return to detail; full-screen container intent on iPad; missing URL disabled; failed/unavailable browser fallback must remain recoverable on Android. | T33 |
| S11 | Listen Mode | Medium/large sheet; auto-start; idle/reading/paused; play/pause/resume/stop/done; dynamic title/status; animated waveform; highlighted spoken progress; structured Story/Why/Takeaway preview; completion/cancellation; missing voice or engine failure; lifecycle cleanup. | T36, T51 |
| S12 | Positive share card | Mini preview; creating progress; deterministic rendered card; branded header; title, mood, source, Why it's good, takeaway, thumbnail treatment; render success/fallback-to-text; native sharesheet with image, text, and optional URL. | T05, T06, T37 |
| S13 | Saved Stories | Empty library; populated count/hero; local query empty/results/no match; image loading/failure/missing; saved date; metadata; open detail; remove; persisted/restarted list newest-first. | T13, T38 |
| S14 | Archive Search | Start state; query input; clear; whitespace normalization; under-two-character guard; 350 ms debounce; explicit submit; loading; results count; image states; save/unsave; open detail; no results; error; load-more; end of pages; cached/offline result. | T09, T10, T13, T39 |
| S15 | Good Mood | Default Hopeful; Calm/Hopeful/Inspired/Curious selector; selected spring animation; featured recommendation; remaining recommendations; image states; saved/unsaved; empty input fallback; open detail; close; haptics on/off. | T13, T18, T40 |
| S16 | Today’s Picks | Empty and populated digest; Stories/Sources/Saved metrics; category mix empty/populated; Start here featured card; Quick read; Worth saving; More from today rows; small-feed deduplication; image states; save/unsave; open detail; close. | T13, T19, T41 |
| S17 | Reading Stats | Zero, partial-goal, and completed-goal messages; current streak; total unique opened; saved; notes; original opens today; bounded seven-day chart including zero bars; close. | T13, T14, T16, T42 |
| S18 | Settings hub | Three rows in order: Theme, Haptics, Widget; selected values in subtitles; themed cards; Home action; navigation/back. | T11, T20, T43 |
| S19 | Theme settings | Amber, Sakura, SaaS, Foxy, Friday, Bambi rows; selected radio; preview swatches; live whole-app switch; system bar scheme switch; transition glow; persistence; Home action. | T06, T11, T44 |
| S20 | Haptics settings | Like button haptics on/off; default on; persistence; unsupported vibrator remains functional; Home action. | T11, T45 |
| S21 | Widget settings | `Show stats on large widget` on/off; default on; explanatory copy; immediate widget timeline refresh; persistence; Home action. | T11, T45 |
| S22 | Help & F.A.Q. | Hero; Start here links; Story tools checklist/link; better voice instructions; daily habit links; platform feature checklist; five FAQ rows; close. Each link opens its feature and returns to Help after that destination closes. Copy must be adapted from iOS terms to Android-native terms without changing meaning. | T20, T46 |
| S23 | Daily notification | Permission not requested until reminder is enabled; granted/denied/error; one repeating local reminder; 8:00/15:00/20:00 local time; title/body/sound; replace/cancel existing request. Android additionally restores after reboot, app update, and timezone change. | T23 |
| S24 | Widget small | Placeholder, live, and fallback; icon; up to four title lines; mood/source footer; no summary or stats; tap opens app. | T05, T06, T47, T48 |
| S25 | Widget medium | Placeholder, live, and fallback; title up to three lines; summary up to four lines; mood/source footer; no stats; tap opens app. | T05, T06, T47, T48 |
| S26 | Widget large | Placeholder, live, and fallback; larger title/summary; optional stats panel with goal progress, streak, and total stories; setting on/off; tap opens app. | T05, T06, T45, T47, T48 |

### Feed and image state rules

- Feed cards render only when thumbnail, non-empty title, and non-empty source
  are present; title must be at most 340 characters and summary at most 4,000.
- Cards with unbroken tokens longer than 46 characters are rejected.
- Async images always define loading, success, failure, missing, and unknown
  fallback states.
- Feed page merges deduplicate by `Article.id`; saved/note/reflection identity
  uses the stable identity rules below.
- Fresh feed cache is 15 minutes. Fresh search cache is five minutes. A failed
  network request returns any decodable stale response for the same normalized
  key.

## Navigation and user-flow inventory

| ID | Flow | Binding sequence |
| --- | --- | --- |
| F01 | First launch | S01 → S02 → persist choices → S03/S04. If reminders are enabled, request permission while completing S02. |
| F02 | Returning launch | S01 → S03/S04 using persisted theme, preferences, and local data. |
| F03 | Browse feed | S03 category selection → S04 refresh/page/error/empty handling → S06. |
| F04 | Dashboard | S05 action card → S14/S15/S16/S17/S13/S02; closing returns to feed. |
| F05 | Open article | S06 Read Story → S07/S08; closing returns to the exact entry route. |
| F06 | Like and save | S06 or S07 heart → glow/haptic/particles → S13 updated; unliking removes it from Saved. |
| F07 | Note and reflection | S09 save/edit/clear note and select/replace reflection; the same record appears regardless of entry route. |
| F08 | Original source | S07/S09 source action → glow → count original open → S10 → return to S07. |
| F09 | Listen | S07 play → S11 auto-start → pause/resume/stop/done; dismiss or leave detail stops speech. |
| F10 | Share | S07/S12 create → render image or text fallback → Android Sharesheet → return to detail. |
| F11 | Saved library | Menu/S05 → S13 search/remove/open → S07 → return to S13. |
| F12 | Archive search | Menu/S05 → S14 query/page/save/open → S07 → return to S14 with query state. |
| F13 | Mood reset | Menu/S05 → S15 mood select/save/open → S07 → return to S15. |
| F14 | Daily digest | Menu/S05 → S16 save/open → S07 → return to S16. |
| F15 | Habit stats | Menu/S05 → S17; opening details elsewhere updates S17 and S26. |
| F16 | Personalize/reminder | Menu/S05 → S02 edit → save → optional S23 permission/schedule → return to feed. |
| F17 | Settings | Menu → S18 → S19/S20/S21 → live persistence/widget refresh → Home. |
| F18 | Help-linked navigation | Menu → S22 → linked screen or first story → close linked screen → S22 restored → close to feed. |
| F19 | Widget | Launcher picker → S24/S25/S26 → scheduled/manual refresh → tap → app feed. |
| F20 | Offline recovery | Cached S04/S14 and all user-owned S09/S13/S17 data remain usable; network errors do not erase last-known-good content. |

## Six-theme design system

| Public name | Frozen raw value | Scheme | Background family | Accent |
| --- | --- | --- | --- | --- |
| Amber | `amber` | Dark | `#0A0A0A` → `#17120A` → `#0A0A0A` | `#FACC15` |
| Sakura | `sakura` | Light | `#FDEFF4` → `#FFF7ED` → `#F4EAD2` | `#7AA95C` |
| SaaS | `modernSaaS` | Dark | `#121212` → `#181818` → `#101010` | `#3B82F6` |
| Foxy | `sanJuan` | Light | `#FFF2D0` → `#FFE4B0` → `#D8F1E4` | `#0077B6` |
| Friday | `creativePremium` | Dark | `#0F172A` → `#111827` → `#0B1120` | `#7C3AED` |
| Bambi | `moodyCyberpunk` | Dark | `#1A211B` → `#20281F` → `#151A16` | `#FACC15` |

Legacy theme values map as follows: `plain` and `dark` → Amber,
`darkPink` → Foxy, and `lilac` → Sakura.

### Shared visual tokens

| Token family | Frozen values |
| --- | --- |
| Spacing | 4, 6, 10, 16, 26, 42 |
| Radii | 6, 10, 16, 26, 42 |
| Card padding | 16 |
| Card corner | 26 |
| Image/control corner | 16 |
| Feed image | 188 high where fixed; cards otherwise use 3:2 |
| Detail hero | 210 high unless wide-image 3:2 crop is selected |
| Button background | Per-theme top-leading to bottom-trailing gradient |
| App background | Per-theme linear gradient plus top-leading radial accent |
| Surface language | Strong/regular cards, one-pixel themed borders, amber/accent glows, capsule chips |

## Branding and source assets

Use the exact assets at the frozen commit. Do not substitute generated artwork
or distort their aspect ratios.

| Frozen path | Pixels | SHA-256 | Role |
| --- | --- | --- | --- |
| `Assets.xcassets/BrandChestnuts.imageset/BrandChestnuts.png` | 1024 × 1024 | `00a812d26633fd2db1e6941d8e64912dc4de321e32b482329250eb75912b7be2` | Brand/source artwork |
| `Assets.xcassets/SplashApprovedIcon.imageset/SplashApprovedIcon.png` | 1024 × 1024 | `00a812d26633fd2db1e6941d8e64912dc4de321e32b482329250eb75912b7be2` | Approved icon |
| `Assets.xcassets/AppIcon.appiconset/AppIcon-ios-marketing-1024x1024@1x.png` | 1024 × 1024 | `00a812d26633fd2db1e6941d8e64912dc4de321e32b482329250eb75912b7be2` | App icon master |
| `Assets.xcassets/SplashChestnuts.imageset/SplashChestnuts.png` | 790 × 740 | `84f9a9966c2226d59966285d3aea46b957a5b41d0f0920447d17ecf359aa1bb2` | Legacy/opaque splash art |
| `Assets.xcassets/SplashTransparentChestnuts.imageset/SplashTransparentChestnuts.png` | 1254 × 1254 | `3ba7557550ccab3720f451cfa8db6c7de3d2eac1d634b4192df395a03bc087f6` | Animated splash art |

## Motion and haptic inventory

| ID | Interaction | Frozen timing/curve |
| --- | --- | --- |
| M01 | Splash | 0.5 s initial pause; icon/title/subtitle enter 0.5 s apart with 0.35 s ease-in-out; hold 1.0 s; icon/title/subtitle leave 0.5 s apart with 0.35 s ease-in-out; content reveal 0.45 s. |
| M02 | Onboarding route change | 0.25 s ease-in-out opacity; app content starts at 0.99 scale under splash. |
| M03 | Theme change | 0.25 s live theme transition; old accent glow starts at radius 22 and fades/morphs to new accent over 1.0 s; reset at 1.05 s. |
| M04 | Feed scroll entrance | 0.32 s ease-in-out; off-phase opacity 0.22, scale 0.96, Y offset 18. |
| M05 | Dashboard For You refresh | Spring response 0.34, damping 0.82, 360-degree refresh rotation. |
| M06 | Mood selection | Spring response 0.35, damping 0.84. |
| M07 | Read/open/settings/source actions | Glow radius 22; destination opens after 0.16 s; ease-out fade 1.0 s; reset 1.05 s. |
| M08 | Like | Button glow plus 0.18 s card glow-in; 1.0 s hold; 0.35 s settle; persistent liked border; 18 emoji particles travel/fade over 2.0 s and clear at 2.15 s. |
| M09 | Unlike | 0.25 s ease-in-out removal; no celebration particles. |
| M10 | Note/reflection status | 0.2 s enter; message holds 1.8 s; 0.25 s exit. Reflection page glow fades over 0.9 s. |
| M11 | Listen waveform | Timeline-driven 28 bars; reading/paused transitions 0.16–0.18 s; frequency/level follow speech progress. |
| M12 | Share | Action glow 1.0 s; creating state; reset spinner state after 0.8 s. |
| M13 | Haptics | Soft impact intensity 0.85 for feed/detail like; light impact for Mood and Digest save; all gated by the persisted preference. |

Android should honor the system animator-duration/reduced-motion setting by
reducing or replacing decorative movement while preserving state feedback.

## Permissions and Android-native surface mapping

| Capability | Frozen iOS behavior | Required Android equivalent |
| --- | --- | --- |
| Notifications | Requests alert, badge, and sound only when reminder is enabled; denial removes pending request. | `POST_NOTIFICATIONS` runtime permission on API 33+, notification channel, inexact daily work/alarm, denial-safe state |
| Original story | `SFSafariViewController`, full-screen container intent on iPad | Android Custom Tabs with themed toolbar; external browser fallback |
| Sharing | `UIActivityViewController` with rendered image, text, optional URL | Android Sharesheet with `content://` URI grant, text, optional URL |
| Listen | `AVSpeechSynthesizer`, best installed English voice, audio-session cleanup | Android `TextToSpeech`, engine availability/failure handling, lifecycle shutdown |
| Haptics | `UIImpactFeedbackGenerator`, optional | `HapticFeedback`/`Vibrator`, gated preference, no-vibrator safe |
| Widget | WidgetKit static configuration and three families | Jetpack Glance responsive small/medium/large sizes |
| Persistence | `UserDefaults` JSON plus app group | DataStore for preferences; Room for user-owned story records; widget-safe shared projection |
| Reboot/update/timezone restore | iOS calendar notification is OS-managed | Android boot, package-replaced, and timezone/time receivers/rescheduling |

No camera, microphone, location, contacts, health, photo-library, or account
permission appears in the frozen source. Listen Mode does not record audio.

## Persistence and identity behavior

| Data | Frozen behavior |
| --- | --- |
| Stable article ID | Original URL string, then nonblank API ID, then trimmed lowercase title |
| Likes | Stored as a deduplicated set of stable IDs |
| Saved stories | Full article snapshot plus `savedAt`; deduplicated; newest first; liking and saving are the same user action |
| Notes | One normalized nonblank note per stable ID; blank deletes; `updatedAt`; legacy API-ID fallback |
| Reflections | One Smile/Hope/Revisit record per stable ID; selecting another replaces it; timestamp and metadata; legacy API-ID fallback |
| Reading stats | Unique stable IDs per local calendar day; last-open time; original source opens counted per day; current consecutive streak; seven-day projection |
| Preferences | Onboarding completion, topics, mood, daily goal, reminder enabled/hour, theme, haptics, large-widget stats |
| Widget projection | Theme, raw stats, goal, and large-widget preference copied to the shared app group and timeline reloaded |

Malformed stored JSON resolves to safe defaults. Invalid topic, mood, goal,
reminder, and theme values are sanitized to the defaults recorded above.

## Network and derived-content behavior

- Feed endpoint: `https://www.nutsnews.com/api/articles`, page required,
  category optional, 20-second timeout, explicit 2xx validation.
- Search endpoint: `https://www.nutsnews.com/api/search`, normalized `q`, page
  clamped to at least 0, limit clamped to 1–50, minimum query length 2.
- Models accept string or integer IDs, snake_case or camelCase API fields,
  multiple URL/date/image fallbacks, string or list categories, and the
  `Uplifting` fallback badge.
- Feed cache key normalizes category case and blank to `all`. Search cache key
  normalizes query case/whitespace and includes page/limit.
- For You scores selected category matches +4, selected topic keywords +2, and
  selected mood keywords +1; ties sort by title; unmatched input fills the
  requested limit in feed order.
- Good Mood defines Calm, Hopeful, Inspired, and Curious keyword sets; ties use
  display-date ordering; no positive scores fall back to safe feed order.
- Daily Digest scores positive categories/keywords, then selects featured,
  quick-read, worth-saving, and remaining rows without duplicates.

## Widget inventory

The widget configuration name is `NutsNews Daily` and the description is
`A calm good-news headline for your Home Screen.`

- Supported source families: `.systemSmall`, `.systemMedium`, `.systemLarge`.
- Timeline fetches page 0, limit 5 with a 12-second timeout and refreshes every
  three hours.
- Placeholder copy and stats are deterministic.
- Failed URL construction, HTTP status, decoding, empty response, or transport
  returns a deterministic fallback entry.
- The selected theme controls background, card, border, text, accent, and icon.
- Large stats are on by default and show progress, streak, and unique story
  count.
- Android must add explicit manual refresh and scheduled refresh behavior
  required by T47/T48 while preserving these states.

## Capture inventory

All evidence lives in [`reference/ios`](reference/ios). Capture checksums are
locked in [`captures.sha256`](reference/ios/captures.sha256).

| File | Evidence |
| --- | --- |
| `iphone-startup.mp4` | Complete phone startup choreography into first-run onboarding |
| `ipad-startup.mp4` | Complete tablet startup choreography and compatibility-mode presentation |
| `iphone-splash.png` | Fully composed splash keyframe |
| `iphone-onboarding.png` | Phone first-run defaults and upper onboarding layout |
| `ipad-onboarding.png` | Tablet first-run presentation |
| `iphone-feed.png` | Amber returning-user feed/dashboard |
| `ipad-feed.png` | Tablet returning-user feed/dashboard presentation |
| `iphone-theme-sakura-feed.png` | Sakura feed |
| `iphone-theme-modernSaaS-feed.png` | SaaS feed |
| `iphone-theme-sanJuan-feed.png` | Foxy feed |
| `iphone-theme-creativePremium-feed.png` | Friday feed |
| `iphone-theme-moodyCyberpunk-feed.png` | Bambi feed |

The startup recordings are the primary timing reference for S01/M01. The
theme screenshots preserve real production-feed rendering at the frozen app
commit; article copy and thumbnail payloads remain live server data and are not
golden test fixtures.

## Known frozen-baseline inconsistencies

These are recorded so Android work makes a deliberate choice instead of
silently inheriting an iOS project-setting accident.

1. The binary target is iPhone-only (`TARGETED_DEVICE_FAMILY = 1`) while
   `FeedView` and `ArticleDetailView` contain explicit iPad full-screen and
   landscape layouts. T49 requires the Android app to implement the adaptive
   source intent on phone, tablet, foldable, and landscape.
2. `ArticleDetailView` defines regular and compact Source cards, but the frozen
   content stacks primarily expose source/date through metadata and the source
   action. T31 explicitly requires the source attribution card, so Android
   should include it using the frozen helper styling.
3. Help copy names iPhone Settings, iOS speech, Safari, the iOS share sheet,
   and the iOS widget gallery. Android must preserve the guidance but replace
   those nouns with Android system surfaces.
4. The frozen local reminder relies on iOS calendar scheduling. Android must
   implement the additional reboot, app-update, and timezone restoration named
   by T23.
5. Frozen capture devices run iOS/iPadOS 18.6 because Xcode 26.5 did not expose
   the installed iOS 17 simulators as valid build destinations for this
   deployment target.

## Completion contract

The executable parity checklist is
[`ios-parity-checklist.md`](ios-parity-checklist.md). A later issue is complete
only when its owned checklist IDs are implemented, tested, and visually
compared where applicable. T66 re-runs every checklist group against this
frozen commit and records only unavoidable Android-native differences.
