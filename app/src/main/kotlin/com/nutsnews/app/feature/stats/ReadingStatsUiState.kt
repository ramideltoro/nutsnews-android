package com.nutsnews.app.feature.stats

import androidx.compose.runtime.Immutable
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.ReadingStatsDay
import com.nutsnews.app.data.preferences.UserPreferenceDefaults
import com.nutsnews.app.data.preferences.UserPreferences

@Immutable
data class ReadingStatsUiState(
    val isLoading: Boolean = true,
    val todayStoryCount: Int = 0,
    val dailyGoal: Int = UserPreferenceDefaults.DefaultDailyGoal,
    val currentStreak: Int = 0,
    val totalUniqueStoryCount: Int = 0,
    val savedStoryCount: Int = 0,
    val noteCount: Int = 0,
    val originalOpensTodayCount: Int = 0,
    val recentDays: List<ReadingStatsDay> = emptyList(),
) {
    val goalProgress: Float
        get() = (todayStoryCount.toFloat() / dailyGoal).coerceIn(0f, 1f)

    val todayProgressLabel: String
        get() = "$todayStoryCount/$dailyGoal stories"

    val todayMessage: String
        get() =
            when {
                todayStoryCount <= 0 ->
                    "Open one uplifting story to start today’s positive streak."

                todayStoryCount < dailyGoal -> {
                    val remaining = dailyGoal - todayStoryCount
                    "Nice start. Open $remaining more positive " +
                        if (remaining == 1) {
                            "story to complete today’s goal."
                        } else {
                            "stories to complete today’s goal."
                        }
                }

                else -> "Today’s good-news goal is complete. Beautiful."
            }

    val maxRecentDayCount: Int
        get() = recentDays.maxOfOrNull(ReadingStatsDay::storyCount)?.coerceAtLeast(1) ?: 1

    companion object {
        fun populated(
            preferences: UserPreferences,
            stats: ReadingStats,
            savedStoryCount: Int,
            noteCount: Int,
        ): ReadingStatsUiState =
            ReadingStatsUiState(
                isLoading = false,
                todayStoryCount = stats.todayStoryCount.coerceAtLeast(0),
                dailyGoal =
                    UserPreferenceDefaults.sanitizeDailyGoal(
                        preferences.dailyGoal,
                    ),
                currentStreak = stats.currentStreak.coerceAtLeast(0),
                totalUniqueStoryCount = stats.totalUniqueStoryCount.coerceAtLeast(0),
                savedStoryCount = savedStoryCount.coerceAtLeast(0),
                noteCount = noteCount.coerceAtLeast(0),
                originalOpensTodayCount =
                    stats.originalOpensTodayCount.coerceAtLeast(0),
                recentDays =
                    stats.recentDays.map { day ->
                        day.copy(storyCount = day.storyCount.coerceAtLeast(0))
                    },
            )
    }
}
