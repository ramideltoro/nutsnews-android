package com.nutsnews.app.feature.personalization

import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
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
class PersonalizationPermissionTest {
    @Test
    fun deniedNotificationPermissionTurnsTheOptionalReminderBackOff() =
        runBlocking {
            val repository = InMemoryUserPreferencesRepository()
            val viewModel = PersonalizationViewModel(repository)
            viewModel.uiState.first { state -> !state.isLoading }

            viewModel.onReminderEnabledChanged(true)
            repository.preferences.first { preferences ->
                preferences.reminder.enabled
            }
            viewModel.onNotificationPermissionDenied()

            assertFalse(viewModel.uiState.value.reminderEnabled)
            assertTrue(
                viewModel.uiState.value.statusText.contains(
                    "permission denied",
                    ignoreCase = true,
                ),
            )
            assertFalse(
                repository.preferences.first { preferences ->
                    !preferences.reminder.enabled
                }.reminder.enabled,
            )
        }
}
