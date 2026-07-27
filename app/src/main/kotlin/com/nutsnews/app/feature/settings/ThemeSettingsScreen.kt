package com.nutsnews.app.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsPalette
import com.nutsnews.app.designsystem.NutsNewsPalettes
import com.nutsnews.app.designsystem.NutsNewsTheme
import kotlinx.coroutines.launch

@Composable
fun ThemeSettingsScreen(
    selectedTheme: NutsNewsAppTheme,
    onThemeSelected: (NutsNewsAppTheme) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayedTheme by remember { mutableStateOf(selectedTheme) }
    var glowTheme by remember { mutableStateOf<NutsNewsAppTheme?>(null) }
    var glowSequence by remember { mutableIntStateOf(0) }
    val glowProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTheme) {
        displayedTheme = selectedTheme
    }

    val selectTheme: (NutsNewsAppTheme) -> Unit = { theme ->
        if (theme != displayedTheme) {
            val sequence = glowSequence + 1
            val previousTheme = displayedTheme
            glowSequence = sequence
            glowTheme = previousTheme
            displayedTheme = theme
            onThemeSelected(theme)
            scope.launch {
                glowProgress.snapTo(1f)
                glowTheme = theme
                glowProgress.animateTo(
                    targetValue = 0f,
                    animationSpec =
                        tween(
                            durationMillis = ThemeGlowDurationMillis,
                            easing = FastOutSlowInEasing,
                        ),
                )
                if (glowSequence == sequence) {
                    glowTheme = null
                }
            }
        }
    }

    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("theme_settings_screen"),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
        ) {
            ThemeSettingsTopBar(
                glowTheme = glowTheme,
                glowProgress = glowProgress.value,
                onBack = onBack,
                onGoHome = onGoHome,
            )
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("theme_settings_list"),
                contentPadding = PaddingValues(NutsNewsTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
            ) {
                items(
                    items = NutsNewsAppTheme.entries,
                    key = NutsNewsAppTheme::rawValue,
                ) { theme ->
                    ThemeOptionRow(
                        theme = theme,
                        isSelected = theme == displayedTheme,
                        glowTheme = glowTheme,
                        glowProgress = glowProgress.value,
                        onClick = { selectTheme(theme) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingsTopBar(
    glowTheme: NutsNewsAppTheme?,
    glowProgress: Float,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = NutsNewsTheme.spacing.small),
    ) {
        ThemeToolbarButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Settings",
            testTag = "theme_settings_back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = "Theme",
            modifier = Modifier.align(Alignment.Center),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
        ThemeToolbarButton(
            icon = Icons.Filled.Home,
            contentDescription = "Go home",
            testTag = "theme_settings_home",
            glowTheme = glowTheme,
            glowProgress = glowProgress,
            onClick = onGoHome,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ThemeToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowTheme: NutsNewsAppTheme? = null,
    glowProgress: Float = 0f,
) {
    val glowColor =
        glowTheme
            ?.let(NutsNewsPalettes::forTheme)
            ?.accent
            ?: Color.Transparent
    val shape = CircleShape
    Surface(
        modifier =
            modifier
                .size(40.dp)
                .shadow(
                    elevation = (22f * glowProgress).dp,
                    shape = shape,
                    ambientColor = glowColor.copy(alpha = glowProgress * 0.72f),
                    spotColor = glowColor.copy(alpha = glowProgress * 0.72f),
                ).testTag(testTag),
        onClick = onClick,
        shape = shape,
        color = NutsNewsTheme.colors.badgeBackground,
        border =
            BorderStroke(
                if (glowProgress > 0f) {
                    NutsNewsTheme.borders.glow
                } else {
                    NutsNewsTheme.borders.hairline
                },
                if (glowProgress > 0f) {
                    glowColor.copy(alpha = glowProgress * 0.86f)
                } else {
                    NutsNewsTheme.colors.cardBorder
                },
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
                tint = NutsNewsTheme.colors.accentHighlight,
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    theme: NutsNewsAppTheme,
    isSelected: Boolean,
    glowTheme: NutsNewsAppTheme?,
    glowProgress: Float,
    onClick: () -> Unit,
) {
    val palette = NutsNewsPalettes.forTheme(theme)
    val glowColor =
        glowTheme
            ?.let(NutsNewsPalettes::forTheme)
            ?.accent
            ?: Color.Transparent
    val shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = (22f * glowProgress).dp,
                    shape = shape,
                    ambientColor = glowColor.copy(alpha = glowProgress * 0.74f),
                    spotColor = glowColor.copy(alpha = glowProgress * 0.74f),
                ).clip(shape)
                .background(palette.backgroundGradient.first())
                .border(
                    width =
                        when {
                            glowProgress > 0f -> NutsNewsTheme.borders.glow
                            isSelected -> NutsNewsTheme.borders.selected
                            else -> NutsNewsTheme.borders.hairline
                        },
                    color =
                        when {
                            glowProgress > 0f ->
                                glowColor.copy(alpha = glowProgress * 0.88f)

                            isSelected -> palette.accent
                            else -> palette.cardBorder
                        },
                    shape = shape,
                ).clickable(
                    role = Role.RadioButton,
                    onClick = onClick,
                ).semantics {
                    contentDescription =
                        if (isSelected) {
                            "${theme.title} theme, selected"
                        } else {
                            "${theme.title} theme"
                        }
                }.testTag("theme_option_${theme.rawValue}")
                .padding(NutsNewsTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeRadioButton(
            isSelected = isSelected,
            palette = palette,
        )
        Icon(
            imageVector = theme.icon,
            contentDescription = null,
            modifier =
                Modifier
                    .size(22.dp)
                    .testTag("theme_icon_${theme.rawValue}"),
            tint = palette.accent,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = theme.title,
                color = palette.primaryText,
                style = NutsNewsTheme.typography.headline,
                maxLines = 1,
            )
            Text(
                text = theme.description,
                color = palette.secondaryText,
                style = NutsNewsTheme.typography.caption2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ThemePreviewSwatch(
            palette = palette,
            testTag = "theme_swatch_${theme.rawValue}",
        )
    }
}

@Composable
private fun ThemeRadioButton(
    isSelected: Boolean,
    palette: NutsNewsPalette,
) {
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) palette.accent else palette.cardBorder,
                    shape = CircleShape,
                ).testTag(
                    if (isSelected) {
                        "theme_radio_selected"
                    } else {
                        "theme_radio_unselected"
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .background(palette.accent, CircleShape),
            )
        }
    }
}

@Composable
private fun ThemePreviewSwatch(
    palette: NutsNewsPalette,
    testTag: String,
) {
    Column(
        modifier =
            Modifier
                .width(92.dp)
                .clip(RoundedCornerShape(NutsNewsTheme.radii.small))
                .background(palette.cardBackgroundStrong)
                .border(
                    width = NutsNewsTheme.borders.hairline,
                    color = palette.cardBorder,
                    shape = RoundedCornerShape(NutsNewsTheme.radii.small),
                ).testTag(testTag)
                .padding(NutsNewsTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(58.dp)
                    .height(6.dp)
                    .background(
                        palette.primaryText,
                        RoundedCornerShape(NutsNewsTheme.radii.xs),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        palette.secondaryText,
                        RoundedCornerShape(NutsNewsTheme.radii.xs),
                    ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs)) {
            Box(
                modifier =
                    Modifier
                        .width(28.dp)
                        .height(8.dp)
                        .background(palette.accent, CircleShape),
            )
            Box(
                modifier =
                    Modifier
                        .width(18.dp)
                        .height(8.dp)
                        .background(
                            palette.secondaryText.copy(alpha = 0.55f),
                            CircleShape,
                        ),
            )
        }
    }
}

private val NutsNewsAppTheme.icon: ImageVector
    get() =
        when (this) {
            NutsNewsAppTheme.Amber -> Icons.Filled.WbSunny
            NutsNewsAppTheme.Sakura -> Icons.Filled.LocalFlorist
            NutsNewsAppTheme.SaaS -> Icons.Filled.Bolt
            NutsNewsAppTheme.Foxy -> Icons.Filled.AutoAwesome
            NutsNewsAppTheme.Friday -> Icons.Filled.AutoFixHigh
            NutsNewsAppTheme.Bambi -> Icons.Filled.Eco
        }

internal const val ThemeGlowDurationMillis = 1_000

@Preview(showBackground = true)
@Composable
private fun ThemeSettingsPreview() {
    NutsNewsTheme(updateSystemBars = false) {
        ThemeSettingsScreen(
            selectedTheme = NutsNewsAppTheme.Amber,
            onThemeSelected = {},
            onBack = {},
            onGoHome = {},
        )
    }
}
