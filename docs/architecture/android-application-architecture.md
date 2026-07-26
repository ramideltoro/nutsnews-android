# Android application architecture

This document defines the boundaries used by NutsNews Android. The frozen iOS
baseline keeps its article models, API/cache code, persistence stores, theme,
and feature views conceptually separate. Android preserves those concepts while
making their dependency direction explicit and testable.

## Package boundaries

| Package | Responsibility | May depend on |
| --- | --- | --- |
| `core.model` | Platform-neutral identities and domain values | Kotlin only |
| `core.network` | Connectivity contracts shared by data sources | `core.model`, coroutines |
| `data.*` | Repository interfaces and their later local/remote implementations | `core.*` |
| `designsystem` | Compose theme, tokens, and reusable branded components | Compose, `core.model` |
| `navigation` | Typed destinations and application back-stack ownership | `core.model`, coroutines |
| `feature.*` | Screen state, AndroidX ViewModels, events, and feature UI | repository interfaces, `designsystem`, `navigation` |
| `di` | Manual application composition root | interfaces and concrete implementations |
| root `app` package | Android lifecycle entry points only | `di`, root feature/navigation UI |

Dependencies point inward toward models and interfaces. A feature does not
instantiate a network client, database, DataStore, or another feature's
ViewModel. Data implementations do not depend on Compose or navigation.

The packages remain in `:app` while the product is small. Their contracts are
module-safe, so a later Gradle module split must not require feature rewrites.

## State and event rules

- Every public UI state is an immutable Kotlin `data class` whose properties
  are `val`s. Compose stability annotations may document that contract.
- A ViewModel exposes state as a read-only `StateFlow`; mutable flows stay
  private to the owner.
- UI sends user intent to ViewModel methods. It does not mutate state or call a
  repository directly.
- One-time platform work remains an explicit event or platform adapter instead
  of being encoded as a durable UI-state flag.
- Repository interfaces expose domain values, `Flow` for observable data, and
  main-safe `suspend` functions for finite work.

## Coroutine ownership

- AndroidX ViewModels own screen work in `viewModelScope`. Leaving a screen
  cancels work with that ViewModel.
- Repositories own dispatcher changes required by blocking I/O and expose
  main-safe APIs. Callers never select a repository's dispatcher.
- Compose may collect state lifecycle-aware and may run animation-only effects;
  it does not launch durable business work.
- Application-long work must have an explicit supervised scope owned by
  `AppContainer`. Deferred guaranteed work belongs in WorkManager. Neither is
  created until a feature requires it.
- Cancellation is propagated. Broad `catch` blocks must rethrow
  `CancellationException`.

## Composition root and testing

`NutsNewsApplication` creates one `DefaultAppContainer`. The container owns
long-lived application dependencies and supplies interfaces to ViewModel
factories. It uses constructor creation and Kotlin lazy properties, with no
service locator or dependency-injection framework.

The production article API client, shared 20-second OkHttp transport,
disk-backed response cache, and Preferences DataStore repository are
application-scoped dependencies owned by this container. Tests replace the
transport, cache, clock, and preferences repository without changing production
URLs or feature code.

`ArticleFeedViewModel` orchestrates first load, category replacement, forced
refresh, pagination, and retry through the injected `FeedArticleSource`.
Immutable state separates initial loading, content refresh, and pagination,
retains non-blocking errors and stale-content provenance, and accumulates
case-insensitively unique category labels. A request generation invalidates
late refresh or pagination results whenever a newer full refresh begins.

User-owned story records use Room with separate DAO boundaries for saved-story
snapshots, notes, reflections, and reading activity. Stable article IDs are
primary keys (or part of the per-day composite key), legacy API IDs and sort
fields are indexed, and all record timestamps use epoch milliseconds. DAOs
expose `Flow` reads and suspending writes so database work never requires
main-thread access.

Room exports versioned schemas to `app/schemas`. Every schema increment must add
an explicit entry to `NutsNewsDatabaseMigrations`; the production builder
registers the complete list and never enables destructive migration fallback.
The debug source set packages the schemas solely for the host-side migration
harness.

`RoomSavedStoryRepository` is the application-scoped owner of liked/saved
stories. One stable article ID maps to one full snapshot, so liking and saving
are the same upsert, unliking is the same delete, and saved-library observers
always receive newest-first records and counts. An injected clock makes saved
timestamps deterministic in tests.

`RoomStoryNoteRepository` owns private note normalization and identity repair.
Reads prefer the stable story ID and retain the frozen API-ID fallback. A
nonblank write transaction removes the old legacy-keyed row before upserting
the stable row; a blank write deletes both identities. Notes and counts are
observable, and an injected clock supplies `updatedAt`.

`RoomStoryReflectionRepository` stores one typed Smile, Hope, or Revisit
reaction per stable story ID. Stable-ID reads retain the legacy API-ID fallback,
while each selection atomically replaces every matching legacy/current row with
fresh story metadata and a timestamp. Reflection lookups and aggregate counts
are observable.

`RoomReadingStatsRepository` records stable story IDs once per local calendar
day while refreshing each story's last-open timestamp. Original-source opens
increment independently. Its observable projection supplies today's counts,
all-time unique stories, a today-anchored streak, and 1–30 recent days with
explicit zero-count entries.

`NutsNewsPersonalization` is the frozen topic/mood catalog and deterministic
For You ranker. It applies category bonuses, weighted topic and mood keyword
matches, title tie-breaking, feed-order fallback filling, result limits, and
the iOS-compatible personalization summary. Preference validation derives its
valid IDs and defaults from the same catalog.

`PersonalizationViewModel` owns the shared first-run and later-edit draft.
Topics and reminder time commit with the explicit save action, while mood,
daily goal, and reminder enabled mirror the iOS `@AppStorage` bindings by
persisting as they change. The editor can discard its remaining draft and
reload the latest persisted values. Its Compose screen is exercised on the JVM
with Robolectric's Compose UI test host.

`AndroidDailyReminderManager` owns the single local daily reminder. It creates
the native notification channel, gates scheduling on Android's runtime and
system notification settings, and replaces or cancels an inexact
`setAndAllowWhileIdle` alarm without requesting exact-alarm access. The alarm
receiver posts the iOS-matched reminder and immediately schedules the next
local occurrence; a separate receiver restores the preference-backed schedule
after boot, package replacement, clock changes, and timezone changes. Tapping
the notification routes through the single-top Activity to Daily Digest after
startup has resolved.

`GoodMoodRecommendationEngine` separately preserves the Good Mood screen's
Calm, Hopeful, Inspired, and Curious definitions and field-specific weights.
It excludes unsafe cards, selects one featured result plus ordered remaining
matches, caps positive results at 16, and falls back to the first 12 safe feed
items only when no article scores positively.

`DailyDigestEngine` derives Today's Picks from the first 24 safe feed cards. It
calculates story/source/saved/category metrics, applies the frozen digest score
and display-date tie-break, selects featured, quick-read, and worth-saving
roles, and returns up to 10 rows excluding the featured and quick-read IDs.

Tests construct ViewModels and domain services with fakes implementing the same
interfaces. `DefaultAppNavigatorTest` is the initial navigation smoke test and
protects the invariant that the back stack is never empty.

The application shell uses a typed, restorable `AppDestination` back stack.
DataStore onboarding state resolves the Startup root to Welcome or Feed;
full-screen feature destinations, settings-stack children, and article sheets
share Android back behavior. The Activity persists route strings in its saved
instance state, including URL-safe story IDs. Help links push their target
above Help so closing nested destinations returns through the linked feature
and then to Help.
