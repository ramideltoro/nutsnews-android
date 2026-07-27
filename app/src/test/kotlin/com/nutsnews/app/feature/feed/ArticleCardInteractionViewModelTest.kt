package com.nutsnews.app.feature.feed

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.data.story.SavedStoryRepository
import java.net.URI
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleCardInteractionViewModelTest {
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
    fun likeAndUnlikeStaySynchronizedWithPersistentSavedStories() =
        runTest(mainDispatcher) {
            val article = testArticle()
            val savedStories =
                FakeSavedStoryRepository(
                    initiallySaved = listOf(SavedStory(article, Instant.EPOCH)),
                )
            val viewModel =
                ArticleCardInteractionViewModel(
                    savedStoryRepository = savedStories,
                    userPreferencesRepository = InMemoryUserPreferencesRepository(),
                )

            assertTrue(
                article.stableId in
                    viewModel.uiState.first { state -> article.stableId in state.likedStoryIds }
                        .likedStoryIds,
            )

            viewModel.toggleLiked(article)
            advanceUntilIdle()
            assertFalse(savedStories.isLiked(article.stableId))

            viewModel.toggleLiked(article)
            advanceUntilIdle()
            assertTrue(savedStories.isLiked(article.stableId))
            assertEquals(article, savedStories.stories.value.single().article)
            assertEquals(listOf(false, true), savedStories.likeChanges)
        }

    @Test
    fun rapidTogglesAreSerializedAndDoNotLoseUpdates() =
        runTest(mainDispatcher) {
            val article = testArticle()
            val savedStories = FakeSavedStoryRepository()
            val viewModel =
                ArticleCardInteractionViewModel(
                    savedStoryRepository = savedStories,
                    userPreferencesRepository = InMemoryUserPreferencesRepository(),
                )

            viewModel.toggleLiked(article)
            viewModel.toggleLiked(article)
            advanceUntilIdle()

            assertFalse(savedStories.isLiked(article.stableId))
            assertEquals(listOf(true, false), savedStories.likeChanges)
        }

    @Test
    fun hapticsPreferenceIsExposedToArticleCards() =
        runTest(mainDispatcher) {
            val preferences =
                InMemoryUserPreferencesRepository(
                    UserPreferences(hapticsEnabled = false),
                )
            val viewModel =
                ArticleCardInteractionViewModel(
                    savedStoryRepository = FakeSavedStoryRepository(),
                    userPreferencesRepository = preferences,
                )

            assertFalse(viewModel.uiState.first { state -> !state.hapticsEnabled }.hapticsEnabled)

            preferences.setHapticsEnabled(true)
            assertTrue(viewModel.uiState.first { state -> state.hapticsEnabled }.hapticsEnabled)
        }
}

private class FakeSavedStoryRepository(
    initiallySaved: List<SavedStory> = emptyList(),
) : SavedStoryRepository {
    override val stories = MutableStateFlow(initiallySaved)
    override val count: Flow<Int> = stories.map { savedStories -> savedStories.size }
    val likeChanges = mutableListOf<Boolean>()

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> =
        stories.map { savedStories -> savedStories.any { story -> story.id == storyId } }

    override suspend fun isLiked(storyId: StoryId): Boolean =
        stories.value.any { story -> story.id == storyId }

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) {
        likeChanges += isLiked
        if (isLiked) save(article) else remove(article.stableId)
    }

    override suspend fun save(article: Article) {
        if (isLiked(article.stableId)) return
        stories.value = stories.value + SavedStory(article, Instant.now())
    }

    override suspend fun remove(storyId: StoryId) {
        stories.value = stories.value.filterNot { story -> story.id == storyId }
    }
}

private fun testArticle() =
    Article(
        id = "like-test",
        title = "A neighborhood turns an empty lot into a garden",
        summary = "Volunteers created a shared place to grow food and meet.",
        originalUrl = URI("https://example.com/like-test"),
        source = "NutsNews",
        publishedAt = "2024-01-02T12:00:00Z",
        createdAt = null,
        thumbnailUrl = null,
        categories = listOf("Community"),
    )
