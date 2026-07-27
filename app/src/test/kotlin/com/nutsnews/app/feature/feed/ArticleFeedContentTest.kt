package com.nutsnews.app.feature.feed

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsTheme
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
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArticleFeedContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun firstLoadThenCategoryEmptyErrorShowsRetryWithoutContentReplacement() {
        var state by
            mutableStateOf(
                ArticleFeedUiState(isInitialLoading = true),
            )
        var retryCount = 0
        setContent(
            uiState = { state },
            onRetry = { retryCount += 1 },
        )

        composeRule.onNodeWithTag("feed_initial_loading").assertIsDisplayed()
        composeRule.onNodeWithText("Loading good news...").assertIsDisplayed()

        composeRule.runOnIdle {
            state =
                ArticleFeedUiState(
                    selectedCategory = "Science",
                )
        }
        composeRule.onNodeWithTag("feed_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("No Science stories yet").assertIsDisplayed()

        composeRule.runOnIdle {
            state =
                ArticleFeedUiState(
                    selectedCategory = "Science",
                    errorMessage = "The network is unavailable.",
                )
        }

        composeRule.onNodeWithText("The network is unavailable.").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun staleCachedStoriesAndInlineErrorRemainReadableAndRetryable() {
        val article = article(1)
        var retryCount = 0
        val opened = mutableListOf<Article>()
        setContent(
            uiState = {
                ArticleFeedUiState(
                    articles = listOf(article),
                    isStale = true,
                    errorMessage = "Couldn’t reach NutsNews.",
                )
            },
            onRetry = { retryCount += 1 },
            onOpenArticle = opened::add,
        )

        composeRule
            .onNodeWithTag("feed_article_list")
            .performScrollToNode(hasTestTag("feed_stale_banner"))
        composeRule
            .onNodeWithText("Showing saved stories while we reconnect.")
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("feed_article_list")
            .performScrollToNode(hasTestTag("feed_story_story-1"))
        composeRule
            .onNodeWithTag("feed_story_story-1")
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("article_read_story")
            .performClick()
        assertEquals(listOf(article), opened)

        composeRule
            .onNodeWithTag("feed_article_list")
            .performScrollToNode(hasTestTag("feed_error_banner"))
        composeRule.onNodeWithText("Couldn’t reach NutsNews.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun pullToRefreshAllowsRepeatedCompletedRefreshesButRejectsOverlap() {
        var state by
            mutableStateOf(
                ArticleFeedUiState(articles = articles(8)),
            )
        var refreshCount = 0
        setContent(
            uiState = { state },
            onRefresh = {
                refreshCount += 1
                state = state.copy(isRefreshing = true)
            },
        )

        composeRule.onNodeWithTag("feed_pull_to_refresh").performTouchInput {
            swipeDown(
                startY = centerY,
                endY = bottom,
                durationMillis = 600,
            )
        }
        assertEquals(1, refreshCount)
        composeRule.onNodeWithTag("feed_refresh_indicator").assertIsDisplayed()

        composeRule.onNodeWithTag("feed_pull_to_refresh").performTouchInput {
            swipeDown(
                startY = centerY,
                endY = bottom,
                durationMillis = 600,
            )
        }
        assertEquals(1, refreshCount)

        composeRule.runOnIdle {
            state = state.copy(isRefreshing = false)
        }
        composeRule.onNodeWithTag("feed_pull_to_refresh").performTouchInput {
            swipeDown(
                startY = centerY,
                endY = bottom,
                durationMillis = 600,
            )
        }
        assertEquals(2, refreshCount)
    }

    @Test
    fun paginationIsSingleFlightShowsProgressAndPreservesScrollWhenItemsAppend() {
        var state by
            mutableStateOf(
                ArticleFeedUiState(
                    articles = articles(20),
                    nextPage = 1,
                ),
            )
        val paginationRequests = mutableListOf<Article>()
        lateinit var listState: LazyListState
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                listState = rememberLazyListState()
                ArticleFeedContent(
                    uiState = state,
                    onRefresh = {},
                    onRetry = {},
                    onLoadMore = { article ->
                        paginationRequests += article
                        state = state.copy(isPaginating = true)
                    },
                    onOpenArticle = {},
                    dashboard = {
                        Text(
                            text = "Dashboard",
                            modifier = Modifier.height(180.dp),
                        )
                    },
                    listState = listState,
                )
            }
        }

        composeRule
            .onNodeWithTag("feed_article_list")
            .performScrollToNode(hasTestTag("feed_story_story-20"))
        composeRule.waitUntil(timeoutMillis = 2_000) {
            paginationRequests.size == 1
        }
        composeRule
            .onNodeWithTag("feed_article_list")
            .performScrollToNode(hasTestTag("feed_load_more_progress"))
        composeRule.onNodeWithTag("feed_load_more_progress").assertIsDisplayed()
        assertEquals(listOf("story-20"), paginationRequests.map(Article::id))
        val indexBeforeAppend =
            composeRule.runOnIdle {
                listState.firstVisibleItemIndex
            }

        composeRule.runOnIdle {
            state =
                state.copy(
                    articles = state.articles + articles(start = 21, count = 3),
                    nextPage = 2,
                    isPaginating = false,
                )
        }
        val indexAfterAppend =
            composeRule.runOnIdle {
                listState.firstVisibleItemIndex
            }
        assertEquals(indexBeforeAppend, indexAfterAppend)

        composeRule
            .onNodeWithTag("feed_article_list")
            .performScrollToNode(hasTestTag("feed_story_story-23"))
        assertEquals(
            listOf("story-20", "story-23"),
            paginationRequests.map(Article::id),
        )
        assertTrue(listState.firstVisibleItemIndex > 0)
    }

    private fun setContent(
        uiState: () -> ArticleFeedUiState,
        onRefresh: () -> Unit = {},
        onRetry: () -> Unit = {},
        onLoadMore: (Article) -> Unit = {},
        onOpenArticle: (Article) -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                ArticleFeedContent(
                    uiState = uiState(),
                    onRefresh = onRefresh,
                    onRetry = onRetry,
                    onLoadMore = onLoadMore,
                    onOpenArticle = onOpenArticle,
                    dashboard = {
                        Text(
                            text = "Dashboard",
                            modifier = Modifier.height(180.dp),
                        )
                    },
                )
            }
        }
    }
}

private fun articles(
    count: Int,
    start: Int = 1,
): List<Article> =
    (start until start + count).map(::article)

private fun article(number: Int): Article =
    Article(
        id = "story-$number",
        title = "Good news story number $number",
        summary = "A hopeful update.",
        originalUrl = URI("https://example.com/story-$number"),
        source = "NutsNews",
        publishedAt = null,
        createdAt = null,
        thumbnailUrl = null,
        categories = listOf("Community"),
    )
