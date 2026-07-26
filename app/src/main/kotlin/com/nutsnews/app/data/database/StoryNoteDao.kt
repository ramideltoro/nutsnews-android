package com.nutsnews.app.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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
    suspend fun findNote(
        stableArticleId: String,
        legacyArticleId: String?,
    ): StoryNoteEntity?

    @Query("SELECT * FROM story_notes ORDER BY updated_at DESC, stable_article_id ASC")
    fun observeNotes(): Flow<List<StoryNoteEntity>>

    @Upsert
    suspend fun upsert(note: StoryNoteEntity)

    @Query("DELETE FROM story_notes WHERE stable_article_id = :stableArticleId")
    suspend fun deleteByStableId(stableArticleId: String): Int

    @Transaction
    suspend fun replaceLegacyWithStableNote(
        note: StoryNoteEntity,
        legacyArticleId: String?,
    ) {
        if (legacyArticleId != null && legacyArticleId != note.stableArticleId) {
            deleteByStableId(legacyArticleId)
        }
        upsert(note)
    }

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
