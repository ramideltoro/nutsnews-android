package com.nutsnews.app.di

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.core.model.StoryNote
import com.nutsnews.app.core.model.StoryReflection
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.data.story.StoryNoteRepository
import com.nutsnews.app.data.story.StoryReflectionRepository
import java.time.Instant
import kotlin.test.assertSame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class DefaultAppContainerTest {
    @Test
    fun applicationDependenciesAreStableForTheContainerLifetime() {
        val readingStatsRepository = EmptyReadingStatsRepository
        val savedStoryRepository = EmptySavedStoryRepository
        val storyNoteRepository = EmptyStoryNoteRepository
        val storyReflectionRepository = EmptyStoryReflectionRepository
        val container =
            DefaultAppContainer(
                readingStatsRepository = readingStatsRepository,
                savedStoryRepository = savedStoryRepository,
                storyNoteRepository = storyNoteRepository,
                storyReflectionRepository = storyReflectionRepository,
            )

        assertSame(container.navigator, container.navigator)
        assertSame(container.articleApiClient, container.articleApiClient)
        assertSame(container.userPreferencesRepository, container.userPreferencesRepository)
        assertSame(readingStatsRepository, container.readingStatsRepository)
        assertSame(savedStoryRepository, container.savedStoryRepository)
        assertSame(storyNoteRepository, container.storyNoteRepository)
        assertSame(storyReflectionRepository, container.storyReflectionRepository)
        assertSame(container.widgetDataProvider, container.widgetDataProvider)
    }
}

private object EmptyReadingStatsRepository : ReadingStatsRepository {
    override fun observeStats(recentDayCount: Int): Flow<ReadingStats> = emptyFlow()

    override suspend fun recordStoryOpen(article: Article) = Unit

    override suspend fun recordOriginalStoryOpen() = Unit

    override suspend fun lastOpenedAt(storyId: StoryId): Instant? = null
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

private object EmptyStoryNoteRepository : StoryNoteRepository {
    override val count: Flow<Int> = emptyFlow()

    override fun observeNote(article: Article): Flow<StoryNote?> = emptyFlow()

    override suspend fun findNote(article: Article): StoryNote? = null

    override suspend fun setNote(
        article: Article,
        text: String,
    ) = Unit

    override suspend fun clearNote(article: Article) = Unit
}

private object EmptyStoryReflectionRepository : StoryReflectionRepository {
    override val count: Flow<Int> = emptyFlow()

    override fun observeReflection(article: Article): Flow<StoryReflection?> = emptyFlow()

    override suspend fun findReflection(article: Article): StoryReflection? = null

    override suspend fun setReaction(
        article: Article,
        reaction: StoryReflectionReaction,
    ) = Unit
}
