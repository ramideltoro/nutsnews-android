package com.nutsnews.app.feature.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Immutable
data class ArticleCardInteractionUiState(
    val likedStoryIds: Set<StoryId> = emptySet(),
    val savedArticlesById: Map<StoryId, Article> = emptyMap(),
    val hapticsEnabled: Boolean = true,
)

class ArticleCardInteractionViewModel(
    private val savedStoryRepository: SavedStoryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val toggleMutex = Mutex()

    val uiState: StateFlow<ArticleCardInteractionUiState> =
        combine(
            savedStoryRepository.stories,
            userPreferencesRepository.preferences,
        ) { savedStories, preferences ->
            ArticleCardInteractionUiState(
                likedStoryIds = savedStories.mapTo(linkedSetOf()) { story -> story.id },
                savedArticlesById =
                    savedStories.associate { story -> story.id to story.article },
                hapticsEnabled = preferences.hapticsEnabled,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArticleCardInteractionUiState(),
        )

    fun toggleLiked(article: Article) {
        viewModelScope.launch {
            toggleMutex.withLock {
                val isLiked = savedStoryRepository.isLiked(article.stableId)
                savedStoryRepository.setLiked(
                    article = article,
                    isLiked = !isLiked,
                )
            }
        }
    }

    class Factory(
        private val savedStoryRepository: SavedStoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ArticleCardInteractionViewModel::class.java))
            return ArticleCardInteractionViewModel(
                savedStoryRepository = savedStoryRepository,
                userPreferencesRepository = userPreferencesRepository,
            ) as T
        }
    }
}
