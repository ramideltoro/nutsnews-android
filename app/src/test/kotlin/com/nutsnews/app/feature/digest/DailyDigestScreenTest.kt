package com.nutsnews.app.feature.digest

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.DefaultAppNavigator
import java.net.URI
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
class DailyDigestScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyDigestShowsIosGuidanceBackNavigationAndScreenshot() {
        var closeCount = 0
        setScreen(
            articles = emptyList(),
            onClose = { closeCount += 1 },
        )

        composeRule.onNodeWithTag("daily_digest_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No picks ready yet").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Load stories on the home screen, then come back for " +
                    "a calm daily digest.",
            ).assertIsDisplayed()
        val screenshot = captureLargestWindow()
        composeRule.onNodeWithTag("daily_digest_back_home").performClick()

        assertEquals(1, closeCount)
        assertTrue(sampledColorCount(screenshot) >= 5)
    }

    @Test
    fun smallDigestShowsMetricsCategoryMixAndBothQuickActions() {
        val featured = article("featured")
        val second = article("second")
        val navigator =
            DefaultAppNavigator(AppDestination.Feed).apply {
                navigate(AppDestination.DailyDigest)
            }
        setScreen(
            articles = listOf(second, featured),
            savedStoryIds = mutableStateOf(setOf(StoryId("library-story"))),
            onOpenArticle = { selected ->
                navigator.navigate(AppDestination.ArticleDetail(selected.stableId))
            },
        )

        composeRule.onNodeWithText("Today’s Picks").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_digest_metric_stories").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_digest_metric_sources").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_digest_metric_saved").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_digest_category_mix").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_digest_quick_read").assertExists()
        composeRule.onNodeWithTag("daily_digest_worth_saving").assertExists()
        composeRule.onAllNodesWithTag("daily_digest_story_second").assertCountEquals(0)

        val screenshot = captureLargestWindow()
        composeRule
            .onNodeWithTag("daily_digest_list")
            .performScrollToNode(hasTestTag("daily_digest_quick_read"))
        composeRule.onNodeWithTag("daily_digest_quick_read").performClick()
        assertEquals(
            AppDestination.ArticleDetail(second.stableId),
            navigator.backStack.value.last(),
        )
        assertTrue(sampledColorCount(screenshot) >= 5)
    }

    @Test
    fun fullDigestShowsFeaturedRemainingCardsSavingHapticsDetailAndScreenshot() {
        val articles =
            listOf(
                article("featured"),
                article("quick"),
                article("remaining-one"),
                article("remaining-two"),
                article("remaining-three"),
            )
        val featured = article("featured")
        val remaining = article("remaining-one")
        val savedStoryIds = mutableStateOf<Set<StoryId>>(emptySet())
        val toggled = mutableListOf<Article>()
        val opened = mutableListOf<Article>()
        var hapticCount = 0
        setScreen(
            articles = articles,
            savedStoryIds = savedStoryIds,
            onToggleSaved = { selected ->
                toggled += selected
                savedStoryIds.value = savedStoryIds.value + selected.stableId
            },
            onSaveHaptic = {
                hapticCount += 1
                true
            },
            onOpenArticle = opened::add,
        )

        composeRule
            .onNodeWithTag("daily_digest_list")
            .performScrollToNode(
                hasTestTag("daily_digest_featured_${featured.stableId.value}"),
            )
        composeRule.onNodeWithText("Start here").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_digest_featured_thumbnail").assertExists()
        composeRule.onNodeWithText(featured.summary).assertIsDisplayed()
        composeRule
            .onNodeWithTag("daily_digest_save_${featured.stableId.value}")
            .performClick()
        composeRule.onNodeWithContentDescription("Remove saved story").assertIsDisplayed()

        composeRule
            .onNodeWithTag("daily_digest_list")
            .performScrollToNode(
                hasTestTag("daily_digest_story_${remaining.stableId.value}"),
            )
        composeRule.onNodeWithText("More from today").assertExists()
        composeRule
            .onNodeWithTag(
                "daily_digest_story_thumbnail_${remaining.stableId.value}",
                useUnmergedTree = true,
            ).assertExists()
        composeRule
            .onNodeWithTag(
                "daily_digest_story_source_${remaining.stableId.value}",
                useUnmergedTree = true,
            ).assertExists()
        val screenshot = captureLargestWindow()
        composeRule
            .onNodeWithTag("daily_digest_story_${remaining.stableId.value}")
            .performClick()

        assertEquals(listOf(featured), toggled)
        assertEquals(1, hapticCount)
        assertEquals(listOf(remaining), opened)
        assertTrue(sampledColorCount(screenshot) >= 5)
    }

    @Test
    fun worthSavingUpdatesPersistentPresentationAndOptionalHaptic() {
        val featured = article("featured")
        val candidate = article("quick")
        val savedStoryIds = mutableStateOf(setOf(featured.stableId))
        val toggled = mutableListOf<Article>()
        var hapticCount = 0
        setScreen(
            articles = listOf(featured, candidate),
            savedStoryIds = savedStoryIds,
            onToggleSaved = { selected ->
                toggled += selected
                savedStoryIds.value = savedStoryIds.value + selected.stableId
            },
            onSaveHaptic = {
                hapticCount += 1
                true
            },
        )

        composeRule
            .onNodeWithTag("daily_digest_list")
            .performScrollToNode(hasTestTag("daily_digest_worth_saving"))
        composeRule.onNodeWithTag("daily_digest_worth_saving").performClick()

        assertEquals(listOf(candidate), toggled)
        assertEquals(1, hapticCount)
        composeRule.onNodeWithTag("daily_digest_metric_saved").assertExists()

        var disabledCalls = 0
        assertFalse(
            performDailyDigestSaveHaptic(enabled = false) {
                disabledCalls += 1
                true
            },
        )
        assertEquals(0, disabledCalls)
        assertFalse(performDailyDigestSaveHaptic(enabled = true) { false })
        assertFalse(
            performDailyDigestSaveHaptic(enabled = true) {
                error("No vibrator")
            },
        )
        assertTrue(performDailyDigestSaveHaptic(enabled = true) { true })
    }

    @Test
    fun closeControlDismissesPopulatedDigest() {
        var closeCount = 0
        setScreen(
            articles = listOf(article("featured")),
            onClose = { closeCount += 1 },
        )

        composeRule
            .onNodeWithContentDescription("Close Today’s Picks")
            .performClick()

        assertEquals(1, closeCount)
    }

    private fun setScreen(
        articles: List<Article>,
        savedStoryIds: androidx.compose.runtime.MutableState<Set<StoryId>> =
            mutableStateOf(emptySet()),
        hapticsEnabled: Boolean = true,
        onToggleSaved: (Article) -> Unit = {},
        onSaveHaptic: () -> Boolean = { true },
        onOpenArticle: (Article) -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            val savedIds by savedStoryIds
            NutsNewsTheme(updateSystemBars = false) {
                DailyDigestScreen(
                    articles = articles,
                    savedStoryIds = savedIds,
                    hapticsEnabled = hapticsEnabled,
                    onToggleSaved = onToggleSaved,
                    onSaveHaptic = onSaveHaptic,
                    onOpenArticle = onOpenArticle,
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

private fun article(id: String): Article {
    val values =
        when (id) {
            "featured" ->
                DigestArticleValues(
                    title = "Kind neighbors bring hope to a community garden",
                    summary = "Volunteers help a local rescue and inspire a new beginning.",
                    categories = listOf("Community", "Uplifting"),
                    source = "Good News Daily",
                )

            "quick" ->
                DigestArticleValues(
                    title = "Students celebrate a science achievement",
                    summary = "A short hopeful update.",
                    categories = listOf("Science"),
                    source = "Bright Wire",
                )

            "second" ->
                DigestArticleValues(
                    title = "A teacher helps students discover nature",
                    summary = "A quick story.",
                    categories = listOf("Science", "Nature"),
                    source = "Bright Wire",
                )

            "remaining-one" ->
                DigestArticleValues(
                    title = "Animal rescue volunteers reunite a family",
                    summary = "A community celebrates together.",
                    categories = listOf("Animals"),
                    source = "Local Joy",
                )

            "remaining-two" ->
                DigestArticleValues(
                    title = "A wellness program brings calm mornings",
                    summary = "Neighbors gather for a healthy start.",
                    categories = listOf("Wellness"),
                    source = "Daily Hope",
                )

            "remaining-three" ->
                DigestArticleValues(
                    title = "Garden club shares its harvest",
                    summary = "The whole community benefits.",
                    categories = listOf("Nature"),
                    source = "Local Joy",
                )

            else -> error("Unknown article $id")
        }
    return Article(
        id = id,
        title = values.title,
        summary = values.summary,
        originalUrl = URI("https://example.com/$id"),
        source = values.source,
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = URI("https://example.com/$id.jpg"),
        categories = values.categories,
    )
}

private data class DigestArticleValues(
    val title: String,
    val summary: String,
    val categories: List<String>,
    val source: String,
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
