package com.nutsnews.app.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_stories",
    indices = [
        Index(value = ["api_id"]),
        Index(value = ["saved_at"]),
    ],
)
data class SavedStoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "stable_article_id")
    val stableArticleId: String,
    @ColumnInfo(name = "api_id")
    val apiId: String,
    val title: String,
    val summary: String,
    @ColumnInfo(name = "original_url")
    val originalUrl: String?,
    val source: String,
    @ColumnInfo(name = "published_at")
    val publishedAt: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String?,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?,
    val categories: List<String>,
    @ColumnInfo(name = "saved_at")
    val savedAtEpochMillis: Long,
)

@Entity(
    tableName = "story_notes",
    indices = [
        Index(value = ["legacy_article_id"]),
        Index(value = ["updated_at"]),
    ],
)
data class StoryNoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "stable_article_id")
    val stableArticleId: String,
    @ColumnInfo(name = "legacy_article_id")
    val legacyArticleId: String?,
    @ColumnInfo(name = "article_title")
    val articleTitle: String,
    val text: String,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "story_reflections",
    indices = [
        Index(value = ["legacy_article_id"]),
        Index(value = ["reaction_id"]),
        Index(value = ["created_at"]),
    ],
)
data class StoryReflectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "stable_article_id")
    val stableArticleId: String,
    @ColumnInfo(name = "legacy_article_id")
    val legacyArticleId: String?,
    @ColumnInfo(name = "article_title")
    val articleTitle: String,
    @ColumnInfo(name = "article_source")
    val articleSource: String,
    @ColumnInfo(name = "reaction_id")
    val reactionId: String,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "reading_story_opens",
    primaryKeys = ["day_key", "stable_article_id"],
    indices = [
        Index(value = ["stable_article_id"]),
        Index(value = ["opened_at"]),
    ],
)
data class ReadingStoryOpenEntity(
    @ColumnInfo(name = "day_key")
    val dayKey: String,
    @ColumnInfo(name = "stable_article_id")
    val stableArticleId: String,
    @ColumnInfo(name = "opened_at")
    val openedAtEpochMillis: Long,
)

@Entity(tableName = "original_story_opens")
data class OriginalStoryOpenEntity(
    @PrimaryKey
    @ColumnInfo(name = "day_key")
    val dayKey: String,
    @ColumnInfo(name = "open_count")
    val openCount: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMillis: Long,
)
