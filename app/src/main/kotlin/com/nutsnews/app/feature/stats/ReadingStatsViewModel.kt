package com.nutsnews.app.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.data.story.StoryNoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ReadingStatsViewModel(
    userPreferencesRepository: UserPreferencesRepository,
    readingStatsRepository: ReadingStatsRepository,
    savedStoryRepository: SavedStoryRepository,
    storyNoteRepository: StoryNoteRepository,
) : ViewModel() {
    val uiState: StateFlow<ReadingStatsUiState> =
        combine(
            userPreferencesRepository.preferences,
            readingStatsRepository.observeStats(),
            savedStoryRepository.count,
            storyNoteRepository.count,
        ) { preferences, stats, savedCount, noteCount ->
            ReadingStatsUiState.populated(
                preferences = preferences,
                stats = stats,
                savedStoryCount = savedCount,
                noteCount = noteCount,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReadingStatsUiState(),
        )

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val readingStatsRepository: ReadingStatsRepository,
        private val savedStoryRepository: SavedStoryRepository,
        private val storyNoteRepository: StoryNoteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReadingStatsViewModel::class.java))
            return ReadingStatsViewModel(
                userPreferencesRepository = userPreferencesRepository,
                readingStatsRepository = readingStatsRepository,
                savedStoryRepository = savedStoryRepository,
                storyNoteRepository = storyNoteRepository,
            ) as T
        }
    }
}
