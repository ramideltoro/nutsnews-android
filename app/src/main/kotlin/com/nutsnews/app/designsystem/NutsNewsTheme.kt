package com.nutsnews.app.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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

private val LocalReducedMotion = staticCompositionLocalOf { false }

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

    /**
     * True when Android's animator duration scale is disabled.
     *
     * Compose animations honor the platform scale automatically. Feature code
     * uses this value for non-animation delays and decorative infinite motion.
     */
    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current

    @Composable
    operator fun invoke(
        theme: NutsNewsAppTheme = NutsNewsAppTheme.Default,
        updateSystemBars: Boolean = true,
        reducedMotionOverride: Boolean? = null,
        content: @Composable () -> Unit,
    ) {
        val reducedMotion =
            reducedMotionOverride
                ?: rememberNutsNewsReducedMotion()
        val palette =
            animateNutsNewsPalette(
                target = NutsNewsPalettes.forTheme(theme),
                reducedMotion = reducedMotion,
            )
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
            LocalReducedMotion provides reducedMotion,
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
private fun animateNutsNewsPalette(
    target: NutsNewsPalette,
    reducedMotion: Boolean,
): NutsNewsPalette {
    val animationSpec: FiniteAnimationSpec<Color> =
        if (reducedMotion) {
            snap()
        } else {
            tween(
                durationMillis = NutsNewsMotion.ThemeChangeMillis,
                easing = FastOutSlowInEasing,
            )
        }

    @Composable
    fun animated(
        color: Color,
        label: String,
    ): Color =
        animateColorAsState(
            targetValue = color,
            animationSpec = animationSpec,
            label = label,
        ).value

    return NutsNewsPalette(
        accent = animated(target.accent, "Theme accent"),
        accentRich = animated(target.accentRich, "Theme rich accent"),
        accentDeep = animated(target.accentDeep, "Theme deep accent"),
        accentSoft = animated(target.accentSoft, "Theme soft accent"),
        accentText = animated(target.accentText, "Theme accent text"),
        accentHighlight = animated(target.accentHighlight, "Theme highlight"),
        accentGlow = animated(target.accentGlow, "Theme glow"),
        cardBackground = animated(target.cardBackground, "Theme card"),
        cardBackgroundStrong = animated(target.cardBackgroundStrong, "Theme strong card"),
        cardBorder = animated(target.cardBorder, "Theme card border"),
        likedCardAccent = animated(target.likedCardAccent, "Theme liked accent"),
        likedCardBorder = animated(target.likedCardBorder, "Theme liked border"),
        likedCardGlow = animated(target.likedCardGlow, "Theme liked glow"),
        badgeBackground = animated(target.badgeBackground, "Theme badge"),
        primaryText = animated(target.primaryText, "Theme primary text"),
        secondaryText = animated(target.secondaryText, "Theme secondary text"),
        mutedText = animated(target.mutedText, "Theme muted text"),
        buttonText = animated(target.buttonText, "Theme button text"),
        backgroundGradient =
            target.backgroundGradient.mapIndexed { index, color ->
                animated(color, "Theme background $index")
            },
        backgroundOverlay = animated(target.backgroundOverlay, "Theme background overlay"),
        buttonGradient =
            target.buttonGradient.mapIndexed { index, color ->
                animated(color, "Theme button gradient $index")
            },
        categoryDots =
            target.categoryDots.mapIndexed { index, color ->
                animated(color, "Theme category dot $index")
            },
    )
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
