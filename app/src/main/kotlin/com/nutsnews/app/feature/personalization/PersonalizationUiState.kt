package com.nutsnews.app.feature.personalization

import androidx.compose.runtime.Immutable
import com.nutsnews.app.data.preferences.ReminderConfiguration
import com.nutsnews.app.data.preferences.UserPreferenceDefaults
import com.nutsnews.app.data.preferences.UserPreferences

@Immutable
data class PersonalizationUiState(
    val isLoading: Boolean = true,
    val selectedTopicIds: Set<String> = UserPreferenceDefaults.DefaultTopicIds,
    val selectedMoodId: String = UserPreferenceDefaults.DefaultMoodId,
    val dailyGoal: Int = UserPreferenceDefaults.DefaultDailyGoal,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = UserPreferenceDefaults.DefaultReminderHour,
    val isSaving: Boolean = false,
    val statusText: String = "",
    val hasUnsavedChanges: Boolean = false,
) {
    val canSave: Boolean
        get() = !isLoading && !isSaving && selectedTopicIds.isNotEmpty()

    fun toPreferences(previous: UserPreferences): UserPreferences =
        previous.copy(
            hasCompletedOnboarding = true,
            selectedTopicIds = selectedTopicIds,
            selectedMoodId = selectedMoodId,
            dailyGoal = dailyGoal,
            reminder =
                ReminderConfiguration(
                    enabled = reminderEnabled,
                    hour = reminderHour,
                ),
        )

    companion object {
        fun fromPreferences(preferences: UserPreferences): PersonalizationUiState =
            PersonalizationUiState(
                isLoading = false,
                selectedTopicIds = preferences.selectedTopicIds,
                selectedMoodId = preferences.selectedMoodId,
                dailyGoal = preferences.dailyGoal,
                reminderEnabled = preferences.reminder.enabled,
                reminderHour = preferences.reminder.hour,
            )
    }
}

enum class PersonalizationMode {
    FirstRun,
    Editor,
}
