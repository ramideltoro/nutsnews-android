package com.nutsnews.app.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedStoryDao {
    @Query(
        """
        SELECT * FROM saved_stories
        ORDER BY saved_at DESC, stable_article_id ASC
        """,
    )
    fun observeStories(): Flow<List<SavedStoryEntity>>

    @Query("SELECT * FROM saved_stories WHERE stable_article_id = :stableArticleId LIMIT 1")
    fun observeStory(stableArticleId: String): Flow<SavedStoryEntity?>

    @Query("SELECT * FROM saved_stories WHERE stable_article_id = :stableArticleId LIMIT 1")
    suspend fun findStory(stableArticleId: String): SavedStoryEntity?

    @Query("SELECT COUNT(*) FROM saved_stories")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(story: SavedStoryEntity)

    @Query("DELETE FROM saved_stories WHERE stable_article_id = :stableArticleId")
    suspend fun delete(stableArticleId: String): Int
}
