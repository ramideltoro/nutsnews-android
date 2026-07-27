package com.nutsnews.app.feature.mood

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.nutsnews.app.data.article.GoodMood
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
class GoodMoodScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyMoodChoiceSelectsItsBestMatchAndProducesACompleteScreenshot() {
        val articles = moodArticles()
        setScreen(articles = articles)

        val expectedFeatured =
            linkedMapOf(
                GoodMood.Calm to "calm",
                GoodMood.Hopeful to "hopeful",
                GoodMood.Inspired to "inspired",
                GoodMood.Curious to "curious",
            )
        val screenshots = mutableListOf<Bitmap>()

        expectedFeatured.forEach { (mood, articleId) ->
            composeRule
                .onNodeWithTag("good_mood_list")
                .performScrollToNode(hasTestTag("good_mood_choice_${mood.id}"))
            composeRule
                .onNodeWithTag("good_mood_choice_${mood.id}")
                .performClick()
            composeRule
                .onNodeWithTag("good_mood_list")
                .performScrollToNode(hasTestTag("good_mood_featured_label"))
            composeRule.onNodeWithText("${mood.title} pick").assertIsDisplayed()
            composeRule
                .onNodeWithTag("good_mood_featured_${article(articleId).stableId.value}")
                .assertIsDisplayed()
            screenshots += captureLargestWindow()
        }

        assertEquals(4, screenshots.size)
        assertTrue(screenshots.all { screenshot -> sampledColorCount(screenshot) >= 5 })
    }

    @Test
    fun featuredAndRemainingRecommendationsShowThumbnailsAndOpenNativeDetail() {
        val articles = moodArticles()
        val expectedFeatured = article("hopeful")
        val expectedRemaining = article("hopeful-more")
        val navigator = DefaultAppNavigator(AppDestination.Feed)
        setScreen(
            articles = articles,
            onOpenArticle = { selected ->
                navigator.navigate(AppDestination.ArticleDetail(selected.stableId))
            },
        )

        composeRule
            .onNodeWithTag("good_mood_list")
            .performScrollToNode(hasTestTag("good_mood_featured_${expectedFeatured.stableId.value}"))
        composeRule.onNodeWithText("Best match").assertIsDisplayed()
        composeRule.onNodeWithTag("good_mood_featured_thumbnail").assertExists()
        composeRule.onNodeWithText(expectedFeatured.title).assertIsDisplayed()
        composeRule.onNodeWithText(expectedFeatured.summary).assertIsDisplayed()

        composeRule
            .onNodeWithTag("good_mood_featured_open")
            .performClick()
        assertEquals(
            listOf(
                AppDestination.Feed,
                AppDestination.ArticleDetail(expectedFeatured.stableId),
            ),
            navigator.backStack.value,
        )

        navigator.navigateUp()
        composeRule
            .onNodeWithTag("good_mood_list")
            .performScrollToNode(hasTestTag("good_mood_result_${expectedRemaining.stableId.value}"))
        composeRule.onNodeWithText("More hopeful stories").assertIsDisplayed()
        composeRule
            .onNodeWithTag(
                "good_mood_result_thumbnail_${expectedRemaining.stableId.value}",
                useUnmergedTree = true,
            )
            .assertExists()
        composeRule
            .onNodeWithTag(
                "good_mood_result_source_${expectedRemaining.stableId.value}",
                useUnmergedTree = true,
            )
            .assertExists()
        composeRule
            .onNodeWithTag("good_mood_result_${expectedRemaining.stableId.value}")
            .performClick()
        assertEquals(
            AppDestination.ArticleDetail(expectedRemaining.stableId),
            navigator.backStack.value.last(),
        )
    }

    @Test
    fun saveUpdatesPersistentPresentationAndPerformsOptionalHapticOnly() {
        val featured = article("hopeful")
        val savedStoryIds = mutableStateOf<Set<StoryId>>(emptySet())
        val toggled = mutableListOf<Article>()
        var hapticCount = 0
        setScreen(
            articles = moodArticles(),
            savedStoryIds = savedStoryIds,
            hapticsEnabled = true,
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
            .onNodeWithTag("good_mood_list")
            .performScrollToNode(hasTestTag("good_mood_featured_${featured.stableId.value}"))
        composeRule
            .onNodeWithTag("good_mood_save_${featured.stableId.value}")
            .performClick()

        composeRule.onNodeWithContentDescription("Remove saved story").assertIsDisplayed()
        assertEquals(listOf(featured), toggled)
        assertEquals(1, hapticCount)

        var disabledCalls = 0
        assertFalse(
            performGoodMoodSaveHaptic(enabled = false) {
                disabledCalls += 1
                true
            },
        )
        assertEquals(0, disabledCalls)
        assertFalse(performGoodMoodSaveHaptic(enabled = true) { false })
        assertFalse(performGoodMoodSaveHaptic(enabled = true) { error("No vibrator") })
        assertTrue(performGoodMoodSaveHaptic(enabled = true) { true })
    }

    @Test
    fun emptyInputShowsTheIosGuidanceAndScreenshot() {
        setScreen(articles = emptyList())

        composeRule.onNodeWithTag("good_mood_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No mood matches yet").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Load a few stories on the home screen, then come back " +
                    "for a personalized pick.",
            ).assertIsDisplayed()
        GoodMood.entries.forEach { mood ->
            composeRule
                .onNodeWithTag("good_mood_choice_${mood.id}")
                .assertExists()
        }
        composeRule
            .onAllNodesWithTag("good_mood_featured_label")
            .assertCountEquals(0)
        assertTrue(sampledColorCount(captureLargestWindow()) >= 5)
    }

    @Test
    fun closeControlDismissesGoodMood() {
        var closeCount = 0
        setScreen(
            articles = emptyList(),
            onClose = { closeCount += 1 },
        )

        composeRule.onNodeWithText("Good Mood").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close Good Mood").performClick()

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
                GoodMoodScreen(
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

private fun moodArticles(): List<Article> =
    listOf(
        article("calm"),
        article("hopeful"),
        article("inspired"),
        article("curious"),
        article("hopeful-more"),
    )

private fun article(id: String): Article =
    when (id) {
        "calm" ->
            moodArticle(
                id = id,
                title = "A peaceful garden walk brings gentle healing",
                summary = "Nature and a quiet path offer a calm place to rest.",
                categories = listOf("Wellness", "Nature"),
            )

        "hopeful" ->
            moodArticle(
                id = id,
                title = "Kind neighbors help a rescue team reunite a family",
                summary = "Community volunteers bring hope and support during recovery.",
                categories = listOf("Community", "Uplifting"),
            )

        "hopeful-more" ->
            moodArticle(
                id = id,
                title = "Volunteers donate supplies to help neighbors recover",
                summary = "A hopeful act of kindness lifts the whole community.",
                categories = listOf("Human-interest"),
            )

        "inspired" ->
            moodArticle(
                id = id,
                title = "Student wins an award after a record achievement",
                summary = "A teacher helps the young artist reach a lifelong dream.",
                categories = listOf("Achievement", "Education"),
            )

        "curious" ->
            moodArticle(
                id = id,
                title = "Rare science discovery reveals new ocean life",
                summary = "Researchers explore animal history and a curious natural mystery.",
                categories = listOf("Science", "Animals"),
            )

        else -> error("Unknown mood article $id")
    }

private fun moodArticle(
    id: String,
    title: String,
    summary: String,
    categories: List<String>,
): Article =
    Article(
        id = id,
        title = title,
        summary = summary,
        originalUrl = URI("https://example.com/$id"),
        source = "Good News Daily",
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = URI("https://example.com/$id.jpg"),
        categories = categories,
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
