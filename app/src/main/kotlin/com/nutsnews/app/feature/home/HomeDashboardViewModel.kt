package com.nutsnews.app.feature.home

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

class HomeDashboardViewModel(
    userPreferencesRepository: UserPreferencesRepository,
    readingStatsRepository: ReadingStatsRepository,
    savedStoryRepository: SavedStoryRepository,
    storyNoteRepository: StoryNoteRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeDashboardUiState> =
        combine(
            userPreferencesRepository.preferences,
            readingStatsRepository.observeStats(),
            savedStoryRepository.count,
            storyNoteRepository.count,
        ) { preferences, readingStats, savedCount, notesCount ->
            HomeDashboardUiState.populated(
                preferences = preferences,
                todayStoryCount = readingStats.todayStoryCount,
                currentStreak = readingStats.currentStreak,
                savedCount = savedCount,
                notesCount = notesCount,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeDashboardUiState(),
        )

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val readingStatsRepository: ReadingStatsRepository,
        private val savedStoryRepository: SavedStoryRepository,
        private val storyNoteRepository: StoryNoteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeDashboardViewModel::class.java))
            return HomeDashboardViewModel(
                userPreferencesRepository = userPreferencesRepository,
                readingStatsRepository = readingStatsRepository,
                savedStoryRepository = savedStoryRepository,
                storyNoteRepository = storyNoteRepository,
            ) as T
        }
    }
}
