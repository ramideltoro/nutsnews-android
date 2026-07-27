package com.nutsnews.app.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NutsNewsThemeMotionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun liveThemeChangeInterpolatesEveryPaletteBeforeReachingTheTarget() {
        composeRule.mainClock.autoAdvance = false
        val selectedTheme = mutableStateOf(NutsNewsAppTheme.Amber)
        var observedColor = Color.Unspecified
        composeRule.setContent {
            NutsNewsTheme(
                theme = selectedTheme.value,
                updateSystemBars = false,
                reducedMotionOverride = false,
            ) {
                val color = NutsNewsTheme.colors.accentRich
                SideEffect {
                    observedColor = color
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        val start = NutsNewsPalettes.forTheme(NutsNewsAppTheme.Amber).accentRich
        val target = NutsNewsPalettes.forTheme(NutsNewsAppTheme.Bambi).accentRich
        assertEquals(start, observedColor)

        composeRule.runOnIdle {
            selectedTheme.value = NutsNewsAppTheme.Bambi
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(NutsNewsMotion.ThemeChangeMillis / 2L)
        composeRule.runOnIdle {
            assertNotEquals(start, observedColor)
            assertNotEquals(target, observedColor)
        }

        composeRule.mainClock.advanceTimeBy(NutsNewsMotion.ThemeChangeMillis.toLong())
        composeRule.runOnIdle {
            assertEquals(target, observedColor)
        }
    }

    @Test
    fun reducedMotionAppliesTheNewPaletteWithoutAVisualDelay() {
        val selectedTheme = mutableStateOf(NutsNewsAppTheme.Amber)
        var observedColor = Color.Unspecified
        composeRule.setContent {
            NutsNewsTheme(
                theme = selectedTheme.value,
                updateSystemBars = false,
                reducedMotionOverride = true,
            ) {
                val color = NutsNewsTheme.colors.accentRich
                SideEffect {
                    observedColor = color
                }
            }
        }

        composeRule.runOnIdle {
            selectedTheme.value = NutsNewsAppTheme.Sakura
        }
        composeRule.waitForIdle()

        assertEquals(
            NutsNewsPalettes.forTheme(NutsNewsAppTheme.Sakura).accentRich,
            observedColor,
        )
    }
}
