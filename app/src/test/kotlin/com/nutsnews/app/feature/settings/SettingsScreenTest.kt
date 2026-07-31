package com.nutsnews.app.feature.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyRowNavigatesToItsStackDestination() {
        val navigator =
            DefaultAppNavigator(AppDestination.Feed).apply {
                navigate(AppDestination.Settings)
            }
        setScreen(
            onAppearance = { navigator.navigate(AppDestination.ThemePicker) },
            onHaptics = { navigator.navigate(AppDestination.HapticsSettings) },
            onWidget = { navigator.navigate(AppDestination.WidgetSettings) },
            onContact = { navigator.navigate(AppDestination.ContactUs) },
        )

        val destinations =
            linkedMapOf(
                "settings_row_theme" to AppDestination.ThemePicker,
                "settings_row_haptics" to AppDestination.HapticsSettings,
                "settings_row_widget" to AppDestination.WidgetSettings,
                "settings_row_contact" to AppDestination.ContactUs,
            )
        destinations.forEach { (tag, destination) ->
            composeRule.onNodeWithTag(tag).performClick()
            assertEquals(destination, navigator.backStack.value.last())
            assertTrue(navigator.navigateUp())
            assertEquals(AppDestination.Settings, navigator.backStack.value.last())
        }
    }

    @Test
    fun rowSubtitlesReflectCurrentSettingsValues() {
        var state by mutableStateOf(settingsState())
        composeRule.setContent {
            NutsNewsTheme(theme = state.theme, updateSystemBars = false) {
                SettingsScreen(
                    uiState = state,
                    onAppearance = {},
                    onHaptics = {},
                    onWidget = {},
                    onContact = {},
                    onGoHome = {},
                )
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule
            .onNodeWithTag("settings_subtitle_theme", useUnmergedTree = true)
            .assertTextEquals("Amber")
        composeRule
            .onNodeWithTag("settings_subtitle_haptics", useUnmergedTree = true)
            .assertTextEquals("On")
        composeRule
            .onNodeWithTag("settings_subtitle_widget", useUnmergedTree = true)
            .assertTextEquals("Large stats on")

        composeRule.runOnIdle {
            state =
                state.copy(
                    theme = NutsNewsAppTheme.Foxy,
                    hapticsEnabled = false,
                    showStatsOnLargeWidget = false,
                )
        }
        composeRule
            .onNodeWithTag("settings_subtitle_theme", useUnmergedTree = true)
            .assertTextEquals("Foxy")
        composeRule
            .onNodeWithTag("settings_subtitle_haptics", useUnmergedTree = true)
            .assertTextEquals("Off")
        composeRule
            .onNodeWithTag("settings_subtitle_widget", useUnmergedTree = true)
            .assertTextEquals("Large stats off")
    }

    @Test
    fun allRowsRenderAcrossAllSixThemesWithScreenshots() {
        var state by mutableStateOf(settingsState())
        composeRule.setContent {
            NutsNewsTheme(theme = state.theme, updateSystemBars = false) {
                SettingsScreen(
                    uiState = state,
                    onAppearance = {},
                    onHaptics = {},
                    onWidget = {},
                    onContact = {},
                    onGoHome = {},
                )
            }
        }
        val screenshots = mutableListOf<Bitmap>()

        NutsNewsAppTheme.entries.forEach { theme ->
            composeRule.runOnIdle {
                state = state.copy(theme = theme)
            }
            composeRule.onNodeWithTag("settings_row_theme").assertIsDisplayed()
            composeRule.onNodeWithTag("settings_row_haptics").assertIsDisplayed()
            composeRule.onNodeWithTag("settings_row_widget").assertIsDisplayed()
            composeRule.onNodeWithTag("settings_row_contact").assertIsDisplayed()
            composeRule
                .onNodeWithTag("settings_subtitle_theme", useUnmergedTree = true)
                .assertTextEquals(theme.title)
            screenshots += captureLargestWindow()
        }

        assertEquals(6, screenshots.size)
        assertTrue(screenshots.all { screenshot -> sampledColorCount(screenshot) >= 5 })
    }

    @Test
    fun homeControlClosesTheSettingsHierarchy() {
        var homeCount = 0
        setScreen(onGoHome = { homeCount += 1 })

        composeRule.onNodeWithContentDescription("Go home").performClick()

        assertEquals(1, homeCount)
    }

    private fun setScreen(
        onAppearance: () -> Unit = {},
        onHaptics: () -> Unit = {},
        onWidget: () -> Unit = {},
        onContact: () -> Unit = {},
        onGoHome: () -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                SettingsScreen(
                    uiState = settingsState(),
                    onAppearance = onAppearance,
                    onHaptics = onHaptics,
                    onWidget = onWidget,
                    onContact = onContact,
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

private fun settingsState(): SettingsUiState =
    SettingsUiState(
        isLoading = false,
        theme = NutsNewsAppTheme.Amber,
        hapticsEnabled = true,
        showStatsOnLargeWidget = true,
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
