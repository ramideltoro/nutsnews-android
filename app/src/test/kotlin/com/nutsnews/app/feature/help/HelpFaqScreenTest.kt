package com.nutsnews.app.feature.help

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.DefaultAppNavigator
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HelpFaqScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyHelpActionOpensItsDestinationAndBackReturnsToHelp() {
        val storyId = StoryId("help-story")
        val navigator =
            DefaultAppNavigator(AppDestination.Feed).apply {
                navigate(AppDestination.Help)
            }
        setScreen(
            onOpenTodayPicks = {
                navigator.navigateFromHelp(AppDestination.DailyDigest)
            },
            onOpenGoodMood = {
                navigator.navigateFromHelp(AppDestination.GoodMood)
            },
            onOpenReadingStats = {
                navigator.navigateFromHelp(AppDestination.ReadingStats)
            },
            onOpenSavedStories = {
                navigator.navigateFromHelp(AppDestination.SavedStories)
            },
            onOpenSearch = {
                navigator.navigateFromHelp(AppDestination.ArchiveSearch)
            },
            onOpenPersonalization = {
                navigator.navigateFromHelp(AppDestination.Personalization)
            },
            onOpenStoryFeatures = {
                navigator.navigateFromHelp(AppDestination.ArticleDetail(storyId))
            },
        )

        val actions =
            linkedMapOf(
                "help_action_personalize" to AppDestination.Personalization,
                "help_action_today_picks" to AppDestination.DailyDigest,
                "help_action_good_mood" to AppDestination.GoodMood,
                "help_action_story" to AppDestination.ArticleDetail(storyId),
                "help_action_reading_stats" to AppDestination.ReadingStats,
                "help_action_saved" to AppDestination.SavedStories,
                "help_action_search" to AppDestination.ArchiveSearch,
            )

        actions.forEach { (tag, destination) ->
            scrollTo(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).performClick()
            assertEquals(destination, navigator.backStack.value.last())
            assertTrue(navigator.navigateUp())
            assertEquals(AppDestination.Help, navigator.backStack.value.last())
        }
    }

    @Test
    fun closeAndEveryActionExposeAccessibleLabels() {
        var closeCount = 0
        setScreen(onClose = { closeCount += 1 })

        composeRule
            .onNodeWithContentDescription("Close help")
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, closeCount)

        HelpActionLabels.forEach { label ->
            scrollTo(hasContentDescription(label))
            composeRule
                .onNodeWithContentDescription(label)
                .assertIsDisplayed()
        }
    }

    @Test
    fun completeFeatureGuideChecklistAndFaqCopyArePresent() {
        setScreen()

        CompleteGuideCopy.forEach { copy ->
            scrollTo(hasText(copy))
            composeRule
                .onNodeWithText(copy, substring = false)
                .assertIsDisplayed()
        }
    }

    @Test
    fun helpScreenRendersAcrossAllThemes() {
        var theme by mutableStateOf(NutsNewsAppTheme.Amber)
        composeRule.setContent {
            NutsNewsTheme(theme = theme, updateSystemBars = false) {
                HelpFaqScreen(
                    onClose = {},
                    onOpenTodayPicks = {},
                    onOpenGoodMood = {},
                    onOpenReadingStats = {},
                    onOpenSavedStories = {},
                    onOpenSearch = {},
                    onOpenPersonalization = {},
                    onOpenStoryFeatures = {},
                )
            }
        }
        val screenshots = mutableListOf<Bitmap>()

        NutsNewsAppTheme.entries.forEach { appTheme ->
            composeRule.runOnIdle {
                theme = appTheme
            }
            composeRule.onNodeWithTag("help_screen").assertIsDisplayed()
            composeRule.onNodeWithTag("help_hero").assertIsDisplayed()
            screenshots += captureLargestWindow()
        }

        assertEquals(NutsNewsAppTheme.entries.size, screenshots.size)
        assertTrue(screenshots.all { screenshot -> sampledColorCount(screenshot) >= 5 })
    }

    private fun scrollTo(matcher: androidx.compose.ui.test.SemanticsMatcher) {
        composeRule
            .onNodeWithTag("help_list")
            .performScrollToNode(matcher)
    }

    private fun setScreen(
        onClose: () -> Unit = {},
        onOpenTodayPicks: () -> Unit = {},
        onOpenGoodMood: () -> Unit = {},
        onOpenReadingStats: () -> Unit = {},
        onOpenSavedStories: () -> Unit = {},
        onOpenSearch: () -> Unit = {},
        onOpenPersonalization: () -> Unit = {},
        onOpenStoryFeatures: () -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                HelpFaqScreen(
                    onClose = onClose,
                    onOpenTodayPicks = onOpenTodayPicks,
                    onOpenGoodMood = onOpenGoodMood,
                    onOpenReadingStats = onOpenReadingStats,
                    onOpenSavedStories = onOpenSavedStories,
                    onOpenSearch = onOpenSearch,
                    onOpenPersonalization = onOpenPersonalization,
                    onOpenStoryFeatures = onOpenStoryFeatures,
                )
            }
        }
    }

    private fun captureLargestWindow(): Bitmap =
        composeRule.runOnIdle {
            val windowManagerClass = Class.forName("android.view.WindowManagerGlobal")
            val instance =
                windowManagerClass
                    .getDeclaredMethod("getInstance")
                    .invoke(null)
            val viewsField =
                windowManagerClass
                    .getDeclaredField("mViews")
                    .apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val views = viewsField.get(instance) as List<View>
            val view =
                views
                    .filter { candidate ->
                        candidate.width > 0 && candidate.height > 0 && candidate.isShown
                    }.maxBy { candidate -> candidate.width * candidate.height }
            Bitmap
                .createBitmap(
                    view.width,
                    view.height,
                    Bitmap.Config.ARGB_8888,
                ).also { bitmap ->
                    view.draw(Canvas(bitmap))
                }
        }
}

private val HelpActionLabels =
    listOf(
        "Open Personalize NutsNews",
        "Open Today’s Picks",
        "Open Good Mood",
        "Open a story",
        "Open Reading Stats",
        "Open Saved Stories",
        "Open Archive Search",
    )

private val CompleteGuideCopy =
    listOf(
        "How to use NutsNews",
        "A simple guide to the native tools that make NutsNews feel calm, personal, and easy to return to.",
        "Start here",
        "Use these features first to shape your daily feed. You can find these options later in the menu.",
        "Story tools",
        "Open a story to use the native reading tools.",
        "NutsNews Brief",
        "A quick feel-good summary and takeaway.",
        "Listen Mode",
        "Have Android read the brief aloud. For the best sound, install a high-quality English Text-to-Speech voice in Android Settings.",
        "Daily Reflection",
        "Save a private reaction like “Made me smile” or “Gave me hope.”",
        "Good News Share Card",
        "Create a branded image card to share through the Android Sharesheet.",
        "Better Listen Mode voice",
        "For the smoothest story listening, install a high-quality English voice on your Android device.",
        "Install a high-quality voice",
        "Open Android Settings and search for “Text-to-speech output.” Choose your preferred engine, then install an English voice.",
        "Use it in NutsNews",
        "After the voice finishes downloading, reopen NutsNews, open any story, and tap Play. Listen Mode will automatically use the best installed English voice available.",
        "Build a small habit",
        "Use NutsNews like a daily positive reset.",
        "Android features",
        "NutsNews also works outside the main feed.",
        "Home Screen Widget",
        "Add NutsNews Daily from the Android widget picker for a quick positive headline.",
        "Local reminders",
        "Use onboarding or personalization to set a gentle good-news reminder.",
        "Native sharing",
        "Share positive story cards through the built-in Android Sharesheet.",
        "Private on-device choices",
        "Your saved stories, reflections, stats, theme, and preferences stay on your device.",
        "FAQ",
        "Common questions about NutsNews.",
        "What is NutsNews for?",
        "NutsNews is for quick, calm breaks with positive stories and simple tools that help you save, reflect, and return to good news.",
        "How do I change what I see?",
        "Open Personalize to adjust topics, mood, reading goal, and reminder preferences.",
        "How do I save something for later?",
        "Open any story and use Save, or use Daily Reflection to mark why a story mattered to you.",
        "How do I add the widget?",
        "Long press the Android Home Screen, tap Widgets, find NutsNews, then add NutsNews Daily.",
        "Can I listen instead of read?",
        "Yes. Open a story and tap Play to hear the NutsNews Brief aloud. For the best listening experience, open Android Settings, search for “Text-to-speech output,” and install a high-quality English voice.",
    )

private fun sampledColorCount(bitmap: Bitmap): Int {
    val colors = mutableSetOf<Int>()
    val stepX = max(1, bitmap.width / 16)
    val stepY = max(1, bitmap.height / 16)
    for (x in 0 until bitmap.width step stepX) {
        for (y in 0 until bitmap.height step stepY) {
            colors += bitmap.getPixel(x, y)
        }
    }
    return colors.size
}
