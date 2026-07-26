package com.nutsnews.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.nutsNewsPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DataStoreUserPreferencesRepository.FileName,
    corruptionHandler =
        ReplaceFileCorruptionHandler {
            emptyPreferences()
        },
)

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map(::preferencesFrom)

    override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { storedPreferences ->
            val updated =
                UserPreferenceDefaults.sanitize(
                    transform(preferencesFrom(storedPreferences)),
                )
            storedPreferences[Keys.HasCompletedOnboarding] = updated.hasCompletedOnboarding
            storedPreferences[Keys.SelectedTopics] = encodeTopicIds(updated.selectedTopicIds)
            storedPreferences[Keys.SelectedMood] = updated.selectedMoodId
            storedPreferences[Keys.DailyGoal] = updated.dailyGoal
            storedPreferences[Keys.ReminderEnabled] = updated.reminder.enabled
            storedPreferences[Keys.ReminderHour] = updated.reminder.hour
            storedPreferences[Keys.SelectedTheme] = updated.theme.rawValue
            storedPreferences[Keys.HapticsEnabled] = updated.hapticsEnabled
            storedPreferences[Keys.ShowStatsOnLargeWidget] =
                updated.showStatsOnLargeWidget
        }
    }

    private fun preferencesFrom(preferences: Preferences): UserPreferences =
        UserPreferences(
            hasCompletedOnboarding = preferences[Keys.HasCompletedOnboarding] ?: false,
            selectedTopicIds = decodeTopicIds(preferences[Keys.SelectedTopics]),
            selectedMoodId =
                UserPreferenceDefaults.sanitizeMoodId(
                    preferences[Keys.SelectedMood].orEmpty(),
                ),
            dailyGoal =
                UserPreferenceDefaults.sanitizeDailyGoal(
                    preferences[Keys.DailyGoal] ?: UserPreferenceDefaults.DefaultDailyGoal,
                ),
            reminder =
                ReminderConfiguration(
                    enabled = preferences[Keys.ReminderEnabled] ?: false,
                    hour =
                        UserPreferenceDefaults.sanitizeReminderHour(
                            preferences[Keys.ReminderHour]
                                ?: UserPreferenceDefaults.DefaultReminderHour,
                        ),
                ),
            theme = NutsNewsAppTheme.fromStoredValue(preferences[Keys.SelectedTheme]),
            hapticsEnabled =
                preferences[Keys.HapticsEnabled] ?: UserPreferenceDefaults.HapticsEnabled,
            showStatsOnLargeWidget =
                preferences[Keys.ShowStatsOnLargeWidget]
                    ?: UserPreferenceDefaults.ShowStatsOnLargeWidget,
        )

    private fun decodeTopicIds(rawValue: String?): Set<String> {
        val selectedIds =
            rawValue
                .orEmpty()
                .split(",")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
        return UserPreferenceDefaults.sanitizeTopicIds(selectedIds)
    }

    private fun encodeTopicIds(topicIds: Set<String>): String =
        UserPreferenceDefaults
            .sanitizeTopicIds(topicIds)
            .sorted()
            .joinToString(",")

    internal object Keys {
        val HasCompletedOnboarding =
            booleanPreferencesKey("nutsnews.onboarding.completed.v1")
        val SelectedTopics = stringPreferencesKey("nutsnews.preferences.topics.v1")
        val SelectedMood = stringPreferencesKey("nutsnews.preferences.mood.v1")
        val DailyGoal = intPreferencesKey("nutsnews.preferences.dailyGoal.v1")
        val ReminderEnabled =
            booleanPreferencesKey("nutsnews.preferences.reminder.enabled.v1")
        val ReminderHour = intPreferencesKey("nutsnews.preferences.reminder.hour.v1")
        val SelectedTheme = stringPreferencesKey(NutsNewsAppTheme.StorageKey)
        val HapticsEnabled = booleanPreferencesKey("nutsnews.hapticsEnabled")
        val ShowStatsOnLargeWidget =
            booleanPreferencesKey("nutsnews.widget.showStatsOnLargeWidget")
    }

    companion object {
        const val FileName = "nutsnews_user_preferences"
    }
}
