package com.nutsnews.app.data.preferences

import com.nutsnews.app.designsystem.NutsNewsAppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    val hasCompletedOnboarding: Flow<Boolean>
        get() = preferences.map { it.hasCompletedOnboarding }

    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences)

    suspend fun setOnboardingCompleted(completed: Boolean) {
        updatePreferences { it.copy(hasCompletedOnboarding = completed) }
    }

    suspend fun setSelectedTopics(topicIds: Set<String>) {
        updatePreferences { it.copy(selectedTopicIds = topicIds) }
    }

    suspend fun setSelectedMood(moodId: String) {
        updatePreferences { it.copy(selectedMoodId = moodId) }
    }

    suspend fun setDailyGoal(dailyGoal: Int) {
        updatePreferences { it.copy(dailyGoal = dailyGoal) }
    }

    suspend fun setReminderConfiguration(configuration: ReminderConfiguration) {
        updatePreferences { it.copy(reminder = configuration) }
    }

    suspend fun setTheme(theme: NutsNewsAppTheme) {
        updatePreferences { it.copy(theme = theme) }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        updatePreferences { it.copy(hapticsEnabled = enabled) }
    }

    suspend fun setShowStatsOnLargeWidget(showStats: Boolean) {
        updatePreferences { it.copy(showStatsOnLargeWidget = showStats) }
    }
}

class InMemoryUserPreferencesRepository(
    initialPreferences: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {
    private val mutablePreferences =
        MutableStateFlow(UserPreferenceDefaults.sanitize(initialPreferences))

    override val preferences: Flow<UserPreferences> = mutablePreferences

    override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        mutablePreferences.update { current ->
            UserPreferenceDefaults.sanitize(transform(current))
        }
    }
}
