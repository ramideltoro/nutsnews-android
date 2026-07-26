package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.core.model.StoryReflection
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.data.database.StoryReflectionDao
import com.nutsnews.app.data.database.StoryReflectionEntity
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomStoryReflectionRepository(
    private val dao: StoryReflectionDao,
    private val clock: Clock = Clock.systemUTC(),
) : StoryReflectionRepository {
    override val count: Flow<Int> = dao.observeCount().distinctUntilChanged()

    override fun observeReflection(article: Article): Flow<StoryReflection?> {
        val identity = article.reflectionIdentity()
        return dao
            .observeReflection(identity.stableId.value, identity.legacyId)
            .map { entity -> entity?.toStoryReflection() }
            .distinctUntilChanged()
    }

    override suspend fun findReflection(article: Article): StoryReflection? {
        val identity = article.reflectionIdentity()
        return dao
            .findReflection(identity.stableId.value, identity.legacyId)
            ?.toStoryReflection()
    }

    override suspend fun setReaction(
        article: Article,
        reaction: StoryReflectionReaction,
    ) {
        val identity = article.reflectionIdentity()
        dao.replaceWithStableReflection(
            reflection =
                StoryReflectionEntity(
                    stableArticleId = identity.stableId.value,
                    legacyArticleId = identity.legacyId,
                    articleTitle = article.title,
                    articleSource = article.source,
                    reactionId = reaction.id,
                    createdAtEpochMillis = clock.millis(),
                ),
            legacyArticleId = identity.legacyId,
        )
    }

    private fun StoryReflectionEntity.toStoryReflection(): StoryReflection? {
        val reaction = StoryReflectionReaction.fromId(reactionId) ?: return null
        return StoryReflection(
            articleId = StoryId(stableArticleId),
            articleTitle = articleTitle,
            articleSource = articleSource,
            reaction = reaction,
            createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        )
    }

    private fun Article.reflectionIdentity(): ReflectionIdentity {
        val currentStableId = stableId
        val legacyId =
            id
                .trim()
                .takeIf(String::isNotEmpty)
                ?.takeUnless { it == currentStableId.value }
        return ReflectionIdentity(
            stableId = currentStableId,
            legacyId = legacyId,
        )
    }

    private data class ReflectionIdentity(
        val stableId: StoryId,
        val legacyId: String?,
    )
}
