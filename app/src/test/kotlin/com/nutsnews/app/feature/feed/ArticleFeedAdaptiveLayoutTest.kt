package com.nutsnews.app.feature.feed

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsAdaptiveWindow
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

abstract class ArticleFeedAdaptiveLayoutContract(
    private val compactExpected: Boolean,
) {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun feedSelectsCardLayoutFromCurrentWindowClassesAndKeepsReadActionReachable() {
        val article = adaptiveArticle()
        val opened = mutableListOf<Article>()
        composeRule.setContent {
            NutsNewsAdaptiveWindow {
                NutsNewsTheme(updateSystemBars = false) {
                    FeedScreen(
                        uiState =
                            ArticleFeedUiState(
                                articles = listOf(article),
                                availableCategories = article.categories,
                            ),
                        onDestinationSelected = {},
                        onCategorySelected = {},
                    ) {
                        ArticleFeedContent(
                            uiState = ArticleFeedUiState(articles = listOf(article)),
                            onRefresh = {},
                            onRetry = {},
                            onLoadMore = {},
                            onOpenArticle = opened::add,
                            dashboard = {
                                Spacer(modifier = androidx.compose.ui.Modifier.height(1.dp))
                            },
                        )
                    }
                }
            }
        }

        composeRule
            .onNodeWithTag("feed_article_list")
            .performScrollToNode(hasTestTag("feed_story_${article.id}"))
        composeRule
            .onNodeWithTag("feed_story_${article.id}")
            .assertIsDisplayed()

        val imageBounds = bounds("article_thumbnail")
        val titleBounds = bounds("article_title")
        if (compactExpected) {
            assertTrue(
                titleBounds.left > imageBounds.right,
                "Compact title must sit beside the fixed-width image",
            )
        } else {
            composeRule
                .onNodeWithTag("article_title")
                .performScrollTo()
                .assertIsDisplayed()
            val visibleTitleBounds = bounds("article_title")
            val cardBounds = bounds("feed_story_${article.id}")
            assertTrue(
                visibleTitleBounds.left > cardBounds.left &&
                    visibleTitleBounds.right < cardBounds.right,
                "Regular title must retain the card’s inset vertical edge: " +
                    "card=$cardBounds title=$visibleTitleBounds",
            )
        }

        composeRule
            .onNodeWithTag("article_read_story")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(listOf(article), opened)
    }

    private fun bounds(tag: String): Rect =
        composeRule
            .onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w852dp-h393dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneLandscapeArticleFeedAdaptiveLayoutTest :
    ArticleFeedAdaptiveLayoutContract(compactExpected = false)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w841dp-h673dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FoldableLandscapeArticleFeedAdaptiveLayoutTest :
    ArticleFeedAdaptiveLayoutContract(compactExpected = true)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletLandscapeArticleFeedAdaptiveLayoutTest :
    ArticleFeedAdaptiveLayoutContract(compactExpected = true)

private fun adaptiveArticle(): Article =
    Article(
        id = "adaptive-story",
        title = "Neighbors transform an empty lot into a flourishing community garden",
        summary =
            "Volunteers created a welcoming green space that everyone can enjoy.",
        originalUrl = URI("https://example.com/adaptive-story"),
        source = "NutsNews",
        publishedAt = "Published today",
        createdAt = null,
        thumbnailUrl = null,
        categories = listOf("Community"),
    )
