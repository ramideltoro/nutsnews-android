package com.nutsnews.app.feature.article

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Immutable
data class ArticleDetailUiState(
    val likedStoryIds: Set<StoryId> = emptySet(),
    val readingStats: ReadingStats? = null,
)

class ArticleDetailViewModel(
    private val savedStoryRepository: SavedStoryRepository,
    private val readingStatsRepository: ReadingStatsRepository,
) : ViewModel() {
    private val likeMutex = Mutex()

    val uiState: StateFlow<ArticleDetailUiState> =
        combine(
            savedStoryRepository.stories,
            readingStatsRepository.observeStats(),
        ) { savedStories, stats ->
            ArticleDetailUiState(
                likedStoryIds = savedStories.mapTo(linkedSetOf()) { story -> story.id },
                readingStats = stats,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArticleDetailUiState(),
        )

    fun onArticleShown(article: Article) {
        viewModelScope.launch {
            readingStatsRepository.recordStoryOpen(article)
        }
    }

    fun toggleLiked(article: Article) {
        viewModelScope.launch {
            likeMutex.withLock {
                savedStoryRepository.setLiked(
                    article = article,
                    isLiked = !savedStoryRepository.isLiked(article.stableId),
                )
            }
        }
    }

    fun onOriginalStoryOpened() {
        viewModelScope.launch {
            readingStatsRepository.recordOriginalStoryOpen()
        }
    }

    class Factory(
        private val savedStoryRepository: SavedStoryRepository,
        private val readingStatsRepository: ReadingStatsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ArticleDetailViewModel::class.java))
            return ArticleDetailViewModel(
                savedStoryRepository = savedStoryRepository,
                readingStatsRepository = readingStatsRepository,
            ) as T
        }
    }
}
