package com.nutsnews.app.feature.article

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.nutsnews.app.core.model.Article
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

abstract class ArticleDetailScreenshotContract(
    private val compactExpected: Boolean,
) {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wideThumbnailUsesThreeTwoCropInScreenshot() {
        setDetail(
            article = representativeArticle(),
            imageModel = testBitmap(width = 900, height = 400, color = Color.rgb(31, 125, 190)),
        )
        waitForLoadedHero()

        val heroBounds = bounds("article_detail_hero")
        assertNear(
            expected = DetailGolden.WideAspectRatio,
            actual = heroBounds.width / heroBounds.height,
            tolerance = 0.03f,
        )
        assertExpectedLayout()
        assertTrue(sampledColorCount(captureLargestWindow()) >= DetailGolden.MinimumGoldenColors)
    }

    @Test
    fun tallThumbnailKeepsDefaultHeroHeightInScreenshot() {
        setDetail(
            article = representativeArticle(),
            imageModel = testBitmap(width = 400, height = 900, color = Color.rgb(63, 145, 88)),
        )
        waitForLoadedHero()

        assertNear(
            expected = DetailGolden.DefaultHeroHeightDp,
            actual = bounds("article_detail_hero").height.toDp(),
        )
        assertExpectedLayout()
        assertTrue(sampledColorCount(captureLargestWindow()) >= DetailGolden.MinimumGoldenColors)
    }

    @Test
    fun missingThumbnailShowsFallbackHierarchyAndNavigationInScreenshot() {
        var closeCount = 0
        val article =
            representativeArticle().copy(
                thumbnailUrl = null,
                categories =
                    listOf(
                        "Nature",
                        "Uplifting",
                        "Science",
                        "Community",
                        "Wellness",
                        "Achievements",
                        "Animals",
                        "Innovation",
                        "Hidden ninth",
                    ),
            )
        setDetail(
            article = article,
            imageModel = null,
            onClose = { closeCount += 1 },
        )

        composeRule.onNodeWithText("Story").assertIsDisplayed()
        composeRule.onNodeWithText(article.title).assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_hero_fallback", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeRule
            .onNodeWithTag("article_detail_close")
            .assertContentDescriptionEquals("Close story")
            .performClick()
        composeRule.runOnIdle { assertEquals(1, closeCount) }
        repeat(DetailGolden.MaximumVisibleCategories) { index ->
            composeRule
                .onNodeWithTag("article_detail_category_$index", useUnmergedTree = true)
                .fetchSemanticsNode()
        }
        composeRule
            .onAllNodesWithTag(
                "article_detail_category_${DetailGolden.MaximumVisibleCategories}",
                useUnmergedTree = true,
            ).assertCountEquals(0)
        assertNear(
            expected = DetailGolden.DefaultHeroHeightDp,
            actual = bounds("article_detail_hero").height.toDp(),
        )
        assertExpectedLayout()
        assertTrue(sampledColorCount(captureLargestWindow()) >= DetailGolden.MinimumGoldenColors)
    }

    @Test
    fun failedThumbnailReturnsToFallbackWithoutChangingGeometryInScreenshot() {
        setDetail(
            article = representativeArticle(),
            imageModel = UnsupportedImageModel,
        )
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(
                    "article_detail_hero_loading",
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isEmpty()
        }

        composeRule
            .onNodeWithTag("article_detail_hero_fallback", useUnmergedTree = true)
            .fetchSemanticsNode()
        assertNear(
            expected = DetailGolden.DefaultHeroHeightDp,
            actual = bounds("article_detail_hero").height.toDp(),
        )
        assertExpectedLayout()
        assertTrue(sampledColorCount(captureLargestWindow()) >= DetailGolden.MinimumGoldenColors)
    }

    protected fun setDetail(
        article: Article,
        imageModel: Any?,
        onClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                ArticleDetailScreen(
                    article = article,
                    onClose = onClose,
                    heroImageModel = imageModel,
                )
            }
        }
    }

    private fun waitForLoadedHero() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(
                    "article_detail_hero_fallback",
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun assertExpectedLayout() {
        val expectedTag =
            if (compactExpected) {
                "article_detail_compact_content"
            } else {
                "article_detail_regular_content"
            }
        val unexpectedTag =
            if (compactExpected) {
                "article_detail_regular_content"
            } else {
                "article_detail_compact_content"
            }
        composeRule.onNodeWithTag(expectedTag).fetchSemanticsNode()
        composeRule.onAllNodesWithTag(unexpectedTag).assertCountEquals(0)

        if (compactExpected) {
            val hero = bounds("article_detail_hero")
            val title = bounds("article_detail_title")
            assertTrue(title.left > hero.right)
            assertTrue(hero.width.toDp() <= DetailGolden.TabletImageMaximumWidthDp)
        }
    }

    protected fun bounds(tag: String): Rect =
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-port")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneArticleDetailScreenTest : ArticleDetailScreenshotContract(compactExpected = false) {
    @Test
    fun phoneLayoutScrollsToKeepAnUnboundedTitleReachable() {
        val article =
            representativeArticle().copy(
                title =
                    List(12) {
                        "A long uplifting headline remains readable from beginning to end."
                    }.joinToString(" "),
                thumbnailUrl = null,
            )
        setDetail(
            article = article,
            imageModel = null,
        )
        val originalTitleTop = bounds("article_detail_title").top

        composeRule
            .onNodeWithTag("article_detail_regular_content")
            .performTouchInput { swipeUp() }

        assertTrue(bounds("article_detail_title").top < originalTitleTop)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1024dp-h768dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletArticleDetailScreenTest : ArticleDetailScreenshotContract(compactExpected = true)

class ArticleDetailHeroCropTest {
    @Test
    fun onlyImagesWiderThanThreeTwoUseTheWideCrop() {
        assertFalse(shouldUseWideDetailHeroCrop(pixelWidth = 0, pixelHeight = 400))
        assertFalse(shouldUseWideDetailHeroCrop(pixelWidth = 1_500, pixelHeight = 1_000))
        assertTrue(shouldUseWideDetailHeroCrop(pixelWidth = 1_501, pixelHeight = 1_000))
    }
}

private object DetailGolden {
    const val WideAspectRatio = 1.5f
    const val DefaultHeroHeightDp = 210
    const val TabletImageMaximumWidthDp = 440
    const val MaximumVisibleCategories = 8
    const val MinimumGoldenColors = 6
}

private object UnsupportedImageModel

private fun testBitmap(
    width: Int,
    height: Int,
    color: Int,
): Bitmap =
    Bitmap
        .createBitmap(width, height, Bitmap.Config.ARGB_8888)
        .apply { eraseColor(color) }

private fun representativeArticle(): Article =
    Article(
        id = "detail-reference",
        title =
            "A very long positive news headline remains fully visible on the native " +
                "article detail screen without clipping on smaller phones",
        summary =
            "A remarkable environmental milestone is helping restore natural waterways.",
        originalUrl = URI("https://example.com/detail-reference"),
        source = "The Optimist Daily",
        publishedAt = "2024-01-02T12:00:00Z",
        createdAt = null,
        thumbnailUrl = URI("https://example.com/detail-reference.jpg"),
        categories = listOf("Nature", "Uplifting"),
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
