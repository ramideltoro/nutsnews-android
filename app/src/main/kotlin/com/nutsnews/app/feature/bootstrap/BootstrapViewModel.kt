package com.nutsnews.app.feature.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppNavigator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BootstrapViewModel(
    private val navigator: AppNavigator,
) : ViewModel() {
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

    fun onNavigateUp(): Boolean = navigator.navigateUp()

    private fun toUiState(backStack: List<AppDestination>): BootstrapUiState =
        BootstrapUiState(
            destination = backStack.last(),
            canNavigateUp = backStack.size > 1,
        )

    class Factory(
        private val navigator: AppNavigator,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BootstrapViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return BootstrapViewModel(navigator) as T
        }
    }
}
