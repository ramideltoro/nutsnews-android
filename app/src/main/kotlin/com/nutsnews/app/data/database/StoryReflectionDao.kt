package com.nutsnews.app.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryReflectionDao {
    @Query(
        """
        SELECT * FROM story_reflections
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
    fun observeReflection(
        stableArticleId: String,
        legacyArticleId: String?,
    ): Flow<StoryReflectionEntity?>

    @Query(
        """
        SELECT * FROM story_reflections
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
    suspend fun findReflection(
        stableArticleId: String,
        legacyArticleId: String?,
    ): StoryReflectionEntity?

    @Query("SELECT COUNT(*) FROM story_reflections")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(reflection: StoryReflectionEntity)

    @Transaction
    suspend fun replaceWithStableReflection(
        reflection: StoryReflectionEntity,
        legacyArticleId: String?,
    ) {
        delete(reflection.stableArticleId, legacyArticleId)
        upsert(reflection)
    }

    @Query(
        """
        DELETE FROM story_reflections
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
