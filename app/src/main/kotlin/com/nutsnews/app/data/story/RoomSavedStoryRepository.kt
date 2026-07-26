package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.database.SavedStoryDao
import com.nutsnews.app.data.database.SavedStoryEntity
import java.net.URI
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomSavedStoryRepository(
    private val dao: SavedStoryDao,
    private val clock: Clock = Clock.systemUTC(),
) : SavedStoryRepository {
    override val stories: Flow<List<SavedStory>> =
        dao
            .observeStories()
            .map { entities -> entities.map { it.toSavedStory() } }
            .distinctUntilChanged()

    override val count: Flow<Int> = dao.observeCount().distinctUntilChanged()

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> =
        dao
            .observeStory(storyId.value)
            .map { it != null }
            .distinctUntilChanged()

    override suspend fun isLiked(storyId: StoryId): Boolean =
        dao.findStory(storyId.value) != null

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) {
        if (isLiked) {
            save(article)
        } else {
            remove(article.stableId)
        }
    }

    override suspend fun save(article: Article) {
        dao.upsert(
            SavedStoryEntity(
                stableArticleId = article.stableId.value,
                apiId = article.id,
                title = article.title,
                summary = article.summary,
                originalUrl = article.originalUrl?.toString(),
                source = article.source,
                publishedAt = article.publishedAt,
                createdAt = article.createdAt,
                thumbnailUrl = article.thumbnailUrl?.toString(),
                categories = article.categories,
                savedAtEpochMillis = clock.millis(),
            ),
        )
    }

    override suspend fun remove(storyId: StoryId) {
        dao.delete(storyId.value)
    }

    private fun SavedStoryEntity.toSavedStory(): SavedStory =
        SavedStory(
            article =
                Article(
                    id = stableArticleId,
                    title = title,
                    summary = summary,
                    originalUrl = originalUrl.toUriOrNull(),
                    source = source,
                    publishedAt = publishedAt,
                    createdAt = createdAt,
                    thumbnailUrl = thumbnailUrl.toUriOrNull(),
                    categories = categories,
                ),
            savedAt = Instant.ofEpochMilli(savedAtEpochMillis),
        )

    private fun String?.toUriOrNull(): URI? =
        this?.let { rawValue ->
            runCatching { URI(rawValue) }.getOrNull()
        }
}
