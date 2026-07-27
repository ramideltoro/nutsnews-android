package com.nutsnews.app.feature.feed

import androidx.lifecycle.SavedStateHandle
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.data.article.ArticleFetchResult
import com.nutsnews.app.data.article.ArticleFetchSource
import com.nutsnews.app.data.article.FeedArticleSource
import com.nutsnews.app.data.article.NutsNewsFetchPolicy
import java.io.IOException
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleFeedViewModelTest {
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
    fun initialLoadExposesLoadingDeduplicatesAndMergesCategories() =
        runTest(mainDispatcher) {
            val source = ControlledFeedArticleSource()
            val viewModel = ArticleFeedViewModel(source)

            viewModel.loadInitialArticles()

            assertTrue(viewModel.uiState.value.isInitialLoading)
            assertTrue(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isEmpty)
            runCurrent()
            source.nextRequest().succeed(
                page(
                    articles =
                        listOf(
                            article("one", listOf(" Science ", "COMMUNITY", "")),
                            article("one", listOf("Ignored duplicate")),
                            article("two", listOf("science", "Animals")),
                        ),
                    nextPage = 1,
                ),
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf("one", "two"), state.articles.map(Article::id))
            assertEquals(
                listOf("Science", "COMMUNITY", "Ignored duplicate", "Animals"),
                state.availableCategories,
            )
            assertEquals(1, state.nextPage)
            assertTrue(state.canLoadMore)
            assertFalse(state.isLoading)
            assertFalse(state.isStale)
            assertNull(state.errorMessage)

            viewModel.loadInitialArticles()
            runCurrent()
            assertTrue(source.requests.tryReceive().isFailure)
        }

    @Test
    fun categoryAndForcedRefreshExposeRefreshingEmptyAndStaleStates() =
        runTest(mainDispatcher) {
            val source = ControlledFeedArticleSource()
            val viewModel = ArticleFeedViewModel(source)
            viewModel.loadInitialArticles()
            runCurrent()
            source.nextRequest().succeed(
                page(
                    articles = listOf(article("all", listOf("Community"))),
                    nextPage = null,
                ),
            )
            advanceUntilIdle()

            viewModel.applyCategory(" Science ")

            val refreshing = viewModel.uiState.value
            assertEquals("Science", refreshing.selectedCategory)
            assertTrue(refreshing.isRefreshing)
            assertFalse(refreshing.isInitialLoading)
            runCurrent()
            val categoryRequest = source.nextRequest()
            assertEquals(0, categoryRequest.page)
            assertEquals("Science", categoryRequest.category)
            assertEquals(NutsNewsFetchPolicy.UseCache, categoryRequest.fetchPolicy)
            categoryRequest.succeed(
                page(
                    articles = emptyList(),
                    nextPage = null,
                    source = ArticleFetchSource.StaleCache,
                ),
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isEmpty)
            assertTrue(viewModel.uiState.value.isStale)
            assertEquals(listOf("Community"), viewModel.uiState.value.availableCategories)

            viewModel.forceRefresh()

            assertTrue(viewModel.uiState.value.isInitialLoading)
            runCurrent()
            val forcedRequest = source.nextRequest()
            assertEquals("Science", forcedRequest.category)
            assertEquals(
                NutsNewsFetchPolicy.ReloadIgnoringCache,
                forcedRequest.fetchPolicy,
            )
            forcedRequest.succeed(
                page(
                    articles = listOf(article("science", listOf("SCIENCE"))),
                    nextPage = null,
                ),
            )
            advanceUntilIdle()

            assertEquals(listOf("science"), viewModel.uiState.value.articles.map(Article::id))
            assertFalse(viewModel.uiState.value.isStale)
        }

    @Test
    fun processRecreationRestoresVisibleStaleFeedWithoutDuplicateFetch() =
        runTest(mainDispatcher) {
            val firstSource = ControlledFeedArticleSource()
            val savedState = SavedStateHandle()
            val original =
                ArticleFeedViewModel(
                    articleSource = firstSource,
                    savedStateHandle = savedState,
                )
            original.refresh(category = "Science")
            runCurrent()
            val expected = article("restored", listOf("Science", "Discovery"))
            firstSource.nextRequest().succeed(
                page(
                    articles = listOf(expected),
                    nextPage = 2,
                    source = ArticleFetchSource.StaleCache,
                ),
            )
            advanceUntilIdle()

            val recreatedSource = ControlledFeedArticleSource()
            val recreated =
                ArticleFeedViewModel(
                    articleSource = recreatedSource,
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf(
                                FeedResponseStateKey to
                                    savedState.get<String>(FeedResponseStateKey),
                                FeedCategoriesStateKey to
                                    savedState.get<ArrayList<String>>(FeedCategoriesStateKey),
                                FeedCategoryStateKey to
                                    savedState.get<String>(FeedCategoryStateKey),
                                FeedStaleStateKey to
                                    savedState.get<Boolean>(FeedStaleStateKey),
                            ),
                        ),
                )

            assertEquals(listOf(expected), recreated.uiState.value.articles)
            assertEquals(listOf("Science", "Discovery"), recreated.uiState.value.availableCategories)
            assertEquals("Science", recreated.uiState.value.selectedCategory)
            assertEquals(2, recreated.uiState.value.nextPage)
            assertTrue(recreated.uiState.value.isStale)

            recreated.loadInitialArticles()
            runCurrent()
            assertTrue(recreatedSource.requests.tryReceive().isFailure)
        }

    @Test
    fun lastArticlePaginatesOnceAndAppendsOnlyUniqueArticles() =
        runTest(mainDispatcher) {
            val source = ControlledFeedArticleSource()
            val viewModel = ArticleFeedViewModel(source)
            val first = article("one", listOf("Science"))
            val last = article("two", listOf("Community"))
            viewModel.loadInitialArticles()
            runCurrent()
            source.nextRequest().succeed(
                page(
                    articles = listOf(first, last),
                    nextPage = 3,
                ),
            )
            advanceUntilIdle()

            viewModel.loadMoreIfNeeded(first)
            runCurrent()
            assertTrue(source.requests.tryReceive().isFailure)

            viewModel.loadMoreIfNeeded(last)
            viewModel.loadMore()

            assertTrue(viewModel.uiState.value.isPaginating)
            assertFalse(viewModel.uiState.value.canLoadMore)
            runCurrent()
            val paginationRequest = source.nextRequest()
            assertEquals(3, paginationRequest.page)
            paginationRequest.succeed(
                page(
                    articles =
                        listOf(
                            article("two", listOf("COMMUNITY")),
                            article("three", listOf("Nature", " science ")),
                            article("three", listOf("Ignored duplicate")),
                        ),
                    nextPage = null,
                ),
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf("one", "two", "three"), state.articles.map(Article::id))
            assertEquals(
                listOf("Science", "Community", "Nature", "Ignored duplicate"),
                state.availableCategories,
            )
            assertFalse(state.isPaginating)
            assertFalse(state.canLoadMore)
        }

    @Test
    fun failuresExposeErrorsAndRetryTheFailedOperation() =
        runTest(mainDispatcher) {
            val source = ControlledFeedArticleSource()
            val viewModel = ArticleFeedViewModel(source)

            viewModel.loadInitialArticles()
            runCurrent()
            source.nextRequest().fail(IOException("You’re offline."))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isEmpty)
            assertEquals("You’re offline.", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)

            viewModel.retry()
            runCurrent()
            val refreshRetry = source.nextRequest()
            assertEquals(0, refreshRetry.page)
            refreshRetry.succeed(
                page(
                    articles = listOf(article("one")),
                    nextPage = 1,
                ),
            )
            advanceUntilIdle()

            viewModel.loadMore()
            runCurrent()
            source.nextRequest().fail(IOException())
            advanceUntilIdle()

            assertEquals(listOf("one"), viewModel.uiState.value.articles.map(Article::id))
            assertEquals(
                "Couldn’t load good news. Try again.",
                viewModel.uiState.value.errorMessage,
            )

            viewModel.retry()
            runCurrent()
            val paginationRetry = source.nextRequest()
            assertEquals(1, paginationRetry.page)
            paginationRetry.succeed(
                page(
                    articles = listOf(article("two")),
                    nextPage = null,
                ),
            )
            advanceUntilIdle()

            assertEquals(listOf("one", "two"), viewModel.uiState.value.articles.map(Article::id))
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun lateCategoryResponseCannotReplaceTheLatestSelection() =
        runTest(mainDispatcher) {
            val source = ControlledFeedArticleSource()
            val viewModel = ArticleFeedViewModel(source)

            viewModel.loadInitialArticles()
            runCurrent()
            val oldRequest = source.nextRequest()

            viewModel.applyCategory("Science")
            runCurrent()
            val latestRequest = source.nextRequest()
            latestRequest.succeed(
                page(
                    articles = listOf(article("science")),
                    nextPage = null,
                ),
            )
            runCurrent()

            oldRequest.succeed(
                page(
                    articles = listOf(article("old-all")),
                    nextPage = 1,
                ),
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Science", state.selectedCategory)
            assertEquals(listOf("science"), state.articles.map(Article::id))
            assertNull(state.nextPage)
        }

    @Test
    fun refreshSupersedesLatePaginationWithoutLosingFreshContent() =
        runTest(mainDispatcher) {
            val source = ControlledFeedArticleSource()
            val viewModel = ArticleFeedViewModel(source)
            viewModel.loadInitialArticles()
            runCurrent()
            source.nextRequest().succeed(
                page(
                    articles = listOf(article("initial")),
                    nextPage = 1,
                ),
            )
            advanceUntilIdle()

            viewModel.loadMore()
            runCurrent()
            val oldPageRequest = source.nextRequest()
            viewModel.forceRefresh()
            runCurrent()
            val refreshRequest = source.nextRequest()
            refreshRequest.succeed(
                page(
                    articles = listOf(article("fresh")),
                    nextPage = null,
                ),
            )
            runCurrent()

            oldPageRequest.succeed(
                page(
                    articles = listOf(article("late-page")),
                    nextPage = 2,
                ),
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf("fresh"), state.articles.map(Article::id))
            assertNull(state.nextPage)
            assertFalse(state.isLoading)
        }
}

private class ControlledFeedArticleSource : FeedArticleSource {
    val requests = Channel<ControlledRequest>(capacity = Channel.UNLIMITED)

    override suspend fun fetchFeedPage(
        page: Int,
        category: String?,
        fetchPolicy: NutsNewsFetchPolicy,
    ): ArticleFetchResult {
        val request =
            ControlledRequest(
                page = page,
                category = category,
                fetchPolicy = fetchPolicy,
            )
        requests.send(request)
        return withContext(NonCancellable) {
            request.outcome.await().getOrThrow()
        }
    }

    suspend fun nextRequest(): ControlledRequest = requests.receive()
}

private data class ControlledRequest(
    val page: Int,
    val category: String?,
    val fetchPolicy: NutsNewsFetchPolicy,
    val outcome: CompletableDeferred<Result<ArticleFetchResult>> = CompletableDeferred(),
) {
    fun succeed(result: ArticleFetchResult) {
        outcome.complete(Result.success(result))
    }

    fun fail(error: Throwable) {
        outcome.complete(Result.failure(error))
    }
}

private fun page(
    articles: List<Article>,
    nextPage: Int?,
    source: ArticleFetchSource = ArticleFetchSource.Network,
): ArticleFetchResult =
    ArticleFetchResult(
        response =
            ArticlesResponse(
                articles = articles,
                nextPage = nextPage,
            ),
        source = source,
    )

private fun article(
    id: String,
    categories: List<String> = emptyList(),
): Article =
    Article(
        id = id,
        title = "Title $id",
        summary = "Summary $id",
        originalUrl = URI("https://example.com/$id"),
        source = "Example",
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = URI("https://example.com/$id.jpg"),
        categories = categories,
    )
