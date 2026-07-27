package com.nutsnews.app.feature.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.data.article.ArticleStateCodec
import com.nutsnews.app.data.article.FeedArticleSource
import com.nutsnews.app.data.article.NutsNewsFetchPolicy
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArticleFeedViewModel(
    private val articleSource: FeedArticleSource,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val restoredUiState = restoreFeedState(savedStateHandle)
    private val mutableUiState =
        MutableStateFlow(restoredUiState ?: ArticleFeedUiState())
    val uiState: StateFlow<ArticleFeedUiState> = mutableUiState.asStateFlow()

    private var refreshJob: Job? = null
    private var paginationJob: Job? = null
    private var requestGeneration = 0L
    private var completedInitialLoad = restoredUiState?.articles?.isNotEmpty() == true
    private var failedRequest: FailedRequest? = null

    fun loadInitialArticles() {
        val state = mutableUiState.value
        if (
            completedInitialLoad ||
            state.articles.isNotEmpty() ||
            state.isLoading
        ) {
            return
        }
        refresh()
    }

    fun refresh(
        category: String? = mutableUiState.value.selectedCategory,
        forceReload: Boolean = false,
    ) {
        val normalizedCategory = normalizeCategory(category)
        val generation = startNewGeneration()
        val hasContent = mutableUiState.value.articles.isNotEmpty()
        failedRequest = null
        mutableUiState.update { current ->
            current.copy(
                selectedCategory = normalizedCategory,
                isInitialLoading = !hasContent,
                isRefreshing = hasContent,
                isPaginating = false,
                errorMessage = null,
            )
        }

        refreshJob =
            viewModelScope.launch {
                try {
                    val result =
                        articleSource.fetchFeedPage(
                            page = FirstPage,
                            category = normalizedCategory,
                            fetchPolicy =
                                if (forceReload) {
                                    NutsNewsFetchPolicy.ReloadIgnoringCache
                                } else {
                                    NutsNewsFetchPolicy.UseCache
                                },
                        )
                    if (generation != requestGeneration) return@launch

                    val responseArticles = result.response.articles
                    val articles = deduplicateArticles(responseArticles)
                    completedInitialLoad = true
                    mutableUiState.update { current ->
                        current.copy(
                            articles = articles,
                            availableCategories =
                                mergeCategories(
                                    existing = current.availableCategories,
                                    articles = responseArticles,
                                ),
                            nextPage = result.response.nextPage,
                            isInitialLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            isStale = result.isStale,
                            errorMessage = null,
                        )
                    }
                    persistRestorableState()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (generation != requestGeneration) return@launch
                    failedRequest =
                        FailedRequest.Refresh(
                            category = normalizedCategory,
                            forceReload = forceReload,
                        )
                    mutableUiState.update { current ->
                        current.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            errorMessage = error.userFacingMessage(),
                        )
                    }
                }
            }
    }

    fun applyCategory(category: String?) {
        refresh(category = category)
    }

    fun forceRefresh() {
        refresh(forceReload = true)
    }

    fun loadMoreIfNeeded(currentArticle: Article) {
        if (mutableUiState.value.articles.lastOrNull()?.id == currentArticle.id) {
            loadMore()
        }
    }

    fun loadMore() {
        val state = mutableUiState.value
        val page = state.nextPage ?: return
        if (state.isLoading) return

        val generation = requestGeneration
        val category = state.selectedCategory
        failedRequest = null
        mutableUiState.update { current ->
            current.copy(
                isPaginating = true,
                errorMessage = null,
            )
        }

        paginationJob =
            viewModelScope.launch {
                try {
                    val result =
                        articleSource.fetchFeedPage(
                            page = page,
                            category = category,
                            fetchPolicy = NutsNewsFetchPolicy.UseCache,
                        )
                    if (generation != requestGeneration) return@launch

                    mutableUiState.update { current ->
                        val articles =
                            appendUniqueArticles(
                                existing = current.articles,
                                incoming = result.response.articles,
                            )
                        current.copy(
                            articles = articles,
                            availableCategories =
                                mergeCategories(
                                    existing = current.availableCategories,
                                    articles = result.response.articles,
                                ),
                            nextPage = result.response.nextPage,
                            isPaginating = false,
                            isStale = current.isStale || result.isStale,
                            errorMessage = null,
                        )
                    }
                    persistRestorableState()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (generation != requestGeneration) return@launch
                    failedRequest = FailedRequest.LoadMore
                    mutableUiState.update { current ->
                        current.copy(
                            isPaginating = false,
                            errorMessage = error.userFacingMessage(),
                        )
                    }
                }
            }
    }

    fun retry() {
        when (val request = failedRequest) {
            FailedRequest.LoadMore -> loadMore()
            is FailedRequest.Refresh ->
                refresh(
                    category = request.category,
                    forceReload = request.forceReload,
                )
            null -> forceRefresh()
        }
    }

    private fun startNewGeneration(): Long {
        requestGeneration += 1
        refreshJob?.cancel()
        paginationJob?.cancel()
        return requestGeneration
    }

    private sealed interface FailedRequest {
        data class Refresh(
            val category: String?,
            val forceReload: Boolean,
        ) : FailedRequest

        data object LoadMore : FailedRequest
    }

    class Factory(
        private val articleSource: FeedArticleSource,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ArticleFeedViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ArticleFeedViewModel(articleSource) as T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T {
            require(modelClass.isAssignableFrom(ArticleFeedViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ArticleFeedViewModel(
                articleSource = articleSource,
                savedStateHandle = extras.createSavedStateHandle(),
            ) as T
        }
    }

    private fun persistRestorableState() {
        val state = mutableUiState.value
        savedStateHandle[FeedResponseStateKey] =
            ArticleStateCodec.encode(
                ArticlesResponse(
                    articles = state.articles,
                    nextPage = state.nextPage,
                ),
            )
        savedStateHandle[FeedCategoriesStateKey] = ArrayList(state.availableCategories)
        state.selectedCategory?.let { category ->
            savedStateHandle[FeedCategoryStateKey] = category
        } ?: savedStateHandle.remove<String>(FeedCategoryStateKey)
        savedStateHandle[FeedStaleStateKey] = state.isStale
    }

    private companion object {
        const val FirstPage = 0
    }
}

private fun restoreFeedState(savedStateHandle: SavedStateHandle): ArticleFeedUiState? {
    val response =
        ArticleStateCodec.decodeOrNull(
            savedStateHandle[FeedResponseStateKey],
        ) ?: return null
    return ArticleFeedUiState(
        articles = response.articles,
        availableCategories =
            savedStateHandle.get<ArrayList<String>>(FeedCategoriesStateKey)
                ?.toList()
                .orEmpty(),
        selectedCategory = savedStateHandle[FeedCategoryStateKey],
        nextPage = response.nextPage,
        isStale = savedStateHandle[FeedStaleStateKey] ?: false,
    )
}

internal const val FeedResponseStateKey = "feed.response"
internal const val FeedCategoriesStateKey = "feed.categories"
internal const val FeedCategoryStateKey = "feed.category"
internal const val FeedStaleStateKey = "feed.stale"

internal fun normalizeCategory(category: String?): String? =
    category
        ?.trim()
        ?.takeIf(String::isNotEmpty)

internal fun deduplicateArticles(articles: List<Article>): List<Article> {
    val seenIds = mutableSetOf<String>()
    return articles.filter { article -> seenIds.add(article.id) }
}

internal fun appendUniqueArticles(
    existing: List<Article>,
    incoming: List<Article>,
): List<Article> {
    val seenIds = existing.mapTo(mutableSetOf()) { article -> article.id }
    return buildList {
        addAll(existing)
        incoming.forEach { article ->
            if (seenIds.add(article.id)) add(article)
        }
    }
}

internal fun mergeCategories(
    existing: List<String>,
    articles: List<Article>,
): List<String> {
    val merged = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    (existing.asSequence() + articles.asSequence().flatMap { it.categories.asSequence() })
        .forEach { category ->
            val cleaned = category.trim()
            if (cleaned.isNotEmpty() && seen.add(cleaned.lowercase(Locale.ROOT))) {
                merged += cleaned
            }
        }
    return merged
}

private fun Throwable.userFacingMessage(): String =
    message
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: "Couldn’t load good news. Try again."
