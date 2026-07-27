package com.nutsnews.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsPalettes

internal data class NutsNewsWidgetPalette(
    val background: ColorProvider,
    val card: ColorProvider,
    val border: ColorProvider,
    val primaryText: ColorProvider,
    val secondaryText: ColorProvider,
    val accent: ColorProvider,
    val buttonText: ColorProvider,
    val symbol: String,
)

internal object NutsNewsWidgetPalettes {
    fun forTheme(theme: NutsNewsAppTheme): NutsNewsWidgetPalette {
        val palette = NutsNewsPalettes.forTheme(theme)
        return NutsNewsWidgetPalette(
            background = palette.backgroundGradient.first().asGlanceColor(),
            card = palette.cardBackgroundStrong.asGlanceColor(),
            border = palette.cardBorder.asGlanceColor(),
            primaryText = palette.primaryText.asGlanceColor(),
            secondaryText = palette.secondaryText.asGlanceColor(),
            accent = palette.accent.asGlanceColor(),
            buttonText = palette.buttonText.asGlanceColor(),
            symbol =
                when (theme) {
                    NutsNewsAppTheme.Amber -> "☀"
                    NutsNewsAppTheme.Sakura -> "✿"
                    NutsNewsAppTheme.SaaS -> "⚡"
                    NutsNewsAppTheme.Foxy -> "✦"
                    NutsNewsAppTheme.Friday -> "✨"
                    NutsNewsAppTheme.Bambi -> "🍃"
                },
        )
    }
}

private fun Color.asGlanceColor(): ColorProvider = ColorProvider(this)
