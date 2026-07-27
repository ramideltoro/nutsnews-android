package com.nutsnews.app.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.data.story.SavedStoryRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedStoriesUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val stories: List<SavedStory> = emptyList(),
    val filteredStories: List<SavedStory> = emptyList(),
) {
    val savedCount: Int
        get() = stories.size

    val hasSearchQuery: Boolean
        get() = query.isNotBlank()
}

class SavedStoriesViewModel(
    private val savedStoryRepository: SavedStoryRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<SavedStoriesUiState> =
        combine(
            savedStoryRepository.stories,
            query,
        ) { stories, currentQuery ->
            val orderedStories = stories.sortedByDescending(SavedStory::savedAt)
            SavedStoriesUiState(
                isLoading = false,
                query = currentQuery,
                stories = orderedStories,
                filteredStories =
                    orderedStories.filter { story ->
                        story.matchesSavedStoriesQuery(currentQuery)
                    },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SavedStoriesUiState(),
        )

    fun onQueryChanged(value: String) {
        query.value = value
    }

    fun remove(story: SavedStory) {
        viewModelScope.launch {
            savedStoryRepository.remove(story.id)
        }
    }

    class Factory(
        private val savedStoryRepository: SavedStoryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SavedStoriesViewModel::class.java))
            return SavedStoriesViewModel(savedStoryRepository) as T
        }
    }
}

internal fun SavedStory.matchesSavedStoriesQuery(query: String): Boolean {
    val searchTerms =
        query
            .trim()
            .lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .filter(String::isNotEmpty)
    if (searchTerms.isEmpty()) return true

    val searchableText =
        buildList {
            add(article.title)
            add(article.summary)
            add(article.source)
            addAll(article.categories)
        }.joinToString(" ").lowercase(Locale.ROOT)

    return searchTerms.all(searchableText::contains)
}
