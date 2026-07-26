package com.nutsnews.app.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalAppTheme =
    staticCompositionLocalOf<NutsNewsAppTheme> {
        error("NutsNewsTheme is missing from the composition.")
    }

private val LocalPalette =
    staticCompositionLocalOf<NutsNewsPalette> {
        error("NutsNewsTheme is missing from the composition.")
    }

object NutsNewsTheme {
    val appTheme: NutsNewsAppTheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTheme.current

    val colors: NutsNewsPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalPalette.current

    val spacing: NutsNewsSpacing
        @Composable
        @ReadOnlyComposable
        get() = NutsNewsSpacingDefaults

    val radii: NutsNewsRadii
        @Composable
        @ReadOnlyComposable
        get() = NutsNewsRadiiDefaults

    val dimensions: NutsNewsDimensions
        @Composable
        @ReadOnlyComposable
        get() = NutsNewsDimensionsDefaults

    val borders: NutsNewsBorders
        @Composable
        @ReadOnlyComposable
        get() = NutsNewsBordersDefaults

    val shadows: NutsNewsShadows
        @Composable
        @ReadOnlyComposable
        get() = NutsNewsShadowsDefaults

    val typography: NutsNewsTypography
        @Composable
        @ReadOnlyComposable
        get() = NutsNewsTypographyDefaults

    @Composable
    operator fun invoke(
        theme: NutsNewsAppTheme = NutsNewsAppTheme.Default,
        updateSystemBars: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        val palette = NutsNewsPalettes.forTheme(theme)
        val colorScheme =
            if (theme.isDark) {
                darkColorScheme(
                    primary = palette.accent,
                    onPrimary = palette.buttonText,
                    secondary = palette.accentRich,
                    onSecondary = palette.buttonText,
                    tertiary = palette.accentDeep,
                    background = palette.backgroundGradient.first(),
                    onBackground = palette.primaryText,
                    surface = palette.cardBackgroundStrong,
                    onSurface = palette.primaryText,
                    surfaceVariant = palette.cardBackground,
                    onSurfaceVariant = palette.secondaryText,
                    outline = palette.cardBorder,
                    outlineVariant = palette.likedCardBorder,
                )
            } else {
                lightColorScheme(
                    primary = palette.accent,
                    onPrimary = palette.buttonText,
                    secondary = palette.accentRich,
                    onSecondary = palette.buttonText,
                    tertiary = palette.accentDeep,
                    background = palette.backgroundGradient.first(),
                    onBackground = palette.primaryText,
                    surface = palette.cardBackgroundStrong,
                    onSurface = palette.primaryText,
                    surfaceVariant = palette.cardBackground,
                    onSurfaceVariant = palette.secondaryText,
                    outline = palette.cardBorder,
                    outlineVariant = palette.likedCardBorder,
                )
            }

        if (updateSystemBars) {
            ApplySystemBarIconStyle(useDarkIcons = !theme.isDark)
        }

        CompositionLocalProvider(
            LocalAppTheme provides theme,
            LocalPalette provides palette,
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = NutsNewsTypographyDefaults.material,
                shapes = NutsNewsShapesDefaults,
                content = content,
            )
        }
    }
}

@Composable
private fun ApplySystemBarIconStyle(useDarkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val activity = view.context.findActivity() ?: return
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
