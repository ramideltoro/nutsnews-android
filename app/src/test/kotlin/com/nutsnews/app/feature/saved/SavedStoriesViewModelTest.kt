package com.nutsnews.app.feature.saved

import androidx.lifecycle.SavedStateHandle
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.story.SavedStoryRepository
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedStoriesViewModelTest {
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
    fun loadsNewestFirstAndSearchesAllIosFieldsWithEveryTerm() =
        runTest(mainDispatcher) {
            val older =
                testStory(
                    id = "older",
                    title = "Solar classroom opens",
                    summary = "Students celebrate a bright new lab.",
                    source = "Beacon Wire",
                    categories = listOf("Science", "Education"),
                    savedAt = "2026-07-24T12:00:00Z",
                )
            val newer =
                testStory(
                    id = "newer",
                    title = "Otters return home",
                    summary = "A rescue team gathers beside the harbor.",
                    source = "Coastal Journal",
                    categories = listOf("Animals"),
                    savedAt = "2026-07-26T12:00:00Z",
                )
            val repository = TestSavedStoryRepository(listOf(older, newer))
            val viewModel = SavedStoriesViewModel(repository)

            val loaded = viewModel.uiState.first { state -> !state.isLoading }
            assertEquals(listOf(newer, older), loaded.stories)
            assertEquals(2, loaded.savedCount)

            listOf(
                "classroom" to older,
                "students" to older,
                "beacon" to older,
                "education" to older,
                "beacon science" to older,
                "HARBOR animals" to newer,
            ).forEach { (query, expected) ->
                viewModel.onQueryChanged(query)
                val filtered =
                    viewModel.uiState.first { state ->
                        state.query == query
                    }
                assertEquals(listOf(expected), filtered.filteredStories)
            }

            viewModel.onQueryChanged("science harbor")
            val noMatch =
                viewModel.uiState.first { state ->
                    state.query == "science harbor"
                }
            assertEquals(emptyList(), noMatch.filteredStories)
        }

    @Test
    fun removalUpdatesTheRepositoryBackedState() =
        runTest(mainDispatcher) {
            val removed = testStory(id = "removed")
            val retained = testStory(id = "retained", savedAt = "2026-07-23T12:00:00Z")
            val repository = TestSavedStoryRepository(listOf(removed, retained))
            val viewModel = SavedStoriesViewModel(repository)

            viewModel.uiState.first { state -> state.savedCount == 2 }
            viewModel.remove(removed)

            val updated =
                viewModel.uiState.first { state ->
                    !state.isLoading && state.savedCount == 1
                }
            assertEquals(listOf(retained), updated.stories)
            assertFalse(repository.isLiked(removed.id))
        }

    @Test
    fun savedLibraryQueryRestoresAfterProcessRecreation() =
        runTest(mainDispatcher) {
            val matching =
                testStory(
                    id = "science",
                    categories = listOf("Science"),
                )
            val repository =
                TestSavedStoryRepository(
                    listOf(matching, testStory(id = "animals", categories = listOf("Animals"))),
                )
            val savedState = SavedStateHandle()
            val original = SavedStoriesViewModel(repository, savedState)
            original.onQueryChanged("science")

            val recreated =
                SavedStoriesViewModel(
                    savedStoryRepository = repository,
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf(
                                SavedStoriesQueryStateKey to
                                    savedState.get<String>(SavedStoriesQueryStateKey),
                            ),
                        ),
                )

            assertEquals("science", recreated.uiState.value.query)
            assertEquals(
                listOf(matching),
                recreated.uiState.first { state -> !state.isLoading }.filteredStories,
            )
        }

    @Test
    fun savedDateUsesTheIosMediumDateStyle() {
        val story = testStory(id = "date", savedAt = "2026-07-26T12:00:00Z")

        assertEquals(
            "Jul 26, 2026",
            story.savedDateText(
                locale = Locale.US,
                zoneId = ZoneOffset.UTC,
            ),
        )
    }
}

private class TestSavedStoryRepository(
    initialStories: List<SavedStory>,
) : SavedStoryRepository {
    private val mutableStories = MutableStateFlow(initialStories)

    override val stories: Flow<List<SavedStory>> = mutableStories
    override val count: Flow<Int> = mutableStories.map { stories -> stories.size }

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> =
        mutableStories.map { stories -> stories.any { story -> story.id == storyId } }

    override suspend fun isLiked(storyId: StoryId): Boolean =
        mutableStories.value.any { story -> story.id == storyId }

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) {
        if (isLiked) save(article) else remove(article.stableId)
    }

    override suspend fun save(article: Article) {
        mutableStories.value =
            listOf(SavedStory(article, Instant.parse("2026-07-26T12:00:00Z"))) +
                mutableStories.value.filterNot { story -> story.id == article.stableId }
    }

    override suspend fun remove(storyId: StoryId) {
        mutableStories.value =
            mutableStories.value.filterNot { story -> story.id == storyId }
    }
}

private fun testStory(
    id: String,
    title: String = "A hopeful story",
    summary: String = "Good things happened.",
    source: String = "NutsNews",
    categories: List<String> = listOf("Community"),
    savedAt: String = "2026-07-25T12:00:00Z",
): SavedStory =
    SavedStory(
        article =
            Article(
                id = id,
                title = title,
                summary = summary,
                originalUrl = URI("https://example.com/$id"),
                source = source,
                publishedAt = null,
                createdAt = null,
                thumbnailUrl = null,
                categories = categories,
            ),
        savedAt = Instant.parse(savedAt),
    )
