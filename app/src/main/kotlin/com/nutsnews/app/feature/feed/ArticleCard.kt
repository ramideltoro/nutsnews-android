package com.nutsnews.app.feature.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsPalettes
import com.nutsnews.app.designsystem.NutsNewsMotion
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsButtonGradient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    hapticsEnabled: Boolean = true,
    onLikeHaptic: () -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val isCompact = layout == ArticleCardLayout.TabletLandscapeCompact
    val cardShape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius)
    var displayedLiked by
        remember(article.stableId.value) {
            mutableStateOf(isLiked)
        }
    var lastExternalLiked by
        remember(article.stableId.value) {
            mutableStateOf(isLiked)
        }
    var animationToken by
        rememberSaveable(article.stableId.value) {
            mutableIntStateOf(0)
        }
    var animationIsLike by
        remember(article.stableId.value) {
            mutableStateOf(false)
        }
    val heartGlow = remember(article.stableId.value) { Animatable(0f) }
    val cardGlow = remember(article.stableId.value) { Animatable(0f) }
    val celebrationProgress = remember(article.stableId.value) { Animatable(1f) }
    var showCelebration by
        remember(article.stableId.value) {
            mutableStateOf(false)
        }
    val reducedMotion = NutsNewsTheme.reducedMotion

    LaunchedEffect(isLiked) {
        if (isLiked != lastExternalLiked) {
            lastExternalLiked = isLiked
            displayedLiked = isLiked
        }
    }
    LaunchedEffect(animationToken) {
        if (animationToken == 0) return@LaunchedEffect
        if (reducedMotion) {
            heartGlow.snapTo(0f)
            cardGlow.snapTo(0f)
            celebrationProgress.snapTo(1f)
            showCelebration = false
            return@LaunchedEffect
        }
        if (animationIsLike) {
            heartGlow.snapTo(1f)
            cardGlow.snapTo(0f)
            celebrationProgress.snapTo(0f)
            showCelebration = true
            coroutineScope {
                launch {
                    heartGlow.animateTo(
                        targetValue = 0f,
                        animationSpec =
                            tween(durationMillis = NutsNewsMotion.ActionGlowMillis),
                    )
                }
                launch {
                    cardGlow.animateTo(
                        targetValue = 1f,
                        animationSpec =
                            tween(durationMillis = NutsNewsMotion.LikeGlowInMillis),
                    )
                    delay(
                        NutsNewsMotion.LikeActiveWindowMillis -
                            NutsNewsMotion.LikeGlowInMillis,
                    )
                    cardGlow.animateTo(
                        targetValue = 0f,
                        animationSpec =
                            tween(durationMillis = NutsNewsMotion.LikeSettleMillis),
                    )
                }
                launch {
                    celebrationProgress.animateTo(
                        targetValue = 1f,
                        animationSpec =
                            tween(durationMillis = NutsNewsMotion.CelebrationTravelMillis),
                    )
                    delay(
                        NutsNewsMotion.CelebrationClearMillis -
                            NutsNewsMotion.CelebrationTravelMillis,
                    )
                    showCelebration = false
                }
            }
        } else {
            heartGlow.snapTo(1f)
            celebrationProgress.snapTo(1f)
            showCelebration = false
            coroutineScope {
                launch {
                    heartGlow.animateTo(
                        targetValue = 0f,
                        animationSpec =
                            tween(durationMillis = NutsNewsMotion.ActionGlowMillis),
                    )
                }
                launch {
                    cardGlow.animateTo(
                        targetValue = 0f,
                        animationSpec =
                            tween(durationMillis = NutsNewsMotion.UnlikeMillis),
                    )
                }
            }
        }
    }

    val showLikedStyling = displayedLiked || cardGlow.value > 0f
    val cardBorderColor =
        if (showLikedStyling) palette.likedCardBorder else palette.cardBorder
    val cardBorderWidth =
        if (showLikedStyling) NutsNewsTheme.borders.selected else 1.25.dp
    val baseShadow = 16f
    val shadowElevation = (baseShadow + (18f * cardGlow.value)).dp
    val shadowColor =
        if (cardGlow.value > 0f) palette.likedCardGlow else palette.accentGlow
    val toggleLike: (Article) -> Unit = { selectedArticle ->
        val willLike = !displayedLiked
        displayedLiked = willLike
        animationIsLike = willLike
        animationToken += 1
        performLikeHapticIfEnabled(
            enabled = hapticsEnabled,
            performer = onLikeHaptic,
        )
        onLikeStory(selectedArticle)
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = shadowElevation,
                    shape = cardShape,
                    ambientColor = shadowColor,
                    spotColor = shadowColor,
                )
                .clip(cardShape)
                .testTag("feed_story_${article.id}"),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            color = palette.cardBackgroundStrong,
            border = BorderStroke(cardBorderWidth, cardBorderColor),
        ) {
            if (isCompact) {
                CompactArticleCardContent(
                    article = article,
                    isLiked = displayedLiked,
                    heartGlow = heartGlow.value,
                    onReadStory = onReadStory,
                    onLikeStory = toggleLike,
                    modifier = Modifier.padding(12.dp),
                )
            } else {
                RegularArticleCardContent(
                    article = article,
                    isLiked = displayedLiked,
                    heartGlow = heartGlow.value,
                    onReadStory = onReadStory,
                    onLikeStory = toggleLike,
                    modifier = Modifier.padding(NutsNewsTheme.dimensions.cardPadding),
                )
            }
        }
        if (cardGlow.value > 0f) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .border(
                            width = NutsNewsTheme.borders.glow,
                            color = palette.likedCardGlow.copy(alpha = cardGlow.value),
                            shape = cardShape,
                        )
                        .testTag("article_card_glow"),
            )
        }
        if (animationIsLike && showCelebration) {
            CelebrationBurst(
                progress = celebrationProgress.value,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 22.dp, bottom = 26.dp),
            )
        }
    }
}

@Composable
private fun RegularArticleCardContent(
    article: Article,
    isLiked: Boolean,
    heartGlow: Float,
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
            heartGlow = heartGlow,
            onReadStory = onReadStory,
            onLikeStory = onLikeStory,
        )
    }
}

@Composable
private fun CompactArticleCardContent(
    article: Article,
    isLiked: Boolean,
    heartGlow: Float,
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
                heartGlow = heartGlow,
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
                .heightIn(min = 30.dp)
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
                .nutsNewsHeading()
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
    heartGlow: Float,
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
                glow = heartGlow,
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
                color = NutsNewsTheme.colors.accentText,
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
    val palette = NutsNewsTheme.colors
    val reducedMotion = NutsNewsTheme.reducedMotion
    val glow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isPending by remember { mutableStateOf(false) }
    val trigger: () -> Unit = {
        if (!isPending) {
            if (reducedMotion) {
                onClick()
            } else {
                isPending = true
                scope.launch {
                    glow.snapTo(1f)
                    launch {
                        glow.animateTo(
                            targetValue = 0f,
                            animationSpec =
                                tween(durationMillis = NutsNewsMotion.ActionGlowMillis),
                        )
                    }
                    delay(NutsNewsMotion.ActionOpenDelayMillis)
                    onClick()
                    delay(
                        NutsNewsMotion.ActionGlowResetMillis -
                            NutsNewsMotion.ActionOpenDelayMillis,
                    )
                    glow.snapTo(0f)
                    isPending = false
                }
            }
        }
    }
    val glowProgress = glow.value

    Text(
        text = "Read Story",
        modifier =
            modifier
                .heightIn(min = MinimumTouchTarget)
                .shadow(
                    elevation = (NutsNewsMotion.ActionGlowRadiusDp * glowProgress).dp,
                    shape = CircleShape,
                    ambientColor =
                        palette.accentHighlight.copy(alpha = glowProgress * 0.72f),
                    spotColor = palette.accentGlow.copy(alpha = glowProgress * 0.55f),
                )
                .graphicsLayer {
                    scaleX = 1f + (glowProgress * 0.035f)
                    scaleY = 1f + (glowProgress * 0.035f)
                }
                .clip(CircleShape)
                .background(nutsNewsButtonGradient())
                .border(
                    width = if (glowProgress > 0f) 2.dp else 0.dp,
                    color =
                        palette.accentHighlight.copy(alpha = glowProgress * 0.86f),
                    shape = CircleShape,
                )
                .clickable(
                    role = Role.Button,
                    onClick = trigger,
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
    glow: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val borderColor = if (isLiked) palette.likedCardBorder else palette.cardBorder
    Box(
        modifier =
            modifier
                .size(MinimumTouchTarget)
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
                .border(NutsNewsTheme.borders.hairline, borderColor, CircleShape)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = if (isLiked) "Liked" else "Like story"
                    stateDescription = if (isLiked) "Liked" else "Not liked"
                }
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
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isLiked) palette.likedCardAccent else palette.accentHighlight,
        )
        if (glow > 0f) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .border(
                            width = 2.dp,
                            color = palette.accentHighlight.copy(alpha = glow * 0.86f),
                            shape = CircleShape,
                        )
                        .testTag("article_heart_glow"),
            )
        }
    }
}

@Composable
private fun CelebrationBurst(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(1.dp)
                .testTag("article_celebration"),
    ) {
        CelebrationParticles.forEach { particle ->
            Text(
                text = particle.emoji,
                modifier =
                    Modifier
                        .offset(
                            x = (particle.xOffset * progress).dp,
                            y = (particle.yOffset * progress).dp,
                        )
                        .graphicsLayer {
                            val scale =
                                0.55f + ((particle.endScale - 0.55f) * progress)
                            scaleX = scale
                            scaleY = scale
                            rotationZ = particle.rotation * progress
                            alpha = 1f - progress
                        }
                        .testTag("article_celebration_particle_${particle.id}"),
                fontSize = particle.size.sp,
            )
        }
    }
}

internal fun performLikeHapticIfEnabled(
    enabled: Boolean,
    performer: () -> Boolean,
): Boolean {
    if (!enabled) return false
    return runCatching(performer).getOrDefault(false)
}

private data class CelebrationParticle(
    val id: Int,
    val emoji: String,
    val xOffset: Float,
    val yOffset: Float,
    val rotation: Float,
    val size: Int,
    val endScale: Float,
)

private val CelebrationParticles =
    listOf(
        CelebrationParticle(0, "❤️", -38f, -54f, -16f, 28, 1.20f),
        CelebrationParticle(1, "✨", -88f, -76f, 22f, 28, 1.35f),
        CelebrationParticle(2, "🎉", -136f, -48f, -28f, 30, 1.18f),
        CelebrationParticle(3, "❤️", -184f, -104f, 18f, 26, 1.25f),
        CelebrationParticle(4, "✨", -232f, -138f, -12f, 27, 1.35f),
        CelebrationParticle(5, "🎉", -282f, -84f, 30f, 29, 1.16f),
        CelebrationParticle(6, "❤️", -68f, -152f, 12f, 25, 1.18f),
        CelebrationParticle(7, "✨", -126f, -194f, -22f, 28, 1.32f),
        CelebrationParticle(8, "🎉", -196f, -226f, 24f, 30, 1.18f),
        CelebrationParticle(9, "❤️", -254f, -176f, -18f, 25, 1.25f),
        CelebrationParticle(10, "✨", -326f, -142f, 18f, 28, 1.35f),
        CelebrationParticle(11, "🎉", -362f, -232f, -32f, 30, 1.15f),
        CelebrationParticle(12, "❤️", -96f, -270f, 20f, 25, 1.22f),
        CelebrationParticle(13, "✨", -168f, -316f, -18f, 27, 1.36f),
        CelebrationParticle(14, "🎉", -256f, -304f, 28f, 29, 1.16f),
        CelebrationParticle(15, "❤️", -342f, -342f, -20f, 25, 1.24f),
        CelebrationParticle(16, "✨", -36f, -236f, 20f, 26, 1.33f),
        CelebrationParticle(17, "🎉", -304f, -32f, -24f, 28, 1.18f),
    )

private enum class ThumbnailLoadState {
    Missing,
    Loading,
    Loaded,
    Failed,
}

private const val ThumbnailAspectRatio = 3f / 2f
private const val MaximumVisibleCategories = 6
private val MinimumTouchTarget = 48.dp
