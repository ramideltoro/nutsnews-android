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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsTheme

@Composable
fun HapticsSettingsScreen(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceSettingsScreen(
        title = "Haptics",
        controlTitle = "Like button haptics",
        controlSubtitle = "Feel a soft tap when liking a story.",
        checked = enabled,
        switchTestTag = "haptics_settings_switch",
        onCheckedChange = onEnabledChanged,
        onBack = onBack,
        onGoHome = onGoHome,
        modifier =
            modifier.testTag("haptics_settings_screen"),
    )
}

@Composable
fun WidgetSettingsScreen(
    showStatsOnLargeWidget: Boolean,
    onShowStatsChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceSettingsScreen(
        title = "Widget",
        controlTitle = "Show stats on large widget",
        controlSubtitle =
            "When the large NutsNews widget is on your Home Screen, " +
                "show today’s progress, streak, and total stories.",
        checked = showStatsOnLargeWidget,
        switchTestTag = "widget_settings_switch",
        onCheckedChange = onShowStatsChanged,
        onBack = onBack,
        onGoHome = onGoHome,
        modifier =
            modifier.testTag("widget_settings_screen"),
    )
}

@Composable
private fun PreferenceSettingsScreen(
    title: String,
    controlTitle: String,
    controlSubtitle: String,
    checked: Boolean,
    switchTestTag: String,
    onCheckedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("preference_settings_screen"),
    ) {
        NutsNewsAdaptivePane {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                PreferenceSettingsTopBar(
                    title = title,
                    onBack = onBack,
                    onGoHome = onGoHome,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(NutsNewsTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                ) {
                    item(key = "preference") {
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("${switchTestTag}_card"),
                            shape =
                                RoundedCornerShape(
                                    NutsNewsTheme.dimensions.cardCornerRadius,
                                ),
                            color = NutsNewsTheme.colors.cardBackgroundStrong,
                            border =
                                BorderStroke(
                                    NutsNewsTheme.borders.hairline,
                                    NutsNewsTheme.colors.cardBorder,
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
                                horizontalArrangement =
                                    Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            NutsNewsTheme.spacing.xxs,
                                        ),
                                ) {
                                    Text(
                                        text = controlTitle,
                                        color = NutsNewsTheme.colors.primaryText,
                                        style = NutsNewsTheme.typography.headline,
                                    )
                                    Text(
                                        text = controlSubtitle,
                                        color = NutsNewsTheme.colors.secondaryText,
                                        style = NutsNewsTheme.typography.subheadline,
                                    )
                                }
                                Switch(
                                    checked = checked,
                                    onCheckedChange = onCheckedChange,
                                    modifier = Modifier.testTag(switchTestTag),
                                    colors =
                                        SwitchDefaults.colors(
                                            checkedThumbColor =
                                                NutsNewsTheme.colors.buttonText,
                                            checkedTrackColor =
                                                NutsNewsTheme.colors.accent,
                                            checkedBorderColor =
                                                NutsNewsTheme.colors.accent,
                                            uncheckedThumbColor =
                                                NutsNewsTheme.colors.secondaryText,
                                            uncheckedTrackColor =
                                                NutsNewsTheme.colors.badgeBackground,
                                            uncheckedBorderColor =
                                                NutsNewsTheme.colors.cardBorder,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSettingsTopBar(
    title: String,
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
        PreferenceToolbarButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Settings",
            testTag = "preference_settings_back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
        PreferenceToolbarButton(
            icon = Icons.Filled.Home,
            contentDescription = "Go home",
            testTag = "preference_settings_home",
            onClick = onGoHome,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun PreferenceToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .size(40.dp)
                .testTag(testTag),
        onClick = onClick,
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
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
                tint = NutsNewsTheme.colors.accentHighlight,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HapticsSettingsPreview() {
    NutsNewsTheme(updateSystemBars = false) {
        HapticsSettingsScreen(
            enabled = true,
            onEnabledChanged = {},
            onBack = {},
            onGoHome = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WidgetSettingsPreview() {
    NutsNewsTheme(updateSystemBars = false) {
        WidgetSettingsScreen(
            showStatsOnLargeWidget = true,
            onShowStatsChanged = {},
            onBack = {},
            onGoHome = {},
        )
    }
}
