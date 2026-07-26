package com.nutsnews.app.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun NutsNewsThemePreview(
    theme: NutsNewsAppTheme,
    modifier: Modifier = Modifier,
) {
    NutsNewsTheme(
        theme = theme,
        updateSystemBars = false,
    ) {
        val colors = NutsNewsTheme.colors
        val spacing = NutsNewsTheme.spacing
        val radii = NutsNewsTheme.radii
        val typography = NutsNewsTheme.typography

        NutsNewsBackground(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Text(
                        text = theme.title.uppercase(),
                        color = colors.accent,
                        style = typography.label,
                    )
                    Text(
                        text = "NutsNews",
                        color = colors.primaryText,
                        style = typography.brandTitle,
                    )
                    Text(
                        text = theme.description,
                        color = colors.secondaryText,
                        style = typography.subheadline,
                    )
                }

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = NutsNewsTheme.shadows.cardBlurRadius,
                                shape = RoundedCornerShape(radii.large),
                                ambientColor = colors.accentGlow,
                                spotColor = colors.accentGlow,
                            ),
                    shape = RoundedCornerShape(radii.large),
                    color = colors.cardBackground,
                    contentColor = colors.primaryText,
                    border = BorderStroke(NutsNewsTheme.borders.hairline, colors.cardBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(NutsNewsTheme.dimensions.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium),
                    ) {
                        Text(
                            text = "TODAY'S GOOD-NEWS RESET",
                            color = colors.accent,
                            style = typography.label,
                        )
                        Text(
                            text = "Good news, beautifully themed.",
                            color = colors.primaryText,
                            style = typography.cardTitle,
                        )
                        Text(
                            text = "Every surface, border, shadow, and type role follows the frozen iOS palette.",
                            color = colors.secondaryText,
                            style = typography.body,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                            PreviewChip(text = "Calm")
                            PreviewChip(text = "Saved")
                            PreviewChip(text = "Notes")
                        }

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(radii.medium))
                                    .background(nutsNewsButtonGradient())
                                    .padding(
                                        horizontal = spacing.medium,
                                        vertical = spacing.small,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Continue",
                                color = colors.buttonText,
                                style = typography.button,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                PaletteStrip()
            }
        }
    }
}

@Composable
fun NutsNewsThemePreviewGallery(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        NutsNewsAppTheme.entries.forEach { theme ->
            NutsNewsThemePreview(
                theme = theme,
                modifier = Modifier.height(760.dp),
            )
        }
    }
}

@Composable
private fun PreviewChip(text: String) {
    val colors = NutsNewsTheme.colors
    Text(
        text = text,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(colors.badgeBackground)
                .border(
                    width = NutsNewsTheme.borders.hairline,
                    color = colors.cardBorder,
                    shape = CircleShape,
                )
                .padding(
                    horizontal = NutsNewsTheme.dimensions.chipHorizontalPadding,
                    vertical = NutsNewsTheme.dimensions.chipVerticalPadding,
                ),
        color = colors.accentHighlight,
        style = NutsNewsTheme.typography.caption,
    )
}

@Composable
private fun PaletteStrip() {
    val colors = NutsNewsTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            colors.accent,
            colors.accentRich,
            colors.accentDeep,
            colors.accentSoft,
            colors.cardBackgroundStrong,
            colors.primaryText,
        ).forEachIndexed { index, color ->
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = NutsNewsTheme.borders.hairline,
                            color = colors.cardBorder,
                            shape = CircleShape,
                        ),
            )
            if (index != 5) {
                Spacer(modifier = Modifier.width(NutsNewsTheme.spacing.xs))
            }
        }
    }
}

@Preview(name = "Amber", widthDp = 393, heightDp = 852)
@Composable
private fun AmberThemePreview() = NutsNewsThemePreview(NutsNewsAppTheme.Amber)

@Preview(name = "Sakura", widthDp = 393, heightDp = 852)
@Composable
private fun SakuraThemePreview() = NutsNewsThemePreview(NutsNewsAppTheme.Sakura)

@Preview(name = "SaaS", widthDp = 393, heightDp = 852)
@Composable
private fun SaaSThemePreview() = NutsNewsThemePreview(NutsNewsAppTheme.SaaS)

@Preview(name = "Foxy", widthDp = 393, heightDp = 852)
@Composable
private fun FoxyThemePreview() = NutsNewsThemePreview(NutsNewsAppTheme.Foxy)

@Preview(name = "Friday", widthDp = 393, heightDp = 852)
@Composable
private fun FridayThemePreview() = NutsNewsThemePreview(NutsNewsAppTheme.Friday)

@Preview(name = "Bambi", widthDp = 393, heightDp = 852)
@Composable
private fun BambiThemePreview() = NutsNewsThemePreview(NutsNewsAppTheme.Bambi)
