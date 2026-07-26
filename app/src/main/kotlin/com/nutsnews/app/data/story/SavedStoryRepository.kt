package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import kotlinx.coroutines.flow.Flow

interface SavedStoryRepository {
    val stories: Flow<List<SavedStory>>
    val count: Flow<Int>

    fun observeIsLiked(storyId: StoryId): Flow<Boolean>

    suspend fun isLiked(storyId: StoryId): Boolean

    suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    )

    suspend fun save(article: Article)

    suspend fun remove(storyId: StoryId)
}
