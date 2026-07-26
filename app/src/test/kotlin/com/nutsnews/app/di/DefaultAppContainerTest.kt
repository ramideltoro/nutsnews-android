package com.nutsnews.app.di

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.story.SavedStoryRepository
import kotlin.test.assertSame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class DefaultAppContainerTest {
    @Test
    fun applicationDependenciesAreStableForTheContainerLifetime() {
        val savedStoryRepository = EmptySavedStoryRepository
        val container =
            DefaultAppContainer(
                savedStoryRepository = savedStoryRepository,
            )

        assertSame(container.navigator, container.navigator)
        assertSame(container.articleApiClient, container.articleApiClient)
        assertSame(container.userPreferencesRepository, container.userPreferencesRepository)
        assertSame(savedStoryRepository, container.savedStoryRepository)
    }
}

private object EmptySavedStoryRepository : SavedStoryRepository {
    override val stories: Flow<List<SavedStory>> = emptyFlow()
    override val count: Flow<Int> = emptyFlow()

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> = emptyFlow()

    override suspend fun isLiked(storyId: StoryId): Boolean = false

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) = Unit

    override suspend fun save(article: Article) = Unit

    override suspend fun remove(storyId: StoryId) = Unit
}
