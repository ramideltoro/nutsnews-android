package com.nutsnews.app.feature.settings

import androidx.compose.runtime.Immutable
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.designsystem.NutsNewsAppTheme

@Immutable
data class SettingsUiState(
    val isLoading: Boolean = true,
    val theme: NutsNewsAppTheme = NutsNewsAppTheme.Default,
    val hapticsEnabled: Boolean = true,
    val showStatsOnLargeWidget: Boolean = true,
) {
    val hapticsSubtitle: String
        get() = if (hapticsEnabled) "On" else "Off"

    val widgetSubtitle: String
        get() =
            if (showStatsOnLargeWidget) {
                "Large stats on"
            } else {
                "Large stats off"
            }

    companion object {
        fun fromPreferences(preferences: UserPreferences): SettingsUiState =
            SettingsUiState(
                isLoading = false,
                theme = preferences.theme,
                hapticsEnabled = preferences.hapticsEnabled,
                showStatsOnLargeWidget = preferences.showStatsOnLargeWidget,
            )
    }
}
