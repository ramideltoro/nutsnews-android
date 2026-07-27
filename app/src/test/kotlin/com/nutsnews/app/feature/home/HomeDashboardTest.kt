package com.nutsnews.app.feature.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.data.preferences.NutsNewsPersonalization
import com.nutsnews.app.designsystem.NutsNewsTheme
import java.net.URI
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
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeDashboardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun heroMetricsAndEveryQuickActionMatchStateAndNavigate() {
        val destinations = mutableListOf<String>()
        setContent(
            uiState =
                populatedState.copy(
                    todayStoryCount = 3,
                    dailyGoal = 3,
                    currentStreak = 6,
                    savedCount = 9,
                    notesCount = 4,
                    reminderEnabled = true,
                    reminderHour = 20,
                ),
            onTodayPicks = { destinations += "daily" },
            onGoodMood = { destinations += "mood" },
            onReadingStats = { destinations += "stats" },
            onSavedStories = { destinations += "saved" },
            onArchiveSearch = { destinations += "search" },
            onPersonalize = { destinations += "personalize" },
        )

        composeRule.onNodeWithText("Goal complete ✨").assertIsDisplayed()
        composeRule.onNodeWithText("100%").assertIsDisplayed()
        composeRule.onNodeWithText("9 saved").assertIsDisplayed()
        composeRule.onNodeWithText("4 notes").assertIsDisplayed()
        composeRule.onNodeWithText("6 day streak").assertIsDisplayed()

        listOf(
            "daily_digest",
            "good_mood",
            "reading_stats",
            "saved",
            "search",
            "personalize",
        ).forEach { tag ->
            composeRule
                .onNodeWithTag("home_dashboard")
                .performScrollToNode(hasTestTag("dashboard_action_$tag"))
            composeRule
                .onNodeWithTag("dashboard_action_$tag")
                .assertIsDisplayed()
                .performClick()
        }

        assertEquals(
            listOf("daily", "mood", "stats", "saved", "search", "personalize"),
            destinations,
        )
        composeRule.onNodeWithText("Reminder On").assertIsDisplayed()
        composeRule.onNodeWithText("Daily at 8:00 PM").assertIsDisplayed()
    }

    @Test
    fun forYouRanksPagesRefreshesAndOpensTheSelectedArticle() {
        val articles = sampleArticles()
        val expectedPool =
            NutsNewsPersonalization.personalizedArticles(
                articles = articles,
                selectedTopicIds = populatedState.selectedTopicIds,
                selectedMoodId = populatedState.selectedMoodId,
                limit = 12,
            )
        var refreshCount = 0
        val opened = mutableListOf<Article>()
        setContent(
            articles = articles,
            onRefreshForYou = { refreshCount += 1 },
            onOpenArticle = opened::add,
        )

        composeRule
            .onNodeWithTag("home_dashboard")
            .performScrollToNode(hasTestTag("for_you_section"))
        composeRule.onNodeWithText("For You").assertIsDisplayed()
        composeRule
            .onNodeWithTag("for_you_story_${expectedPool.first().stableId.value}")
            .assertIsDisplayed()
            .performClick()
        assertEquals(listOf(expectedPool.first()), opened)

        composeRule
            .onNodeWithContentDescription("Refresh For You stories")
            .performClick()
        assertEquals(1, refreshCount)
        composeRule
            .onNodeWithTag("for_you_story_${expectedPool[3].stableId.value}")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Edit For You preferences")
            .assertIsDisplayed()
    }

    @Test
    fun populatedDashboardScreenshotContainsPersonalizedStoryRows() {
        setContent(articles = sampleArticles())
        composeRule
            .onNodeWithTag("home_dashboard")
            .performScrollToNode(hasTestTag("for_you_section"))
        val populatedScreenshot =
            captureWindowBitmaps()
                .maxBy { image -> image.width * image.height }
        assertTrue(sampledColorCount(populatedScreenshot) >= 5)
    }

    @Test
    fun loadingDashboardScreenshotContainsNativeProgressState() {
        setContent(articles = emptyList(), isFeedLoading = true)
        composeRule
            .onNodeWithTag("home_dashboard")
            .performScrollToNode(hasTestTag("for_you_section"))
        composeRule.onNodeWithTag("for_you_loading").assertIsDisplayed()
        val loadingScreenshot =
            captureWindowBitmaps()
                .maxBy { image -> image.width * image.height }
        assertTrue(sampledColorCount(loadingScreenshot) >= 4)
    }

    @Test
    fun emptyDashboardScreenshotOmitsRecommendations() {
        setContent(articles = emptyList(), isFeedLoading = false)
        composeRule.onAllNodesWithTag("for_you_section").assertCountEquals(0)
        val emptyScreenshot =
            captureWindowBitmaps()
                .maxBy { image -> image.width * image.height }
        assertTrue(sampledColorCount(emptyScreenshot) >= 5)
    }

    private fun captureWindowBitmaps(): List<Bitmap> =
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

            views
                .filter { view -> view.width > 0 && view.height > 0 && view.isShown }
                .map { view ->
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

    private fun setContent(
        uiState: HomeDashboardUiState = populatedState,
        articles: List<Article> = emptyList(),
        isFeedLoading: Boolean = false,
        onTodayPicks: () -> Unit = {},
        onGoodMood: () -> Unit = {},
        onReadingStats: () -> Unit = {},
        onSavedStories: () -> Unit = {},
        onArchiveSearch: () -> Unit = {},
        onPersonalize: () -> Unit = {},
        onRefreshForYou: () -> Unit = {},
        onOpenArticle: (Article) -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                HomeDashboard(
                    uiState = uiState,
                    articles = articles,
                    isFeedLoading = isFeedLoading,
                    onTodayPicks = onTodayPicks,
                    onGoodMood = onGoodMood,
                    onReadingStats = onReadingStats,
                    onSavedStories = onSavedStories,
                    onArchiveSearch = onArchiveSearch,
                    onPersonalize = onPersonalize,
                    onRefreshForYou = onRefreshForYou,
                    onOpenArticle = onOpenArticle,
                )
            }
        }
    }
}

private val populatedState =
    HomeDashboardUiState(
        isLoading = false,
        todayStoryCount = 2,
        dailyGoal = 3,
        currentStreak = 4,
        savedCount = 8,
        notesCount = 2,
        selectedTopicIds = setOf("community", "science", "animals"),
        selectedMoodId = "hopeful",
    )

private fun sampleArticles(): List<Article> =
    listOf(
        article(
            id = "science",
            title = "Community scientists celebrate a hopeful discovery",
            categories = listOf("Science", "Community"),
        ),
        article(
            id = "animals",
            title = "Rescued animals find new homes",
            categories = listOf("Animals"),
        ),
        article(
            id = "community",
            title = "Neighbors restore a community garden",
            categories = listOf("Community"),
        ),
        article(
            id = "space",
            title = "NASA research reveals a new world",
            categories = listOf("Science"),
        ),
        article(
            id = "travel",
            title = "A quiet island trail welcomes travelers",
            categories = listOf("Travel"),
        ),
    )

private fun article(
    id: String,
    title: String,
    categories: List<String>,
): Article =
    Article(
        id = id,
        title = title,
        summary = "Good news from $id.",
        originalUrl = URI("https://example.com/$id"),
        source = "NutsNews",
        publishedAt = null,
        createdAt = null,
        thumbnailUrl = null,
        categories = categories,
    )

private fun sampledColorCount(image: Bitmap): Int {
    val xStep = max(1, image.width / 30)
    val yStep = max(1, image.height / 30)
    return buildSet {
        for (y in 0 until image.height step yStep) {
            for (x in 0 until image.width step xStep) {
                add(image.getPixel(x, y))
            }
        }
    }.size
}
