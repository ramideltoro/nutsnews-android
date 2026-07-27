package com.nutsnews.app.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.test.assertTrue
import org.junit.Test

class NutsNewsAccessibilityTest {
    @Test
    fun everyReaderFacingPalettePairMeetsWcagAaNormalTextContrast() {
        NutsNewsAppTheme.entries.forEach { theme ->
            val palette = NutsNewsPalettes.forTheme(theme)
            val surfaces =
                listOf(
                    "card" to palette.cardBackgroundStrong,
                    "background" to palette.backgroundGradient.first(),
                )
            val foregrounds =
                listOf(
                    "primary" to palette.primaryText,
                    "secondary" to palette.secondaryText,
                    "muted" to palette.mutedText,
                    "accent text" to palette.accentText,
                    "accent highlight" to palette.accentHighlight,
                )

            surfaces.forEach { (surfaceName, background) ->
                foregrounds.forEach { (foregroundName, foreground) ->
                    assertContrast(
                        theme = theme,
                        pair = "$foregroundName on $surfaceName",
                        foreground = foreground,
                        background = background,
                    )
                }
            }
            palette.buttonGradient.forEachIndexed { index, background ->
                assertContrast(
                    theme = theme,
                    pair = "button text on gradient stop $index",
                    foreground = palette.buttonText,
                    background = background,
                )
            }
        }
    }

    @Test
    fun translucentForegroundIsCompositedBeforeContrastIsMeasured() {
        val ratio =
            nutsNewsContrastRatio(
                foreground = Color.White.copy(alpha = 0.5f),
                background = Color.Black,
            )

        assertTrue(ratio in 5.2f..5.4f, "Expected composited ratio near 5.3, was $ratio")
    }

    private fun assertContrast(
        theme: NutsNewsAppTheme,
        pair: String,
        foreground: Color,
        background: Color,
    ) {
        val ratio = nutsNewsContrastRatio(foreground, background)
        assertTrue(
            ratio >= NormalTextMinimumContrast,
            "${theme.rawValue}: $pair has $ratio:1 contrast; " +
                "expected at least $NormalTextMinimumContrast:1",
        )
    }

    private companion object {
        const val NormalTextMinimumContrast = 4.5f
    }
}
