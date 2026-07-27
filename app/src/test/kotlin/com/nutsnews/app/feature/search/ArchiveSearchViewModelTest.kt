package com.nutsnews.app.feature.search

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.article.ArchiveArticleSearchSource
import com.nutsnews.app.data.article.NutsNewsApiException
import com.nutsnews.app.data.article.NutsNewsFetchPolicy
import com.nutsnews.app.data.story.SavedStoryRepository
import java.io.IOException
import java.net.URI
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveSearchViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun typingUsesTwoCharacterMinimumAndDebouncesTheNormalizedQuery() =
        runTest(mainDispatcher) {
            val source =
                FakeArchiveArticleSearchSource { request ->
                    ArticlesResponse(
                        articles = listOf(searchArticle(request.query)),
                        nextPage = null,
                    )
                }
            val viewModel =
                ArchiveSearchViewModel(
                    articleSearchSource = source,
                    savedStoryRepository = FakeSearchSavedStoryRepository(),
                    debounceMillis = 350,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.onQueryChanged("s")
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(emptyList(), source.requests)
            assertTrue(viewModel.uiState.value.showShortQueryHint)
            assertFalse(viewModel.uiState.value.hasSearched)

            viewModel.onQueryChanged("  good")
            advanceTimeBy(200)
            viewModel.onQueryChanged("  good   science  ")
            advanceTimeBy(349)
            runCurrent()
            assertEquals(emptyList(), source.requests)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(1, source.requests.size)
            assertEquals("good science", source.requests.single().query)
            assertEquals(NutsNewsFetchPolicy.UseCache, source.requests.single().fetchPolicy)
            assertEquals("good science", viewModel.uiState.value.searchedQuery)
            assertEquals(listOf(searchArticle("good science")), viewModel.uiState.value.articles)
        }

    @Test
    fun pagingUsesCachedPagesAndDeduplicatesOverlappingResults() =
        runTest(mainDispatcher) {
            val first = searchArticle("first")
            val duplicate = searchArticle("duplicate")
            val last = searchArticle("last")
            val source =
                FakeArchiveArticleSearchSource { request ->
                    when (request.page) {
                        0 -> ArticlesResponse(listOf(first, duplicate), nextPage = 1)
                        1 -> ArticlesResponse(listOf(duplicate, last), nextPage = null)
                        else -> error("Unexpected page ${request.page}")
                    }
                }
            val viewModel =
                ArchiveSearchViewModel(
                    articleSearchSource = source,
                    savedStoryRepository = FakeSearchSavedStoryRepository(),
                    debounceMillis = 350,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.onQueryChanged("community")
            viewModel.submitSearch()
            runCurrent()

            assertEquals(listOf(first, duplicate), viewModel.uiState.value.articles)
            assertTrue(viewModel.uiState.value.canLoadMore)

            viewModel.loadMore()
            runCurrent()

            assertEquals(listOf(first, duplicate, last), viewModel.uiState.value.articles)
            assertFalse(viewModel.uiState.value.canLoadMore)
            assertEquals(listOf(0, 1), source.requests.map(SearchRequestRecord::page))
            assertTrue(
                source.requests.all { request ->
                    request.fetchPolicy == NutsNewsFetchPolicy.UseCache
                },
            )
        }

    @Test
    fun failureKeepsARecoverableStateAndRetryBypassesFreshCache() =
        runTest(mainDispatcher) {
            var shouldFail = true
            val result = searchArticle("recovered")
            val source =
                FakeArchiveArticleSearchSource {
                    if (shouldFail) {
                        shouldFail = false
                        throw NutsNewsApiException.Network(IOException("offline"))
                    }
                    ArticlesResponse(listOf(result), nextPage = null)
                }
            val viewModel =
                ArchiveSearchViewModel(
                    articleSearchSource = source,
                    savedStoryRepository = FakeSearchSavedStoryRepository(),
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.onQueryChanged("animals")
            viewModel.submitSearch()
            runCurrent()

            assertEquals(
                "NutsNews could not reach the archive. Check your connection and try again.",
                viewModel.uiState.value.errorMessage,
            )
            assertEquals(0, viewModel.uiState.value.failedPage)
            assertEquals(emptyList(), viewModel.uiState.value.articles)

            viewModel.retry()
            runCurrent()

            assertEquals(listOf(result), viewModel.uiState.value.articles)
            assertEquals(null, viewModel.uiState.value.errorMessage)
            assertEquals(
                listOf(
                    NutsNewsFetchPolicy.UseCache,
                    NutsNewsFetchPolicy.ReloadIgnoringCache,
                ),
                source.requests.map(SearchRequestRecord::fetchPolicy),
            )
        }

    @Test
    fun savingAndRemovingAResultUpdatesPersistentSavedState() =
        runTest(mainDispatcher) {
            val article = searchArticle("saved")
            val repository = FakeSearchSavedStoryRepository()
            val viewModel =
                ArchiveSearchViewModel(
                    articleSearchSource =
                        FakeArchiveArticleSearchSource {
                            ArticlesResponse(listOf(article), nextPage = null)
                        },
                    savedStoryRepository = repository,
                )
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.toggleSaved(article)
            runCurrent()

            assertTrue(article.stableId in viewModel.uiState.value.savedStoryIds)
            assertTrue(repository.isLiked(article.stableId))

            viewModel.toggleSaved(article)
            runCurrent()

            assertFalse(article.stableId in viewModel.uiState.value.savedStoryIds)
            assertFalse(repository.isLiked(article.stableId))
        }
}

private data class SearchRequestRecord(
    val query: String,
    val page: Int,
    val limit: Int,
    val fetchPolicy: NutsNewsFetchPolicy,
)

private class FakeArchiveArticleSearchSource(
    private val response: suspend (SearchRequestRecord) -> ArticlesResponse,
) : ArchiveArticleSearchSource {
    val requests = mutableListOf<SearchRequestRecord>()

    override suspend fun searchArticles(
        query: String,
        page: Int,
        limit: Int,
        fetchPolicy: NutsNewsFetchPolicy,
    ): ArticlesResponse {
        val request =
            SearchRequestRecord(
                query = query,
                page = page,
                limit = limit,
                fetchPolicy = fetchPolicy,
            )
        requests += request
        return response(request)
    }
}

private class FakeSearchSavedStoryRepository : SavedStoryRepository {
    private val mutableStories = MutableStateFlow<List<SavedStory>>(emptyList())

    override val stories: Flow<List<SavedStory>> = mutableStories
    override val count: Flow<Int> = mutableStories.map { stories -> stories.size }

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> =
        mutableStories.map { stories -> stories.any { story -> story.id == storyId } }

    override suspend fun isLiked(storyId: StoryId): Boolean =
        mutableStories.value.any { story -> story.id == storyId }

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) {
        if (isLiked) save(article) else remove(article.stableId)
    }

    override suspend fun save(article: Article) {
        mutableStories.value =
            listOf(SavedStory(article, Instant.parse("2026-07-26T12:00:00Z"))) +
                mutableStories.value.filterNot { story -> story.id == article.stableId }
    }

    override suspend fun remove(storyId: StoryId) {
        mutableStories.value =
            mutableStories.value.filterNot { story -> story.id == storyId }
    }
}

private fun searchArticle(id: String): Article =
    Article(
        id = id,
        title = "Hopeful $id story",
        summary = "A bright update about $id.",
        originalUrl = URI("https://example.com/${id.replace(' ', '-')}"),
        source = "Good News Daily",
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = null,
        categories = listOf("Community"),
    )
