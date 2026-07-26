package com.nutsnews.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface AppNavigator {
    val backStack: StateFlow<List<AppDestination>>

    fun navigate(destination: AppDestination)

    fun navigateUp(): Boolean

    fun resetTo(destination: AppDestination)
}

class DefaultAppNavigator(
    startDestination: AppDestination = AppDestination.Startup,
) : AppNavigator {
    private val mutableBackStack = MutableStateFlow(listOf(startDestination))

    override val backStack: StateFlow<List<AppDestination>> =
        mutableBackStack.asStateFlow()

    override fun navigate(destination: AppDestination) {
        mutableBackStack.update { current ->
            if (current.last() == destination) current else current + destination
        }
    }

    override fun navigateUp(): Boolean {
        var navigated = false
        mutableBackStack.update { current ->
            if (current.size == 1) {
                current
            } else {
                navigated = true
                current.dropLast(1)
            }
        }
        return navigated
    }

    override fun resetTo(destination: AppDestination) {
        mutableBackStack.value = listOf(destination)
    }
}
