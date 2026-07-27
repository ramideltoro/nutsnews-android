package com.nutsnews.app.feature.article

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: Article,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    heroImageModel: Any? = article.thumbnailUrl?.toString(),
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        configuration.smallestScreenWidthDp >= TabletMinimumWidthDp &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val palette = NutsNewsTheme.colors

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
                    heroImageModel = heroImageModel,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                )
            } else {
                RegularArticleDetail(
                    article = article,
                    heroImageModel = heroImageModel,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                )
            }
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
    heroImageModel: Any?,
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
    }
}

@Composable
private fun CompactLandscapeArticleDetail(
    article: Article,
    heroImageModel: Any?,
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
                Spacer(modifier = Modifier.weight(1f))
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
