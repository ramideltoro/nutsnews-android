package com.nutsnews.app.feature.article

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Insets
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryReflection
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.designsystem.NutsNewsAdaptiveWindow
import com.nutsnews.app.designsystem.NutsNewsMotion
import com.nutsnews.app.designsystem.NutsNewsTheme
import java.net.URI
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
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
        composeRule
            .onNodeWithTag("article_detail_title")
            .assertTextEquals(article.title)
            .assertIsDisplayed()
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
        isLiked: Boolean = false,
        onToggleLiked: (Article) -> Unit = {},
        onArticleShown: (Article) -> Unit = {},
        onOpenOriginalStory: (Article, OriginalStoryColors) -> OriginalStoryOpenResult =
            { _, _ -> OriginalStoryOpenResult.OpenedCustomTab },
        onOriginalStoryOpened: () -> Unit = {},
        noteDraft: String = "",
        hasSavedNote: Boolean = false,
        isNoteLoading: Boolean = false,
        noteStatusMessage: String? = null,
        onNoteDraftChanged: (String) -> Unit = {},
        onSaveNote: () -> Unit = {},
        onClearNote: () -> Unit = {},
        reflection: StoryReflection? = null,
        isReflectionLoading: Boolean = false,
        reflectionStatusMessage: String? = null,
        onReflectionSelected: (StoryReflectionReaction) -> Unit = {},
        listenUiState: () -> ArticleListenUiState = { ArticleListenUiState() },
        onToggleListening: (ArticleListenScript) -> Unit = {},
        onStopListening: () -> Unit = {},
        shareCardUiState: () -> ArticleShareCardUiState = { ArticleShareCardUiState() },
        onShareCard: (Article) -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsAdaptiveWindow {
                NutsNewsTheme(updateSystemBars = false) {
                    ArticleDetailScreen(
                        article = article,
                        onClose = onClose,
                        heroImageModel = imageModel,
                        isLiked = isLiked,
                        onToggleLiked = onToggleLiked,
                        onArticleShown = onArticleShown,
                        onOpenOriginalStory = onOpenOriginalStory,
                        onOriginalStoryOpened = onOriginalStoryOpened,
                        noteDraft = noteDraft,
                        hasSavedNote = hasSavedNote,
                        isNoteLoading = isNoteLoading,
                        noteStatusMessage = noteStatusMessage,
                        onNoteDraftChanged = onNoteDraftChanged,
                        onSaveNote = onSaveNote,
                        onClearNote = onClearNote,
                        reflection = reflection,
                        isReflectionLoading = isReflectionLoading,
                        reflectionStatusMessage = reflectionStatusMessage,
                        onReflectionSelected = onReflectionSelected,
                        listenUiState = listenUiState(),
                        onToggleListening = onToggleListening,
                        onStopListening = onStopListening,
                        shareCardUiState = shareCardUiState(),
                        onShareCard = onShareCard,
                    )
                }
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
            composeRule
                .onNodeWithTag("article_detail_open_original")
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    protected fun bounds(tag: String): Rect =
        composeRule
            .onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

    protected fun applyNavigationBarInset(bottomInsetDp: Int) {
        val insetPixels =
            (bottomInsetDp * composeRule.activity.resources.displayMetrics.density).roundToInt()
        composeRule.runOnUiThread {
            val navigationBars = WindowInsets.Type.navigationBars()
            composeRule.activity.window.decorView.dispatchApplyWindowInsets(
                WindowInsets
                    .Builder()
                    .setInsets(
                        navigationBars,
                        Insets.of(0, 0, 0, insetPixels),
                    ).setVisible(navigationBars, true)
                    .build(),
            )
        }
        composeRule.waitForIdle()
    }

    protected fun assertAboveNavigationBar(
        tag: String,
        bottomInsetDp: Int,
    ) {
        val insetPixels =
            bottomInsetDp * composeRule.activity.resources.displayMetrics.density
        val screenBottom = bounds("article_detail").bottom
        val contentBottom = bounds(tag).bottom
        assertTrue(
            contentBottom <= screenBottom - insetPixels + 1f,
            "$tag ended at $contentBottom; expected it above ${screenBottom - insetPixels}",
        )
    }

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

    protected fun captureLargestWindow(): Bitmap =
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
    fun regularContentEndsAboveTheNavigationBarInset() {
        val bottomInsetDp = 48
        setDetail(
            article = contentArticle(),
            imageModel = null,
        )
        applyNavigationBarInset(bottomInsetDp)

        composeRule
            .onNodeWithTag("article_detail_open_original")
            .performScrollTo()
            .assertIsDisplayed()

        assertAboveNavigationBar("article_detail_open_original", bottomInsetDp)
    }

    @Test
    fun detailToolbarLikeUpdatesImmediatelyAndAppearanceRecordsOnce() {
        val article = contentArticle()
        val toggled = mutableListOf<Article>()
        val shown = mutableListOf<Article>()
        composeRule.mainClock.autoAdvance = false
        setDetail(
            article = article,
            imageModel = null,
            onToggleLiked = toggled::add,
            onArticleShown = shown::add,
        )
        composeRule.mainClock.advanceTimeByFrame()

        composeRule
            .onNodeWithTag("article_detail_like")
            .assertContentDescriptionEquals("Like story")
            .performClick()
        composeRule.mainClock.advanceTimeByFrame()

        composeRule
            .onNodeWithTag("article_detail_like")
            .assertContentDescriptionEquals("Liked")
        composeRule
            .onNodeWithTag("article_detail_like_surface", useUnmergedTree = true)
            .fetchSemanticsNode()
        assertEquals(listOf(article), toggled)
        assertEquals(listOf(article), shown)
    }

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

    @Test
    fun phoneBriefSummaryAndSourceMatchTheIosHierarchyInScreenshot() {
        val article = contentArticle()
        setDetail(
            article = article,
            imageModel = null,
        )

        composeRule.onNodeWithTag("article_detail_brief").performScrollTo()
        composeRule.onNodeWithText("NUTSNEWS BRIEF").assertIsDisplayed()
        composeRule.onNodeWithText("1 min native brief").assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_brief_mood")
            .assertIsDisplayed()
        composeRule.onNodeWithText("What happened").fetchSemanticsNode()
        composeRule.onNodeWithText("Why it’s good news").fetchSemanticsNode()
        composeRule.onNodeWithText("Feel-good takeaway").fetchSemanticsNode()
        assertTrue(sampledColorCount(captureLargestWindow()) >= DetailGolden.MinimumGoldenColors)

        composeRule.onNodeWithTag("article_detail_source").performScrollTo()
        composeRule.onNodeWithTag("article_detail_summary").fetchSemanticsNode()
        composeRule.onAllNodesWithText(article.summary).assertCountEquals(2)
        composeRule
            .onNodeWithTag("article_detail_source_name")
            .assertTextEquals(article.source)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_source_date")
            .assertTextEquals("Published today")
            .assertIsDisplayed()
        assertTrue(sampledColorCount(captureLargestWindow()) >= DetailGolden.MinimumGoldenColors)
    }

    @Test
    fun missingSummaryUsesTitleFallbackAndOmitsSummaryCardInScreenshot() {
        val article =
            contentArticle().copy(
                title = "A title-only positive update",
                summary = "",
                source = "",
                publishedAt = null,
            )
        setDetail(
            article = article,
            imageModel = null,
        )

        composeRule.onAllNodesWithTag("article_detail_summary").assertCountEquals(0)
        composeRule.onAllNodesWithText(article.title).assertCountEquals(3)
        composeRule.onNodeWithTag("article_detail_source").performScrollTo()
        composeRule.onNodeWithText("SOURCE").assertIsDisplayed()
        composeRule.onNodeWithText("Recently").assertIsDisplayed()
        assertTrue(sampledColorCount(captureLargestWindow()) >= DetailGolden.MinimumGoldenColors)
    }

    @Test
    fun validOriginalStoryUsesThemeColorsAndRecordsTheOpen() {
        val article = contentArticle()
        val launched = mutableListOf<Pair<Article, OriginalStoryColors>>()
        var recordedCount = 0
        setDetail(
            article = article,
            imageModel = null,
            onOpenOriginalStory = { selectedArticle, colors ->
                launched += selectedArticle to colors
                OriginalStoryOpenResult.OpenedCustomTab
            },
            onOriginalStoryOpened = { recordedCount += 1 },
        )

        composeRule
            .onNodeWithTag("article_detail_open_original")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, launched.size)
            assertEquals(article, launched.single().first)
            assertTrue(launched.single().second.toolbar != 0)
            assertTrue(launched.single().second.navigationBar != 0)
            assertEquals(1, recordedCount)
        }
        composeRule.onAllNodesWithTag("article_detail_browser_status").assertCountEquals(0)
    }

    @Test
    fun invalidOriginalStoryShowsFailureWithoutRecordingAnOpen() {
        val article =
            contentArticle().copy(
                originalUrl = URI("ftp://example.com/not-a-web-story"),
            )
        var recordedCount = 0
        setDetail(
            article = article,
            imageModel = null,
            onOpenOriginalStory = { _, _ -> OriginalStoryOpenResult.InvalidUrl },
            onOriginalStoryOpened = { recordedCount += 1 },
        )

        composeRule
            .onNodeWithTag("article_detail_open_original")
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithText("This story does not have a valid web address.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, recordedCount) }
    }

    @Test
    fun absentOriginalStoryIsDisabledAndExplainsWhy() {
        val article = contentArticle().copy(originalUrl = null)
        var launchCount = 0
        setDetail(
            article = article,
            imageModel = null,
            onOpenOriginalStory = { _, _ ->
                launchCount += 1
                OriginalStoryOpenResult.OpenedCustomTab
            },
        )

        composeRule
            .onNodeWithTag("article_detail_browser_status")
            .performScrollTo()
        composeRule
            .onNodeWithText("Original story link unavailable.")
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, launchCount) }
    }

    @Test
    fun noteUiSavesReopensEditsClearsAndSharesTheStableRouteDraft() {
        val feedArticle = contentArticle()
        val routeArticle = mutableStateOf(feedArticle)
        val routeEntry = mutableIntStateOf(0)
        val persistedNote = mutableStateOf("Existing private note")
        val draft = mutableStateOf(persistedNote.value)
        val hasSavedNote = mutableStateOf(true)
        val status = mutableStateOf<String?>(null)
        val savedDrafts = mutableListOf<String>()
        var clearCount = 0

        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                key(routeEntry.intValue) {
                    ArticleDetailScreen(
                        article = routeArticle.value,
                        onClose = {},
                        heroImageModel = null,
                        noteDraft = draft.value,
                        hasSavedNote = hasSavedNote.value,
                        noteStatusMessage = status.value,
                        onNoteDraftChanged = { draft.value = it },
                        onSaveNote = {
                            persistedNote.value = draft.value.trim()
                            draft.value = persistedNote.value
                            hasSavedNote.value = persistedNote.value.isNotEmpty()
                            savedDrafts += persistedNote.value
                            status.value =
                                if (persistedNote.value.isEmpty()) {
                                    "Note cleared"
                                } else {
                                    "Note saved on this device"
                                }
                        },
                        onClearNote = {
                            persistedNote.value = ""
                            draft.value = ""
                            hasSavedNote.value = false
                            clearCount += 1
                            status.value = "Note cleared"
                        },
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("article_detail_note_editor")
            .performScrollTo()
            .assertTextEquals("Existing private note")
            .performClick()
            .assertIsFocused()
            .performTextReplacement("  Edited from the feed  ")
        composeRule
            .onNodeWithTag("article_detail_note_save")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("article_detail_note_editor")
            .assertIsNotFocused()
            .assertTextEquals("Edited from the feed")
        composeRule
            .onNodeWithText("Note saved on this device")
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals(listOf("Edited from the feed"), savedDrafts)

        composeRule.runOnIdle {
            routeArticle.value =
                feedArticle.copy(
                    id = "search-route-id",
                    title = "The same article reopened from search",
                )
            routeEntry.intValue += 1
            draft.value = persistedNote.value
            status.value = null
        }
        composeRule
            .onNodeWithTag("article_detail_note_editor")
            .performScrollTo()
            .assertTextEquals("Edited from the feed")
            .performTextReplacement("Edited after reopening")
        composeRule
            .onNodeWithTag("article_detail_note_clear")
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithTag("article_detail_note_editor")
            .assertTextEquals("")
        composeRule
            .onNodeWithText("Note cleared")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_note_clear")
            .assertIsNotEnabled()
        assertEquals(1, clearCount)
        assertEquals(feedArticle.stableId, routeArticle.value.stableId)
    }

    @Test
    fun reflectionUiSelectsReplacesAndShowsTheDatedConfirmation() {
        val article = contentArticle()
        val selected = mutableStateOf<StoryReflection?>(null)
        val status = mutableStateOf<String?>(null)
        val selections = mutableListOf<StoryReflectionReaction>()
        val createdAt = Instant.parse("2026-07-26T12:00:00Z")
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                ArticleDetailScreen(
                    article = article,
                    onClose = {},
                    heroImageModel = null,
                    reflection = selected.value,
                    reflectionStatusMessage = status.value,
                    onReflectionSelected = { reaction ->
                        selections += reaction
                        selected.value =
                            StoryReflection(
                                articleId = article.stableId,
                                articleTitle = article.title,
                                articleSource = article.source,
                                reaction = reaction,
                                createdAt = createdAt,
                            )
                        status.value = "Saved: ${reaction.title}"
                    },
                )
            }
        }

        composeRule
            .onNodeWithTag("article_detail_reflection")
            .performScrollTo()
        composeRule.onNodeWithText("DAILY REFLECTION").assertIsDisplayed()
        composeRule.onNodeWithText("How did this story land?").assertIsDisplayed()
        composeRule.onNodeWithText("Made me smile").assertIsDisplayed()
        composeRule.onNodeWithText("Gave me hope").assertIsDisplayed()
        composeRule.onNodeWithText("Worth revisiting").assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_reflection_smile")
            .assertIsNotSelected()
            .performClick()

        composeRule
            .onNodeWithTag("article_detail_reflection_smile")
            .assertIsSelected()
        composeRule
            .onNodeWithText("This one made you smile")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "You marked this story as made me smile on Jul 26, 2026.",
            ).assertIsDisplayed()
        composeRule
            .onNodeWithText("Saved: Made me smile")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("article_detail_reflection_hope")
            .performClick()
        composeRule
            .onNodeWithTag("article_detail_reflection_smile")
            .assertIsNotSelected()
        composeRule
            .onNodeWithTag("article_detail_reflection_hope")
            .assertIsSelected()
        composeRule
            .onNodeWithText("This one gave you hope")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Saved: Gave me hope")
            .assertIsDisplayed()
        assertEquals(
            listOf(
                StoryReflectionReaction.Smile,
                StoryReflectionReaction.Hope,
            ),
            selections,
        )
    }

    @Test
    fun listenModeAutoStartsHighlightsProgressAndSupportsAllControls() {
        val article = contentArticle()
        val state =
            mutableStateOf(
                ArticleListenUiState(
                    statusMessage = "Ready to listen",
                    isEngineReady = true,
                ),
            )
        val requestedScripts = mutableListOf<ArticleListenScript>()
        var stopCount = 0
        setDetail(
            article = article,
            imageModel = null,
            listenUiState = { state.value },
            onToggleListening = { script ->
                requestedScripts += script
                state.value =
                    when (state.value.playbackState) {
                        ArticleListenPlaybackState.Idle,
                        ArticleListenPlaybackState.Failed,
                        ->
                            state.value.copy(
                                playbackState = ArticleListenPlaybackState.Reading,
                                statusMessage = "Reading with on-device voice",
                                segments = script.segments,
                                currentSegmentIndex = 2,
                            )

                        ArticleListenPlaybackState.Reading ->
                            state.value.copy(
                                playbackState = ArticleListenPlaybackState.Paused,
                                statusMessage = "Paused",
                            )

                        ArticleListenPlaybackState.Paused ->
                            state.value.copy(
                                playbackState = ArticleListenPlaybackState.Reading,
                                statusMessage = "Reading with on-device voice",
                            )
                    }
            },
            onStopListening = {
                stopCount += 1
                state.value =
                    state.value.copy(
                        playbackState = ArticleListenPlaybackState.Idle,
                        statusMessage = "Stopped",
                        currentSegmentIndex = null,
                    )
            },
        )

        composeRule
            .onNodeWithTag("article_detail_listen")
            .assertContentDescriptionEquals("Listen to story brief")
            .performClick()
        composeRule.onNodeWithTag("listen_mode_sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Listen Mode").assertIsDisplayed()
        composeRule.onNodeWithText("AUDIO BRIEF").assertIsDisplayed()
        composeRule.onNodeWithText("NOW PLAYING").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            requestedScripts.size == 1
        }

        assertEquals(
            listOf(
                "Here’s your NutsNews brief.",
                article.title,
                "What happened: ${article.summary}",
            ),
            requestedScripts.single().segments.take(3).map(ArticleListenSegment::text),
        )
        composeRule
            .onNodeWithTag("listen_mode_segment_what_happened")
            .performScrollTo()
            .assertIsSelected()
        composeRule
            .onNodeWithTag("listen_mode_primary")
            .performScrollTo()
            .assertTextEquals("Pause")
            .performClick()
        composeRule.mainClock.advanceTimeBy(
            NutsNewsMotion.ListenPausedTransitionMillis + 16L,
        )
        composeRule
            .onNodeWithTag(
                "listen_mode_paused_indicator",
                useUnmergedTree = true,
            )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("listen_mode_primary")
            .performScrollTo()
            .assertTextEquals("Resume")
            .performClick()
        composeRule
            .onNodeWithTag("listen_mode_stop")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("listen_mode_status").assertTextEquals("Stopped")
        assertEquals(3, requestedScripts.size)
        assertEquals(1, stopCount)

        composeRule.onNodeWithTag("listen_mode_done").performClick()
        composeRule.onAllNodesWithTag("listen_mode_sheet").assertCountEquals(0)
        assertEquals(2, stopCount)
    }

    @Test
    fun positiveShareCardPreviewShowsLoadingAndRetryableFailureStates() {
        val article = contentArticle()
        val brief = deriveArticleBrief(article)
        val state = mutableStateOf(ArticleShareCardUiState())
        val shared = mutableListOf<Article>()
        setDetail(
            article = article,
            imageModel = null,
            shareCardUiState = { state.value },
            onShareCard = { selectedArticle ->
                shared += selectedArticle
                state.value = ArticleShareCardUiState(isCreating = true)
            },
        )

        composeRule
            .onNodeWithTag("article_detail_share_card")
            .performScrollTo()
        composeRule.onNodeWithText("NUTSNEWS SHARE CARD").assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_share_preview_title")
            .assertTextEquals(article.title)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_share_preview_takeaway")
            .assertTextEquals(brief.takeaway)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_share_preview_mood")
            .assertTextEquals(brief.primaryMoodLabel)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_share_preview_source")
            .assertTextEquals(article.source)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_share_button")
            .assertTextEquals("Share card with someone special")
            .performClick()

        composeRule
            .onNodeWithTag("article_detail_share_button")
            .assertTextEquals("Creating card")
            .assertIsNotEnabled()
        composeRule
            .onNodeWithTag("article_detail_share_loading")
            .assertIsDisplayed()
        assertEquals(listOf(article), shared)

        composeRule.runOnIdle {
            state.value =
                ArticleShareCardUiState(
                    failureMessage =
                        "The positive share card couldn’t be created. Please try again.",
                )
        }
        composeRule
            .onNodeWithTag("article_detail_share_failure")
            .performScrollTo()
            .assertTextEquals(
                "The positive share card couldn’t be created. Please try again.",
            ).assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_detail_share_button")
            .assertTextEquals("Share card with someone special")
            .performClick()
        assertEquals(listOf(article, article), shared)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1024dp-h768dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletArticleDetailScreenTest : ArticleDetailScreenshotContract(compactExpected = true) {
    @Test
    fun compactContentEndsAboveTheNavigationBarInset() {
        val bottomInsetDp = 48
        setDetail(
            article = contentArticle(),
            imageModel = null,
        )
        applyNavigationBarInset(bottomInsetDp)

        composeRule
            .onNodeWithTag("article_detail_open_original")
            .performScrollTo()
            .assertIsDisplayed()

        assertAboveNavigationBar("article_detail_open_original", bottomInsetDp)
    }

    @Test
    fun compactTabletBriefSummaryAndSourceMatchIosScreenshot() {
        val article = contentArticle()
        setDetail(
            article = article,
            imageModel = null,
        )

        composeRule.onNodeWithTag("article_detail_brief").assertIsDisplayed()
        composeRule.onNodeWithTag("article_detail_summary").assertIsDisplayed()
        composeRule.onNodeWithTag("article_detail_source").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Takeaway: Progress is still happening, one discovery at a time.",
            ).assertIsDisplayed()
        composeRule.onNodeWithText(article.source).assertIsDisplayed()
        composeRule.onNodeWithText("Published today").assertIsDisplayed()
        composeRule.onNodeWithText("Smile").assertIsDisplayed()
        composeRule.onNodeWithText("Hope").assertIsDisplayed()
        composeRule.onNodeWithText("Revisit").assertIsDisplayed()
        val editorHeightDp =
            bounds("article_detail_note_editor_frame").height /
                composeRule.activity.resources.displayMetrics.density
        assertTrue(
            editorHeightDp in 58f..74.5f,
            "Expected compact note editor height in 58..74dp but was $editorHeightDp",
        )
        assertTrue(sampledColorCount(captureLargestWindow()) >= DetailGolden.MinimumGoldenColors)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w852dp-h393dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneLandscapeArticleDetailScreenTest :
    ArticleDetailScreenshotContract(compactExpected = false)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w841dp-h673dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FoldableLandscapeArticleDetailScreenTest :
    ArticleDetailScreenshotContract(compactExpected = true)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp-port")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletPortraitArticleDetailScreenTest :
    ArticleDetailScreenshotContract(compactExpected = false)

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

private fun contentArticle(): Article =
    Article(
        id = "detail-content",
        title = "Researchers discover a practical clean-energy breakthrough",
        summary =
            "A university research team found a promising way to store renewable energy.",
        originalUrl = URI("https://example.com/detail-content"),
        source = "Positive Science Daily",
        publishedAt = "Published today",
        createdAt = null,
        thumbnailUrl = null,
        categories = listOf("Science", "Discovery"),
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
