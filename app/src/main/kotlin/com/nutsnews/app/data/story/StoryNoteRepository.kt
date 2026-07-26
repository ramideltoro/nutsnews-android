package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryNote
import kotlinx.coroutines.flow.Flow

interface StoryNoteRepository {
    val count: Flow<Int>

    fun observeNote(article: Article): Flow<StoryNote?>

    suspend fun findNote(article: Article): StoryNote?

    suspend fun setNote(
        article: Article,
        text: String,
    )

    suspend fun clearNote(article: Article)
}
