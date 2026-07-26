package com.nutsnews.app.navigation

import com.nutsnews.app.core.model.StoryId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

class DefaultAppNavigatorTest {
    @Test
    fun onboardingStatusRoutesFirstRunAndReturningUsersFromStartup() {
        val firstRunNavigator = DefaultAppNavigator()
        firstRunNavigator.applyOnboardingStatus(hasCompletedOnboarding = false)
        assertEquals(
            listOf(AppDestination.Onboarding),
            firstRunNavigator.backStack.value,
        )

        val returningNavigator = DefaultAppNavigator()
        returningNavigator.applyOnboardingStatus(hasCompletedOnboarding = true)
        assertEquals(listOf(AppDestination.Feed), returningNavigator.backStack.value)

        firstRunNavigator.applyOnboardingStatus(hasCompletedOnboarding = true)
        assertEquals(listOf(AppDestination.Feed), firstRunNavigator.backStack.value)
    }

    @Test
    fun everyFeatureDestinationCanOpenAndAndroidBackReturnsToFeed() {
        val storyId = StoryId("https://nutsnews.com/a-story")
        val destinations =
            listOf(
                AppDestination.Personalization,
                AppDestination.ArticleDetail(storyId),
                AppDestination.SavedStories,
                AppDestination.ArchiveSearch,
                AppDestination.GoodMood,
                AppDestination.DailyDigest,
                AppDestination.ReadingStats,
                AppDestination.Settings,
                AppDestination.ThemePicker,
                AppDestination.HapticsSettings,
                AppDestination.WidgetSettings,
                AppDestination.Help,
                AppDestination.ListenMode(storyId),
                AppDestination.ShareCard(storyId),
            )

        destinations.forEach { destination ->
            val navigator = DefaultAppNavigator(AppDestination.Feed)
            navigator.navigate(destination)

            assertEquals(destination, navigator.backStack.value.last())
            assertTrue(navigator.navigateUp())
            assertEquals(listOf(AppDestination.Feed), navigator.backStack.value)
            assertFalse(navigator.navigateUp())
        }
    }

    @Test
    fun rootNavigationResetsInsteadOfNestingAndNonRootResetIsRejected() {
        val navigator = DefaultAppNavigator(AppDestination.Feed)
        navigator.navigate(AppDestination.Settings)
        navigator.navigate(AppDestination.Onboarding)

        assertEquals(listOf(AppDestination.Onboarding), navigator.backStack.value)
        assertFailsWith<IllegalArgumentException> {
            navigator.resetTo(AppDestination.Settings)
        }
        assertFailsWith<IllegalArgumentException> {
            DefaultAppNavigator(AppDestination.Settings)
        }
    }

    @Test
    fun helpLinkedDestinationAndNestedDetailReturnThroughHelp() {
        val navigator = DefaultAppNavigator(AppDestination.Feed)
        navigator.navigate(AppDestination.Help)
        assertTrue(navigator.navigateFromHelp(AppDestination.SavedStories))
        navigator.navigate(AppDestination.ArticleDetail(StoryId("saved-story")))

        assertTrue(navigator.navigateUp())
        assertEquals(AppDestination.SavedStories, navigator.backStack.value.last())
        assertTrue(navigator.navigateUp())
        assertEquals(AppDestination.Help, navigator.backStack.value.last())
        assertTrue(navigator.navigateUp())
        assertEquals(AppDestination.Feed, navigator.backStack.value.last())

        assertFalse(navigator.navigateFromHelp(AppDestination.GoodMood))
        navigator.navigate(AppDestination.Help)
        assertFalse(navigator.navigateFromHelp(AppDestination.Settings))
    }

    @Test
    fun savedStateRestoresCompleteTypedStackAndUnicodeStoryIds() {
        val storyId = StoryId("https://nutsnews.com/mañana/😊?value=one\ntwo")
        val original = DefaultAppNavigator(AppDestination.Feed)
        original.navigate(AppDestination.Help)
        original.navigateFromHelp(AppDestination.SavedStories)
        original.navigate(AppDestination.ArticleDetail(storyId))
        original.navigate(AppDestination.ListenMode(storyId))

        val savedState = original.saveState()
        val restored = DefaultAppNavigator()

        assertTrue(restored.restoreState(savedState))
        assertEquals(original.backStack.value, restored.backStack.value)
        assertFalse(savedState.contains(storyId.value))
    }

    @Test
    fun invalidRestoredStateIsRejectedWithoutChangingTheCurrentStack() {
        val navigator = DefaultAppNavigator()

        assertFalse(navigator.restoreState(null))
        assertFalse(navigator.restoreState(""))
        assertFalse(navigator.restoreState("feed\nnot-a-route"))
        assertFalse(navigator.restoreState("settings"))
        assertFalse(navigator.restoreState("feed\nonboarding"))
        assertEquals(listOf(AppDestination.Startup), navigator.backStack.value)
    }
}
