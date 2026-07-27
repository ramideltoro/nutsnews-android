package com.nutsnews.app.feature.article

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import java.net.URI
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun likeTogglesStaySynchronizedWithSavedStoryState() =
        runTest(dispatcher) {
            val article = detailArticle()
            val saved = DetailSavedStoryRepository()
            val viewModel = ArticleDetailViewModel(saved, DetailReadingStatsRepository())

            viewModel.toggleLiked(article)
            advanceUntilIdle()
            assertTrue(saved.isLiked(article.stableId))
            assertTrue(
                article.stableId in
                    viewModel.uiState.first { article.stableId in it.likedStoryIds }
                        .likedStoryIds,
            )

            viewModel.toggleLiked(article)
            advanceUntilIdle()
            assertFalse(saved.isLiked(article.stableId))
            assertEquals(listOf(true, false), saved.changes)
        }

    @Test
    fun repeatedDetailAppearancesRemainOneUniqueStoryInStats() =
        runTest(dispatcher) {
            val article = detailArticle()
            val stats = DetailReadingStatsRepository()
            val viewModel = ArticleDetailViewModel(DetailSavedStoryRepository(), stats)

            viewModel.onArticleShown(article)
            viewModel.onArticleShown(article)
            advanceUntilIdle()

            assertEquals(2, stats.storyOpenCalls)
            assertEquals(
                1,
                viewModel.uiState.first { it.readingStats?.todayStoryCount == 1 }
                    .readingStats
                    ?.totalUniqueStoryCount,
            )
        }

    @Test
    fun everyOriginalStoryOpenUpdatesObservableStats() =
        runTest(dispatcher) {
            val stats = DetailReadingStatsRepository()
            val viewModel = ArticleDetailViewModel(DetailSavedStoryRepository(), stats)

            viewModel.onOriginalStoryOpened()
            viewModel.onOriginalStoryOpened()
            advanceUntilIdle()

            assertEquals(2, stats.originalOpenCalls)
            assertEquals(
                2,
                viewModel.uiState.first {
                    it.readingStats?.originalOpensTodayCount == 2
                }.readingStats
                    ?.originalOpensTodayCount,
            )
        }
}

private class DetailSavedStoryRepository : SavedStoryRepository {
    override val stories = MutableStateFlow<List<SavedStory>>(emptyList())
    override val count: Flow<Int> = stories.map { it.size }
    val changes = mutableListOf<Boolean>()

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> =
        stories.map { saved -> saved.any { it.id == storyId } }

    override suspend fun isLiked(storyId: StoryId): Boolean =
        stories.value.any { it.id == storyId }

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) {
        changes += isLiked
        if (isLiked) save(article) else remove(article.stableId)
    }

    override suspend fun save(article: Article) {
        stories.value = listOf(SavedStory(article, Instant.EPOCH))
    }

    override suspend fun remove(storyId: StoryId) {
        stories.value = stories.value.filterNot { it.id == storyId }
    }
}

private class DetailReadingStatsRepository : ReadingStatsRepository {
    private val openedStories = linkedSetOf<StoryId>()
    private val stats =
        MutableStateFlow(
            ReadingStats(
                todayStoryCount = 0,
                originalOpensTodayCount = 0,
                totalUniqueStoryCount = 0,
                currentStreak = 0,
                recentDays = emptyList(),
            ),
        )
    var storyOpenCalls = 0
    var originalOpenCalls = 0

    override fun observeStats(recentDayCount: Int): Flow<ReadingStats> = stats

    override suspend fun recordStoryOpen(article: Article) {
        storyOpenCalls += 1
        openedStories += article.stableId
        stats.value =
            stats.value.copy(
                todayStoryCount = openedStories.size,
                totalUniqueStoryCount = openedStories.size,
                currentStreak = if (openedStories.isEmpty()) 0 else 1,
            )
    }

    override suspend fun recordOriginalStoryOpen() {
        originalOpenCalls += 1
        stats.value =
            stats.value.copy(
                originalOpensTodayCount = stats.value.originalOpensTodayCount + 1,
            )
    }

    override suspend fun lastOpenedAt(storyId: StoryId): Instant? = null
}

private fun detailArticle() =
    Article(
        id = "detail-actions",
        title = "A hopeful detail story",
        summary = "People helped each other.",
        originalUrl = URI("https://example.com/detail-actions"),
        source = "NutsNews",
        publishedAt = "Published today",
        createdAt = null,
        thumbnailUrl = null,
        categories = listOf("Community"),
    )
