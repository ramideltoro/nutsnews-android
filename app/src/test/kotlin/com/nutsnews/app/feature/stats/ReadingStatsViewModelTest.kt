package com.nutsnews.app.feature.stats

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.ReadingStatsDay
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.core.model.StoryNote
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.data.story.StoryNoteRepository
import java.time.Instant
import java.time.LocalDate
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
class ReadingStatsViewModelTest {
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
    fun combinesEveryPrivateDashboardMetricAndTracksUpdates() =
        runTest(mainDispatcher) {
            val preferences =
                InMemoryUserPreferencesRepository(
                    UserPreferences(dailyGoal = 4),
                )
            val stats =
                MutableStateFlow(
                    ReadingStats(
                        todayStoryCount = 2,
                        originalOpensTodayCount = 3,
                        totalUniqueStoryCount = 18,
                        currentStreak = 5,
                        recentDays =
                            listOf(
                                ReadingStatsDay(LocalDate.of(2026, 7, 25), 1),
                                ReadingStatsDay(LocalDate.of(2026, 7, 26), 2),
                            ),
                    ),
                )
            val savedCount = MutableStateFlow(7)
            val noteCount = MutableStateFlow(4)
            val viewModel =
                ReadingStatsViewModel(
                    userPreferencesRepository = preferences,
                    readingStatsRepository = FakeReadingStatsRepository(stats),
                    savedStoryRepository = FakeSavedStoryRepository(savedCount),
                    storyNoteRepository = FakeStoryNoteRepository(noteCount),
                )

            assertEquals(true, viewModel.uiState.first().isLoading)
            val active = viewModel.uiState.first { state -> !state.isLoading }

            assertEquals(2, active.todayStoryCount)
            assertEquals(4, active.dailyGoal)
            assertEquals(0.5f, active.goalProgress)
            assertEquals("2/4 stories", active.todayProgressLabel)
            assertEquals(
                "Nice start. Open 2 more positive stories to complete today’s goal.",
                active.todayMessage,
            )
            assertEquals(5, active.currentStreak)
            assertEquals(18, active.totalUniqueStoryCount)
            assertEquals(7, active.savedStoryCount)
            assertEquals(4, active.noteCount)
            assertEquals(3, active.originalOpensTodayCount)
            assertEquals(2, active.maxRecentDayCount)

            stats.value =
                stats.value.copy(
                    todayStoryCount = 6,
                    originalOpensTodayCount = 4,
                )
            savedCount.value = 8
            noteCount.value = 5

            val complete =
                viewModel.uiState.first { state ->
                    state.todayStoryCount == 6 &&
                        state.savedStoryCount == 8 &&
                        state.noteCount == 5
                }
            assertEquals(1f, complete.goalProgress)
            assertEquals(
                "Today’s good-news goal is complete. Beautiful.",
                complete.todayMessage,
            )
            assertEquals(4, complete.originalOpensTodayCount)
            assertFalse(complete.isLoading)
        }

    @Test
    fun populatedStateSanitizesInvalidRepositoryCounts() {
        val state =
            ReadingStatsUiState.populated(
                preferences = UserPreferences(dailyGoal = 99),
                stats =
                    ReadingStats(
                        todayStoryCount = -2,
                        originalOpensTodayCount = -3,
                        totalUniqueStoryCount = -4,
                        currentStreak = -5,
                        recentDays =
                            listOf(
                                ReadingStatsDay(LocalDate.of(2026, 7, 26), -6),
                            ),
                    ),
                savedStoryCount = -7,
                noteCount = -8,
            )

        assertEquals(0, state.todayStoryCount)
        assertEquals(5, state.dailyGoal)
        assertEquals(0, state.currentStreak)
        assertEquals(0, state.totalUniqueStoryCount)
        assertEquals(0, state.savedStoryCount)
        assertEquals(0, state.noteCount)
        assertEquals(0, state.originalOpensTodayCount)
        assertEquals(0, state.recentDays.single().storyCount)
        assertEquals(
            "Open one uplifting story to start today’s positive streak.",
            state.todayMessage,
        )
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
