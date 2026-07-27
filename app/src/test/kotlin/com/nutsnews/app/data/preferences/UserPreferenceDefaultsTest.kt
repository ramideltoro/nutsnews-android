package com.nutsnews.app.data.preferences

import com.nutsnews.app.designsystem.NutsNewsAppTheme
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UserPreferenceDefaultsTest {
    @Test
    fun numericAndCatalogSanitizersCoverEveryBoundary() {
        assertEquals(
            listOf(1, 1, 1, 3, 5, 5, 5),
            listOf(Int.MIN_VALUE, 0, 1, 3, 5, 6, Int.MAX_VALUE)
                .map(UserPreferenceDefaults::sanitizeDailyGoal),
        )
        assertEquals(
            listOf(8, 8, 8, 15, 20, 8, 8),
            listOf(Int.MIN_VALUE, 0, 8, 15, 20, 21, Int.MAX_VALUE)
                .map(UserPreferenceDefaults::sanitizeReminderHour),
        )
        assertEquals(
            setOf("science", "nature"),
            UserPreferenceDefaults.sanitizeTopicIds(
                setOf("science", "unknown", "nature"),
            ),
        )
        assertEquals(
            UserPreferenceDefaults.DefaultTopicIds,
            UserPreferenceDefaults.sanitizeTopicIds(emptySet()),
        )
        assertEquals(
            UserPreferenceDefaults.DefaultMoodId,
            UserPreferenceDefaults.sanitizeMoodId("unknown"),
        )
    }

    @Test
    fun inMemoryRepositorySanitizesBothInitialAndUpdatedValues() =
        runTest {
            val repository =
                InMemoryUserPreferencesRepository(
                    UserPreferences(
                        selectedTopicIds = setOf("invalid"),
                        selectedMoodId = "invalid",
                        dailyGoal = -10,
                        reminder = ReminderConfiguration(enabled = true, hour = 99),
                        theme = NutsNewsAppTheme.Bambi,
                    ),
                )

            assertEquals(
                UserPreferences(
                    selectedTopicIds = UserPreferenceDefaults.DefaultTopicIds,
                    selectedMoodId = UserPreferenceDefaults.DefaultMoodId,
                    dailyGoal = 1,
                    reminder = ReminderConfiguration(enabled = true, hour = 8),
                    theme = NutsNewsAppTheme.Bambi,
                ),
                repository.preferences.first(),
            )

            repository.updatePreferences {
                it.copy(
                    selectedTopicIds = setOf("travel", "bad"),
                    selectedMoodId = "curious",
                    dailyGoal = 99,
                    reminder = ReminderConfiguration(enabled = false, hour = 20),
                )
            }

            assertEquals(
                UserPreferences(
                    selectedTopicIds = setOf("travel"),
                    selectedMoodId = "curious",
                    dailyGoal = 5,
                    reminder = ReminderConfiguration(enabled = false, hour = 20),
                    theme = NutsNewsAppTheme.Bambi,
                ),
                repository.preferences.first(),
            )
        }
}
