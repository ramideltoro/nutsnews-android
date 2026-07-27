package com.nutsnews.app.feature.saved

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.DefaultAppNavigator
import java.net.URI
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SavedStoriesScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyLibraryMatchesTheIosMessageAndCloses() {
        var closeCount = 0
        setScreen(
            stories = emptyList(),
            onClose = { closeCount += 1 },
        )

        composeRule.onNodeWithText("Saved Stories").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_stories_empty").assertIsDisplayed()
        composeRule.onNodeWithText("No saved stories yet").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "Tap the heart on any story to build your own calm, positive reading list.",
            ).assertIsDisplayed()
        composeRule.onAllNodesWithTag("saved_stories_search").assertCountEquals(0)
        composeRule.onNodeWithTag("saved_stories_close").performClick()

        assertEquals(1, closeCount)
    }

    @Test
    fun populatedLibraryShowsCountThumbnailSavedDateAndArticleMetadata() {
        val story =
            savedStory(
                id = "garden",
                title = "Neighbors grow a community garden",
                categories =
                    listOf(
                        "Community",
                        "Environment",
                        "Food",
                        "Kindness",
                        "Local",
                        "Hidden",
                    ),
                savedAt = "2026-07-26T12:00:00Z",
            )
        setScreen(stories = listOf(story))

        composeRule.onNodeWithText("Your good-news library").assertIsDisplayed()
        composeRule.onNodeWithText("1 story saved on this device").assertIsDisplayed()
        scrollToStory(story)
        composeRule.onNodeWithText(story.article.title).assertIsDisplayed()
        composeRule.onNodeWithText(story.article.summary).assertIsDisplayed()
        composeRule.onNodeWithText(story.article.source).assertIsDisplayed()
        composeRule.onNodeWithText("Saved Jul 26, 2026").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_story_thumbnail").assertIsDisplayed()
        repeat(5) { index ->
            composeRule.onNodeWithTag("saved_story_category_$index").assertExists()
        }
        composeRule.onAllNodesWithTag("saved_story_category_5").assertCountEquals(0)
    }

    @Test
    fun localSearchIndexesTitleSummarySourceCategoryAndEveryEnteredTerm() {
        val first =
            savedStory(
                id = "science",
                title = "Solar breakthrough reaches remote schools",
                summary = "A bright battery keeps classrooms connected.",
                source = "Beacon Wire",
                categories = listOf("Science", "Education"),
            )
        val second =
            savedStory(
                id = "animals",
                title = "Rescued otters return home",
                summary = "Volunteers celebrate beside the harbor.",
                source = "Coastal Journal",
                categories = listOf("Animals"),
            )
        setScreen(stories = listOf(first, second))

        assertSearchResult("breakthrough", expected = first, hidden = second)
        assertSearchResult("classrooms", expected = first, hidden = second)
        assertSearchResult("beacon", expected = first, hidden = second)
        assertSearchResult("education", expected = first, hidden = second)
        assertSearchResult("beacon science", expected = first, hidden = second)

        enterSearch("volcano")
        composeRule.onNodeWithTag("saved_stories_empty_search").assertIsDisplayed()
        composeRule.onNodeWithText("No saved stories found").assertIsDisplayed()
        composeRule
            .onNodeWithText("Try searching by title, summary, source, or category.")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear saved story search").performClick()
        scrollToStory(first)
        composeRule.onNodeWithTag(storyTag(first)).assertIsDisplayed()
        scrollToStory(second)
        composeRule.onNodeWithTag(storyTag(second)).assertIsDisplayed()
    }

    @Test
    fun openUsesTheNativeArticleDetailDestination() {
        val story = savedStory(id = "detail")
        val navigator = DefaultAppNavigator(AppDestination.Feed)
        setScreen(
            stories = listOf(story),
            onOpenStory = { opened ->
                navigator.navigate(AppDestination.ArticleDetail(opened.id))
            },
        )

        scrollToStory(story)
        composeRule
            .onNodeWithTag("saved_story_open_${story.id.value}")
            .performClick()

        assertEquals(
            listOf(
                AppDestination.Feed,
                AppDestination.ArticleDetail(story.id),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun removalPersistsWhenTheSavedStoriesViewModelIsRecreated() {
        val removed = savedStory(id = "removed")
        val retained = savedStory(id = "retained")
        val repository = PersistentFakeSavedStoryRepository(listOf(removed, retained))
        val activeViewModel = mutableStateOf(SavedStoriesViewModel(repository))

        composeRule.setContent {
            val viewModel = activeViewModel.value
            val uiState by viewModel.uiState.collectAsState()
            NutsNewsTheme(updateSystemBars = false) {
                SavedStoriesScreen(
                    uiState = uiState,
                    onQueryChanged = viewModel::onQueryChanged,
                    onOpenStory = {},
                    onRemoveStory = viewModel::remove,
                    onClose = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(storyTag(removed))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        scrollToStory(removed)
        composeRule
            .onNodeWithTag("saved_story_remove_${removed.id.value}")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(storyTag(removed))
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeRule
            .onNodeWithTag("saved_stories_list")
            .performScrollToNode(hasTestTag("saved_stories_stats"))
        composeRule.onNodeWithText("1 story saved on this device").assertIsDisplayed()

        composeRule.runOnIdle {
            activeViewModel.value = SavedStoriesViewModel(repository)
        }

        composeRule.onAllNodesWithTag(storyTag(removed)).assertCountEquals(0)
        scrollToStory(retained)
        composeRule.onNodeWithTag(storyTag(retained)).assertIsDisplayed()
        assertEquals(listOf(retained), repository.currentStories)
    }

    private fun assertSearchResult(
        query: String,
        expected: SavedStory,
        hidden: SavedStory,
    ) {
        enterSearch(query)
        composeRule.onNodeWithTag(storyTag(expected)).assertExists()
        composeRule.onAllNodesWithTag(storyTag(hidden)).assertCountEquals(0)
    }

    private fun enterSearch(query: String) {
        composeRule
            .onNodeWithTag("saved_stories_list")
            .performScrollToNode(hasTestTag("saved_stories_search"))
        composeRule.onNodeWithTag("saved_stories_search").performTextClearance()
        composeRule.onNodeWithTag("saved_stories_search").performTextInput(query)
    }

    private fun scrollToStory(story: SavedStory) {
        composeRule
            .onNodeWithTag("saved_stories_list")
            .performScrollToNode(hasTestTag(storyTag(story)))
    }

    private fun setScreen(
        stories: List<SavedStory>,
        onOpenStory: (SavedStory) -> Unit = {},
        onRemoveStory: (SavedStory) -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            var query by remember { mutableStateOf("") }
            NutsNewsTheme(updateSystemBars = false) {
                SavedStoriesScreen(
                    uiState =
                        SavedStoriesUiState(
                            isLoading = false,
                            query = query,
                            stories = stories,
                            filteredStories =
                                stories.filter { story ->
                                    story.matchesSavedStoriesQuery(query)
                                },
                        ),
                    onQueryChanged = { query = it },
                    onOpenStory = onOpenStory,
                    onRemoveStory = onRemoveStory,
                    onClose = onClose,
                )
            }
        }
    }
}

private class PersistentFakeSavedStoryRepository(
    initialStories: List<SavedStory>,
) : SavedStoryRepository {
    private val mutableStories = MutableStateFlow(initialStories)

    override val stories: Flow<List<SavedStory>> = mutableStories
    override val count: Flow<Int> = mutableStories.map { stories -> stories.size }

    val currentStories: List<SavedStory>
        get() = mutableStories.value

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> =
        mutableStories.map { stories -> stories.any { story -> story.id == storyId } }

    override suspend fun isLiked(storyId: StoryId): Boolean =
        mutableStories.value.any { story -> story.id == storyId }

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) {
        if (isLiked) {
            save(article)
        } else {
            remove(article.stableId)
        }
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

private fun savedStory(
    id: String,
    title: String = "A hopeful local story",
    summary: String = "Neighbors worked together and made the day brighter.",
    source: String = "Good News Daily",
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
                publishedAt = "2026-07-24T12:00:00Z",
                createdAt = null,
                thumbnailUrl = null,
                categories = categories,
            ),
        savedAt = Instant.parse(savedAt),
    )

private fun storyTag(story: SavedStory): String = "saved_story_${story.id.value}"
