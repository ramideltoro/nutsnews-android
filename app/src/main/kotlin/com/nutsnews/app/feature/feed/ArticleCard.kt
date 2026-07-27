package com.nutsnews.app.feature.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsPalettes
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsButtonGradient

enum class ArticleCardLayout {
    Regular,
    TabletLandscapeCompact,
}

@Composable
fun ArticleCard(
    article: Article,
    layout: ArticleCardLayout = ArticleCardLayout.Regular,
    isLiked: Boolean = false,
    onReadStory: (Article) -> Unit,
    onLikeStory: (Article) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val isCompact = layout == ArticleCardLayout.TabletLandscapeCompact
    val cardShape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isCompact) 10.dp else 16.dp,
                    shape = cardShape,
                    ambientColor = palette.accentGlow,
                    spotColor = palette.accentGlow,
                )
                .testTag("feed_story_${article.id}"),
        shape = cardShape,
        color = palette.cardBackgroundStrong,
        border = BorderStroke(1.25.dp, palette.cardBorder),
    ) {
        if (isCompact) {
            CompactArticleCardContent(
                article = article,
                isLiked = isLiked,
                onReadStory = onReadStory,
                onLikeStory = onLikeStory,
                modifier = Modifier.padding(12.dp),
            )
        } else {
            RegularArticleCardContent(
                article = article,
                isLiked = isLiked,
                onReadStory = onReadStory,
                onLikeStory = onLikeStory,
                modifier = Modifier.padding(NutsNewsTheme.dimensions.cardPadding),
            )
        }
    }
}

@Composable
private fun RegularArticleCardContent(
    article: Article,
    isLiked: Boolean,
    onReadStory: (Article) -> Unit,
    onLikeStory: (Article) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ArticleThumbnail(article)
        ArticleCategoryRow(article.categories)
        ArticleTitle(
            title = article.title,
            isCompact = false,
        )
        ArticleSummary(
            summary = article.summary,
            isCompact = false,
        )
        ArticleCardFooter(
            article = article,
            isLiked = isLiked,
            onReadStory = onReadStory,
            onLikeStory = onLikeStory,
        )
    }
}

@Composable
private fun CompactArticleCardContent(
    article: Article,
    isLiked: Boolean,
    onReadStory: (Article) -> Unit,
    onLikeStory: (Article) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ArticleThumbnail(
            article = article,
            modifier = Modifier.width(286.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ArticleCategoryRow(article.categories)
            ArticleTitle(
                title = article.title,
                isCompact = true,
            )
            ArticleSummary(
                summary = article.summary,
                isCompact = true,
            )
            Spacer(modifier = Modifier.weight(1f))
            ArticleCardFooter(
                article = article,
                isLiked = isLiked,
                onReadStory = onReadStory,
                onLikeStory = onLikeStory,
            )
        }
    }
}

@Composable
private fun ArticleThumbnail(
    article: Article,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val imageShape = RoundedCornerShape(NutsNewsTheme.dimensions.imageCornerRadius)
    var loadState by
        remember(article.thumbnailUrl) {
            mutableStateOf(
                if (article.thumbnailUrl == null) {
                    ThumbnailLoadState.Missing
                } else {
                    ThumbnailLoadState.Loading
                },
            )
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(ThumbnailAspectRatio)
                .clip(imageShape)
                .background(palette.badgeBackground)
                .testTag("article_thumbnail"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Newspaper,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = palette.accent,
            )
            Text(
                text = "NutsNews",
                color = palette.secondaryText,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
            )
        }

        article.thumbnailUrl?.let { thumbnailUrl ->
            AsyncImage(
                model = thumbnailUrl.toString(),
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("article_thumbnail_image"),
                onLoading = { loadState = ThumbnailLoadState.Loading },
                onSuccess = { loadState = ThumbnailLoadState.Loaded },
                onError = { loadState = ThumbnailLoadState.Failed },
                contentScale = ContentScale.Crop,
                alpha =
                    if (loadState == ThumbnailLoadState.Loaded) {
                        1f
                    } else {
                        0f
                    },
            )
        }

        if (loadState == ThumbnailLoadState.Loading) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(24.dp)
                        .testTag("article_thumbnail_loading"),
                color = palette.accent,
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
private fun ArticleCategoryRow(categories: List<String>) {
    if (categories.isEmpty()) return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .horizontalScroll(rememberScrollState())
                .testTag("article_categories"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.take(MaximumVisibleCategories).forEachIndexed { index, category ->
            ArticleCategoryBadge(
                title = category,
                dotIndex = index,
            )
        }
    }
}

@Composable
private fun ArticleCategoryBadge(
    title: String,
    dotIndex: Int,
) {
    val palette = NutsNewsTheme.colors
    val shape = CircleShape
    Row(
        modifier =
            Modifier
                .clip(shape)
                .background(palette.badgeBackground)
                .border(0.75.dp, palette.cardBorder, shape)
                .testTag("article_category_$dotIndex")
                .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .background(
                        color =
                            NutsNewsPalettes.categoryDotColor(
                                theme = NutsNewsTheme.appTheme,
                                index = dotIndex,
                                isSelected = false,
                            ),
                        shape = CircleShape,
                    ),
        )
        Text(
            text = title,
            color = palette.secondaryText,
            style = NutsNewsTheme.typography.caption2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ArticleTitle(
    title: String,
    isCompact: Boolean,
) {
    Text(
        text = title,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("article_title"),
        color = NutsNewsTheme.colors.primaryText,
        style =
            NutsNewsTheme.typography.cardTitle.copy(
                fontSize = if (isCompact) 18.sp else 20.sp,
                lineHeight = if (isCompact) 24.sp else 27.sp,
            ),
        maxLines = if (isCompact) 3 else Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ArticleSummary(
    summary: String,
    isCompact: Boolean,
) {
    if (summary.isEmpty()) return

    Text(
        text = summary,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("article_summary"),
        color = NutsNewsTheme.colors.secondaryText,
        style =
            if (isCompact) {
                NutsNewsTheme.typography.subheadline
            } else {
                NutsNewsTheme.typography.body
            },
        maxLines = if (isCompact) 4 else Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ArticleCardFooter(
    article: Article,
    isLiked: Boolean,
    onReadStory: (Article) -> Unit,
    onLikeStory: (Article) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("article_footer"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ReadStoryButton(
                onClick = { onReadStory(article) },
                modifier = Modifier.align(Alignment.Center),
            )
            LikeStoryButton(
                isLiked = isLiked,
                onClick = { onLikeStory(article) },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = article.displayDate,
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("article_date"),
                color = NutsNewsTheme.colors.mutedText,
                style = NutsNewsTheme.typography.caption,
                maxLines = 1,
            )
            Text(
                text = article.source,
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("article_source"),
                color = NutsNewsTheme.colors.accentSoft,
                style = NutsNewsTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun ReadStoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Read Story",
        modifier =
            modifier
                .clip(CircleShape)
                .background(nutsNewsButtonGradient())
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                .testTag("article_read_story")
                .padding(horizontal = 18.dp, vertical = 9.dp),
        color = NutsNewsTheme.colors.buttonText,
        style = NutsNewsTheme.typography.caption,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun LikeStoryButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val borderColor = if (isLiked) palette.likedCardBorder else palette.cardBorder
    Box(
        modifier =
            modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(palette.badgeBackground)
                .border(NutsNewsTheme.borders.hairline, borderColor, CircleShape)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                .testTag("article_like_story"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector =
                if (isLiked) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Filled.FavoriteBorder
                },
            contentDescription = if (isLiked) "Liked" else "Like story",
            modifier = Modifier.size(16.dp),
            tint = if (isLiked) palette.likedCardAccent else palette.accentHighlight,
        )
    }
}

private enum class ThumbnailLoadState {
    Missing,
    Loading,
    Loaded,
    Failed,
}

private const val ThumbnailAspectRatio = 3f / 2f
private const val MaximumVisibleCategories = 6
