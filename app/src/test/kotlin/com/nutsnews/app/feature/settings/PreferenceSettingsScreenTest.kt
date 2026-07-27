package com.nutsnews.app.feature.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class PreferenceSettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun hapticsToggleShowsIosCopyAndChangesWithoutDeviceFeedback() {
        var enabled by mutableStateOf(true)
        var changeCount = 0
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                HapticsSettingsScreen(
                    enabled = enabled,
                    onEnabledChanged = { updated ->
                        changeCount += 1
                        enabled = updated
                    },
                    onBack = {},
                    onGoHome = {},
                )
            }
        }

        composeRule.onNodeWithText("Haptics").assertExists()
        composeRule.onNodeWithText("Like button haptics").assertExists()
        composeRule
            .onNodeWithText("Feel a soft tap when liking a story.")
            .assertExists()
        composeRule.onNodeWithTag("haptics_settings_switch").assertIsOn()
        composeRule.onNodeWithTag("haptics_settings_switch").performClick()
        composeRule.onNodeWithTag("haptics_settings_switch").assertIsOff()

        assertEquals(1, changeCount)
        assertTrue(sampledColorCount(captureLargestWindow()) >= 5)
    }

    @Test
    fun widgetToggleShowsIosCopyAndRequestsPreferenceChange() {
        var enabled by mutableStateOf(false)
        val updates = mutableListOf<Boolean>()
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                WidgetSettingsScreen(
                    showStatsOnLargeWidget = enabled,
                    onShowStatsChanged = { updated ->
                        updates += updated
                        enabled = updated
                    },
                    onBack = {},
                    onGoHome = {},
                )
            }
        }

        composeRule.onNodeWithText("Widget").assertExists()
        composeRule.onNodeWithText("Show stats on large widget").assertExists()
        composeRule
            .onNodeWithText(
                "When the large NutsNews widget is on your Home Screen, " +
                    "show today’s progress, streak, and total stories.",
            ).assertExists()
        composeRule.onNodeWithTag("widget_settings_switch").assertIsOff()
        composeRule.onNodeWithTag("widget_settings_switch").performClick()
        composeRule.onNodeWithTag("widget_settings_switch").assertIsOn()

        assertEquals(listOf(true), updates)
        assertTrue(sampledColorCount(captureLargestWindow()) >= 5)
    }

    @Test
    fun childSettingsExposeBackAndHomeActions() {
        var backCount = 0
        var homeCount = 0
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                HapticsSettingsScreen(
                    enabled = true,
                    onEnabledChanged = {},
                    onBack = { backCount += 1 },
                    onGoHome = { homeCount += 1 },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back to Settings").performClick()
        composeRule.onNodeWithContentDescription("Go home").performClick()

        assertEquals(1, backCount)
        assertEquals(1, homeCount)
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
