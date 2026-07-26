package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.StoryId
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {
    fun observeArticleIds(): Flow<List<StoryId>>

    suspend fun refresh(): Result<Unit>
}
