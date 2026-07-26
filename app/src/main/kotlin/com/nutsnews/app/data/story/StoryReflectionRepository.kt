package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryReflection
import com.nutsnews.app.core.model.StoryReflectionReaction
import kotlinx.coroutines.flow.Flow

interface StoryReflectionRepository {
    val count: Flow<Int>

    fun observeReflection(article: Article): Flow<StoryReflection?>

    suspend fun findReflection(article: Article): StoryReflection?

    suspend fun setReaction(
        article: Article,
        reaction: StoryReflectionReaction,
    )
}
