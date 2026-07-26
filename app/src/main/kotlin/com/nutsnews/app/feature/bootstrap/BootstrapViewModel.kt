package com.nutsnews.app.feature.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppNavigator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BootstrapViewModel(
    private val navigator: AppNavigator,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            userPreferencesRepository.hasCompletedOnboarding
                .distinctUntilChanged()
                .collect(navigator::applyOnboardingStatus)
        }
    }

    val uiState: StateFlow<BootstrapUiState> =
        navigator.backStack
            .map(::toUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = toUiState(navigator.backStack.value),
            )

    fun onDestinationRequested(destination: AppDestination) {
        navigator.navigate(destination)
    }

    fun onHelpDestinationRequested(destination: AppDestination): Boolean =
        navigator.navigateFromHelp(destination)

    fun onNavigateUp(): Boolean = navigator.navigateUp()

    fun onHomeRequested() {
        navigator.resetTo(AppDestination.Feed)
    }

    private fun toUiState(backStack: List<AppDestination>): BootstrapUiState =
        BootstrapUiState(
            destination = backStack.last(),
            canNavigateUp = backStack.size > 1,
            presentation = backStack.last().presentation,
            returnDestination = backStack.dropLast(1).lastOrNull(),
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
