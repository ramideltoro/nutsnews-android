package com.nutsnews.app.feature.home

import androidx.compose.runtime.Immutable
import com.nutsnews.app.data.preferences.NutsNewsPersonalization
import com.nutsnews.app.data.preferences.UserPreferenceDefaults
import com.nutsnews.app.data.preferences.UserPreferences
import java.util.Locale

@Immutable
data class HomeDashboardUiState(
    val isLoading: Boolean = true,
    val todayStoryCount: Int = 0,
    val dailyGoal: Int = UserPreferenceDefaults.DefaultDailyGoal,
    val currentStreak: Int = 0,
    val savedCount: Int = 0,
    val notesCount: Int = 0,
    val selectedTopicIds: Set<String> = UserPreferenceDefaults.DefaultTopicIds,
    val selectedMoodId: String = UserPreferenceDefaults.DefaultMoodId,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = UserPreferenceDefaults.DefaultReminderHour,
) {
    val goalProgress: Float
        get() =
            if (dailyGoal <= 0) {
                0f
            } else {
                (todayStoryCount.toFloat() / dailyGoal).coerceIn(0f, 1f)
            }

    val goalProgressPercent: Int
        get() = (goalProgress * 100).toInt()

    val goalTitle: String
        get() =
            if (todayStoryCount >= dailyGoal) {
                "Goal complete ✨"
            } else {
                "$todayStoryCount of $dailyGoal stories today"
            }

    val selectedMoodTitle: String
        get() = NutsNewsPersonalization.mood(selectedMoodId).title

    val selectedTopicText: String
        get() =
            NutsNewsPersonalization
                .topicTitles(selectedTopicIds)
                .take(3)
                .joinToString(", ")

    val personalizationSummary: String
        get() =
            NutsNewsPersonalization.personalizationSummary(
                selectedTopicIds = selectedTopicIds,
                selectedMoodId = selectedMoodId,
            )

    val streakText: String
        get() =
            when (currentStreak) {
                0 -> "Start your streak"
                1 -> "1 day streak"
                else -> "$currentStreak day streak"
            }

    val reminderDisplayTime: String
        get() =
            when (reminderHour) {
                15 -> "3:00 PM"
                20 -> "8:00 PM"
                else -> "8:00 AM"
            }

    companion object {
        fun populated(
            preferences: UserPreferences,
            todayStoryCount: Int,
            currentStreak: Int,
            savedCount: Int,
            notesCount: Int,
        ): HomeDashboardUiState =
            HomeDashboardUiState(
                isLoading = false,
                todayStoryCount = todayStoryCount.coerceAtLeast(0),
                dailyGoal = preferences.dailyGoal,
                currentStreak = currentStreak.coerceAtLeast(0),
                savedCount = savedCount.coerceAtLeast(0),
                notesCount = notesCount.coerceAtLeast(0),
                selectedTopicIds = preferences.selectedTopicIds,
                selectedMoodId = preferences.selectedMoodId.lowercase(Locale.ROOT),
                reminderEnabled = preferences.reminder.enabled,
                reminderHour = preferences.reminder.hour,
            )
    }
}
