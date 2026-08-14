package com.nutsnews.app.feature.stats

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.nutsnews.app.core.model.ReadingStatsDay
import com.nutsnews.app.designsystem.NutsNewsTheme
import java.time.LocalDate
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
class ReadingStatsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun zeroDatasetShowsStartMessageFlatChartAndScreenshot() {
        setScreen(statsState())

        composeRule.onNodeWithText("Reading Stats").assertIsDisplayed()
        composeRule.onNodeWithText("0/3 stories").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Open one uplifting story to start today’s positive streak.",
            ).assertIsDisplayed()
        composeRule.onNodeWithTag("reading_stats_weekly_chart").assertExists()
        weekDates.forEach { date ->
            composeRule.onNodeWithTag("reading_stats_bar_$date").assertExists()
        }
        assertTrue(sampledColorCount(captureLargestWindow()) >= 5)
    }

    @Test
    fun activeDatasetShowsPartialGoalOriginalOpensAndScreenshot() {
        setScreen(
            statsState(
                todayStoryCount = 2,
                dailyGoal = 4,
                totalUniqueStoryCount = 11,
                savedStoryCount = 5,
                noteCount = 3,
                originalOpensTodayCount = 2,
                recentCounts = listOf(0, 1, 0, 2, 1, 3, 2),
            ),
        )

        composeRule.onNodeWithText("2/4 stories").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Nice start. Open 2 more positive stories to complete today’s goal.",
            ).assertIsDisplayed()
        composeRule
            .onNodeWithTag("reading_stats_list")
            .performScrollToNode(hasTestTag("reading_stats_tile_originals"))
        composeRule.onNodeWithTag("reading_stats_tile_opened").assertExists()
        composeRule.onNodeWithTag("reading_stats_tile_favorites").assertExists()
        composeRule.onNodeWithTag("reading_stats_tile_notes").assertExists()
        composeRule.onNodeWithTag("reading_stats_tile_originals").assertIsDisplayed()
        composeRule.onNodeWithTag("reading_stats_value_opened").assertTextEquals("11")
        composeRule.onNodeWithTag("reading_stats_value_favorites").assertTextEquals("5")
        composeRule.onNodeWithTag("reading_stats_value_notes").assertTextEquals("3")
        composeRule.onNodeWithTag("reading_stats_value_originals").assertTextEquals("2")
        assertTrue(sampledColorCount(captureLargestWindow()) >= 5)
    }

    @Test
    fun streakDatasetUsesSingularAndPluralDayLabelsAndScreenshot() {
        setScreen(
            statsState(
                todayStoryCount = 1,
                dailyGoal = 3,
                currentStreak = 1,
                recentCounts = listOf(0, 0, 0, 0, 0, 0, 1),
            ),
        )

        composeRule
            .onNodeWithTag("reading_stats_list")
            .performScrollToNode(hasTestTag("reading_stats_tile_streak"))
        composeRule.onNodeWithTag("reading_stats_tile_streak").assertIsDisplayed()
        composeRule.onNodeWithText("day").assertExists()
        assertTrue(sampledColorCount(captureLargestWindow()) >= 5)
    }

    @Test
    fun completedGoalShowsCappedProgressCompletionMessageAndScreenshot() {
        setScreen(
            statsState(
                todayStoryCount = 5,
                dailyGoal = 3,
                currentStreak = 7,
                totalUniqueStoryCount = 42,
                savedStoryCount = 12,
                noteCount = 8,
                originalOpensTodayCount = 4,
                recentCounts = listOf(1, 2, 3, 1, 4, 2, 5),
            ),
        )

        composeRule.onNodeWithText("5/3 stories").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Today’s good-news goal is complete. Beautiful.",
            ).assertIsDisplayed()
        composeRule.onNodeWithTag("reading_stats_goal_progress").assertExists()
        assertEquals(1f, statsState(todayStoryCount = 5).goalProgress)
        val screenshot = captureLargestWindow()
        composeRule
            .onNodeWithTag("reading_stats_list")
            .performScrollToNode(hasTestTag("reading_stats_tile_streak"))
        composeRule.onNodeWithText("days").assertExists()
        assertTrue(sampledColorCount(screenshot) >= 5)
    }

    @Test
    fun closeControlDismissesDashboard() {
        var closeCount = 0
        setScreen(
            state = statsState(),
            onClose = { closeCount += 1 },
        )

        composeRule.onNodeWithTag("reading_stats_close").performClick()

        assertEquals(1, closeCount)
    }

    private fun setScreen(
        state: ReadingStatsUiState,
        onClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                ReadingStatsScreen(
                    uiState = state,
                    onClose = onClose,
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

private val weekDates =
    (6 downTo 0).map { daysAgo ->
        LocalDate.of(2026, 7, 26).minusDays(daysAgo.toLong())
    }

private fun statsState(
    todayStoryCount: Int = 0,
    dailyGoal: Int = 3,
    currentStreak: Int = 0,
    totalUniqueStoryCount: Int = 0,
    savedStoryCount: Int = 0,
    noteCount: Int = 0,
    originalOpensTodayCount: Int = 0,
    recentCounts: List<Int> = List(7) { 0 },
): ReadingStatsUiState =
    ReadingStatsUiState(
        isLoading = false,
        todayStoryCount = todayStoryCount,
        dailyGoal = dailyGoal,
        currentStreak = currentStreak,
        totalUniqueStoryCount = totalUniqueStoryCount,
        savedStoryCount = savedStoryCount,
        noteCount = noteCount,
        originalOpensTodayCount = originalOpensTodayCount,
        recentDays =
            weekDates.zip(recentCounts).map { (date, count) ->
                ReadingStatsDay(date = date, storyCount = count)
            },
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
