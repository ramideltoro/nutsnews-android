package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.StoryId
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface ReadingStatsRepository {
    fun observeStats(recentDayCount: Int = DefaultRecentDayCount): Flow<ReadingStats>

    suspend fun recordStoryOpen(article: Article)

    suspend fun recordOriginalStoryOpen()

    suspend fun lastOpenedAt(storyId: StoryId): Instant?

    companion object {
        const val DefaultRecentDayCount = 7
        const val MinimumRecentDayCount = 1
        const val MaximumRecentDayCount = 30
    }
}
