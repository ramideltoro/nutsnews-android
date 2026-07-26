package com.nutsnews.app.data.preferences

import androidx.compose.runtime.Immutable
import com.nutsnews.app.designsystem.NutsNewsAppTheme

@Immutable
data class ReminderConfiguration(
    val enabled: Boolean = false,
    val hour: Int = UserPreferenceDefaults.DefaultReminderHour,
)

@Immutable
data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val selectedTopicIds: Set<String> = UserPreferenceDefaults.DefaultTopicIds,
    val selectedMoodId: String = UserPreferenceDefaults.DefaultMoodId,
    val dailyGoal: Int = UserPreferenceDefaults.DefaultDailyGoal,
    val reminder: ReminderConfiguration = ReminderConfiguration(),
    val theme: NutsNewsAppTheme = NutsNewsAppTheme.Default,
    val hapticsEnabled: Boolean = UserPreferenceDefaults.HapticsEnabled,
    val showStatsOnLargeWidget: Boolean = UserPreferenceDefaults.ShowStatsOnLargeWidget,
)

object UserPreferenceDefaults {
    val ValidTopicIds: Set<String> =
        setOf(
            "animals",
            "science",
            "community",
            "wellness",
            "achievements",
            "travel",
            "culture",
            "nature",
        )
    val DefaultTopicIds: Set<String> = setOf("community", "science", "animals")
    val ValidMoodIds: Set<String> = setOf("calm", "hopeful", "inspired", "curious")
    val ValidReminderHours: Set<Int> = setOf(8, 15, 20)

    const val DefaultMoodId = "calm"
    const val DefaultDailyGoal = 3
    const val DefaultReminderHour = 8
    const val HapticsEnabled = true
    const val ShowStatsOnLargeWidget = true

    fun sanitize(preferences: UserPreferences): UserPreferences =
        preferences.copy(
            selectedTopicIds = sanitizeTopicIds(preferences.selectedTopicIds),
            selectedMoodId = sanitizeMoodId(preferences.selectedMoodId),
            dailyGoal = sanitizeDailyGoal(preferences.dailyGoal),
            reminder =
                preferences.reminder.copy(
                    hour = sanitizeReminderHour(preferences.reminder.hour),
                ),
            theme = NutsNewsAppTheme.fromStoredValue(preferences.theme.rawValue),
        )

    fun sanitizeTopicIds(topicIds: Set<String>): Set<String> {
        val validIds = topicIds.intersect(ValidTopicIds)
        return validIds.ifEmpty { DefaultTopicIds }
    }

    fun sanitizeMoodId(moodId: String): String =
        moodId.takeIf(ValidMoodIds::contains) ?: DefaultMoodId

    fun sanitizeDailyGoal(dailyGoal: Int): Int = dailyGoal.coerceIn(1, 5)

    fun sanitizeReminderHour(hour: Int): Int =
        hour.takeIf(ValidReminderHours::contains) ?: DefaultReminderHour
}
