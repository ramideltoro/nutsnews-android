package com.nutsnews.app.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import java.io.File
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

class NutsNewsWindowInfoTest {
    @Test
    fun phoneTabletAndFoldableProfilesUseWindowBreakpoints() {
        assertProfile(
            widthDp = 393,
            heightDp = 852,
            widthClass = NutsNewsWindowWidthClass.Compact,
            heightClass = NutsNewsWindowHeightClass.Medium,
            compactLandscape = false,
        )
        assertProfile(
            widthDp = 852,
            heightDp = 393,
            widthClass = NutsNewsWindowWidthClass.Expanded,
            heightClass = NutsNewsWindowHeightClass.Compact,
            compactLandscape = false,
        )
        assertProfile(
            widthDp = 673,
            heightDp = 841,
            widthClass = NutsNewsWindowWidthClass.Medium,
            heightClass = NutsNewsWindowHeightClass.Medium,
            compactLandscape = false,
        )
        assertProfile(
            widthDp = 841,
            heightDp = 673,
            widthClass = NutsNewsWindowWidthClass.Expanded,
            heightClass = NutsNewsWindowHeightClass.Medium,
            compactLandscape = true,
        )
        assertProfile(
            widthDp = 800,
            heightDp = 1_280,
            widthClass = NutsNewsWindowWidthClass.Medium,
            heightClass = NutsNewsWindowHeightClass.Expanded,
            compactLandscape = false,
        )
        assertProfile(
            widthDp = 1_280,
            heightDp = 800,
            widthClass = NutsNewsWindowWidthClass.Expanded,
            heightClass = NutsNewsWindowHeightClass.Medium,
            compactLandscape = true,
        )
    }

    @Test
    fun adaptiveScreensAvoidDeviceModelAndSmallestWidthChecks() {
        val windowAwareSources =
            listOf(
                "src/main/kotlin/com/nutsnews/app/feature/article/ArticleDetailScreen.kt",
                "src/main/kotlin/com/nutsnews/app/feature/feed/ArticleFeedContent.kt",
            ).map(::File)
        windowAwareSources.forEach { source ->
            val content = source.readText()
            assertFalse("smallestScreenWidthDp" in content, source.path)
            assertFalse("LocalConfiguration" in content, source.path)
        }

        val boundedScreens =
            listOf(
                "article/ArticleDetailScreen.kt",
                "digest/DailyDigestScreen.kt",
                "feed/FeedScreen.kt",
                "help/HelpFaqScreen.kt",
                "mood/GoodMoodScreen.kt",
                "personalization/PersonalizationScreen.kt",
                "saved/SavedStoriesScreen.kt",
                "search/ArchiveSearchScreen.kt",
                "settings/PreferenceSettingsScreen.kt",
                "settings/SettingsScreen.kt",
                "settings/ThemeSettingsScreen.kt",
                "stats/ReadingStatsScreen.kt",
            )
        boundedScreens.forEach { relativePath ->
            val source =
                File(
                    "src/main/kotlin/com/nutsnews/app/feature/$relativePath",
                ).readText()
            assertTrue(
                "NutsNewsAdaptivePane" in source,
                "$relativePath must retain an adaptive presentation pane",
            )
        }
    }

    private fun assertProfile(
        widthDp: Int,
        heightDp: Int,
        widthClass: NutsNewsWindowWidthClass,
        heightClass: NutsNewsWindowHeightClass,
        compactLandscape: Boolean,
    ) {
        val profile = NutsNewsWindowInfo.fromDp(widthDp, heightDp)

        assertEquals(widthClass, profile.widthClass)
        assertEquals(heightClass, profile.heightClass)
        assertEquals(compactLandscape, profile.usesCompactLandscapeLayout)
    }
}

abstract class AdaptiveProfileScreenshotContract(
    private val expectedProfileTag: String,
    private val expectedPaneMaximumWidthDp: Int,
) {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun adaptivePaneScreenshotKeepsContentBoundedAndBottomActionReachable() {
        var actionCount = 0
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                NutsNewsAdaptiveWindow {
                    NutsNewsBackground {
                        NutsNewsAdaptivePane {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(NutsNewsTheme.spacing.medium)
                                        .testTag("adaptive_profile_content"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                            ) {
                                Surface(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .testTag("adaptive_profile_header"),
                                    shape =
                                        RoundedCornerShape(
                                            NutsNewsTheme.dimensions.cardCornerRadius,
                                        ),
                                    color = NutsNewsTheme.colors.cardBackgroundStrong,
                                    border =
                                        BorderStroke(
                                            NutsNewsTheme.borders.hairline,
                                            NutsNewsTheme.colors.cardBorder,
                                        ),
                                    onClick = { actionCount += 1 },
                                ) {
                                    Text(
                                        text = "NutsNews adaptive preview",
                                        modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
                                        color = NutsNewsTheme.colors.primaryText,
                                        style = NutsNewsTheme.typography.headline,
                                    )
                                }
                                Spacer(modifier = Modifier.height(1_600.dp))
                                Surface(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .testTag("adaptive_profile_bottom_action"),
                                    shape =
                                        RoundedCornerShape(
                                            NutsNewsTheme.dimensions.controlCornerRadius,
                                        ),
                                    color = NutsNewsTheme.colors.accent,
                                    onClick = { actionCount += 1 },
                                ) {
                                    Text(
                                        text = "Continue",
                                        modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
                                        color = NutsNewsTheme.colors.buttonText,
                                        style = NutsNewsTheme.typography.button,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composeRule
            .onNodeWithTag(expectedProfileTag)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("adaptive_profile_header")
            .performClick()
        composeRule
            .onNodeWithTag("adaptive_profile_bottom_action")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(2, actionCount)

        val windowBounds = bounds(expectedProfileTag)
        val paneBounds = bounds("adaptive_pane")
        assertTrue(paneBounds.left >= windowBounds.left)
        assertTrue(paneBounds.right <= windowBounds.right)
        assertTrue(paneBounds.top >= windowBounds.top)
        assertTrue(paneBounds.bottom <= windowBounds.bottom)
        assertTrue(
            paneBounds.width.toDp() <= expectedPaneMaximumWidthDp + 1f,
            "Pane ${paneBounds.width.toDp()}dp exceeded ${expectedPaneMaximumWidthDp}dp",
        )
        assertNear(
            expected = windowBounds.center.x,
            actual = paneBounds.center.x,
        )
        assertTrue(sampledColorCount(captureLargestWindow()) >= 5)
    }

    private fun bounds(tag: String): Rect =
        composeRule
            .onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

    private fun Float.toDp(): Float =
        this / composeRule.activity.resources.displayMetrics.density

    private fun assertNear(
        expected: Float,
        actual: Float,
        tolerance: Float = 2f,
    ) {
        assertTrue(
            abs(expected - actual) <= tolerance,
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
class PhonePortraitAdaptiveProfileTest :
    AdaptiveProfileScreenshotContract(
        expectedProfileTag = "adaptive_window_compact_medium",
        expectedPaneMaximumWidthDp = 393,
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w852dp-h393dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneLandscapeAdaptiveProfileTest :
    AdaptiveProfileScreenshotContract(
        expectedProfileTag = "adaptive_window_expanded_compact",
        expectedPaneMaximumWidthDp = 852,
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w673dp-h841dp-port")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FoldablePortraitAdaptiveProfileTest :
    AdaptiveProfileScreenshotContract(
        expectedProfileTag = "adaptive_window_medium_medium",
        expectedPaneMaximumWidthDp = 673,
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w841dp-h673dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FoldableLandscapeAdaptiveProfileTest :
    AdaptiveProfileScreenshotContract(
        expectedProfileTag = "adaptive_window_expanded_medium",
        expectedPaneMaximumWidthDp = 841,
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp-port")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletPortraitAdaptiveProfileTest :
    AdaptiveProfileScreenshotContract(
        expectedProfileTag = "adaptive_window_medium_expanded",
        expectedPaneMaximumWidthDp = 720,
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletLandscapeAdaptiveProfileTest :
    AdaptiveProfileScreenshotContract(
        expectedProfileTag = "adaptive_window_expanded_medium",
        expectedPaneMaximumWidthDp = 960,
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
