package com.nutsnews.app.feature.home

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.core.model.StoryNote
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.ReminderConfiguration
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.data.story.StoryNoteRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDashboardViewModelTest {
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
    fun combinesPreferencesAndRepositoryMetricsIntoDashboardState() =
        runTest(mainDispatcher) {
            val preferences =
                InMemoryUserPreferencesRepository(
                    UserPreferences(
                        selectedTopicIds = setOf("science", "community"),
                        selectedMoodId = "hopeful",
                        dailyGoal = 4,
                        reminder = ReminderConfiguration(enabled = true, hour = 15),
                    ),
                )
            val stats =
                MutableStateFlow(
                    ReadingStats(
                        todayStoryCount = 3,
                        originalOpensTodayCount = 1,
                        totalUniqueStoryCount = 12,
                        currentStreak = 5,
                        recentDays = emptyList(),
                    ),
                )
            val savedCount = MutableStateFlow(7)
            val noteCount = MutableStateFlow(2)
            val viewModel =
                HomeDashboardViewModel(
                    userPreferencesRepository = preferences,
                    readingStatsRepository = FakeReadingStatsRepository(stats),
                    savedStoryRepository = FakeSavedStoryRepository(savedCount),
                    storyNoteRepository = FakeStoryNoteRepository(noteCount),
                )

            val initial = viewModel.uiState.first()
            assertEquals(true, initial.isLoading)

            val state = viewModel.uiState.first { value -> !value.isLoading }
            assertEquals(3, state.todayStoryCount)
            assertEquals(4, state.dailyGoal)
            assertEquals(75, state.goalProgressPercent)
            assertEquals("3 of 4 stories today", state.goalTitle)
            assertEquals(5, state.currentStreak)
            assertEquals("5 day streak", state.streakText)
            assertEquals(7, state.savedCount)
            assertEquals(2, state.notesCount)
            assertEquals("Hopeful", state.selectedMoodTitle)
            assertEquals("Science, Community", state.selectedTopicText)
            assertEquals("Daily at 3:00 PM", "Daily at ${state.reminderDisplayTime}")

            stats.value = stats.value.copy(todayStoryCount = 4)
            savedCount.value = 8
            noteCount.value = 3

            val updated =
                viewModel.uiState.first { value ->
                    value.todayStoryCount == 4 &&
                        value.savedCount == 8 &&
                        value.notesCount == 3
                }
            assertEquals("Goal complete ✨", updated.goalTitle)
            assertEquals(100, updated.goalProgressPercent)
            assertFalse(updated.isLoading)
        }
}

private class FakeReadingStatsRepository(
    private val stats: Flow<ReadingStats>,
) : ReadingStatsRepository {
    override fun observeStats(recentDayCount: Int): Flow<ReadingStats> = stats

    override suspend fun recordStoryOpen(article: Article) = Unit

    override suspend fun recordOriginalStoryOpen() = Unit

    override suspend fun lastOpenedAt(storyId: StoryId): Instant? = null
}

private class FakeSavedStoryRepository(
    override val count: Flow<Int>,
) : SavedStoryRepository {
    override val stories: Flow<List<SavedStory>> = flowOf(emptyList())

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> = flowOf(false)

    override suspend fun isLiked(storyId: StoryId): Boolean = false

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) = Unit

    override suspend fun save(article: Article) = Unit

    override suspend fun remove(storyId: StoryId) = Unit
}

private class FakeStoryNoteRepository(
    override val count: Flow<Int>,
) : StoryNoteRepository {
    override fun observeNote(article: Article): Flow<StoryNote?> = flowOf(null)

    override suspend fun findNote(article: Article): StoryNote? = null

    override suspend fun setNote(
        article: Article,
        text: String,
    ) = Unit

    override suspend fun clearNote(article: Article) = Unit
}
