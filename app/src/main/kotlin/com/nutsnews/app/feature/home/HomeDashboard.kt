package com.nutsnews.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.data.preferences.NutsNewsPersonalization
import com.nutsnews.app.designsystem.NutsNewsTheme
import java.net.URI

@Composable
fun HomeDashboard(
    uiState: HomeDashboardUiState,
    articles: List<Article>,
    isFeedLoading: Boolean,
    onTodayPicks: () -> Unit,
    onGoodMood: () -> Unit,
    onReadingStats: () -> Unit,
    onSavedStories: () -> Unit,
    onArchiveSearch: () -> Unit,
    onPersonalize: () -> Unit,
    onRefreshForYou: () -> Unit,
    onOpenArticle: (Article) -> Unit,
    modifier: Modifier = Modifier,
) {
    var refreshPage by rememberSaveable { mutableIntStateOf(0) }
    var refreshTurns by rememberSaveable { mutableIntStateOf(0) }
    val refreshRotation by
        animateFloatAsState(
            targetValue = refreshTurns * 360f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 370f),
            label = "For You refresh",
        )
    val personalizedPool =
        remember(
            articles,
            uiState.selectedTopicIds,
            uiState.selectedMoodId,
        ) {
            NutsNewsPersonalization.personalizedArticles(
                articles = articles,
                selectedTopicIds = uiState.selectedTopicIds,
                selectedMoodId = uiState.selectedMoodId,
                limit = PersonalizedPoolLimit,
            )
        }
    val personalizedArticles =
        remember(personalizedPool, refreshPage) {
            personalizedPool.page(refreshPage)
        }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("home_dashboard"),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = NutsNewsTheme.spacing.medium,
                vertical = NutsNewsTheme.spacing.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        item(key = "hero") {
            DashboardHero(uiState)
        }
        item(key = "actions") {
            QuickActions(
                uiState = uiState,
                onTodayPicks = onTodayPicks,
                onGoodMood = onGoodMood,
                onReadingStats = onReadingStats,
                onSavedStories = onSavedStories,
                onArchiveSearch = onArchiveSearch,
                onPersonalize = onPersonalize,
            )
        }
        if (personalizedArticles.isNotEmpty() || isFeedLoading) {
            item(key = "for-you") {
                ForYouSection(
                    uiState = uiState,
                    articles = personalizedArticles,
                    isLoading = personalizedArticles.isEmpty() && isFeedLoading,
                    refreshRotation = refreshRotation,
                    onRefresh = {
                        refreshPage += 1
                        refreshTurns += 1
                        onRefreshForYou()
                    },
                    onPersonalize = onPersonalize,
                    onOpenArticle = onOpenArticle,
                )
            }
        }
    }
}

@Composable
private fun DashboardHero(uiState: HomeDashboardUiState) {
    val palette = NutsNewsTheme.colors
    val spacing = NutsNewsTheme.spacing

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("dashboard_hero"),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = palette.cardBackgroundStrong,
        border = BorderStroke(NutsNewsTheme.borders.emphasized, palette.cardBorder),
        shadowElevation = spacing.xs,
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text(
                        text = "TODAY’S GOOD-NEWS RESET",
                        color = palette.accent,
                        style = NutsNewsTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = uiState.goalTitle,
                        color = palette.primaryText,
                        style = NutsNewsTheme.typography.metric,
                    )
                    Text(
                        text =
                            "Your home is now a native good-news dashboard, " +
                                "not just a list of links.",
                        color = palette.secondaryText,
                        style = NutsNewsTheme.typography.subheadline,
                    )
                }
                GoalProgressRing(uiState)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                DashboardPill(
                    icon = Icons.Filled.WbTwilight,
                    text = uiState.selectedMoodTitle,
                    tag = "dashboard_mood",
                )
                DashboardPill(
                    icon = Icons.Filled.Bookmark,
                    text = "${uiState.savedCount} saved",
                    tag = "dashboard_saved_count",
                )
                DashboardPill(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    text = "${uiState.notesCount} notes",
                    tag = "dashboard_notes_count",
                )
            }
        }
    }
}

@Composable
private fun GoalProgressRing(uiState: HomeDashboardUiState) {
    val palette = NutsNewsTheme.colors
    Box(
        modifier =
            Modifier
                .size(66.dp)
                .testTag("dashboard_goal_progress"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            drawCircle(
                color = palette.cardBorder,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = palette.accentHighlight,
                startAngle = -90f,
                sweepAngle = uiState.goalProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${uiState.goalProgressPercent}%",
            color = palette.primaryText,
            style = NutsNewsTheme.typography.caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DashboardPill(
    icon: ImageVector,
    text: String,
    tag: String,
) {
    val palette = NutsNewsTheme.colors
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(palette.badgeBackground)
                .testTag(tag)
                .padding(
                    horizontal = NutsNewsTheme.spacing.small,
                    vertical = NutsNewsTheme.spacing.xs,
                ),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = palette.secondaryText,
        )
        Text(
            text = text,
            color = palette.secondaryText,
            style = NutsNewsTheme.typography.caption2,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickActions(
    uiState: HomeDashboardUiState,
    onTodayPicks: () -> Unit,
    onGoodMood: () -> Unit,
    onReadingStats: () -> Unit,
    onSavedStories: () -> Unit,
    onArchiveSearch: () -> Unit,
    onPersonalize: () -> Unit,
) {
    val actions =
        listOf(
            DashboardAction(
                Icons.Filled.Newspaper,
                "Today’s Picks",
                "Native daily digest",
                "daily_digest",
                onTodayPicks,
            ),
            DashboardAction(
                Icons.Filled.AutoAwesome,
                "Good Mood",
                "Pick how you feel",
                "good_mood",
                onGoodMood,
            ),
            DashboardAction(
                Icons.Filled.BarChart,
                "Goal + Streak",
                uiState.streakText,
                "reading_stats",
                onReadingStats,
            ),
            DashboardAction(
                Icons.Filled.Bookmark,
                "Saved Library",
                "Your feel-good archive",
                "saved",
                onSavedStories,
            ),
            DashboardAction(
                Icons.Filled.Search,
                "Search Archive",
                "Find old good news",
                "search",
                onArchiveSearch,
            ),
            DashboardAction(
                if (uiState.reminderEnabled) {
                    Icons.Filled.NotificationsActive
                } else {
                    Icons.Filled.Tune
                },
                if (uiState.reminderEnabled) "Reminder On" else "Personalize",
                if (uiState.reminderEnabled) {
                    "Daily at ${uiState.reminderDisplayTime}"
                } else {
                    uiState.selectedTopicText
                },
                "personalize",
                onPersonalize,
            ),
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("dashboard_actions"),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                rowActions.forEach { action ->
                    DashboardActionCard(
                        action = action,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardActionCard(
    action: DashboardAction,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    Surface(
        modifier =
            modifier
                .heightIn(min = 128.dp)
                .clickable(onClick = action.onClick)
                .testTag("dashboard_action_${action.tag}"),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = palette.cardBackgroundStrong,
        border = BorderStroke(NutsNewsTheme.borders.hairline, palette.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.badgeBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = palette.accentHighlight,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            ) {
                Text(
                    text = action.title,
                    color = palette.primaryText,
                    style = NutsNewsTheme.typography.subheadline,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = action.subtitle,
                    color = palette.secondaryText,
                    style = NutsNewsTheme.typography.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ForYouSection(
    uiState: HomeDashboardUiState,
    articles: List<Article>,
    isLoading: Boolean,
    refreshRotation: Float,
    onRefresh: () -> Unit,
    onPersonalize: () -> Unit,
    onOpenArticle: (Article) -> Unit,
) {
    val palette = NutsNewsTheme.colors
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("for_you_section"),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = palette.cardBackgroundStrong.copy(alpha = 0.72f),
        border = BorderStroke(NutsNewsTheme.borders.hairline, palette.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
                ) {
                    Text(
                        text = "For You",
                        color = palette.primaryText,
                        style = NutsNewsTheme.typography.title3,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = uiState.personalizationSummary,
                        color = palette.secondaryText,
                        style = NutsNewsTheme.typography.caption,
                    )
                }
                Spacer(Modifier.width(NutsNewsTheme.spacing.small))
                Row(horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs)) {
                    ForYouIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "Refresh For You stories",
                        modifier = Modifier.graphicsLayer { rotationZ = refreshRotation },
                        onClick = onRefresh,
                    )
                    ForYouIconButton(
                        icon = Icons.Filled.Edit,
                        contentDescription = "Edit For You preferences",
                        onClick = onPersonalize,
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .testTag("for_you_loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = palette.accent,
                        strokeWidth = 3.dp,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small)) {
                    articles.forEach { article ->
                        ForYouStoryRow(
                            article = article,
                            onClick = { onOpenArticle(article) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForYouIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.badgeBackground)
                .border(
                    NutsNewsTheme.borders.hairline,
                    palette.cardBorder,
                    CircleShape,
                ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = palette.accentHighlight,
        )
    }
}

@Composable
private fun ForYouStoryRow(
    article: Article,
    onClick: () -> Unit,
) {
    val palette = NutsNewsTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius))
                .background(palette.badgeBackground)
                .border(
                    NutsNewsTheme.borders.hairline,
                    palette.cardBorder,
                    RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius),
                )
                .clickable(onClick = onClick)
                .testTag("for_you_story_${article.stableId.value}")
                .padding(NutsNewsTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 74.dp, height = 58.dp)
                    .clip(RoundedCornerShape(NutsNewsTheme.radii.small))
                    .background(palette.badgeBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Newspaper,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = palette.accent,
            )
            article.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url.toString(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = article.title,
                color = palette.primaryText,
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.Bold,
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
            modifier =
                Modifier
                    .padding(top = NutsNewsTheme.spacing.xxs)
                    .size(12.dp),
            tint = palette.mutedText,
        )
    }
}

private fun List<Article>.page(page: Int): List<Article> {
    if (isEmpty()) return emptyList()
    val startIndex = (page * PersonalizedPageSize) % size
    return List(minOf(PersonalizedPageSize, size)) { offset ->
        this[(startIndex + offset) % size]
    }
}

private data class DashboardAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tag: String,
    val onClick: () -> Unit,
)

private const val PersonalizedPoolLimit = 12
private const val PersonalizedPageSize = 3

@Preview(showBackground = true)
@Composable
private fun HomeDashboardPreview() {
    NutsNewsTheme {
        HomeDashboard(
            uiState =
                HomeDashboardUiState(
                    isLoading = false,
                    todayStoryCount = 2,
                    dailyGoal = 3,
                    currentStreak = 4,
                    savedCount = 8,
                    notesCount = 2,
                    reminderEnabled = true,
                ),
            articles =
                listOf(
                    Article(
                        id = "preview",
                        title = "Neighbors turn an empty lot into a thriving community garden",
                        summary = "A little kindness grew into something special.",
                        originalUrl = URI("https://example.com/story"),
                        source = "NutsNews",
                        publishedAt = null,
                        createdAt = null,
                        thumbnailUrl = null,
                        categories = listOf("Community"),
                    ),
                ),
            isFeedLoading = false,
            onTodayPicks = {},
            onGoodMood = {},
            onReadingStats = {},
            onSavedStories = {},
            onArchiveSearch = {},
            onPersonalize = {},
            onRefreshForYou = {},
            onOpenArticle = {},
        )
    }
}
