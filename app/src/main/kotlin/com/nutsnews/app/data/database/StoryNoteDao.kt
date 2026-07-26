package com.nutsnews.app.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryNoteDao {
    @Query(
        """
        SELECT * FROM story_notes
        WHERE stable_article_id = :stableArticleId
            OR (
                :legacyArticleId IS NOT NULL
                AND (
                    stable_article_id = :legacyArticleId
                    OR legacy_article_id = :legacyArticleId
                )
            )
        ORDER BY
            CASE
                WHEN stable_article_id = :stableArticleId THEN 0
                WHEN stable_article_id = :legacyArticleId THEN 1
                ELSE 2
            END
        LIMIT 1
        """,
    )
    fun observeNote(
        stableArticleId: String,
        legacyArticleId: String?,
    ): Flow<StoryNoteEntity?>

    @Query("SELECT COUNT(*) FROM story_notes WHERE TRIM(text) != ''")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(note: StoryNoteEntity)

    @Query(
        """
        DELETE FROM story_notes
        WHERE stable_article_id = :stableArticleId
            OR (
                :legacyArticleId IS NOT NULL
                AND (
                    stable_article_id = :legacyArticleId
                    OR legacy_article_id = :legacyArticleId
                )
            )
        """,
    )
    suspend fun delete(
        stableArticleId: String,
        legacyArticleId: String?,
    ): Int
}
