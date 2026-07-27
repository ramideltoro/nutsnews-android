package com.nutsnews.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
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

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(userPreferencesRepository) as T
        }
    }
}
