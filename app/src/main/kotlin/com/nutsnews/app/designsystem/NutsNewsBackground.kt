package com.nutsnews.app.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun NutsNewsBackground(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    val palette = NutsNewsTheme.colors
    val density = LocalDensity.current
    val overlayRadius = with(density) { 430.dp.toPx() }
    val overlayStart = with(density) { NutsNewsTheme.spacing.small.toPx() } / overlayRadius
    val background =
        remember(palette.backgroundGradient) {
            Brush.linearGradient(
                colors = palette.backgroundGradient,
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        }
    val overlay =
        remember(palette.backgroundOverlay, overlayRadius, overlayStart) {
            Brush.radialGradient(
                colorStops =
                    arrayOf(
                        0f to palette.backgroundOverlay,
                        overlayStart to palette.backgroundOverlay,
                        1f to Color.Transparent,
                    ),
                center = Offset.Zero,
                radius = overlayRadius,
            )
        }

    Box(
        modifier =
            modifier
                .background(background)
                .background(overlay),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

@Composable
fun nutsNewsButtonGradient(): Brush {
    val colors = NutsNewsTheme.colors.buttonGradient
    return remember(colors) {
        Brush.linearGradient(
            colors = colors,
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    }
}
