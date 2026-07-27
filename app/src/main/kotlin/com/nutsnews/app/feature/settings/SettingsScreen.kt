package com.nutsnews.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsTheme
import java.util.Locale

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAppearance: () -> Unit,
    onHaptics: () -> Unit,
    onWidget: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("settings_screen"),
    ) {
        NutsNewsAdaptivePane {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                SettingsTopBar(onGoHome = onGoHome)
                if (uiState.isLoading) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .testTag("settings_loading"),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = NutsNewsTheme.colors.accent,
                        )
                    }
                } else {
                    SettingsContent(
                        uiState = uiState,
                        onAppearance = onAppearance,
                        onHaptics = onHaptics,
                        onWidget = onWidget,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onGoHome: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = NutsNewsTheme.spacing.small),
    ) {
        Text(
            text = "Settings",
            modifier = Modifier.align(Alignment.Center),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .testTag("settings_home"),
            onClick = onGoHome,
            shape = CircleShape,
            color = NutsNewsTheme.colors.badgeBackground,
            border =
                BorderStroke(
                    NutsNewsTheme.borders.hairline,
                    NutsNewsTheme.colors.cardBorder,
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Go home",
                    modifier = Modifier.size(18.dp),
                    tint = NutsNewsTheme.colors.accentHighlight,
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onAppearance: () -> Unit,
    onHaptics: () -> Unit,
    onWidget: () -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("settings_list"),
        contentPadding = PaddingValues(NutsNewsTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        item(key = "theme") {
            SettingsRow(
                icon = Icons.Filled.Palette,
                title = "Theme",
                subtitle = uiState.theme.title,
                testTag = "settings_row_theme",
                onClick = onAppearance,
            )
        }
        item(key = "haptics") {
            SettingsRow(
                icon = Icons.Filled.Vibration,
                title = "Haptics",
                subtitle = uiState.hapticsSubtitle,
                testTag = "settings_row_haptics",
                onClick = onHaptics,
            )
        }
        item(key = "widget") {
            SettingsRow(
                icon = Icons.Filled.Widgets,
                title = "Widget",
                subtitle = uiState.widgetSubtitle,
                testTag = "settings_row_widget",
                onClick = onWidget,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag),
        onClick = onClick,
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = NutsNewsTheme.colors.cardBackgroundStrong,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Row(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = NutsNewsTheme.colors.badgeBackground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = NutsNewsTheme.colors.accentHighlight,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            ) {
                Text(
                    text = title,
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.headline,
                )
                Text(
                    text = subtitle,
                    modifier =
                        Modifier.testTag(
                            "settings_subtitle_${title.lowercase(Locale.ROOT)}",
                        ),
                    color = NutsNewsTheme.colors.secondaryText,
                    style = NutsNewsTheme.typography.subheadline,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = NutsNewsTheme.colors.mutedText,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    NutsNewsTheme(updateSystemBars = false) {
        SettingsScreen(
            uiState =
                SettingsUiState(
                    isLoading = false,
                    theme = NutsNewsAppTheme.Amber,
                    hapticsEnabled = true,
                    showStatsOnLargeWidget = true,
                ),
            onAppearance = {},
            onHaptics = {},
            onWidget = {},
            onGoHome = {},
        )
    }
}
