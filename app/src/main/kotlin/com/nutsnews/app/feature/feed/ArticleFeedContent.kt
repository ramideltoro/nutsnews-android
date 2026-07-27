package com.nutsnews.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsButtonGradient

@Composable
fun ArticleFeedContent(
    uiState: ArticleFeedUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: (Article) -> Unit,
    onOpenArticle: (Article) -> Unit,
    dashboard: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    when {
        uiState.articles.isEmpty() && uiState.isInitialLoading -> {
            InitialFeedLoading(modifier)
        }

        uiState.articles.isEmpty() -> {
            EmptyFeedState(
                selectedCategory = uiState.selectedCategory,
                errorMessage = uiState.errorMessage,
                onRetry = onRetry,
                modifier = modifier,
            )
        }

        else -> {
            PopulatedFeed(
                uiState = uiState,
                onRefresh = onRefresh,
                onRetry = onRetry,
                onLoadMore = onLoadMore,
                onOpenArticle = onOpenArticle,
                dashboard = dashboard,
                listState = listState,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun InitialFeedLoading(modifier: Modifier) {
    val palette = NutsNewsTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("feed_initial_loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = palette.accent,
            strokeWidth = 3.dp,
        )
        Text(
            text = "Loading good news...",
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.small),
            color = palette.secondaryText,
            style = NutsNewsTheme.typography.subheadline,
        )
    }
}

@Composable
private fun EmptyFeedState(
    selectedCategory: String?,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    val palette = NutsNewsTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("feed_empty_state")
                .padding(NutsNewsTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Eco,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            tint = palette.accent,
        )
        Text(
            text =
                selectedCategory
                    ?.let { category -> "No $category stories yet" }
                    ?: "No stories loaded yet",
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.medium),
            color = palette.primaryText,
            style = NutsNewsTheme.typography.headline,
            textAlign = TextAlign.Center,
        )
        errorMessage?.let { message ->
            Text(
                text = message,
                modifier =
                    Modifier
                        .padding(horizontal = NutsNewsTheme.spacing.large)
                        .padding(top = NutsNewsTheme.spacing.small)
                        .testTag("feed_empty_error"),
                color = palette.secondaryText,
                style = NutsNewsTheme.typography.subheadline,
                textAlign = TextAlign.Center,
            )
        }
        RetryButton(
            text = "Try again",
            onClick = onRetry,
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.medium),
        )
    }
}

@Composable
private fun PopulatedFeed(
    uiState: ArticleFeedUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: (Article) -> Unit,
    onOpenArticle: (Article) -> Unit,
    dashboard: @Composable () -> Unit,
    listState: LazyListState,
    modifier: Modifier,
) {
    val pullState = rememberPullToRefreshState()
    val palette = NutsNewsTheme.colors

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            if (!uiState.isLoading) onRefresh()
        },
        modifier =
            modifier
                .fillMaxSize()
                .testTag("feed_pull_to_refresh"),
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = uiState.isRefreshing,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .testTag("feed_refresh_indicator"),
                containerColor = palette.cardBackgroundStrong,
                color = palette.accent,
            )
        },
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("feed_article_list"),
            state = listState,
            contentPadding =
                PaddingValues(
                    horizontal = NutsNewsTheme.spacing.medium,
                    vertical = NutsNewsTheme.spacing.large,
                ),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        ) {
            item(key = DashboardKey) {
                dashboard()
            }
            item(key = LatestStoriesKey) {
                Text(
                    text = "Latest stories",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = NutsNewsTheme.spacing.xs)
                            .testTag("feed_latest_stories"),
                    color = palette.primaryText,
                    style = NutsNewsTheme.typography.title3,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (uiState.isStale) {
                item(key = StaleBannerKey) {
                    StaleFeedBanner()
                }
            }
            itemsIndexed(
                items = uiState.articles,
                key = { _, article -> article.id },
            ) { index, article ->
                ArticleFeedStoryPlaceholder(
                    article = article,
                    onClick = { onOpenArticle(article) },
                )
                if (index == uiState.articles.lastIndex) {
                    LaunchedEffect(article.id, uiState.nextPage) {
                        if (uiState.canLoadMore) {
                            onLoadMore(article)
                        }
                    }
                }
            }
            if (uiState.isPaginating) {
                item(key = PaginationProgressKey) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = NutsNewsTheme.spacing.medium)
                                .testTag("feed_load_more_progress"),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = palette.accent,
                            strokeWidth = 3.dp,
                        )
                    }
                }
            }
            uiState.errorMessage?.let { errorMessage ->
                item(key = ErrorBannerKey) {
                    FeedErrorBanner(
                        message = errorMessage,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleFeedStoryPlaceholder(
    article: Article,
    onClick: () -> Unit,
) {
    val palette = NutsNewsTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .clip(RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius))
                .background(palette.cardBackgroundStrong)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                .testTag("feed_story_${article.id}")
                .padding(NutsNewsTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = article.title,
                color = palette.primaryText,
                style = NutsNewsTheme.typography.headline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = article.source,
                color = palette.mutedText,
                style = NutsNewsTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = palette.mutedText,
        )
    }
}

@Composable
private fun StaleFeedBanner() {
    val palette = NutsNewsTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NutsNewsTheme.radii.small))
                .background(palette.badgeBackground)
                .testTag("feed_stale_banner")
                .padding(NutsNewsTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = palette.accent,
        )
        Text(
            text = "Showing saved stories while we reconnect.",
            color = palette.secondaryText,
            style = NutsNewsTheme.typography.caption,
        )
    }
}

@Composable
private fun FeedErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    val palette = NutsNewsTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NutsNewsTheme.radii.small))
                .background(androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.16f))
                .testTag("feed_error_banner")
                .padding(NutsNewsTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = palette.secondaryText,
            style = NutsNewsTheme.typography.caption,
        )
        RetryButton(
            text = "Retry",
            onClick = onRetry,
        )
    }
}

@Composable
private fun RetryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier =
            modifier
                .clip(CircleShape)
                .background(nutsNewsButtonGradient())
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                .testTag("feed_retry")
                .padding(
                    horizontal = NutsNewsTheme.spacing.medium,
                    vertical = NutsNewsTheme.spacing.small,
                ),
        color = NutsNewsTheme.colors.buttonText,
        style = NutsNewsTheme.typography.subheadline,
        fontWeight = FontWeight.SemiBold,
    )
}

private const val DashboardKey = "feed-dashboard"
private const val LatestStoriesKey = "feed-latest-stories"
private const val StaleBannerKey = "feed-stale-banner"
private const val PaginationProgressKey = "feed-pagination-progress"
private const val ErrorBannerKey = "feed-error-banner"
