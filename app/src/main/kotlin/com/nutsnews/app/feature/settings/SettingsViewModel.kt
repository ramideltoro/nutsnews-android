package com.nutsnews.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.widget.NoOpWidgetRefreshRequester
import com.nutsnews.app.widget.WidgetRefreshRequester
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val widgetRefreshRequester: WidgetRefreshRequester =
        NoOpWidgetRefreshRequester,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        userPreferencesRepository.preferences
            .map(SettingsUiState::fromPreferences)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState(),
            )

    fun selectTheme(theme: NutsNewsAppTheme) {
        if (theme == uiState.value.theme) return
        viewModelScope.launch {
            userPreferencesRepository.setTheme(theme)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        if (enabled == uiState.value.hapticsEnabled) return
        viewModelScope.launch {
            userPreferencesRepository.setHapticsEnabled(enabled)
        }
    }

    fun setShowStatsOnLargeWidget(enabled: Boolean) {
        if (enabled == uiState.value.showStatsOnLargeWidget) return
        viewModelScope.launch {
            userPreferencesRepository.setShowStatsOnLargeWidget(enabled)
            widgetRefreshRequester.requestRefresh()
        }
    }

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val widgetRefreshRequester: WidgetRefreshRequester =
            NoOpWidgetRefreshRequester,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(
                userPreferencesRepository = userPreferencesRepository,
                widgetRefreshRequester = widgetRefreshRequester,
            ) as T
        }
    }
}
