package com.nutsnews.app.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.article.ArchiveArticleSearchSource
import com.nutsnews.app.data.article.ArchiveSearchRequest
import com.nutsnews.app.data.article.ArticleStateCodec
import com.nutsnews.app.data.article.NutsNewsApiException
import com.nutsnews.app.data.article.NutsNewsFetchPolicy
import com.nutsnews.app.data.story.SavedStoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ArchiveSearchUiState(
    val query: String = "",
    val searchedQuery: String = "",
    val articles: List<Article> = emptyList(),
    val savedStoryIds: Set<StoryId> = emptySet(),
    val nextPage: Int? = null,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
    val failedPage: Int? = null,
) {
    val queryRequest: ArchiveSearchRequest
        get() = ArchiveSearchRequest.create(query)

    val queryMeetsMinimum: Boolean
        get() = queryRequest.meetsMinimum

    val showShortQueryHint: Boolean
        get() = query.isNotBlank() && !queryMeetsMinimum

    val canSubmit: Boolean
        get() = queryMeetsMinimum && !isSearching

    val canLoadMore: Boolean
        get() =
            nextPage != null &&
                !isSearching &&
                !isLoadingMore &&
                errorMessage == null

    val isInitialLoading: Boolean
        get() = isSearching && articles.isEmpty()
}

class ArchiveSearchViewModel(
    private val articleSearchSource: ArchiveArticleSearchSource,
    private val savedStoryRepository: SavedStoryRepository,
    private val debounceMillis: Long = SearchDebounceMillis,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val mutableSearchState =
        MutableStateFlow(restoreArchiveSearchState(savedStateHandle))
    private val saveMutex = Mutex()
    private var debounceJob: Job? = null
    private var searchGeneration = 0L

    val uiState: StateFlow<ArchiveSearchUiState> =
        combine(
            mutableSearchState,
            savedStoryRepository.stories,
        ) { searchState, savedStories ->
            searchState.copy(
                savedStoryIds =
                    savedStories.mapTo(linkedSetOf()) { story -> story.id },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = mutableSearchState.value,
        )

    fun onQueryChanged(value: String) {
        val request = ArchiveSearchRequest.create(value)
        searchGeneration += 1
        debounceJob?.cancel()

        mutableSearchState.update { current ->
            if (request.meetsMinimum) {
                current.copy(query = value)
            } else {
                ArchiveSearchUiState(query = value)
            }
        }
        savedStateHandle[ArchiveSearchQueryStateKey] = value

        if (!request.meetsMinimum) {
            clearPersistedResults()
            return
        }

        val generation = searchGeneration
        debounceJob =
            viewModelScope.launch {
                delay(debounceMillis)
                searchFirstPage(
                    request = request,
                    generation = generation,
                    fetchPolicy = NutsNewsFetchPolicy.UseCache,
                )
            }
    }

    fun submitSearch() {
        val request = ArchiveSearchRequest.create(mutableSearchState.value.query)
        searchGeneration += 1
        debounceJob?.cancel()

        if (!request.meetsMinimum) {
            mutableSearchState.value =
                ArchiveSearchUiState(query = mutableSearchState.value.query)
            return
        }

        val generation = searchGeneration
        mutableSearchState.update { current ->
            current.copy(query = request.query)
        }
        savedStateHandle[ArchiveSearchQueryStateKey] = request.query
        viewModelScope.launch {
            searchFirstPage(
                request = request,
                generation = generation,
                fetchPolicy = NutsNewsFetchPolicy.UseCache,
            )
        }
    }

    fun loadMore() {
        val current = mutableSearchState.value
        val page = current.nextPage ?: return
        if (!current.canLoadMore) return

        val generation = searchGeneration
        viewModelScope.launch {
            loadPage(
                query = current.searchedQuery,
                page = page,
                generation = generation,
                fetchPolicy = NutsNewsFetchPolicy.UseCache,
            )
        }
    }

    fun retry() {
        val current = mutableSearchState.value
        val page = current.failedPage ?: return
        val query =
            current.searchedQuery.ifBlank {
                ArchiveSearchRequest.create(current.query).query
            }
        if (!ArchiveSearchRequest.create(query).meetsMinimum) return

        searchGeneration += 1
        debounceJob?.cancel()
        val generation = searchGeneration
        viewModelScope.launch {
            if (page == FirstPage) {
                searchFirstPage(
                    request = ArchiveSearchRequest.create(query),
                    generation = generation,
                    fetchPolicy = NutsNewsFetchPolicy.ReloadIgnoringCache,
                )
            } else {
                loadPage(
                    query = query,
                    page = page,
                    generation = generation,
                    fetchPolicy = NutsNewsFetchPolicy.ReloadIgnoringCache,
                )
            }
        }
    }

    fun clearSearch() {
        searchGeneration += 1
        debounceJob?.cancel()
        mutableSearchState.value = ArchiveSearchUiState()
        savedStateHandle.remove<String>(ArchiveSearchQueryStateKey)
        clearPersistedResults()
    }

    fun toggleSaved(article: Article) {
        viewModelScope.launch {
            saveMutex.withLock {
                val isSaved = savedStoryRepository.isLiked(article.stableId)
                savedStoryRepository.setLiked(
                    article = article,
                    isLiked = !isSaved,
                )
            }
        }
    }

    private suspend fun searchFirstPage(
        request: ArchiveSearchRequest,
        generation: Long,
        fetchPolicy: NutsNewsFetchPolicy,
    ) {
        if (generation != searchGeneration) return
        mutableSearchState.update { current ->
            current.copy(
                query = request.query,
                searchedQuery = request.query,
                articles = emptyList(),
                nextPage = null,
                isSearching = true,
                isLoadingMore = false,
                hasSearched = true,
                errorMessage = null,
                failedPage = null,
            )
        }

        try {
            val response =
                articleSearchSource.searchArticles(
                    query = request.query,
                    page = FirstPage,
                    limit = ArchiveSearchRequest.DefaultPageSize,
                    fetchPolicy = fetchPolicy,
                )
            if (generation != searchGeneration) return
            mutableSearchState.update { current ->
                current.copy(
                    articles = response.articles.distinctBy { article -> article.stableId },
                    nextPage = response.nextPage,
                    isSearching = false,
                )
            }
            persistRestorableResults()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (generation != searchGeneration) return
            mutableSearchState.update { current ->
                current.copy(
                    articles = emptyList(),
                    nextPage = null,
                    isSearching = false,
                    errorMessage = error.archiveSearchMessage(),
                    failedPage = FirstPage,
                )
            }
        }
    }

    private suspend fun loadPage(
        query: String,
        page: Int,
        generation: Long,
        fetchPolicy: NutsNewsFetchPolicy,
    ) {
        if (generation != searchGeneration) return
        mutableSearchState.update { current ->
            current.copy(
                isLoadingMore = true,
                errorMessage = null,
                failedPage = null,
            )
        }

        try {
            val response =
                articleSearchSource.searchArticles(
                    query = query,
                    page = page,
                    limit = ArchiveSearchRequest.DefaultPageSize,
                    fetchPolicy = fetchPolicy,
                )
            if (generation != searchGeneration) return
            mutableSearchState.update { current ->
                current.copy(
                    articles =
                        (current.articles + response.articles)
                            .distinctBy { article -> article.stableId },
                    nextPage = response.nextPage,
                    isLoadingMore = false,
                )
            }
            persistRestorableResults()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (generation != searchGeneration) return
            mutableSearchState.update { current ->
                current.copy(
                    isLoadingMore = false,
                    errorMessage = error.archiveSearchMessage(),
                    failedPage = page,
                )
            }
        }
    }

    class Factory(
        private val articleSearchSource: ArchiveArticleSearchSource,
        private val savedStoryRepository: SavedStoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ArchiveSearchViewModel::class.java))
            return ArchiveSearchViewModel(
                articleSearchSource = articleSearchSource,
                savedStoryRepository = savedStoryRepository,
            ) as T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T {
            require(modelClass.isAssignableFrom(ArchiveSearchViewModel::class.java))
            return ArchiveSearchViewModel(
                articleSearchSource = articleSearchSource,
                savedStoryRepository = savedStoryRepository,
                savedStateHandle = extras.createSavedStateHandle(),
            ) as T
        }
    }

    private fun persistRestorableResults() {
        val state = mutableSearchState.value
        savedStateHandle[ArchiveSearchQueryStateKey] = state.query
        savedStateHandle[ArchiveSearchSearchedQueryStateKey] = state.searchedQuery
        savedStateHandle[ArchiveSearchResponseStateKey] =
            ArticleStateCodec.encode(
                ArticlesResponse(
                    articles = state.articles,
                    nextPage = state.nextPage,
                ),
            )
        savedStateHandle[ArchiveSearchHasSearchedStateKey] = state.hasSearched
    }

    private fun clearPersistedResults() {
        savedStateHandle.remove<String>(ArchiveSearchSearchedQueryStateKey)
        savedStateHandle.remove<String>(ArchiveSearchResponseStateKey)
        savedStateHandle.remove<Boolean>(ArchiveSearchHasSearchedStateKey)
    }

    private fun Exception.archiveSearchMessage(): String =
        when (this) {
            is NutsNewsApiException.Timeout ->
                "The archive search took too long. Please try again."

            is NutsNewsApiException.Network ->
                "NutsNews could not reach the archive. Check your connection and try again."

            else -> message ?: "The NutsNews archive could not be searched. Please try again."
        }

    private companion object {
        const val FirstPage = 0
        const val SearchDebounceMillis = 350L
    }
}

private fun restoreArchiveSearchState(
    savedStateHandle: SavedStateHandle,
): ArchiveSearchUiState {
    val query = savedStateHandle[ArchiveSearchQueryStateKey] ?: ""
    val response =
        ArticleStateCodec.decodeOrNull(
            savedStateHandle[ArchiveSearchResponseStateKey],
        ) ?: return ArchiveSearchUiState(query = query)
    return ArchiveSearchUiState(
        query = query,
        searchedQuery = savedStateHandle[ArchiveSearchSearchedQueryStateKey] ?: query,
        articles = response.articles,
        nextPage = response.nextPage,
        hasSearched = savedStateHandle[ArchiveSearchHasSearchedStateKey] ?: true,
    )
}

internal const val ArchiveSearchQueryStateKey = "archiveSearch.query"
internal const val ArchiveSearchSearchedQueryStateKey = "archiveSearch.searchedQuery"
internal const val ArchiveSearchResponseStateKey = "archiveSearch.response"
internal const val ArchiveSearchHasSearchedStateKey = "archiveSearch.hasSearched"
