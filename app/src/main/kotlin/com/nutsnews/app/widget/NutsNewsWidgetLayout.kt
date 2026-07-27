package com.nutsnews.app.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal enum class NutsNewsWidgetSizeClass {
    Small,
    Medium,
    Large,
}

internal object NutsNewsWidgetSizes {
    val Small = DpSize(width = 140.dp, height = 140.dp)
    val Medium = DpSize(width = 280.dp, height = 140.dp)
    val Large = DpSize(width = 280.dp, height = 280.dp)

    val Supported = setOf(Small, Medium, Large)

    fun classify(size: DpSize): NutsNewsWidgetSizeClass =
        when {
            size.height >= 220.dp -> NutsNewsWidgetSizeClass.Large
            size.width >= 220.dp -> NutsNewsWidgetSizeClass.Medium
            else -> NutsNewsWidgetSizeClass.Small
        }
}
