package com.nutsnews.app.feature.article

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsTheme
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: Article,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    heroImageModel: Any? = article.thumbnailUrl?.toString(),
    isLiked: Boolean = false,
    onToggleLiked: (Article) -> Unit = {},
    onArticleShown: (Article) -> Unit = {},
    onOpenOriginalStory: (Article, OriginalStoryColors) -> OriginalStoryOpenResult =
        { selectedArticle, _ ->
            if (selectedArticle.originalUrl == null) {
                OriginalStoryOpenResult.MissingUrl
            } else {
                OriginalStoryOpenResult.BrowserUnavailable
            }
        },
    onOriginalStoryOpened: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        configuration.smallestScreenWidthDp >= TabletMinimumWidthDp &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val palette = NutsNewsTheme.colors
    val brief = remember(article) { deriveArticleBrief(article) }
    var displayedLiked by
        remember(article.stableId.value) {
            mutableStateOf(isLiked)
        }
    var lastExternalLiked by
        remember(article.stableId.value) {
            mutableStateOf(isLiked)
        }
    var likeAnimationToken by
        remember(article.stableId.value) {
            mutableIntStateOf(0)
        }
    val likeGlow = remember(article.stableId.value) { Animatable(0f) }
    var originalStoryStatus by
        remember(article.stableId.value) {
            mutableStateOf<String?>(null)
        }
    val originalStoryColors =
        OriginalStoryColors(
            toolbar = palette.cardBackgroundStrong.toArgb(),
            navigationBar = palette.backgroundGradient.last().toArgb(),
            isDark = NutsNewsTheme.appTheme.isDark,
        )
    val openOriginalStory = {
        val result = onOpenOriginalStory(article, originalStoryColors)
        if (result.didOpen) onOriginalStoryOpened()
        originalStoryStatus = result.userFacingMessage()
    }

    LaunchedEffect(isLiked) {
        if (lastExternalLiked != isLiked) {
            lastExternalLiked = isLiked
            displayedLiked = isLiked
        }
    }
    LaunchedEffect(likeAnimationToken) {
        if (likeAnimationToken == 0) return@LaunchedEffect
        likeGlow.snapTo(1f)
        likeGlow.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = LikeGlowDurationMillis),
        )
    }
    LaunchedEffect(article.stableId) {
        onArticleShown(article)
    }

    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("article_detail"),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.testTag("article_detail_top_bar"),
                    title = {
                        Text(
                            text = "Story",
                            color = palette.primaryText,
                            style = NutsNewsTheme.typography.headline,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("article_detail_close"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close story",
                                tint = palette.accentHighlight,
                            )
                        }
                    },
                    actions = {
                        ArticleDetailLikeButton(
                            isLiked = displayedLiked,
                            glow = likeGlow.value,
                            onClick = {
                                displayedLiked = !displayedLiked
                                likeAnimationToken += 1
                                onToggleLiked(article)
                            },
                        )
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            scrolledContainerColor =
                                androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
            },
        ) { contentPadding ->
            if (isTabletLandscape) {
                CompactLandscapeArticleDetail(
                    article = article,
                    brief = brief,
                    heroImageModel = heroImageModel,
                    onOpenOriginalStory = openOriginalStory,
                    originalStoryStatus = originalStoryStatus,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                )
            } else {
                RegularArticleDetail(
                    article = article,
                    brief = brief,
                    heroImageModel = heroImageModel,
                    onOpenOriginalStory = openOriginalStory,
                    originalStoryStatus = originalStoryStatus,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                )
            }
        }
    }
}

@Composable
private fun ArticleDetailLikeButton(
    isLiked: Boolean,
    glow: Float,
    onClick: () -> Unit,
) {
    val palette = NutsNewsTheme.colors
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag("article_detail_like"),
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .shadow(
                        elevation = (22f * glow).dp,
                        shape = CircleShape,
                        ambientColor = palette.accentHighlight.copy(alpha = glow * 0.72f),
                        spotColor = palette.accentGlow.copy(alpha = glow * 0.55f),
                    )
                    .graphicsLayer {
                        scaleX = 1f + (glow * 0.035f)
                        scaleY = 1f + (glow * 0.035f)
                    }
                    .clip(CircleShape)
                    .background(palette.badgeBackground)
                    .testTag("article_detail_like_surface"),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnavailableArticleDetailScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    NutsNewsBackground(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Story",
                            color = palette.primaryText,
                            style = NutsNewsTheme.typography.headline,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("article_detail_close"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close story",
                                tint = palette.accentHighlight,
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
            },
        ) { contentPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(NutsNewsTheme.spacing.large)
                        .testTag("article_detail_unavailable"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Newspaper,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = palette.accent,
                )
                Text(
                    text = "This story is no longer available.",
                    modifier = Modifier.padding(top = NutsNewsTheme.spacing.medium),
                    color = palette.secondaryText,
                    style = NutsNewsTheme.typography.body,
                )
            }
        }
    }
}

@Composable
private fun RegularArticleDetail(
    article: Article,
    brief: ArticleBriefContent,
    heroImageModel: Any?,
    onOpenOriginalStory: () -> Unit,
    originalStoryStatus: String?,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(NutsNewsTheme.spacing.medium)
                .testTag("article_detail_regular_content"),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        ArticleDetailHero(
            article = article,
            imageModel = heroImageModel,
        )
        ArticleDetailCategoryRow(article.categories)
        ArticleDetailTitle(
            title = article.title,
            compact = false,
        )
        RegularArticleBrief(brief)
        ArticleDetailSummary(
            summary = article.summary,
            compact = false,
        )
        ArticleDetailSource(
            article = article,
            compact = false,
        )
        OriginalStoryButton(
            article = article,
            compact = false,
            status = originalStoryStatus,
            onClick = onOpenOriginalStory,
        )
    }
}

@Composable
private fun CompactLandscapeArticleDetail(
    article: Article,
    brief: ArticleBriefContent,
    heroImageModel: Any?,
    onOpenOriginalStory: () -> Unit,
    originalStoryStatus: String?,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val imageColumnWidth = minOf(maxWidth * TabletImageColumnFraction, TabletImageMaximumWidth)
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = NutsNewsTheme.spacing.medium,
                        vertical = NutsNewsTheme.spacing.small,
                    )
                    .testTag("article_detail_compact_content"),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.width(imageColumnWidth),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                ArticleDetailHero(
                    article = article,
                    imageModel = heroImageModel,
                )
                ArticleDetailCategoryRow(article.categories)
            }
            Column(
                modifier = Modifier.fillMaxHeight().weight(1f),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                ArticleDetailTitle(
                    title = article.title,
                    compact = true,
                )
                CompactArticleBrief(brief)
                ArticleDetailSummary(
                    summary = article.summary,
                    compact = true,
                )
                ArticleDetailSource(
                    article = article,
                    compact = true,
                )
                Spacer(modifier = Modifier.weight(1f))
                OriginalStoryButton(
                    article = article,
                    compact = true,
                    status = originalStoryStatus,
                    onClick = onOpenOriginalStory,
                )
            }
        }
    }
}

@Composable
internal fun ArticleDetailHero(
    article: Article,
    imageModel: Any?,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius)
    var loadState by
        remember(article.thumbnailUrl, imageModel) {
            mutableStateOf(
                if (imageModel == null) {
                    DetailHeroLoadState.Missing
                } else {
                    DetailHeroLoadState.Loading
                },
            )
        }
    var useWideCrop by
        remember(article.thumbnailUrl, imageModel) {
            mutableStateOf(false)
        }
    val frameModifier =
        if (useWideCrop) {
            Modifier.fillMaxWidth().aspectRatio(WideThumbnailCropAspectRatio)
        } else {
            Modifier.fillMaxWidth().height(NutsNewsTheme.dimensions.detailHeroHeight)
        }

    Box(
        modifier =
            modifier
                .then(frameModifier)
                .clip(shape)
                .background(palette.badgeBackground)
                .testTag("article_detail_hero"),
        contentAlignment = Alignment.Center,
    ) {
        if (loadState != DetailHeroLoadState.Loaded) {
            ArticleDetailHeroFallback()
        }

        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("article_detail_hero_image"),
                onLoading = {
                    loadState = DetailHeroLoadState.Loading
                    useWideCrop = false
                },
                onSuccess = { state ->
                    val image = state.result.image
                    useWideCrop =
                        shouldUseWideDetailHeroCrop(
                            pixelWidth = image.width,
                            pixelHeight = image.height,
                        )
                    loadState = DetailHeroLoadState.Loaded
                },
                onError = {
                    loadState = DetailHeroLoadState.Failed
                    useWideCrop = false
                },
                contentScale = ContentScale.Crop,
                alpha = if (loadState == DetailHeroLoadState.Loaded) 1f else 0f,
            )
        }

        if (loadState == DetailHeroLoadState.Loading) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(28.dp)
                        .testTag("article_detail_hero_loading"),
                color = palette.accent,
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
private fun ArticleDetailHeroFallback() {
    Column(
        modifier = Modifier.testTag("article_detail_hero_fallback"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        Icon(
            imageVector = Icons.Filled.Newspaper,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Text(
            text = "NutsNews",
            color = NutsNewsTheme.colors.secondaryText,
            style = NutsNewsTheme.typography.headline,
        )
    }
}

@Composable
private fun ArticleDetailCategoryRow(categories: List<String>) {
    if (categories.isEmpty()) return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("article_detail_categories"),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.take(MaximumVisibleCategories).forEachIndexed { index, category ->
            Row(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(NutsNewsTheme.colors.badgeBackground)
                        .border(
                            width = 0.75.dp,
                            color = NutsNewsTheme.colors.cardBorder,
                            shape = CircleShape,
                        )
                        .testTag("article_detail_category_$index")
                        .padding(
                            horizontal = NutsNewsTheme.dimensions.chipHorizontalPadding,
                            vertical = NutsNewsTheme.dimensions.chipVerticalPadding,
                        ),
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(NutsNewsTheme.spacing.xs)
                            .background(
                                color = NutsNewsTheme.colors.accent,
                                shape = CircleShape,
                            ),
                )
                Text(
                    text = category,
                    color = NutsNewsTheme.colors.secondaryText,
                    style = NutsNewsTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ArticleDetailTitle(
    title: String,
    compact: Boolean,
) {
    Text(
        text = title,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { heading() }
                .testTag("article_detail_title"),
        color = NutsNewsTheme.colors.primaryText,
        style =
            NutsNewsTheme.typography.title3.copy(
                fontSize = if (compact) 20.sp else 21.sp,
                lineHeight = if (compact) 26.sp else 29.sp,
                fontWeight = FontWeight.Bold,
            ),
        maxLines = if (compact) 3 else Int.MAX_VALUE,
        overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

@Composable
private fun RegularArticleBrief(brief: ArticleBriefContent) {
    DetailInfoCard(
        label = "NutsNews Brief",
        compact = false,
        modifier = Modifier.testTag("article_detail_brief"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                ArticleBriefMetric(
                    text = brief.estimatedReadTime,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                        )
                    },
                )
                ArticleBriefMetric(
                    text = brief.primaryMoodLabel,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                        )
                    },
                )
            }
            ArticleBriefBullet(
                title = "What happened",
                text = brief.whatHappened,
            )
            ArticleBriefBullet(
                title = "Why it’s good news",
                text = brief.whyGoodNews,
            )
            ArticleBriefBullet(
                title = "Feel-good takeaway",
                text = brief.takeaway,
            )
        }
    }
}

@Composable
private fun CompactArticleBrief(brief: ArticleBriefContent) {
    DetailInfoCard(
        label = "NutsNews Brief",
        compact = true,
        modifier = Modifier.testTag("article_detail_brief"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        ) {
            Text(
                text = brief.whyGoodNews,
                modifier = Modifier.testTag("article_detail_brief_why"),
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.subheadline,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Takeaway: ${brief.takeaway}",
                modifier = Modifier.testTag("article_detail_brief_takeaway"),
                color = NutsNewsTheme.colors.accent,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArticleBriefMetric(
    text: String,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(NutsNewsTheme.colors.badgeBackground)
                .padding(
                    horizontal = NutsNewsTheme.spacing.small,
                    vertical = NutsNewsTheme.spacing.xs,
                ),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            text = text,
            color = NutsNewsTheme.colors.secondaryText,
            style = NutsNewsTheme.typography.caption2,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ArticleBriefBullet(
    title: String,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 7.dp)
                    .size(NutsNewsTheme.spacing.xs)
                    .background(
                        color = NutsNewsTheme.colors.accentHighlight,
                        shape = CircleShape,
                    ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = title,
                color = NutsNewsTheme.colors.primaryText,
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = text,
                color = NutsNewsTheme.colors.secondaryText,
                style =
                    NutsNewsTheme.typography.subheadline.copy(
                        lineHeight = 22.sp,
                    ),
            )
        }
    }
}

@Composable
private fun ArticleDetailSummary(
    summary: String,
    compact: Boolean,
) {
    if (summary.isEmpty()) return

    DetailInfoCard(
        label = "Summary",
        compact = compact,
        modifier = Modifier.testTag("article_detail_summary"),
    ) {
        Text(
            text = summary,
            color = NutsNewsTheme.colors.secondaryText,
            style =
                if (compact) {
                    NutsNewsTheme.typography.subheadline
                } else {
                    NutsNewsTheme.typography.body.copy(lineHeight = 26.sp)
                },
            maxLines = if (compact) 5 else Int.MAX_VALUE,
            overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
        )
    }
}

@Composable
private fun ArticleDetailSource(
    article: Article,
    compact: Boolean,
) {
    DetailInfoCard(
        label = "Source",
        compact = compact,
        modifier = Modifier.testTag("article_detail_source"),
    ) {
        if (compact) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                Text(
                    text = article.source,
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("article_detail_source_name"),
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.subheadline,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.displayDate,
                    modifier = Modifier.testTag("article_detail_source_date"),
                    color = NutsNewsTheme.colors.mutedText,
                    style = NutsNewsTheme.typography.caption,
                    maxLines = 1,
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            ) {
                Text(
                    text = article.source,
                    modifier = Modifier.testTag("article_detail_source_name"),
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.headline,
                )
                Text(
                    text = article.displayDate,
                    modifier = Modifier.testTag("article_detail_source_date"),
                    color = NutsNewsTheme.colors.mutedText,
                    style = NutsNewsTheme.typography.subheadline,
                )
            }
        }
    }
}

@Composable
private fun OriginalStoryButton(
    article: Article,
    compact: Boolean,
    status: String?,
    onClick: () -> Unit,
) {
    val enabled = article.originalUrl != null
    val cleanSource = article.source.trim()
    Column(
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (enabled) 1f else 0.55f }
                    .clip(RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius))
                    .background(NutsNewsTheme.colors.badgeBackground)
                    .clickable(
                        enabled = enabled,
                        onClick = onClick,
                    )
                    .testTag("article_detail_open_original")
                    .padding(
                        horizontal = NutsNewsTheme.spacing.medium,
                        vertical = if (compact) 11.dp else 14.dp,
                    ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 17.dp else 19.dp),
                tint = NutsNewsTheme.colors.primaryText,
            )
            Text(
                text = if (cleanSource.isEmpty()) "Source" else "Source - $cleanSource",
                modifier = Modifier.padding(start = NutsNewsTheme.spacing.xs),
                color = NutsNewsTheme.colors.primaryText,
                style =
                    if (compact) {
                        NutsNewsTheme.typography.subheadline
                    } else {
                        NutsNewsTheme.typography.headline
                    },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        (status ?: if (!enabled) "Original story link unavailable." else null)?.let { message ->
            Text(
                text = message,
                modifier = Modifier.testTag("article_detail_browser_status"),
                color = NutsNewsTheme.colors.mutedText,
                style = NutsNewsTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun DetailInfoCard(
    label: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape =
        RoundedCornerShape(
            if (compact) {
                NutsNewsTheme.dimensions.controlCornerRadius
            } else {
                NutsNewsTheme.dimensions.cardCornerRadius
            },
        )
    val elevation =
        if (compact) {
            NutsNewsTheme.spacing.xs
        } else {
            NutsNewsTheme.spacing.small
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = NutsNewsTheme.colors.accentGlow,
                    spotColor = NutsNewsTheme.colors.accentGlow,
                )
                .clip(shape)
                .background(NutsNewsTheme.colors.cardBackgroundStrong)
                .then(
                    if (compact) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 1.25.dp,
                            color = NutsNewsTheme.colors.cardBorder,
                            shape = shape,
                        )
                    },
                )
                .padding(
                    if (compact) {
                        NutsNewsTheme.spacing.small
                    } else {
                        NutsNewsTheme.spacing.medium
                    },
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                if (compact) {
                    NutsNewsTheme.spacing.xs
                } else {
                    NutsNewsTheme.spacing.small
                },
            ),
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label.uppercase(Locale.ROOT),
                modifier = Modifier.testTag("article_detail_section_label"),
                color = NutsNewsTheme.colors.accent,
                style =
                    if (compact) {
                        NutsNewsTheme.typography.caption2
                    } else {
                        NutsNewsTheme.typography.caption
                    },
                fontWeight = FontWeight.SemiBold,
            )
        }
        content()
    }
}

@Immutable
internal data class ArticleBriefContent(
    val estimatedReadTime: String,
    val primaryMoodLabel: String,
    val whatHappened: String,
    val whyGoodNews: String,
    val takeaway: String,
)

internal fun deriveArticleBrief(article: Article): ArticleBriefContent {
    val searchableText =
        (
            listOf(article.title, article.summary, article.source) +
                article.categories
        ).joinToString(" ")
            .lowercase(Locale.ROOT)
    fun containsAny(vararg keywords: String): Boolean =
        keywords.any(searchableText::contains)

    val combinedText = "${article.title} ${article.summary}"
    val wordCount =
        combinedText
            .split(WhitespacePattern)
            .count(String::isNotBlank)
    val minutes = max(1, ceil(wordCount / WordsPerMinute).toInt())
    val mood =
        when {
            containsAny("science", "research", "space", "technology") -> "Curious"
            containsAny("achievement", "record", "award", "success") -> "Inspired"
            containsAny("community", "kindness", "volunteer", "family") -> "Hopeful"
            else -> "Calm"
        }
    val whyGood =
        when {
            containsAny("animal", "wildlife", "rescue", "pet") ->
                "It gives readers a wholesome moment centered on care, protection, " +
                    "and the bond people share with animals."

            containsAny("science", "research", "space", "technology", "discovery") ->
                "It highlights progress and curiosity, showing how discovery can make " +
                    "the world feel more hopeful."

            containsAny("community", "volunteer", "school", "family", "kindness") ->
                "It shows people helping each other in a practical way, which is exactly " +
                    "the kind of local goodness NutsNews is built to surface."

            containsAny("wellness", "health", "garden", "nature", "healing") ->
                "It offers a calmer kind of news moment, focused on wellbeing, " +
                    "restoration, and small positive changes."

            containsAny("achievement", "record", "award", "first", "milestone") ->
                "It celebrates effort, persistence, and a meaningful win that can leave " +
                    "readers feeling encouraged."

            else ->
                "It gives readers a positive, low-stress story with a clear reason to " +
                    "feel a little better about the day."
        }
    val takeaway =
        when {
            containsAny("community", "volunteer", "kindness") ->
                "Good news often starts close to home."

            containsAny("science", "research", "discovery") ->
                "Progress is still happening, one discovery at a time."

            containsAny("animal", "wildlife", "rescue") ->
                "Care and compassion can travel farther than expected."

            containsAny("achievement", "record", "milestone") ->
                "Small steps can turn into a story worth celebrating."

            else -> "A quick reminder that the world still has soft spots."
        }

    return ArticleBriefContent(
        estimatedReadTime = "$minutes min native brief",
        primaryMoodLabel = mood,
        whatHappened = article.summary.trim().ifEmpty { article.title.trim() },
        whyGoodNews = whyGood,
        takeaway = takeaway,
    )
}

private fun OriginalStoryOpenResult.userFacingMessage(): String? =
    when (this) {
        OriginalStoryOpenResult.OpenedCustomTab,
        OriginalStoryOpenResult.OpenedFallbackBrowser,
        -> null

        OriginalStoryOpenResult.MissingUrl -> "Original story link unavailable."
        OriginalStoryOpenResult.InvalidUrl -> "This story does not have a valid web address."
        OriginalStoryOpenResult.BrowserUnavailable -> "No browser is available on this device."
        OriginalStoryOpenResult.Failed -> "The original story could not be opened."
    }

internal fun shouldUseWideDetailHeroCrop(
    pixelWidth: Int,
    pixelHeight: Int,
): Boolean =
    pixelWidth > 0 &&
        pixelHeight > 0 &&
        pixelWidth.toFloat() / pixelHeight.toFloat() > WideThumbnailCropAspectRatio

private enum class DetailHeroLoadState {
    Missing,
    Loading,
    Loaded,
    Failed,
}

private const val TabletMinimumWidthDp = 600
private const val TabletImageColumnFraction = 0.39f
private val TabletImageMaximumWidth = 440.dp
private const val WideThumbnailCropAspectRatio = 3f / 2f
private const val MaximumVisibleCategories = 8
private const val WordsPerMinute = 180.0
private val WhitespacePattern = Regex("\\s+")
private const val LikeGlowDurationMillis = 1_000
