package com.nutsnews.app.feature.digest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.article.DailyDigest
import com.nutsnews.app.data.article.DailyDigestEngine
import com.nutsnews.app.data.article.DigestCategoryCount
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsButtonGradient
import java.net.URI

@Composable
fun DailyDigestScreen(
    articles: List<Article>,
    savedStoryIds: Set<StoryId>,
    hapticsEnabled: Boolean,
    onToggleSaved: (Article) -> Unit,
    onSaveHaptic: () -> Boolean,
    onOpenArticle: (Article) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val digest =
        remember(articles, savedStoryIds) {
            DailyDigestEngine.digest(
                articles = articles,
                savedStoryIds = savedStoryIds,
            )
        }
    val toggleSaved: (Article) -> Unit = { article ->
        performDailyDigestSaveHaptic(
            enabled = hapticsEnabled,
            performer = onSaveHaptic,
        )
        onToggleSaved(article)
    }

    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("daily_digest_screen"),
    ) {
        if (digest.featuredArticle == null) {
            DailyDigestEmptyState(onClose = onClose)
        } else {
            DailyDigestContent(
                digest = digest,
                savedStoryIds = savedStoryIds,
                onOpenArticle = onOpenArticle,
                onToggleSaved = toggleSaved,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun DailyDigestContent(
    digest: DailyDigest,
    savedStoryIds: Set<StoryId>,
    onOpenArticle: (Article) -> Unit,
    onToggleSaved: (Article) -> Unit,
    onClose: () -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("daily_digest_list"),
        contentPadding = PaddingValues(NutsNewsTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        item(key = "header") {
            DailyDigestHeader(onClose = onClose)
        }
        item(key = "metrics") {
            DailyDigestMetrics(digest = digest)
        }
        item(key = "categories") {
            DailyDigestCategoryMix(categories = digest.categoryCounts)
        }
        digest.featuredArticle?.let { article ->
            item(key = "featured-title") {
                DailyDigestSectionTitle("Start here")
            }
            item(key = "featured-${article.stableId.value}") {
                DailyDigestFeaturedCard(
                    article = article,
                    isSaved = article.stableId in savedStoryIds,
                    onOpen = { onOpenArticle(article) },
                    onToggleSaved = { onToggleSaved(article) },
                )
            }
        }
        if (digest.quickReadArticle != null || digest.worthSavingArticle != null) {
            item(key = "quick-actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                ) {
                    digest.quickReadArticle?.let { article ->
                        DailyDigestActionCard(
                            title = "Quick read",
                            subtitle = article.title,
                            icon = Icons.Filled.Timer,
                            testTag = "daily_digest_quick_read",
                            onClick = { onOpenArticle(article) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    digest.worthSavingArticle?.let { article ->
                        DailyDigestActionCard(
                            title = "Worth saving",
                            subtitle = article.title,
                            icon = Icons.Filled.Bookmark,
                            testTag = "daily_digest_worth_saving",
                            onClick = { onToggleSaved(article) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (digest.remainingArticles.isNotEmpty()) {
            item(key = "remaining-title") {
                DailyDigestSectionTitle("More from today")
            }
            items(
                items = digest.remainingArticles,
                key = { article -> article.stableId.value },
            ) { article ->
                DailyDigestStoryRow(
                    article = article,
                    isSaved = article.stableId in savedStoryIds,
                    onOpen = { onOpenArticle(article) },
                    onToggleSaved = { onToggleSaved(article) },
                )
            }
        }
    }
}

@Composable
private fun DailyDigestHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        ) {
            Text(
                text = "Today’s Picks",
                color = NutsNewsTheme.colors.accentHighlight,
                style =
                    NutsNewsTheme.typography.title.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.3.sp,
                    ),
            )
            Text(
                text =
                    "A calm native digest from the positive stories " +
                        "currently ready for you.",
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.subheadline,
            )
        }
        Surface(
            modifier =
                Modifier
                    .size(36.dp)
                    .testTag("daily_digest_close"),
            onClick = onClose,
            shape = CircleShape,
            color = NutsNewsTheme.colors.badgeBackground,
            border =
                BorderStroke(
                    NutsNewsTheme.borders.hairline,
                    NutsNewsTheme.colors.cardBorder,
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close Today’s Picks",
                    modifier = Modifier.size(16.dp),
                    tint = NutsNewsTheme.colors.accentHighlight,
                )
            }
        }
    }
}

@Composable
private fun DailyDigestMetrics(digest: DailyDigest) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        DailyDigestMetricTile(
            title = "Stories",
            value = digest.storyCount.toString(),
            icon = Icons.Filled.Newspaper,
            testTag = "daily_digest_metric_stories",
            modifier = Modifier.weight(1f),
        )
        DailyDigestMetricTile(
            title = "Sources",
            value = digest.uniqueSourceCount.toString(),
            icon = Icons.Filled.Business,
            testTag = "daily_digest_metric_sources",
            modifier = Modifier.weight(1f),
        )
        DailyDigestMetricTile(
            title = "Saved",
            value = digest.savedStoryCount.toString(),
            icon = Icons.Filled.Bookmark,
            testTag = "daily_digest_metric_saved",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DailyDigestMetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .height(104.dp)
                .testTag(testTag),
        shape = RoundedCornerShape(NutsNewsTheme.radii.medium),
        color = NutsNewsTheme.colors.cardBackgroundStrong,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = NutsNewsTheme.colors.accentHighlight,
            )
            Text(
                text = value,
                color = NutsNewsTheme.colors.primaryText,
                style =
                    NutsNewsTheme.typography.metric.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Text(
                text = title,
                color = NutsNewsTheme.colors.mutedText,
                style = NutsNewsTheme.typography.caption2,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DailyDigestCategoryMix(categories: List<DigestCategoryCount>) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("daily_digest_category_mix"),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = NutsNewsTheme.colors.cardBackground,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = NutsNewsTheme.colors.accentHighlight,
                )
                Text(
                    text = "Today’s positive mix",
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.subheadline,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (categories.isEmpty()) {
                Text(
                    text = "A simple uplifting feed is ready for you.",
                    color = NutsNewsTheme.colors.secondaryText,
                    style = NutsNewsTheme.typography.subheadline,
                )
            } else {
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
                    ) {
                        rowCategories.forEach { category ->
                            DailyDigestCategoryChip(
                                category = category,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyDigestCategoryChip(
    category: DigestCategoryCount,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .height(32.dp)
                .testTag("daily_digest_category_${category.label}"),
        shape = CircleShape,
        color = NutsNewsTheme.colors.badgeBackground,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NutsNewsTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.label,
                modifier = Modifier.weight(1f),
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = category.count.toString(),
                color = NutsNewsTheme.colors.accent,
                style = NutsNewsTheme.typography.caption2,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DailyDigestSectionTitle(title: String) {
    Text(
        text = title,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = NutsNewsTheme.spacing.xs),
        color = NutsNewsTheme.colors.primaryText,
        style = NutsNewsTheme.typography.subheadline,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun DailyDigestFeaturedCard(
    article: Article,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("daily_digest_featured_${article.stableId.value}"),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = NutsNewsTheme.colors.cardBackgroundStrong,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Column {
            DailyDigestFeaturedThumbnail(article = article)
            Column(
                modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = NutsNewsTheme.colors.accentHighlight,
                    )
                    Text(
                        text = "Daily pick",
                        color = NutsNewsTheme.colors.accentHighlight,
                        style = NutsNewsTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = article.title,
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.headline,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.summary.isNotEmpty()) {
                    Text(
                        text = article.summary,
                        color = NutsNewsTheme.colors.secondaryText,
                        style = NutsNewsTheme.typography.subheadline,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onOpen,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(nutsNewsButtonGradient())
                                .testTag("daily_digest_featured_open"),
                        shape = CircleShape,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                            ),
                    ) {
                        Text(
                            text = "Open story",
                            color = NutsNewsTheme.colors.buttonText,
                            style = NutsNewsTheme.typography.subheadline,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    DailyDigestSaveButton(
                        article = article,
                        isSaved = isSaved,
                        size = 42.dp,
                        iconSize = 17.dp,
                        onToggleSaved = onToggleSaved,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyDigestFeaturedThumbnail(article: Article) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(NutsNewsTheme.colors.badgeBackground)
                .testTag("daily_digest_featured_thumbnail"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Newspaper,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        article.thumbnailUrl?.let { thumbnailUrl ->
            AsyncImage(
                model = thumbnailUrl.toString(),
                contentDescription = "Thumbnail for ${article.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun DailyDigestActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .height(132.dp)
                .testTag(testTag)
                .semantics { role = Role.Button },
        onClick = onClick,
        shape = RoundedCornerShape(NutsNewsTheme.radii.medium),
        color = NutsNewsTheme.colors.cardBackgroundStrong,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = NutsNewsTheme.colors.accentHighlight,
            )
            Text(
                text = title,
                color = NutsNewsTheme.colors.primaryText,
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.caption,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DailyDigestStoryRow(
    article: Article,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("daily_digest_story_${article.stableId.value}"),
        onClick = onOpen,
        shape = RoundedCornerShape(NutsNewsTheme.radii.medium),
        color = NutsNewsTheme.colors.cardBackgroundStrong,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Row(
            modifier = Modifier.padding(NutsNewsTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            verticalAlignment = Alignment.Top,
        ) {
            DailyDigestStoryThumbnail(article = article)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
            ) {
                Text(
                    text = article.source,
                    modifier =
                        Modifier.testTag(
                            "daily_digest_story_source_${article.stableId.value}",
                        ),
                    color = NutsNewsTheme.colors.accentSoft,
                    style = NutsNewsTheme.typography.caption2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.title,
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.subheadline,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = article.displayDate,
                        modifier = Modifier.weight(1f),
                        color = NutsNewsTheme.colors.mutedText,
                        style = NutsNewsTheme.typography.caption2,
                        maxLines = 1,
                    )
                    DailyDigestSaveButton(
                        article = article,
                        isSaved = isSaved,
                        size = 30.dp,
                        iconSize = 14.dp,
                        onToggleSaved = onToggleSaved,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyDigestStoryThumbnail(article: Article) {
    val shape = RoundedCornerShape(NutsNewsTheme.radii.small)
    Box(
        modifier =
            Modifier
                .size(width = 96.dp, height = 72.dp)
                .clip(shape)
                .background(NutsNewsTheme.colors.badgeBackground)
                .testTag("daily_digest_story_thumbnail_${article.stableId.value}"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Newspaper,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        article.thumbnailUrl?.let { thumbnailUrl ->
            AsyncImage(
                model = thumbnailUrl.toString(),
                contentDescription = "Thumbnail for ${article.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun DailyDigestSaveButton(
    article: Article,
    isSaved: Boolean,
    size: Dp,
    iconSize: Dp,
    onToggleSaved: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .size(size)
                .testTag("daily_digest_save_${article.stableId.value}"),
        onClick = onToggleSaved,
        shape = CircleShape,
        color = NutsNewsTheme.colors.badgeBackground,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector =
                    if (isSaved) {
                        Icons.Filled.Bookmark
                    } else {
                        Icons.Outlined.BookmarkBorder
                    },
                contentDescription =
                    if (isSaved) {
                        "Remove saved story"
                    } else {
                        "Save story"
                    },
                modifier = Modifier.size(iconSize),
                tint = NutsNewsTheme.colors.accentHighlight,
            )
        }
    }
}

@Composable
private fun DailyDigestEmptyState(onClose: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(NutsNewsTheme.spacing.medium)
                .testTag("daily_digest_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Newspaper,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Spacer(modifier = Modifier.height(NutsNewsTheme.spacing.medium))
        Text(
            text = "No picks ready yet",
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
        )
        Spacer(modifier = Modifier.height(NutsNewsTheme.spacing.medium))
        Text(
            text =
                "Load stories on the home screen, then come back for " +
                    "a calm daily digest.",
            modifier = Modifier.padding(horizontal = NutsNewsTheme.spacing.large),
            color = NutsNewsTheme.colors.secondaryText,
            style = NutsNewsTheme.typography.subheadline,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(NutsNewsTheme.spacing.medium))
        Button(
            onClick = onClose,
            modifier =
                Modifier
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(nutsNewsButtonGradient())
                    .testTag("daily_digest_back_home"),
            shape = CircleShape,
            contentPadding =
                PaddingValues(
                    horizontal = NutsNewsTheme.spacing.medium,
                    vertical = NutsNewsTheme.spacing.small,
                ),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                ),
        ) {
            Text(
                text = "Back to home",
                color = NutsNewsTheme.colors.buttonText,
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

internal fun performDailyDigestSaveHaptic(
    enabled: Boolean,
    performer: () -> Boolean,
): Boolean {
    if (!enabled) return false
    return runCatching(performer).getOrDefault(false)
}

@Preview(showBackground = true)
@Composable
private fun DailyDigestPreview() {
    NutsNewsTheme(updateSystemBars = false) {
        DailyDigestScreen(
            articles =
                listOf(
                    digestPreviewArticle(
                        id = "community",
                        title = "Kind neighbors help a community garden bloom",
                        categories = listOf("Community", "Nature"),
                    ),
                    digestPreviewArticle(
                        id = "science",
                        title = "Students make an inspiring science discovery",
                        categories = listOf("Science", "Achievement"),
                    ),
                    digestPreviewArticle(
                        id = "rescue",
                        title = "Volunteers reunite a rescued animal with family",
                        categories = listOf("Animals", "Uplifting"),
                    ),
                ),
            savedStoryIds = emptySet(),
            hapticsEnabled = true,
            onToggleSaved = {},
            onSaveHaptic = { true },
            onOpenArticle = {},
            onClose = {},
        )
    }
}

private fun digestPreviewArticle(
    id: String,
    title: String,
    categories: List<String>,
): Article =
    Article(
        id = id,
        title = title,
        summary = "A bright local update gives everyone a reason to feel hopeful.",
        originalUrl = URI("https://example.com/$id"),
        source = "Good News Daily",
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = URI("https://example.com/$id.jpg"),
        categories = categories,
    )
