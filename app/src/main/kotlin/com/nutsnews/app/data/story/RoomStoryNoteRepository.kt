package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.core.model.StoryNote
import com.nutsnews.app.data.database.StoryNoteDao
import com.nutsnews.app.data.database.StoryNoteEntity
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomStoryNoteRepository(
    private val dao: StoryNoteDao,
    private val clock: Clock = Clock.systemUTC(),
) : StoryNoteRepository {
    override val count: Flow<Int> =
        dao
            .observeNotes()
            .map { notes -> notes.count { it.text.trim().isNotEmpty() } }
            .distinctUntilChanged()

    override fun observeNote(article: Article): Flow<StoryNote?> {
        val identity = article.noteIdentity()
        return dao
            .observeNote(identity.stableId.value, identity.legacyId)
            .map { entity ->
                entity
                    ?.takeIf { it.text.trim().isNotEmpty() }
                    ?.toStoryNote()
            }
            .distinctUntilChanged()
    }

    override suspend fun findNote(article: Article): StoryNote? {
        val identity = article.noteIdentity()
        return dao
            .findNote(identity.stableId.value, identity.legacyId)
            ?.takeIf { it.text.trim().isNotEmpty() }
            ?.toStoryNote()
    }

    override suspend fun setNote(
        article: Article,
        text: String,
    ) {
        val cleanedText = text.trim()
        if (cleanedText.isEmpty()) {
            clearNote(article)
            return
        }

        val identity = article.noteIdentity()
        dao.replaceLegacyWithStableNote(
            note =
                StoryNoteEntity(
                    stableArticleId = identity.stableId.value,
                    legacyArticleId = identity.legacyId,
                    articleTitle = article.title,
                    text = cleanedText,
                    updatedAtEpochMillis = clock.millis(),
                ),
            legacyArticleId = identity.legacyId,
        )
    }

    override suspend fun clearNote(article: Article) {
        val identity = article.noteIdentity()
        dao.delete(identity.stableId.value, identity.legacyId)
    }

    private fun StoryNoteEntity.toStoryNote(): StoryNote =
        StoryNote(
            articleId = StoryId(stableArticleId),
            articleTitle = articleTitle,
            text = text,
            updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
        )

    private fun Article.noteIdentity(): NoteIdentity {
        val currentStableId = stableId
        val legacyId =
            id
                .trim()
                .takeIf(String::isNotEmpty)
                ?.takeUnless { it == currentStableId.value }
        return NoteIdentity(
            stableId = currentStableId,
            legacyId = legacyId,
        )
    }

    private data class NoteIdentity(
        val stableId: StoryId,
        val legacyId: String?,
    )
}
