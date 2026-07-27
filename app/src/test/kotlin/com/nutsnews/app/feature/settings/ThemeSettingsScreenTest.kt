package com.nutsnews.app.feature.settings

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
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsTheme
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
class ThemeSettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyThemeShowsNameDescriptionIconSwatchAndOneSelectedState() {
        setScreen()

        NutsNewsAppTheme.entries.forEach { theme ->
            composeRule
                .onNodeWithTag("theme_settings_list")
                .performScrollToNode(hasTestTag("theme_option_${theme.rawValue}"))
            composeRule.onNodeWithText(theme.title).assertIsDisplayed()
            composeRule.onNodeWithText(theme.description).assertIsDisplayed()
            composeRule
                .onNodeWithTag(
                    "theme_icon_${theme.rawValue}",
                    useUnmergedTree = true,
                ).assertExists()
            composeRule
                .onNodeWithTag(
                    "theme_swatch_${theme.rawValue}",
                    useUnmergedTree = true,
                ).assertExists()
        }
        composeRule
            .onAllNodesWithTag("theme_radio_selected", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule
            .onNodeWithContentDescription("Amber theme, selected")
            .assertExists()
    }

    @Test
    fun selectingEachThemeSwitchesTheWholeCompositionAndProducesUniqueScreenshots() {
        var selectedTheme by mutableStateOf(NutsNewsAppTheme.Amber)
        val selected = mutableListOf<NutsNewsAppTheme>()
        composeRule.setContent {
            NutsNewsTheme(theme = selectedTheme, updateSystemBars = false) {
                ThemeSettingsScreen(
                    selectedTheme = selectedTheme,
                    onThemeSelected = { theme ->
                        selected += theme
                        selectedTheme = theme
                    },
                    onBack = {},
                    onGoHome = {},
                )
            }
        }
        val screenshotSignatures = linkedSetOf<Int>()

        NutsNewsAppTheme.entries.forEach { theme ->
            composeRule
                .onNodeWithTag("theme_settings_list")
                .performScrollToNode(hasTestTag("theme_option_${theme.rawValue}"))
            if (theme != NutsNewsAppTheme.Amber) {
                composeRule
                    .onNodeWithTag("theme_option_${theme.rawValue}")
                    .performClick()
            }
            composeRule
                .onNodeWithContentDescription("${theme.title} theme, selected")
                .assertIsDisplayed()
            composeRule
                .onAllNodesWithTag("theme_radio_selected", useUnmergedTree = true)
                .assertCountEquals(1)
            screenshotSignatures += sampledSignature(captureLargestWindow())
        }

        assertEquals(NutsNewsAppTheme.entries.drop(1), selected)
        assertEquals(6, screenshotSignatures.size)
    }

    @Test
    fun glowTransitionActivatesThenSettlesWithoutStaleSelection() {
        var selectedTheme by mutableStateOf(NutsNewsAppTheme.Amber)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NutsNewsTheme(theme = selectedTheme, updateSystemBars = false) {
                ThemeSettingsScreen(
                    selectedTheme = selectedTheme,
                    onThemeSelected = { selectedTheme = it },
                    onBack = {},
                    onGoHome = {},
                )
            }
        }

        composeRule
            .onNodeWithTag("theme_settings_list")
            .performScrollToNode(hasTestTag("theme_option_modernSaaS"))
        composeRule.onNodeWithTag("theme_option_modernSaaS").performClick()
        composeRule.mainClock.advanceTimeBy(16)
        composeRule
            .onNodeWithContentDescription("SaaS theme, selected")
            .assertExists()

        composeRule
            .onNodeWithTag("theme_settings_list")
            .performScrollToNode(hasTestTag("theme_option_sanJuan"))
        composeRule.onNodeWithTag("theme_option_sanJuan").performClick()
        composeRule.mainClock.advanceTimeBy(ThemeGlowDurationMillis.toLong() + 32)
        composeRule
            .onNodeWithContentDescription("Foxy theme, selected")
            .assertExists()
        assertEquals(NutsNewsAppTheme.Foxy, selectedTheme)
    }

    @Test
    fun backAndHomeControlsPreserveTheirDistinctNavigation() {
        var backCount = 0
        var homeCount = 0
        setScreen(
            onBack = { backCount += 1 },
            onGoHome = { homeCount += 1 },
        )

        composeRule.onNodeWithContentDescription("Back to Settings").performClick()
        composeRule.onNodeWithContentDescription("Go home").performClick()

        assertEquals(1, backCount)
        assertEquals(1, homeCount)
    }

    private fun setScreen(
        onBack: () -> Unit = {},
        onGoHome: () -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                ThemeSettingsScreen(
                    selectedTheme = NutsNewsAppTheme.Amber,
                    onThemeSelected = {},
                    onBack = onBack,
                    onGoHome = onGoHome,
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

private fun sampledSignature(bitmap: Bitmap): Int {
    var signature = 1
    val stepX = max(1, bitmap.width / 16)
    val stepY = max(1, bitmap.height / 16)
    for (x in 0 until bitmap.width step stepX) {
        for (y in 0 until bitmap.height step stepY) {
            signature = (31 * signature) + bitmap.getPixel(x, y)
        }
    }
    return signature
}
