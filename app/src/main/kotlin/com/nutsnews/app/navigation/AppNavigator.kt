package com.nutsnews.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface AppNavigator {
    val backStack: StateFlow<List<AppDestination>>

    fun navigate(destination: AppDestination)

    fun navigateFromHelp(destination: AppDestination): Boolean

    fun navigateUp(): Boolean

    fun resetTo(destination: AppDestination)

    fun applyOnboardingStatus(hasCompletedOnboarding: Boolean)

    fun saveState(): String

    fun restoreState(savedState: String?): Boolean
}

class DefaultAppNavigator(
    startDestination: AppDestination = AppDestination.Startup,
) : AppNavigator {
    init {
        require(startDestination.presentation == AppPresentation.Root) {
            "The navigator must start at a root destination."
        }
    }

    private val mutableBackStack = MutableStateFlow(listOf(startDestination))

    override val backStack: StateFlow<List<AppDestination>> =
        mutableBackStack.asStateFlow()

    override fun navigate(destination: AppDestination) {
        if (destination.presentation == AppPresentation.Root) {
            resetTo(destination)
            return
        }
        mutableBackStack.update { current ->
            if (current.last() == destination) current else current + destination
        }
    }

    override fun navigateFromHelp(destination: AppDestination): Boolean {
        if (
            mutableBackStack.value.lastOrNull() != AppDestination.Help ||
            !destination.isHelpLinkedDestination()
        ) {
            return false
        }
        navigate(destination)
        return true
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
        require(destination.presentation == AppPresentation.Root) {
            "Only root destinations can reset the app back stack."
        }
        mutableBackStack.value = listOf(destination)
    }

    override fun applyOnboardingStatus(hasCompletedOnboarding: Boolean) {
        val rootDestination = mutableBackStack.value.first()
        if (!hasCompletedOnboarding) {
            if (rootDestination != AppDestination.Onboarding) {
                resetTo(AppDestination.Onboarding)
            }
        } else if (
            rootDestination == AppDestination.Startup ||
            rootDestination == AppDestination.Onboarding
        ) {
            resetTo(AppDestination.Feed)
        }
    }

    override fun saveState(): String =
        mutableBackStack.value.joinToString(StateSeparator) { destination ->
            destination.route
        }

    override fun restoreState(savedState: String?): Boolean {
        if (savedState.isNullOrBlank()) {
            return false
        }
        val restoredBackStack =
            savedState
                .split(StateSeparator)
                .map { route -> AppDestination.fromRoute(route) ?: return false }
        if (
            restoredBackStack.isEmpty() ||
            restoredBackStack.first().presentation != AppPresentation.Root ||
            restoredBackStack.drop(1).any { destination ->
                destination.presentation == AppPresentation.Root
            }
        ) {
            return false
        }
        mutableBackStack.value = restoredBackStack
        return true
    }

    private fun AppDestination.isHelpLinkedDestination(): Boolean =
        when (this) {
            AppDestination.DailyDigest,
            AppDestination.GoodMood,
            AppDestination.ReadingStats,
            AppDestination.SavedStories,
            AppDestination.ArchiveSearch,
            AppDestination.Personalization,
            is AppDestination.ArticleDetail,
            -> true

            else -> false
        }

    private companion object {
        const val StateSeparator = "\n"
    }
}
