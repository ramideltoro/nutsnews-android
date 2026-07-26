package com.nutsnews.app.feature.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootstrapViewModel(
    private val navigator: AppNavigator,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private var selectedTheme = NutsNewsAppTheme.Default
    private val mutableUiState =
        MutableStateFlow(
            toUiState(
                backStack = navigator.backStack.value,
                theme = selectedTheme,
            ),
        )
    val uiState: StateFlow<BootstrapUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            navigator.backStack.collect { backStack ->
                mutableUiState.value =
                    toUiState(
                        backStack = backStack,
                        theme = selectedTheme,
                    )
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.preferences
                .distinctUntilChanged()
                .collect { preferences ->
                    selectedTheme = preferences.theme
                    navigator.applyOnboardingStatus(
                        preferences.hasCompletedOnboarding,
                    )
                    mutableUiState.value =
                        toUiState(
                            backStack = navigator.backStack.value,
                            theme = selectedTheme,
                        )
                }
        }
    }

    fun onDestinationRequested(destination: AppDestination) {
        navigator.navigate(destination)
    }

    fun onHelpDestinationRequested(destination: AppDestination): Boolean =
        navigator.navigateFromHelp(destination)

    fun onNavigateUp(): Boolean = navigator.navigateUp()

    fun onHomeRequested() {
        navigator.resetTo(AppDestination.Feed)
    }

    fun onDailyReminderNotificationOpened() {
        viewModelScope.launch {
            val resolvedDestination =
                uiState.first { state ->
                    state.destination != AppDestination.Startup
                }.destination
            if (resolvedDestination != AppDestination.Onboarding) {
                navigator.navigate(AppDestination.DailyDigest)
            }
        }
    }

    private fun toUiState(
        backStack: List<AppDestination>,
        theme: NutsNewsAppTheme,
    ): BootstrapUiState =
        BootstrapUiState(
            destination = backStack.last(),
            canNavigateUp = backStack.size > 1,
            presentation = backStack.last().presentation,
            returnDestination = backStack.dropLast(1).lastOrNull(),
            theme = theme,
        )

    class Factory(
        private val navigator: AppNavigator,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BootstrapViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return BootstrapViewModel(navigator, userPreferencesRepository) as T
        }
    }
}
