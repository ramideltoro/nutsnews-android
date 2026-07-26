package com.nutsnews.app.core.model

import java.time.LocalDate

data class ReadingStatsDay(
    val date: LocalDate,
    val storyCount: Int,
) {
    val id: String
        get() = date.toString()
}

data class ReadingStats(
    val todayStoryCount: Int,
    val originalOpensTodayCount: Int,
    val totalUniqueStoryCount: Int,
    val currentStreak: Int,
    val recentDays: List<ReadingStatsDay>,
)
