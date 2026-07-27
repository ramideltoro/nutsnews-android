package com.nutsnews.app.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlin.math.roundToInt

enum class NutsNewsWindowWidthClass {
    Compact,
    Medium,
    Expanded,
}

enum class NutsNewsWindowHeightClass {
    Compact,
    Medium,
    Expanded,
}

/**
 * The adaptive information NutsNews needs from the current content window.
 *
 * This deliberately describes available space instead of a phone, tablet, or
 * foldable model. Resizing, rotating, split-screening, or unfolding therefore
 * recomputes the same layout decisions from the new bounds.
 */
@Immutable
data class NutsNewsWindowInfo(
    val sizeClass: WindowSizeClass,
    val widthDp: Int,
    val heightDp: Int,
) {
    val widthClass: NutsNewsWindowWidthClass
        get() =
            when {
                sizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
                ) -> NutsNewsWindowWidthClass.Expanded

                sizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                ) -> NutsNewsWindowWidthClass.Medium

                else -> NutsNewsWindowWidthClass.Compact
            }

    val heightClass: NutsNewsWindowHeightClass
        get() =
            when {
                sizeClass.isHeightAtLeastBreakpoint(
                    WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND,
                ) -> NutsNewsWindowHeightClass.Expanded

                sizeClass.isHeightAtLeastBreakpoint(
                    WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
                ) -> NutsNewsWindowHeightClass.Medium

                else -> NutsNewsWindowHeightClass.Compact
            }

    /**
     * Mirrors the compact iOS landscape presentation only when both axes have
     * enough room. A short phone landscape remains vertically scrollable.
     */
    val usesCompactLandscapeLayout: Boolean
        get() =
            widthClass == NutsNewsWindowWidthClass.Expanded &&
                heightClass != NutsNewsWindowHeightClass.Compact &&
                widthDp > heightDp

    val contentMaximumWidth: Dp
        get() =
            when (widthClass) {
                NutsNewsWindowWidthClass.Compact -> CompactContentMaximumWidth
                NutsNewsWindowWidthClass.Medium -> MediumContentMaximumWidth
                NutsNewsWindowWidthClass.Expanded -> ExpandedContentMaximumWidth
            }

    val immersiveContentMaximumWidth: Dp
        get() =
            when {
                heightClass == NutsNewsWindowHeightClass.Compact ->
                    CompactHeightImmersiveContentMaximumWidth

                widthClass == NutsNewsWindowWidthClass.Expanded ->
                    ImmersiveContentMaximumWidth

                else -> contentMaximumWidth
            }

    val profileName: String
        get() =
            "${widthClass.name.lowercase()}_${heightClass.name.lowercase()}"

    companion object {
        fun fromDp(
            widthDp: Int,
            heightDp: Int,
        ): NutsNewsWindowInfo {
            val safeWidth = widthDp.coerceAtLeast(0)
            val safeHeight = heightDp.coerceAtLeast(0)
            return NutsNewsWindowInfo(
                sizeClass =
                    WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(
                        widthDp = safeWidth,
                        heightDp = safeHeight,
                    ),
                widthDp = safeWidth,
                heightDp = safeHeight,
            )
        }
    }
}

val LocalNutsNewsWindowInfo =
    staticCompositionLocalOf {
        NutsNewsWindowInfo.fromDp(widthDp = 0, heightDp = 0)
    }

@Composable
fun NutsNewsAdaptiveWindow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthDp = maxWidth.value.roundToInt()
        val heightDp = maxHeight.value.roundToInt()
        val windowInfo =
            remember(widthDp, heightDp) {
                NutsNewsWindowInfo.fromDp(
                    widthDp = widthDp,
                    heightDp = heightDp,
                )
            }
        CompositionLocalProvider(LocalNutsNewsWindowInfo provides windowInfo) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("adaptive_window_${windowInfo.profileName}"),
            ) {
                content()
            }
        }
    }
}

/**
 * Centers a full-height presentation while bounding its working width. The
 * surrounding branded background can continue to fill the entire window.
 */
@Composable
fun NutsNewsAdaptivePane(
    modifier: Modifier = Modifier,
    maximumWidth: Dp = LocalNutsNewsWindowInfo.current.contentMaximumWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .widthIn(max = maximumWidth)
                    .fillMaxWidth()
                    .testTag("adaptive_pane"),
            content = content,
        )
    }
}

private val CompactContentMaximumWidth = 600.dp
private val MediumContentMaximumWidth = 720.dp
private val ExpandedContentMaximumWidth = 960.dp
private val CompactHeightImmersiveContentMaximumWidth = 480.dp
private val ImmersiveContentMaximumWidth = 1_200.dp
