package com.nutsnews.app.feature.personalization

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.nutsnews.app.data.preferences.UserPreferenceDefaults
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.widget.NoOpWidgetRefreshRequester
import com.nutsnews.app.widget.WidgetRefreshRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalizationViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val widgetRefreshRequester: WidgetRefreshRequester =
        NoOpWidgetRefreshRequester,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(
            restorePersonalizationDraft(savedStateHandle)
                ?: PersonalizationUiState(),
        )
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
        persistIosBoundEdit(refreshWidget = true) { preferences ->
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
        clearSavedDraft()
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
                widgetRefreshRequester.requestRefresh()
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
                clearSavedDraft()
                onSaved()
            }.onFailure {
                mutableUiState.value =
                    draft.copy(
                        isSaving = false,
                        statusText = "Couldn’t save personalization. Try again.",
                    )
                persistDraft()
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
        persistDraft()
    }

    private fun persistIosBoundEdit(
        refreshWidget: Boolean = false,
        transform: (UserPreferences) -> UserPreferences,
    ) {
        if (mutableUiState.value.isLoading || mutableUiState.value.isSaving) return
        persistedPreferences = transform(persistedPreferences)
        viewModelScope.launch {
            runCatching {
                userPreferencesRepository.updatePreferences(transform)
            }.onSuccess {
                if (refreshWidget) {
                    widgetRefreshRequester.requestRefresh()
                }
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
        private val widgetRefreshRequester: WidgetRefreshRequester =
            NoOpWidgetRefreshRequester,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PersonalizationViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return PersonalizationViewModel(
                userPreferencesRepository = userPreferencesRepository,
                widgetRefreshRequester = widgetRefreshRequester,
            ) as T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T {
            require(modelClass.isAssignableFrom(PersonalizationViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return PersonalizationViewModel(
                userPreferencesRepository = userPreferencesRepository,
                widgetRefreshRequester = widgetRefreshRequester,
                savedStateHandle = extras.createSavedStateHandle(),
            ) as T
        }
    }

    private fun persistDraft() {
        val state = mutableUiState.value
        if (!state.hasUnsavedChanges) {
            clearSavedDraft()
            return
        }
        savedStateHandle[PersonalizationHasDraftStateKey] = true
        savedStateHandle[PersonalizationTopicsStateKey] = ArrayList(state.selectedTopicIds)
        savedStateHandle[PersonalizationMoodStateKey] = state.selectedMoodId
        savedStateHandle[PersonalizationGoalStateKey] = state.dailyGoal
        savedStateHandle[PersonalizationReminderEnabledStateKey] = state.reminderEnabled
        savedStateHandle[PersonalizationReminderHourStateKey] = state.reminderHour
    }

    private fun clearSavedDraft() {
        savedStateHandle.remove<Boolean>(PersonalizationHasDraftStateKey)
        savedStateHandle.remove<ArrayList<String>>(PersonalizationTopicsStateKey)
        savedStateHandle.remove<String>(PersonalizationMoodStateKey)
        savedStateHandle.remove<Int>(PersonalizationGoalStateKey)
        savedStateHandle.remove<Boolean>(PersonalizationReminderEnabledStateKey)
        savedStateHandle.remove<Int>(PersonalizationReminderHourStateKey)
    }
}

private fun restorePersonalizationDraft(
    savedStateHandle: SavedStateHandle,
): PersonalizationUiState? {
    if (
        savedStateHandle.get<Boolean>(PersonalizationHasDraftStateKey) != true
    ) {
        return null
    }
    val topics =
        savedStateHandle
            .get<ArrayList<String>>(PersonalizationTopicsStateKey)
            ?.filterTo(linkedSetOf()) { topic ->
                topic in UserPreferenceDefaults.ValidTopicIds
            }.orEmpty()
            .ifEmpty { UserPreferenceDefaults.DefaultTopicIds }
    val mood =
        savedStateHandle
            .get<String>(PersonalizationMoodStateKey)
            ?.takeIf(UserPreferenceDefaults.ValidMoodIds::contains)
            ?: UserPreferenceDefaults.DefaultMoodId
    val reminderHour =
        savedStateHandle
            .get<Int>(PersonalizationReminderHourStateKey)
            ?.takeIf(UserPreferenceDefaults.ValidReminderHours::contains)
            ?: UserPreferenceDefaults.DefaultReminderHour
    return PersonalizationUiState(
        isLoading = false,
        selectedTopicIds = topics,
        selectedMoodId = mood,
        dailyGoal =
            UserPreferenceDefaults.sanitizeDailyGoal(
                savedStateHandle[PersonalizationGoalStateKey]
                    ?: UserPreferenceDefaults.DefaultDailyGoal,
            ),
        reminderEnabled =
            savedStateHandle[PersonalizationReminderEnabledStateKey] ?: false,
        reminderHour = reminderHour,
        hasUnsavedChanges = true,
    )
}

internal const val PersonalizationHasDraftStateKey = "personalization.hasDraft"
internal const val PersonalizationTopicsStateKey = "personalization.topics"
internal const val PersonalizationMoodStateKey = "personalization.mood"
internal const val PersonalizationGoalStateKey = "personalization.goal"
internal const val PersonalizationReminderEnabledStateKey = "personalization.reminderEnabled"
internal const val PersonalizationReminderHourStateKey = "personalization.reminderHour"

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
