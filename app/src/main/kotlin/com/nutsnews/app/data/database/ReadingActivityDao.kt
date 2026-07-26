package com.nutsnews.app.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStoryOpen(storyOpen: ReadingStoryOpenEntity): Long

    @Upsert
    suspend fun upsertStoryOpen(storyOpen: ReadingStoryOpenEntity)

    @Query(
        """
        SELECT * FROM reading_story_opens
        WHERE day_key BETWEEN :firstDayKey AND :lastDayKey
        ORDER BY day_key ASC, opened_at ASC, stable_article_id ASC
        """,
    )
    fun observeStoryOpens(
        firstDayKey: String,
        lastDayKey: String,
    ): Flow<List<ReadingStoryOpenEntity>>

    @Query(
        """
        SELECT day_key, COUNT(*) AS story_count
        FROM reading_story_opens
        GROUP BY day_key
        ORDER BY day_key ASC
        """,
    )
    fun observeDailyStoryCounts(): Flow<List<ReadingDayCount>>

    @Query("SELECT COUNT(DISTINCT stable_article_id) FROM reading_story_opens")
    fun observeTotalUniqueStoryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reading_story_opens WHERE day_key = :dayKey")
    fun observeStoryCount(dayKey: String): Flow<Int>

    @Query(
        """
        SELECT MAX(opened_at) FROM reading_story_opens
        WHERE stable_article_id = :stableArticleId
        """,
    )
    suspend fun findLastOpenedAt(stableArticleId: String): Long?

    @Query("SELECT * FROM original_story_opens WHERE day_key = :dayKey LIMIT 1")
    suspend fun findOriginalStoryOpens(dayKey: String): OriginalStoryOpenEntity?

    @Query("SELECT * FROM original_story_opens WHERE day_key = :dayKey LIMIT 1")
    fun observeOriginalStoryOpens(dayKey: String): Flow<OriginalStoryOpenEntity?>

    @Upsert
    suspend fun upsertOriginalStoryOpens(originalStoryOpens: OriginalStoryOpenEntity)

    @Transaction
    suspend fun incrementOriginalStoryOpens(
        dayKey: String,
        openedAtEpochMillis: Long,
    ) {
        val currentCount = findOriginalStoryOpens(dayKey)?.openCount ?: 0
        upsertOriginalStoryOpens(
            OriginalStoryOpenEntity(
                dayKey = dayKey,
                openCount = currentCount + 1,
                updatedAtEpochMillis = openedAtEpochMillis,
            ),
        )
    }
}

data class ReadingDayCount(
    @ColumnInfo(name = "day_key")
    val dayKey: String,
    @ColumnInfo(name = "story_count")
    val storyCount: Int,
)
