package com.nutsnews.app.feature.bootstrap

import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppPresentation
import com.nutsnews.app.navigation.DefaultAppNavigator
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class BootstrapViewModelTest {
    @Test
    fun firstRunRoutesToOnboardingAndCompletionResetsToFeed() =
        runBlocking {
            val navigator = DefaultAppNavigator()
            val preferences = InMemoryUserPreferencesRepository()
            val viewModel = BootstrapViewModel(navigator, preferences)

            val onboardingState =
                viewModel.uiState.first { state ->
                    state.destination == AppDestination.Onboarding
                }
            assertFalse(onboardingState.canNavigateUp)
            assertEquals(AppPresentation.Root, onboardingState.presentation)

            preferences.setOnboardingCompleted(true)

            val feedState =
                viewModel.uiState.first { state ->
                    state.destination == AppDestination.Feed
                }
            assertFalse(feedState.canNavigateUp)
            assertEquals(AppPresentation.Root, feedState.presentation)
        }

    @Test
    fun returningUserKeepsRestoredFeatureStackAndShellActionsNavigate() =
        runBlocking {
            val navigator = DefaultAppNavigator(AppDestination.Feed)
            navigator.navigate(AppDestination.Settings)
            navigator.navigate(AppDestination.ThemePicker)
            val preferences =
                InMemoryUserPreferencesRepository(
                    UserPreferences(
                        hasCompletedOnboarding = true,
                        theme = NutsNewsAppTheme.Friday,
                    ),
                )
            val viewModel = BootstrapViewModel(navigator, preferences)

            val restoredState =
                viewModel.uiState.first { state ->
                    state.destination == AppDestination.ThemePicker
                }
            assertTrue(restoredState.canNavigateUp)
            assertEquals(AppPresentation.Stack, restoredState.presentation)
            assertEquals(AppDestination.Settings, restoredState.returnDestination)
            assertEquals(NutsNewsAppTheme.Friday, restoredState.theme)

            assertTrue(viewModel.onNavigateUp())
            assertEquals(
                AppDestination.Settings,
                viewModel.uiState.first { it.destination == AppDestination.Settings }.destination,
            )

            viewModel.onHomeRequested()
            assertEquals(
                AppDestination.Feed,
                viewModel.uiState.first { it.destination == AppDestination.Feed }.destination,
            )
        }

    @Test
    fun reminderNotificationTapOpensDailyDigestAfterStartupResolves() =
        runBlocking {
            val navigator = DefaultAppNavigator()
            val preferences =
                InMemoryUserPreferencesRepository(
                    UserPreferences(hasCompletedOnboarding = true),
                )
            val viewModel = BootstrapViewModel(navigator, preferences)

            viewModel.onDailyReminderNotificationOpened()

            val openedState =
                viewModel.uiState.first { state ->
                    state.destination == AppDestination.DailyDigest
                }
            assertEquals(AppDestination.DailyDigest, openedState.destination)
            assertEquals(AppDestination.Feed, openedState.returnDestination)
        }

    @Test
    fun reminderNotificationTapDoesNotBypassFirstRunOnboarding() =
        runBlocking {
            val navigator = DefaultAppNavigator()
            val viewModel =
                BootstrapViewModel(
                    navigator = navigator,
                    userPreferencesRepository = InMemoryUserPreferencesRepository(),
                )

            viewModel.onDailyReminderNotificationOpened()

            viewModel.uiState.first { state ->
                state.destination == AppDestination.Onboarding
            }
            assertEquals(
                listOf(AppDestination.Onboarding),
                navigator.backStack.value,
            )
        }
}
