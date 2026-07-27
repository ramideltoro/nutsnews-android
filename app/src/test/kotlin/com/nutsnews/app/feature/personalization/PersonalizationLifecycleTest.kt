package com.nutsnews.app.feature.personalization

import androidx.lifecycle.SavedStateHandle
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferenceDefaults
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalizationLifecycleTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unsavedTopicsAndReminderTimeSurviveProcessRecreationUntilDiscarded() =
        runTest(dispatcher) {
            val repository = InMemoryUserPreferencesRepository()
            val savedState = SavedStateHandle()
            val original =
                PersonalizationViewModel(
                    userPreferencesRepository = repository,
                    savedStateHandle = savedState,
                )
            runCurrent()

            original.onTopicToggled("animals")
            original.onReminderHourSelected(15)

            val recreated =
                PersonalizationViewModel(
                    userPreferencesRepository = repository,
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf(
                                PersonalizationHasDraftStateKey to
                                    savedState.get<Boolean>(PersonalizationHasDraftStateKey),
                                PersonalizationTopicsStateKey to
                                    savedState.get<ArrayList<String>>(
                                        PersonalizationTopicsStateKey,
                                    ),
                                PersonalizationMoodStateKey to
                                    savedState.get<String>(PersonalizationMoodStateKey),
                                PersonalizationGoalStateKey to
                                    savedState.get<Int>(PersonalizationGoalStateKey),
                                PersonalizationReminderEnabledStateKey to
                                    savedState.get<Boolean>(
                                        PersonalizationReminderEnabledStateKey,
                                    ),
                                PersonalizationReminderHourStateKey to
                                    savedState.get<Int>(PersonalizationReminderHourStateKey),
                            ),
                        ),
                )
            runCurrent()

            assertFalse(recreated.uiState.value.isLoading)
            assertTrue(recreated.uiState.value.hasUnsavedChanges)
            assertEquals(
                UserPreferenceDefaults.DefaultTopicIds - "animals",
                recreated.uiState.value.selectedTopicIds,
            )
            assertEquals(15, recreated.uiState.value.reminderHour)
            assertEquals(
                UserPreferenceDefaults.DefaultTopicIds,
                repository.preferences.first().selectedTopicIds,
            )
            assertEquals(
                UserPreferenceDefaults.DefaultReminderHour,
                repository.preferences.first().reminder.hour,
            )

            recreated.discardChanges()

            assertFalse(recreated.uiState.value.hasUnsavedChanges)
            assertEquals(
                UserPreferenceDefaults.DefaultTopicIds,
                recreated.uiState.value.selectedTopicIds,
            )
        }
}
