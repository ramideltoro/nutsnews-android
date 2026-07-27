# T56 Compose end-to-end coverage

`NutsNewsEndToEndTest` launches the production `MainActivity`, navigator,
ViewModels, and Compose screens on the primary emulator. Its `AppContainer`
uses deterministic in-memory repositories, a fixed clock, a synchronous JSON
transport, a fake reminder scheduler, and no production network service.

All journey interactions use stable `testTag` semantics. Focused JVM Compose
tests remain responsible for exhaustive state and platform-boundary variants
that should not invoke Android-owned surfaces during an end-to-end run.

| Flow | End-to-end or focused deterministic evidence |
| --- | --- |
| F01 First launch | `onboardingFeedAndArticleUserDataJourney` edits and persists onboarding, goal, mood, topics, and a scheduled reminder before reaching feed. |
| F02 Returning launch | `discoverySettingsHelpAndBackNavigationJourney` starts directly on the populated feed from persisted preferences. |
| F03 Browse feed | The first journey selects a category and opens a card; `ArticleFeedContentTest` covers refresh, paging, empty, stale, and error states. |
| F04 Dashboard | The instrumented journeys enter feature destinations from feed; `HomeDashboardTest` covers every dashboard action. |
| F05 Open article | Feed and search both open detail, and Android back restores the exact entry route. |
| F06 Like and save | Feed and search save through the shared fake repository, then Saved Stories reflects the same record. |
| F07 Note and reflection | The first journey saves both records; `ArticleDetailScreenTest` covers edit, clear, replacement, and stable-route reopening. |
| F08 Original source | `ArticleDetailScreenTest` covers valid, invalid, and absent source URLs plus reading-stat updates. |
| F09 Listen | `ArticleDetailScreenTest` covers start, pause, resume, stop, done, dismissal, and unavailable speech. |
| F10 Share | `ArticleDetailScreenTest` and `ArticleShareCardTest` cover image creation, text fallback, cancellation, and return to detail. |
| F11 Saved library | The second journey observes the search-saved story; `SavedStoriesScreenTest` covers query, remove, open, and return. |
| F12 Archive search | The second journey queries, saves, opens detail, and returns with the search route intact. |
| F13 Mood reset | The second journey changes mood; `GoodMoodScreenTest` covers ranked save/open actions and return behavior. |
| F14 Daily digest | The second journey opens the populated digest; `DailyDigestScreenTest` covers save/open actions and empty recovery. |
| F15 Habit stats | Detail records a view and the second journey opens stats; `ReadingStatsScreenTest` covers the full metric/chart surface. |
| F16 Personalize/reminder | The first journey schedules a reminder; `PersonalizationScreenTest` covers editor save/discard and every preference. |
| F17 Settings | The second journey persists theme, haptics, and widget changes, verifies nested back, and returns Home. |
| F18 Help-linked navigation | The second journey opens a Help-linked digest, returns to Help with Android back, and closes to feed. |

Primary device validation:

```text
./gradlew connectedDebugAndroidTest
API 36, 1080x2400, 2 tests, 0 failures
```
