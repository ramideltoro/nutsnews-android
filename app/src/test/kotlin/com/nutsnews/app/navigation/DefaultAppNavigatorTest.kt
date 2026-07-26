package com.nutsnews.app.navigation

import com.nutsnews.app.core.model.StoryId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class DefaultAppNavigatorTest {
    @Test
    fun navigationSmokeTestPreservesAValidBackStack() {
        val navigator = DefaultAppNavigator()

        assertEquals(listOf(AppDestination.Startup), navigator.backStack.value)
        assertFalse(navigator.navigateUp())

        navigator.navigate(AppDestination.Onboarding)
        navigator.navigate(AppDestination.Feed)
        navigator.navigate(AppDestination.ArticleDetail(StoryId("story-1")))

        assertEquals(
            listOf(
                AppDestination.Startup,
                AppDestination.Onboarding,
                AppDestination.Feed,
                AppDestination.ArticleDetail(StoryId("story-1")),
            ),
            navigator.backStack.value,
        )
        assertTrue(navigator.navigateUp())
        assertEquals(AppDestination.Feed, navigator.backStack.value.last())

        navigator.resetTo(AppDestination.Feed)
        navigator.navigate(AppDestination.Feed)

        assertEquals(listOf(AppDestination.Feed), navigator.backStack.value)
    }
}
