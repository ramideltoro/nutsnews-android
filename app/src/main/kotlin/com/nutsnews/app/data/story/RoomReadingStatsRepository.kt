package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.ReadingStatsDay
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.database.ReadingActivityDao
import com.nutsnews.app.data.database.ReadingStoryOpenEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class RoomReadingStatsRepository(
    private val dao: ReadingActivityDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ReadingStatsRepository {
    override fun observeStats(recentDayCount: Int): Flow<ReadingStats> {
        val today = LocalDate.now(clock)
        val safeRecentDayCount =
            recentDayCount.coerceIn(
                ReadingStatsRepository.MinimumRecentDayCount,
                ReadingStatsRepository.MaximumRecentDayCount,
            )

        return combine(
            dao.observeDailyStoryCounts(),
            dao.observeTotalUniqueStoryCount(),
            dao.observeOriginalStoryOpens(today.toString()),
        ) { dailyCounts, totalUniqueStoryCount, originalStoryOpens ->
            val countByDay =
                dailyCounts.associate { dailyCount ->
                    dailyCount.dayKey to dailyCount.storyCount
                }
            val recentDays =
                (safeRecentDayCount - 1 downTo 0).map { daysAgo ->
                    val date = today.minusDays(daysAgo.toLong())
                    ReadingStatsDay(
                        date = date,
                        storyCount = countByDay[date.toString()] ?: 0,
                    )
                }

            ReadingStats(
                todayStoryCount = countByDay[today.toString()] ?: 0,
                originalOpensTodayCount = originalStoryOpens?.openCount ?: 0,
                totalUniqueStoryCount = totalUniqueStoryCount,
                currentStreak = currentStreak(today, countByDay),
                recentDays = recentDays,
            )
        }.distinctUntilChanged()
    }

    override suspend fun recordStoryOpen(article: Article) {
        val now = clock.instant()
        dao.upsertStoryOpen(
            ReadingStoryOpenEntity(
                dayKey = now.atZone(clock.zone).toLocalDate().toString(),
                stableArticleId = article.stableId.value,
                openedAtEpochMillis = now.toEpochMilli(),
            ),
        )
    }

    override suspend fun recordOriginalStoryOpen() {
        val now = clock.instant()
        dao.incrementOriginalStoryOpens(
            dayKey = now.atZone(clock.zone).toLocalDate().toString(),
            openedAtEpochMillis = now.toEpochMilli(),
        )
    }

    override suspend fun lastOpenedAt(storyId: StoryId): Instant? =
        dao
            .findLastOpenedAt(storyId.value)
            ?.let(Instant::ofEpochMilli)

    private fun currentStreak(
        today: LocalDate,
        countByDay: Map<String, Int>,
    ): Int {
        var date = today
        var streak = 0
        while ((countByDay[date.toString()] ?: 0) > 0) {
            streak += 1
            date = date.minusDays(1)
        }
        return streak
    }
}
