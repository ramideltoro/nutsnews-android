package com.nutsnews.app.navigation

import com.nutsnews.app.core.model.StoryId
import java.nio.charset.StandardCharsets
import java.util.Base64

enum class AppPresentation {
    Root,
    FullScreen,
    Stack,
    Sheet,
}

sealed interface AppDestination {
    val route: String
    val presentation: AppPresentation
    val shellTitle: String

    data object Startup : RootDestination("startup", "Starting NutsNews")

    data object Onboarding : RootDestination("onboarding", "Welcome")

    data object Feed : RootDestination("feed", "NutsNews")

    data object Personalization :
        StaticDestination("personalization", AppPresentation.FullScreen, "Personalize")

    data class ArticleDetail(
        override val storyId: StoryId,
    ) : StoryDestination("article", storyId, AppPresentation.FullScreen, "Story")

    data object SavedStories :
        StaticDestination("saved", AppPresentation.FullScreen, "Favorites")

    data object ArchiveSearch :
        StaticDestination("search", AppPresentation.FullScreen, "Search")

    data object GoodMood :
        StaticDestination("good-mood", AppPresentation.FullScreen, "Good Mood")

    data object DailyDigest :
        StaticDestination("daily-digest", AppPresentation.FullScreen, "Today’s Picks")

    data object ReadingStats :
        StaticDestination("reading-stats", AppPresentation.FullScreen, "Reading Stats")

    data object Settings :
        StaticDestination("settings", AppPresentation.FullScreen, "Settings")

    data object ThemePicker :
        StaticDestination("settings/theme", AppPresentation.Stack, "Appearance")

    data object HapticsSettings :
        StaticDestination("settings/haptics", AppPresentation.Stack, "Haptics")

    data object WidgetSettings :
        StaticDestination("settings/widget", AppPresentation.Stack, "Widget")

    data object ContactUs :
        StaticDestination("settings/contact", AppPresentation.Stack, "Contact us")

    data object Help :
        StaticDestination("help", AppPresentation.FullScreen, "Help & F.A.Q.")

    data class ListenMode(
        override val storyId: StoryId,
    ) : StoryDestination("listen", storyId, AppPresentation.Sheet, "Listen Mode")

    data class ShareCard(
        override val storyId: StoryId,
    ) : StoryDestination("share", storyId, AppPresentation.Sheet, "Share")

    companion object {
        val staticDestinations: List<AppDestination> =
            listOf(
                Startup,
                Onboarding,
                Feed,
                Personalization,
                SavedStories,
                ArchiveSearch,
                GoodMood,
                DailyDigest,
                ReadingStats,
                Settings,
                ThemePicker,
                HapticsSettings,
                WidgetSettings,
                ContactUs,
                Help,
            )

        fun fromRoute(route: String): AppDestination? {
            staticDestinations.firstOrNull { destination ->
                destination.route == route
            }?.let { return it }

            return StoryDestinationPrefixes.firstNotNullOfOrNull { prefix ->
                val encodedStoryId = route.removePrefix("$prefix/")
                if (encodedStoryId == route || encodedStoryId.isEmpty()) {
                    null
                } else {
                    decodeStoryDestination(prefix, encodedStoryId)
                }
            }
        }

        private val StoryDestinationPrefixes = listOf("article", "listen", "share")

        private fun decodeStoryDestination(
            prefix: String,
            encodedStoryId: String,
        ): AppDestination? =
            runCatching {
                val storyId =
                    StoryId(
                        String(
                            Base64.getUrlDecoder().decode(encodedStoryId),
                            StandardCharsets.UTF_8,
                        ),
                    )
                when (prefix) {
                    "article" -> ArticleDetail(storyId)
                    "listen" -> ListenMode(storyId)
                    "share" -> ShareCard(storyId)
                    else -> null
                }
            }.getOrNull()
    }
}

sealed class RootDestination(
    final override val route: String,
    final override val shellTitle: String,
) : AppDestination {
    final override val presentation: AppPresentation = AppPresentation.Root
}

sealed class StaticDestination(
    final override val route: String,
    final override val presentation: AppPresentation,
    final override val shellTitle: String,
) : AppDestination

sealed class StoryDestination(
    routePrefix: String,
    storyIdValue: StoryId,
    final override val presentation: AppPresentation,
    final override val shellTitle: String,
) : AppDestination {
    abstract val storyId: StoryId

    final override val route: String =
        "$routePrefix/${
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(storyIdValue.value.toByteArray(StandardCharsets.UTF_8))
        }"
}
