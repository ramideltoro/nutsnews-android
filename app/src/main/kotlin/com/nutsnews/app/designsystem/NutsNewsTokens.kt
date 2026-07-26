package com.nutsnews.app.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val NutsNewsPhi = 1.61803398875

@Immutable
data class NutsNewsSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 6.dp,
    val small: Dp = 10.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 26.dp,
    val xl: Dp = 42.dp,
)

@Immutable
data class NutsNewsRadii(
    val xs: Dp = 6.dp,
    val small: Dp = 10.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 26.dp,
    val xl: Dp = 42.dp,
)

@Immutable
data class NutsNewsDimensions(
    val cardPadding: Dp = 16.dp,
    val cardCornerRadius: Dp = 26.dp,
    val imageCornerRadius: Dp = 16.dp,
    val controlCornerRadius: Dp = 16.dp,
    val chipVerticalPadding: Dp = 8.dp,
    val chipHorizontalPadding: Dp = 13.dp,
    val feedImageHeight: Dp = 188.dp,
    val detailHeroHeight: Dp = 210.dp,
)

@Immutable
data class NutsNewsBorders(
    val hairline: Dp = 1.dp,
    val emphasized: Dp = 1.2.dp,
    val selected: Dp = 1.7.dp,
    val glow: Dp = 2.2.dp,
)

@Immutable
data class NutsNewsShadows(
    val cardBlurRadius: Dp = 14.dp,
    val cardOffsetY: Dp = 8.dp,
    val buttonBlurRadius: Dp = 10.dp,
    val buttonOffsetY: Dp = 4.dp,
    val themeGlowRadius: Dp = 22.dp,
    val themeGlowOuterRadius: Dp = 34.1.dp,
)

@Immutable
data class NutsNewsTypography(
    val largeTitle: TextStyle,
    val title: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subheadline: TextStyle,
    val footnote: TextStyle,
    val caption: TextStyle,
    val caption2: TextStyle,
    val brandHero: TextStyle,
    val brandTitle: TextStyle,
    val cardTitle: TextStyle,
    val metric: TextStyle,
    val button: TextStyle,
    val label: TextStyle,
    val material: Typography,
)

internal val NutsNewsSpacingDefaults = NutsNewsSpacing()
internal val NutsNewsRadiiDefaults = NutsNewsRadii()
internal val NutsNewsDimensionsDefaults = NutsNewsDimensions()
internal val NutsNewsBordersDefaults = NutsNewsBorders()
internal val NutsNewsShadowsDefaults = NutsNewsShadows()

private val sans = FontFamily.SansSerif
private val serif = FontFamily.Serif

private fun systemStyle(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = sans,
    lineHeight: Int = size + 5,
): TextStyle =
    TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

internal val NutsNewsTypographyDefaults: NutsNewsTypography =
    NutsNewsTypography(
        largeTitle = systemStyle(size = 34, lineHeight = 41),
        title = systemStyle(size = 28, lineHeight = 34),
        title2 = systemStyle(size = 22, lineHeight = 28),
        title3 = systemStyle(size = 20, lineHeight = 25),
        headline = systemStyle(size = 17, weight = FontWeight.SemiBold, lineHeight = 22),
        body = systemStyle(size = 17, lineHeight = 22),
        callout = systemStyle(size = 16, lineHeight = 21),
        subheadline = systemStyle(size = 15, lineHeight = 20),
        footnote = systemStyle(size = 13, lineHeight = 18),
        caption = systemStyle(size = 12, lineHeight = 16),
        caption2 = systemStyle(size = 11, lineHeight = 14),
        brandHero = systemStyle(size = 38, weight = FontWeight.SemiBold, lineHeight = 44),
        brandTitle =
            systemStyle(
                size = 31,
                weight = FontWeight.Light,
                family = serif,
                lineHeight = 37,
            ),
        cardTitle = systemStyle(size = 20, weight = FontWeight.Bold, lineHeight = 25),
        metric = systemStyle(size = 26, weight = FontWeight.Bold, lineHeight = 31),
        button = systemStyle(size = 17, weight = FontWeight.SemiBold, lineHeight = 22),
        label = systemStyle(size = 13, weight = FontWeight.Bold, lineHeight = 17),
        material =
            Typography(
                displayLarge = systemStyle(size = 38, weight = FontWeight.SemiBold, lineHeight = 44),
                displayMedium = systemStyle(size = 34, lineHeight = 41),
                displaySmall = systemStyle(size = 31, family = serif, lineHeight = 37),
                headlineLarge = systemStyle(size = 28, lineHeight = 34),
                headlineMedium = systemStyle(size = 22, lineHeight = 28),
                headlineSmall = systemStyle(size = 20, weight = FontWeight.Bold, lineHeight = 25),
                titleLarge = systemStyle(size = 20, lineHeight = 25),
                titleMedium = systemStyle(size = 17, weight = FontWeight.SemiBold, lineHeight = 22),
                titleSmall = systemStyle(size = 15, weight = FontWeight.SemiBold, lineHeight = 20),
                bodyLarge = systemStyle(size = 17, lineHeight = 22),
                bodyMedium = systemStyle(size = 15, lineHeight = 20),
                bodySmall = systemStyle(size = 13, lineHeight = 18),
                labelLarge = systemStyle(size = 17, weight = FontWeight.SemiBold, lineHeight = 22),
                labelMedium = systemStyle(size = 12, weight = FontWeight.Medium, lineHeight = 16),
                labelSmall = systemStyle(size = 11, weight = FontWeight.Medium, lineHeight = 14),
            ),
    )

internal val NutsNewsShapesDefaults =
    Shapes(
        extraSmall = RoundedCornerShape(NutsNewsRadiiDefaults.xs),
        small = RoundedCornerShape(NutsNewsRadiiDefaults.small),
        medium = RoundedCornerShape(NutsNewsRadiiDefaults.medium),
        large = RoundedCornerShape(NutsNewsRadiiDefaults.large),
        extraLarge = RoundedCornerShape(NutsNewsRadiiDefaults.xl),
    )
