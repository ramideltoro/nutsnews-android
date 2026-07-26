package com.nutsnews.app.navigation

import com.nutsnews.app.core.model.StoryId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class AppDestinationTest {
    @Test
    fun everyStaticDestinationHasAUniqueRestorableRoute() {
        assertEquals(
            listOf(
                "startup",
                "onboarding",
                "feed",
                "personalization",
                "saved",
                "search",
                "good-mood",
                "daily-digest",
                "reading-stats",
                "settings",
                "settings/theme",
                "settings/haptics",
                "settings/widget",
                "help",
            ),
            AppDestination.staticDestinations.map(AppDestination::route),
        )
        assertEquals(
            AppDestination.staticDestinations.size,
            AppDestination.staticDestinations.map(AppDestination::route).toSet().size,
        )
        AppDestination.staticDestinations.forEach { destination ->
            assertEquals(destination, AppDestination.fromRoute(destination.route))
        }
    }

    @Test
    fun storyDestinationRoutesRoundTripUrlSafeIdsAndPresentation() {
        val storyId = StoryId("https://nutsnews.com/story?q=kind news#mañana")
        val destinations =
            listOf(
                AppDestination.ArticleDetail(storyId),
                AppDestination.ListenMode(storyId),
                AppDestination.ShareCard(storyId),
            )

        destinations.forEach { destination ->
            assertEquals(destination, AppDestination.fromRoute(destination.route))
            check(destination.route.none { character -> character.isWhitespace() })
        }
        assertEquals(AppPresentation.FullScreen, destinations[0].presentation)
        assertEquals(AppPresentation.Sheet, destinations[1].presentation)
        assertEquals(AppPresentation.Sheet, destinations[2].presentation)
    }

    @Test
    fun malformedAndUnknownRoutesAreIgnored() {
        assertNull(AppDestination.fromRoute("missing"))
        assertNull(AppDestination.fromRoute("article/"))
        assertNull(AppDestination.fromRoute("article/not-base64!"))
        assertNull(AppDestination.fromRoute("listen/IA"))
    }
}
