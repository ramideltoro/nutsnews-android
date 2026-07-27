package com.nutsnews.app.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.DefaultAppNavigator
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArchiveSearchScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun typingShowsTheMinimumHintAndEnablesEligibleSearch() {
        val state = mutableStateOf(ArchiveSearchUiState())
        var submitCount = 0
        setScreen(
            state = state,
            onQueryChanged = { query ->
                state.value = state.value.copy(query = query)
            },
            onSubmitSearch = { submitCount += 1 },
        )

        composeRule.onNodeWithText("Search the archive").assertIsDisplayed()
        composeRule.onNodeWithTag("archive_search_query").performTextInput("a")
        composeRule
            .onNodeWithText("Type at least 2 characters to search.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Keep typing").assertIsDisplayed()
        composeRule.onNodeWithTag("archive_search_submit").assertIsNotEnabled()

        composeRule.onNodeWithTag("archive_search_query").performTextInput("n")
        composeRule.onNodeWithTag("archive_search_submit").assertIsEnabled().performClick()

        assertEquals(1, submitCount)
        assertEquals("an", state.value.query)
    }

    @Test
    fun resultShowsThumbnailSourceCategorySummaryDateAndNativeDetailNavigation() {
        val article = resultArticle(id = "detail")
        val navigator = DefaultAppNavigator(AppDestination.Feed)
        val state =
            mutableStateOf(
                ArchiveSearchUiState(
                    query = "science",
                    searchedQuery = "science",
                    articles = listOf(article),
                    hasSearched = true,
                ),
            )
        setScreen(
            state = state,
            onOpenArticle = { selected ->
                navigator.navigate(AppDestination.ArticleDetail(selected.stableId))
            },
        )

        composeRule.onNodeWithText("Results for “science”").assertIsDisplayed()
        composeRule.onNodeWithTag("archive_search_result_count").assertIsDisplayed()
        composeRule
            .onNodeWithTag("archive_search_results")
            .performScrollToNode(hasTestTag(resultTag(article)))
        composeRule
            .onNodeWithTag("archive_search_result_thumbnail", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText(article.source).assertIsDisplayed()
        composeRule.onNodeWithText(article.categories.first()).assertIsDisplayed()
        composeRule.onNodeWithText(article.title).assertIsDisplayed()
        composeRule.onNodeWithText(article.summary).assertIsDisplayed()
        composeRule.onNodeWithText("Jul 26, 2026").assertIsDisplayed()
        composeRule
            .onNodeWithTag(resultTag(article))
            .performClick()

        assertEquals(
            listOf(
                AppDestination.Feed,
                AppDestination.ArticleDetail(article.stableId),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun savingAResultUpdatesItsControlWithoutOpeningTheStory() {
        val article = resultArticle(id = "save")
        val state =
            mutableStateOf(
                ArchiveSearchUiState(
                    query = "community",
                    searchedQuery = "community",
                    articles = listOf(article),
                    hasSearched = true,
                ),
            )
        val opened = mutableListOf<Article>()
        val saved = mutableListOf<Article>()
        setScreen(
            state = state,
            onToggleSaved = { selected ->
                saved += selected
                state.value =
                    state.value.copy(
                        savedStoryIds = state.value.savedStoryIds + selected.stableId,
                    )
            },
            onOpenArticle = opened::add,
        )

        composeRule.onNodeWithContentDescription("Save story").assertIsDisplayed()
        composeRule
            .onNodeWithTag("archive_search_save_${article.stableId.value}")
            .performClick()

        composeRule.onNodeWithText("Saved").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove saved story").assertIsDisplayed()
        assertEquals(listOf(article), saved)
        assertEquals(emptyList(), opened)
    }

    @Test
    fun loadingNoResultsAndFailureStatesExposeRetry() {
        val state =
            mutableStateOf(
                ArchiveSearchUiState(
                    query = "animals",
                    searchedQuery = "animals",
                    hasSearched = true,
                    isSearching = true,
                ),
            )
        var retryCount = 0
        setScreen(
            state = state,
            onRetry = { retryCount += 1 },
        )

        composeRule.onNodeWithTag("archive_search_loading").assertIsDisplayed()
        composeRule.onNodeWithText("Searching good news...").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = state.value.copy(isSearching = false)
        }
        composeRule.onNodeWithTag("archive_search_no_results").assertIsDisplayed()
        composeRule.onNodeWithText("No matching stories yet").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value =
                state.value.copy(
                    errorMessage = "NutsNews could not reach the archive.",
                    failedPage = 0,
                )
        }
        composeRule.onNodeWithTag("archive_search_failure").assertIsDisplayed()
        composeRule.onNodeWithText("Search is taking a breather").assertIsDisplayed()
        composeRule.onNodeWithTag("archive_search_retry").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun reachingTheLastResultRequestsTheNextPageAndShowsPagingProgress() {
        val articles = (1..8).map { index -> resultArticle("page-$index") }
        val state =
            mutableStateOf(
                ArchiveSearchUiState(
                    query = "good news",
                    searchedQuery = "good news",
                    articles = articles,
                    hasSearched = true,
                    nextPage = 1,
                ),
            )
        var loadMoreCount = 0
        setScreen(
            state = state,
            onLoadMore = {
                loadMoreCount += 1
                state.value =
                    state.value.copy(
                        isLoadingMore = true,
                        errorMessage = null,
                    )
            },
        )

        composeRule
            .onNodeWithTag("archive_search_results")
            .performScrollToNode(hasTestTag(resultTag(articles.last())))
        composeRule.waitUntil(timeoutMillis = 5_000) { loadMoreCount == 1 }
        composeRule
            .onNodeWithTag("archive_search_results")
            .performScrollToNode(hasTestTag("archive_search_loading_more"))
        composeRule.onNodeWithTag("archive_search_loading_more").assertIsDisplayed()
    }

    @Test
    fun clearAndCloseControlsInvokeTheirNativeFlowCallbacks() {
        val state = mutableStateOf(ArchiveSearchUiState(query = "science"))
        var clearCount = 0
        var closeCount = 0
        setScreen(
            state = state,
            onClearSearch = {
                clearCount += 1
                state.value = ArchiveSearchUiState()
            },
            onClose = { closeCount += 1 },
        )

        composeRule.onNodeWithContentDescription("Clear search").performClick()
        composeRule.onAllNodesWithTag("archive_search_failure").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Close search").performClick()

        assertEquals(1, clearCount)
        assertEquals(1, closeCount)
    }

    private fun setScreen(
        state: androidx.compose.runtime.MutableState<ArchiveSearchUiState>,
        onQueryChanged: (String) -> Unit = {},
        onSubmitSearch: () -> Unit = {},
        onClearSearch: () -> Unit = {},
        onRetry: () -> Unit = {},
        onLoadMore: () -> Unit = {},
        onToggleSaved: (Article) -> Unit = {},
        onOpenArticle: (Article) -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        composeRule.setContent {
            val uiState by state
            NutsNewsTheme(updateSystemBars = false) {
                ArchiveSearchScreen(
                    uiState = uiState,
                    onQueryChanged = onQueryChanged,
                    onSubmitSearch = onSubmitSearch,
                    onClearSearch = onClearSearch,
                    onRetry = onRetry,
                    onLoadMore = onLoadMore,
                    onToggleSaved = onToggleSaved,
                    onOpenArticle = onOpenArticle,
                    onClose = onClose,
                    requestInitialFocus = false,
                )
            }
        }
    }
}

private fun resultArticle(id: String): Article =
    Article(
        id = id,
        title = "A hopeful discovery brings neighbors together",
        summary = "Researchers and volunteers shared an encouraging result.",
        originalUrl = URI("https://example.com/$id"),
        source = "Good News Daily",
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = null,
        categories = listOf("Science", "Community"),
    )

private fun resultTag(article: Article): String =
    "archive_search_result_${article.stableId.value}"
