package com.nutsnews.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStoryOpen(storyOpen: ReadingStoryOpenEntity): Long

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

    @Query("SELECT COUNT(DISTINCT stable_article_id) FROM reading_story_opens")
    fun observeTotalUniqueStoryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reading_story_opens WHERE day_key = :dayKey")
    fun observeStoryCount(dayKey: String): Flow<Int>

    @Query("SELECT * FROM original_story_opens WHERE day_key = :dayKey LIMIT 1")
    suspend fun findOriginalStoryOpens(dayKey: String): OriginalStoryOpenEntity?

    @Query("SELECT * FROM original_story_opens WHERE day_key = :dayKey LIMIT 1")
    fun observeOriginalStoryOpens(dayKey: String): Flow<OriginalStoryOpenEntity?>

    @Upsert
    suspend fun upsertOriginalStoryOpens(originalStoryOpens: OriginalStoryOpenEntity)
}
