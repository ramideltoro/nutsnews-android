package com.nutsnews.app.feature.feed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsDimensions
import com.nutsnews.app.designsystem.NutsNewsMotion
import com.nutsnews.app.designsystem.NutsNewsTheme
import java.net.URI
import kotlin.math.abs
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
@Config(sdk = [35], qualifiers = "w1024dp-h768dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArticleCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun readStoryButtonUsesTheStandardControlShapeInsteadOfCapsuleGeometry() {
        val outline =
            readStoryButtonShape(NutsNewsDimensions())
                .createOutline(
                    size = Size(width = 120f, height = 48f),
                    layoutDirection = LayoutDirection.Ltr,
                    density = Density(1f),
                )
        val roundedOutline = outline as Outline.Rounded
        val cornerRadius = roundedOutline.roundRect.topLeftCornerRadius.x

        assertEquals(16f, cornerRadius)
        assertTrue(cornerRadius < roundedOutline.roundRect.height / 2f)
    }

    @Test
    fun readStoryButtonIsWiderAndCentersItsLabel() {
        setCard(
            article = representativeArticle(),
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
        )

        val buttonBounds = bounds("article_read_story")
        val labelBounds =
            composeRule
                .onNodeWithText("Read Story", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot

        assertNear(expected = ReadStoryButtonWidth.value, actual = buttonBounds.width.toDp())
        assertNear(expected = 48, actual = buttonBounds.height.toDp())
        assertNear(expected = buttonBounds.center.x, actual = labelBounds.center.x)
        assertNear(expected = buttonBounds.center.y, actual = labelBounds.center.y)
    }

    @Test
    fun regularGoldenScreenshotMatchesIosReferenceCompositionAndControls() {
        val article = representativeArticle()
        val readStories = mutableListOf<Article>()
        val likedStories = mutableListOf<Article>()
        setCard(
            article = article,
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
            isLiked = true,
            onReadStory = readStories::add,
            onLikeStory = likedStories::add,
        )

        composeRule.onNodeWithTag("reference_card").assertIsDisplayed()
        composeRule.onNodeWithText(article.title).assertIsDisplayed()
        composeRule.onNodeWithText(article.summary).assertIsDisplayed()
        composeRule.onNodeWithText(article.source).assertIsDisplayed()
        composeRule.onNodeWithText(article.displayDate).assertIsDisplayed()
        composeRule.onNodeWithText("Read Story").performClick()
        composeRule
            .onNodeWithTag("article_like_story")
            .assertContentDescriptionEquals("Liked")
            .performClick()
        assertEquals(listOf(article), readStories)
        assertEquals(listOf(article), likedStories)

        repeat(IosCardGolden.MaximumCategoryBadges) { index ->
            composeRule
                .onNodeWithTag("article_category_$index", useUnmergedTree = true)
                .fetchSemanticsNode()
        }
        composeRule
            .onAllNodesWithTag("article_category_6")
            .assertCountEquals(0)

        val cardBounds = bounds("reference_card")
        val imageBounds = bounds("article_thumbnail")
        assertNear(
            expected = IosCardGolden.RegularCardWidthDp,
            actual = cardBounds.width.toDp(),
        )
        assertNear(
            expected = IosCardGolden.CardPaddingRegularDp,
            actual = (imageBounds.left - cardBounds.left).toDp(),
        )
        assertNear(
            expected = IosCardGolden.ThumbnailAspectRatio,
            actual = imageBounds.width / imageBounds.height,
            tolerance = 0.02f,
        )

        val screenshot = captureLargestWindow()
        assertTrue(sampledColorCount(screenshot) >= IosCardGolden.MinimumGoldenColors)
    }

    @Test
    fun compactGoldenScreenshotMatchesIosReferenceGeometryAndLineLimits() {
        val article =
            representativeArticle().copy(
                title =
                    "A long uplifting headline demonstrates the compact tablet landscape " +
                        "card’s three-line title limit without changing its hierarchy",
                summary =
                    "This intentionally long summary verifies that the compact presentation " +
                        "uses four lines at most while retaining the source, date, category " +
                        "badges, read control, and like control beside the fixed-width image.",
            )
        setCard(
            article = article,
            layout = ArticleCardLayout.TabletLandscapeCompact,
            widthDp = IosCardGolden.CompactCardWidthDp,
        )

        val cardBounds = bounds("reference_card")
        val imageBounds = bounds("article_thumbnail")
        val titleBounds = bounds("article_title")
        val summaryBounds = bounds("article_summary")
        assertNear(
            expected = IosCardGolden.CompactCardWidthDp,
            actual = cardBounds.width.toDp(),
        )
        assertNear(
            expected = IosCardGolden.CompactImageWidthDp,
            actual = imageBounds.width.toDp(),
        )
        assertNear(
            expected = IosCardGolden.ThumbnailAspectRatio,
            actual = imageBounds.width / imageBounds.height,
            tolerance = 0.02f,
        )
        assertNear(
            expected = IosCardGolden.CardPaddingCompactDp,
            actual = (imageBounds.left - cardBounds.left).toDp(),
        )
        assertTrue(titleBounds.height.toDp() <= IosCardGolden.CompactTitleMaximumHeightDp)
        assertTrue(summaryBounds.height.toDp() <= IosCardGolden.CompactSummaryMaximumHeightDp)
        assertTrue(titleBounds.left > imageBounds.right)

        val screenshot = captureLargestWindow()
        assertTrue(sampledColorCount(screenshot) >= IosCardGolden.MinimumGoldenColors)
    }

    @Test
    fun missingThumbnailAndEmptySummaryUseIosFallbackWithoutBlankSpace() {
        val article =
            representativeArticle().copy(
                summary = "",
                thumbnailUrl = null,
                categories = emptyList(),
            )
        setCard(
            article = article,
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
        )

        composeRule.onNodeWithTag("article_thumbnail").assertIsDisplayed()
        composeRule.onNodeWithText("NutsNews").assertIsDisplayed()
        composeRule.onAllNodesWithTag("article_summary").assertCountEquals(0)
        composeRule.onAllNodesWithTag("article_categories").assertCountEquals(0)
    }

    @Test
    fun likeAndUnlikeUpdateHeartGlowCardGlowAndCelebrationFeedback() {
        composeRule.mainClock.autoAdvance = false
        val article = representativeArticle()
        setCard(
            article = article,
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
        )

        composeRule.onNodeWithTag("article_like_story").performClick()
        composeRule.mainClock.advanceTimeBy(200)

        composeRule
            .onNodeWithTag("article_like_story")
            .assertContentDescriptionEquals("Liked")
        composeRule
            .onNodeWithTag("article_heart_glow", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeRule
            .onNodeWithTag("article_card_glow", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeRule
            .onNodeWithTag("article_celebration", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeRule
            .onAllNodesWithTag("article_celebration_particle_0", useUnmergedTree = true)
            .assertCountEquals(1)

        composeRule.onNodeWithTag("article_like_story").performClick()
        composeRule.mainClock.advanceTimeBy(32)

        composeRule
            .onNodeWithTag("article_like_story")
            .assertContentDescriptionEquals("Like story")
        composeRule
            .onAllNodesWithTag("article_celebration", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun readStoryWaitsForTheIosActionGlowBeforeOpening() {
        composeRule.mainClock.autoAdvance = false
        val article = representativeArticle()
        val opened = mutableListOf<Article>()
        setCard(
            article = article,
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
            onReadStory = opened::add,
        )

        composeRule.onNodeWithTag("article_read_story").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertTrue(opened.isEmpty())
        composeRule.mainClock.advanceTimeBy(
            NutsNewsMotion.ActionOpenDelayMillis - 32L,
        )
        assertTrue(opened.isEmpty())
        composeRule.mainClock.advanceTimeBy(32L)
        composeRule.runOnIdle {
            assertEquals(listOf(article), opened)
        }
    }

    @Test
    fun aNewLikeCelebrationIsNotClearedByAnOlderInterruptedAnimation() {
        composeRule.mainClock.autoAdvance = false
        setCard(
            article = representativeArticle(),
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
        )

        composeRule.onNodeWithTag("article_like_story").performClick()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.onNodeWithTag("article_like_story").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("article_like_story").performClick()
        composeRule.mainClock.advanceTimeBy(1_600)

        composeRule
            .onNodeWithTag("article_celebration", useUnmergedTree = true)
            .fetchSemanticsNode()

        composeRule.mainClock.advanceTimeBy(600)
        composeRule
            .onAllNodesWithTag("article_celebration", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun disabledOrUnavailableHapticsNeverBlockLikeInteraction() {
        var disabledHapticCalls = 0
        composeRule.mainClock.autoAdvance = false
        setCard(
            article = representativeArticle(),
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
            hapticsEnabled = false,
            onLikeHaptic = {
                disabledHapticCalls += 1
                error("Disabled haptics must not invoke the performer")
            },
        )

        composeRule.onNodeWithTag("article_like_story").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        composeRule
            .onNodeWithTag("article_like_story")
            .assertContentDescriptionEquals("Liked")
        assertEquals(0, disabledHapticCalls)
        assertFalse(performLikeHapticIfEnabled(enabled = true) { false })
        assertFalse(performLikeHapticIfEnabled(enabled = true) { error("No vibrator") })
    }

    @Test
    fun customStoryControlsMeetTouchTargetsAndWorkFromAKeyboard() {
        val article = representativeArticle()
        val readStories = mutableListOf<Article>()
        val likedStories = mutableListOf<Article>()
        setCard(
            article = article,
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
            onReadStory = readStories::add,
            onLikeStory = likedStories::add,
            keyboardInputMode = true,
        )

        composeRule
            .onNodeWithTag("article_read_story")
            .assertHeightIsAtLeast(48.dp)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle { assertEquals(listOf(article), readStories) }

        composeRule
            .onNodeWithTag("article_like_story")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Liked",
                ),
            )
        composeRule.runOnIdle { assertEquals(listOf(article), likedStories) }
    }

    @Test
    fun reducedMotionKeepsLikedStateFeedbackWithoutCelebrationMotion() {
        setCard(
            article = representativeArticle(),
            layout = ArticleCardLayout.Regular,
            widthDp = IosCardGolden.RegularCardWidthDp,
            reducedMotionOverride = true,
        )

        composeRule.onNodeWithTag("article_like_story").performClick()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag("article_like_story")
            .assertContentDescriptionEquals("Liked")
        composeRule
            .onAllNodesWithTag("article_celebration", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    private fun setCard(
        article: Article,
        layout: ArticleCardLayout,
        widthDp: Int,
        isLiked: Boolean = false,
        onReadStory: (Article) -> Unit = {},
        onLikeStory: (Article) -> Unit = {},
        hapticsEnabled: Boolean = true,
        onLikeHaptic: () -> Boolean = { false },
        reducedMotionOverride: Boolean? = null,
        keyboardInputMode: Boolean = false,
    ) {
        composeRule.setContent {
            val inputModeManager = LocalInputModeManager.current
            LaunchedEffect(keyboardInputMode) {
                if (keyboardInputMode) {
                    inputModeManager.requestInputMode(InputMode.Keyboard)
                }
            }
            NutsNewsTheme(
                updateSystemBars = false,
                reducedMotionOverride = reducedMotionOverride,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    ArticleCard(
                        article = article,
                        layout = layout,
                        isLiked = isLiked,
                        onReadStory = onReadStory,
                        onLikeStory = onLikeStory,
                        hapticsEnabled = hapticsEnabled,
                        onLikeHaptic = onLikeHaptic,
                        modifier =
                            Modifier
                                .width(widthDp.dp)
                                .testTag("reference_card"),
                    )
                }
            }
        }
    }

    private fun bounds(tag: String): Rect =
        composeRule
            .onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

    private fun Float.toDp(): Float =
        this / composeRule.activity.resources.displayMetrics.density

    private fun assertNear(
        expected: Number,
        actual: Float,
        tolerance: Float = 1.1f,
    ) {
        assertTrue(
            abs(expected.toFloat() - actual) <= tolerance,
            "Expected $expected ± $tolerance but was $actual",
        )
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
                }.maxBy { image -> image.width * image.height }
        }
}

private object IosCardGolden {
    const val RegularCardWidthDp = 358
    const val CompactCardWidthDp = 860
    const val CardPaddingRegularDp = 16
    const val CardPaddingCompactDp = 12
    const val CompactImageWidthDp = 286
    const val ThumbnailAspectRatio = 1.5f
    const val CompactTitleMaximumHeightDp = 72f
    const val CompactSummaryMaximumHeightDp = 80f
    const val MaximumCategoryBadges = 6
    const val MinimumGoldenColors = 7
}

private fun representativeArticle(): Article =
    Article(
        id = "reference",
        title = "Europe removed a record number of river barriers last year",
        summary =
            "A remarkable environmental milestone is helping restore natural waterways " +
                "and support healthier ecosystems.",
        originalUrl = URI("https://example.com/reference"),
        source = "The Optimist Daily",
        publishedAt = "2024-01-02T12:00:00Z",
        createdAt = null,
        thumbnailUrl = null,
        categories =
            listOf(
                "Nature",
                "Uplifting",
                "Science",
                "Community",
                "Wellness",
                "Achievements",
                "Hidden seventh",
            ),
    )

private fun sampledColorCount(image: Bitmap): Int {
    val xStep = max(1, image.width / 40)
    val yStep = max(1, image.height / 40)
    return buildSet {
        for (y in 0 until image.height step yStep) {
            for (x in 0 until image.width step xStep) {
                add(image.getPixel(x, y))
            }
        }
    }.size
}
