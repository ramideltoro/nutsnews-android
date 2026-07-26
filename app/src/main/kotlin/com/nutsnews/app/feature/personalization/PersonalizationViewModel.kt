package com.nutsnews.app.feature.personalization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.data.preferences.UserPreferenceDefaults
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalizationViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PersonalizationUiState())
    val uiState: StateFlow<PersonalizationUiState> = mutableUiState.asStateFlow()

    private var persistedPreferences = UserPreferences()

    init {
        viewModelScope.launch {
            userPreferencesRepository.preferences.collect { preferences ->
                persistedPreferences = preferences
                mutableUiState.update { current ->
                    if (current.hasUnsavedChanges || current.isSaving) {
                        current
                    } else {
                        PersonalizationUiState.fromPreferences(preferences)
                    }
                }
            }
        }
    }

    fun onTopicToggled(topicId: String) {
        if (topicId !in UserPreferenceDefaults.ValidTopicIds) return
        updateDraft { current ->
            val selectedIds = current.selectedTopicIds
            val updatedIds =
                if (topicId in selectedIds) {
                    if (selectedIds.size == 1) selectedIds else selectedIds - topicId
                } else {
                    selectedIds + topicId
                }
            current.copy(selectedTopicIds = updatedIds)
        }
    }

    fun onMoodSelected(moodId: String) {
        if (moodId !in UserPreferenceDefaults.ValidMoodIds) return
        updateDraft { current ->
            current.copy(selectedMoodId = moodId)
        }
        persistIosBoundEdit { preferences ->
            preferences.copy(selectedMoodId = moodId)
        }
    }

    fun onDailyGoalChanged(dailyGoal: Int) {
        val sanitizedGoal = UserPreferenceDefaults.sanitizeDailyGoal(dailyGoal)
        updateDraft { current ->
            current.copy(dailyGoal = sanitizedGoal)
        }
        persistIosBoundEdit { preferences ->
            preferences.copy(dailyGoal = sanitizedGoal)
        }
    }

    fun onReminderEnabledChanged(enabled: Boolean) {
        updateDraft { current ->
            current.copy(reminderEnabled = enabled)
        }
        persistIosBoundEdit { preferences ->
            preferences.copy(
                reminder = preferences.reminder.copy(enabled = enabled),
            )
        }
    }

    fun onReminderHourSelected(hour: Int) {
        if (hour !in UserPreferenceDefaults.ValidReminderHours) return
        updateDraft { current ->
            current.copy(reminderHour = hour)
        }
    }

    fun onNotificationPermissionDenied() {
        onReminderEnabledChanged(false)
        mutableUiState.update { current ->
            current.copy(
                statusText = "Notification permission denied. Reminder is off.",
            )
        }
    }

    fun discardChanges() {
        mutableUiState.value =
            PersonalizationUiState.fromPreferences(persistedPreferences)
    }

    fun save(onSaved: () -> Unit = {}) {
        val draft = mutableUiState.value
        if (!draft.canSave) return

        mutableUiState.value =
            draft.copy(
                isSaving = true,
                statusText =
                    if (draft.reminderEnabled) {
                        "Scheduling reminder…"
                    } else {
                        ""
                    },
            )
        viewModelScope.launch {
            runCatching {
                userPreferencesRepository.updatePreferences { previous ->
                    draft.toPreferences(previous)
                }
            }.onSuccess {
                val savedPreferences = draft.toPreferences(persistedPreferences)
                persistedPreferences = savedPreferences
                mutableUiState.value =
                    PersonalizationUiState
                        .fromPreferences(savedPreferences)
                        .copy(
                            statusText =
                                if (draft.reminderEnabled) {
                                    "Reminder saved for ${
                                        reminderTime(draft.reminderHour).displayTime
                                    }."
                                } else {
                                    "Reminder off."
                                },
                        )
                onSaved()
            }.onFailure {
                mutableUiState.value =
                    draft.copy(
                        isSaving = false,
                        statusText = "Couldn’t save personalization. Try again.",
                    )
            }
        }
    }

    private fun updateDraft(
        transform: (PersonalizationUiState) -> PersonalizationUiState,
    ) {
        mutableUiState.update { current ->
            if (current.isLoading || current.isSaving) {
                current
            } else {
                transform(current).copy(
                    statusText = "",
                    hasUnsavedChanges = true,
                )
            }
        }
    }

    private fun persistIosBoundEdit(
        transform: (UserPreferences) -> UserPreferences,
    ) {
        if (mutableUiState.value.isLoading || mutableUiState.value.isSaving) return
        persistedPreferences = transform(persistedPreferences)
        viewModelScope.launch {
            runCatching {
                userPreferencesRepository.updatePreferences(transform)
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(
                        statusText = "Couldn’t save personalization. Try again.",
                    )
                }
            }
        }
    }

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PersonalizationViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return PersonalizationViewModel(userPreferencesRepository) as T
        }
    }
}

internal data class ReminderTimeOption(
    val hour: Int,
    val title: String,
    val displayTime: String,
)

internal val ReminderTimeOptions =
    listOf(
        ReminderTimeOption(8, "Morning reset", "8:00 AM"),
        ReminderTimeOption(15, "Afternoon lift", "3:00 PM"),
        ReminderTimeOption(20, "Evening calm", "8:00 PM"),
    )

internal fun reminderTime(hour: Int): ReminderTimeOption =
    ReminderTimeOptions.firstOrNull { option -> option.hour == hour }
        ?: ReminderTimeOptions.first()
