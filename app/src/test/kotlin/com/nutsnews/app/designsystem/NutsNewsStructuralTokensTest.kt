package com.nutsnews.app.designsystem

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.assertEquals
import org.junit.Test

class NutsNewsStructuralTokensTest {
    @Test
    fun spacingRadiiAndDimensionsMatchIosPointsOneForOne() {
        assertEquals(
            listOf(4.dp, 6.dp, 10.dp, 16.dp, 26.dp, 42.dp),
            NutsNewsSpacingDefaults.run { listOf(xxs, xs, small, medium, large, xl) },
        )
        assertEquals(
            listOf(6.dp, 10.dp, 16.dp, 26.dp, 42.dp),
            NutsNewsRadiiDefaults.run { listOf(xs, small, medium, large, xl) },
        )
        assertEquals(16.dp, NutsNewsDimensionsDefaults.cardPadding)
        assertEquals(26.dp, NutsNewsDimensionsDefaults.cardCornerRadius)
        assertEquals(16.dp, NutsNewsDimensionsDefaults.imageCornerRadius)
        assertEquals(16.dp, NutsNewsDimensionsDefaults.controlCornerRadius)
        assertEquals(8.dp, NutsNewsDimensionsDefaults.chipVerticalPadding)
        assertEquals(13.dp, NutsNewsDimensionsDefaults.chipHorizontalPadding)
        assertEquals(188.dp, NutsNewsDimensionsDefaults.feedImageHeight)
        assertEquals(210.dp, NutsNewsDimensionsDefaults.detailHeroHeight)
        assertEquals(1.61803398875, NutsNewsPhi)
    }

    @Test
    fun bordersAndShadowsCoverIosSurfaceTreatments() {
        assertEquals(
            listOf(1.dp, 1.2.dp, 1.7.dp, 2.2.dp),
            NutsNewsBordersDefaults.run { listOf(hairline, emphasized, selected, glow) },
        )
        assertEquals(14.dp, NutsNewsShadowsDefaults.cardBlurRadius)
        assertEquals(8.dp, NutsNewsShadowsDefaults.cardOffsetY)
        assertEquals(10.dp, NutsNewsShadowsDefaults.buttonBlurRadius)
        assertEquals(4.dp, NutsNewsShadowsDefaults.buttonOffsetY)
        assertEquals(22.dp, NutsNewsShadowsDefaults.themeGlowRadius)
        assertEquals(34.1.dp, NutsNewsShadowsDefaults.themeGlowOuterRadius)
    }

    @Test
    fun typographyMatchesSwiftUiSemanticAndBrandRoles() {
        val typography = NutsNewsTypographyDefaults

        assertEquals(
            listOf(34.sp, 28.sp, 22.sp, 20.sp, 17.sp, 17.sp, 16.sp, 15.sp, 13.sp, 12.sp, 11.sp),
            typography.run {
                listOf(
                    largeTitle.fontSize,
                    title.fontSize,
                    title2.fontSize,
                    title3.fontSize,
                    headline.fontSize,
                    body.fontSize,
                    callout.fontSize,
                    subheadline.fontSize,
                    footnote.fontSize,
                    caption.fontSize,
                    caption2.fontSize,
                )
            },
        )
        assertEquals(38.sp, typography.brandHero.fontSize)
        assertEquals(FontWeight.SemiBold, typography.brandHero.fontWeight)
        assertEquals(31.sp, typography.brandTitle.fontSize)
        assertEquals(FontWeight.Light, typography.brandTitle.fontWeight)
        assertEquals(FontFamily.Serif, typography.brandTitle.fontFamily)
        assertEquals(20.sp, typography.cardTitle.fontSize)
        assertEquals(FontWeight.Bold, typography.cardTitle.fontWeight)
        assertEquals(26.sp, typography.metric.fontSize)
        assertEquals(17.sp, typography.button.fontSize)
        assertEquals(13.sp, typography.label.fontSize)
    }
}
