package com.nutsnews.app.feature.article

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryReflection
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsButtonGradient
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.delay

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
    noteDraft: String = "",
    hasSavedNote: Boolean = false,
    isNoteLoading: Boolean = false,
    noteStatusMessage: String? = null,
    onNoteDraftChanged: (String) -> Unit = {},
    onSaveNote: () -> Unit = {},
    onClearNote: () -> Unit = {},
    reflection: StoryReflection? = null,
    isReflectionLoading: Boolean = false,
    reflectionStatusMessage: String? = null,
    onReflectionSelected: (StoryReflectionReaction) -> Unit = {},
    listenUiState: ArticleListenUiState = ArticleListenUiState(),
    onToggleListening: (ArticleListenScript) -> Unit = {},
    onStopListening: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isTabletLandscape =
        configuration.smallestScreenWidthDp >= TabletMinimumWidthDp &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val palette = NutsNewsTheme.colors
    val brief = remember(article) { deriveArticleBrief(article) }
    val listenScript = remember(article, brief) { buildArticleListenScript(article, brief) }
    var isShowingListenMode by
        remember(article.stableId.value) {
            mutableStateOf(false)
        }
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
    LaunchedEffect(isShowingListenMode) {
        if (
            isShowingListenMode &&
            listenUiState.playbackState != ArticleListenPlaybackState.Reading &&
            listenUiState.playbackState != ArticleListenPlaybackState.Paused
        ) {
            delay(ListenAutoStartDelayMillis)
            onToggleListening(listenScript)
        }
    }
    DisposableEffect(article.stableId) {
        onDispose {
            onStopListening()
        }
    }

    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
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
                            onClick = {
                                onStopListening()
                                onClose()
                            },
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
                        ArticleListenToolbarButton(
                            isActive = listenUiState.isActive,
                            onClick = { isShowingListenMode = true },
                        )
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
                    noteDraft = noteDraft,
                    hasSavedNote = hasSavedNote,
                    isNoteLoading = isNoteLoading,
                    noteStatusMessage = noteStatusMessage,
                    onNoteDraftChanged = onNoteDraftChanged,
                    onSaveNote = onSaveNote,
                    onClearNote = onClearNote,
                    reflection = reflection,
                    isReflectionLoading = isReflectionLoading,
                    reflectionStatusMessage = reflectionStatusMessage,
                    onReflectionSelected = onReflectionSelected,
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
                    noteDraft = noteDraft,
                    hasSavedNote = hasSavedNote,
                    isNoteLoading = isNoteLoading,
                    noteStatusMessage = noteStatusMessage,
                    onNoteDraftChanged = onNoteDraftChanged,
                    onSaveNote = onSaveNote,
                    onClearNote = onClearNote,
                    reflection = reflection,
                    isReflectionLoading = isReflectionLoading,
                    reflectionStatusMessage = reflectionStatusMessage,
                    onReflectionSelected = onReflectionSelected,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                )
            }
        }
    }

    if (isShowingListenMode) {
        ArticleListenModeSheet(
            brief = brief,
            script = listenScript,
            uiState = listenUiState,
            onToggle = { onToggleListening(listenScript) },
            onStop = onStopListening,
            onDismiss = {
                onStopListening()
                isShowingListenMode = false
            },
        )
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

@Composable
private fun ArticleListenToolbarButton(
    isActive: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag("article_detail_listen"),
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NutsNewsTheme.colors.badgeBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
                contentDescription =
                    if (isActive) {
                        "Open Listen Mode"
                    } else {
                        "Listen to story brief"
                    },
                modifier = Modifier.size(17.dp),
                tint =
                    if (isActive) {
                        NutsNewsTheme.colors.accent
                    } else {
                        NutsNewsTheme.colors.accentHighlight
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleListenModeSheet(
    brief: ArticleBriefContent,
    script: ArticleListenScript,
    uiState: ArticleListenUiState,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("listen_mode_sheet"),
        containerColor = NutsNewsTheme.colors.cardBackgroundStrong,
        contentColor = NutsNewsTheme.colors.primaryText,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = NutsNewsTheme.spacing.medium,
                        end = NutsNewsTheme.spacing.medium,
                        bottom = 32.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Listen Mode",
                    modifier = Modifier.weight(1f),
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.title3,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Done",
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = onDismiss,
                            ).testTag("listen_mode_done")
                            .padding(
                                horizontal = NutsNewsTheme.spacing.small,
                                vertical = NutsNewsTheme.spacing.xs,
                            ),
                    color = NutsNewsTheme.colors.accent,
                    style = NutsNewsTheme.typography.subheadline,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DetailInfoCard(
                label = "Audio Brief",
                compact = false,
                modifier = Modifier.testTag("listen_mode_hero"),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(58.dp)
                                .shadow(
                                    elevation =
                                        if (
                                            uiState.playbackState ==
                                            ArticleListenPlaybackState.Reading
                                        ) {
                                            18.dp
                                        } else {
                                            8.dp
                                        },
                                    shape = CircleShape,
                                    ambientColor = NutsNewsTheme.colors.accentGlow,
                                    spotColor = NutsNewsTheme.colors.accentGlow,
                                )
                                .clip(CircleShape)
                                .background(nutsNewsButtonGradient()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                when (uiState.playbackState) {
                                    ArticleListenPlaybackState.Reading ->
                                        Icons.Filled.VolumeUp

                                    ArticleListenPlaybackState.Paused ->
                                        Icons.Filled.Pause

                                    ArticleListenPlaybackState.Failed ->
                                        Icons.Filled.Headphones

                                    ArticleListenPlaybackState.Idle ->
                                        Icons.Filled.GraphicEq
                                },
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = NutsNewsTheme.colors.buttonText,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
                    ) {
                        Text(
                            text =
                                when (uiState.playbackState) {
                                    ArticleListenPlaybackState.Reading ->
                                        "Playing your audio brief"

                                    ArticleListenPlaybackState.Paused ->
                                        "Audio brief paused"

                                    ArticleListenPlaybackState.Failed ->
                                        "Listen Mode needs attention"

                                    ArticleListenPlaybackState.Idle ->
                                        "Listen to this story"
                                },
                            color = NutsNewsTheme.colors.primaryText,
                            style = NutsNewsTheme.typography.title3,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text =
                                "A calm spoken version of the NutsNews Brief, read with " +
                                    "on-device Android speech and natural pauses.",
                            color = NutsNewsTheme.colors.secondaryText,
                            style = NutsNewsTheme.typography.subheadline,
                        )
                        Text(
                            text = uiState.statusMessage,
                            modifier = Modifier.testTag("listen_mode_status"),
                            color = NutsNewsTheme.colors.accent,
                            style = NutsNewsTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            DetailInfoCard(
                label = "Now Playing",
                compact = false,
                modifier = Modifier.testTag("listen_mode_now_playing"),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                ) {
                    ListenWaveform(
                        uiState = uiState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .clip(
                                    RoundedCornerShape(
                                        NutsNewsTheme.dimensions.controlCornerRadius,
                                    ),
                                ).background(NutsNewsTheme.colors.badgeBackground)
                                .clickable(
                                    role = Role.Button,
                                    onClick = onToggle,
                                ).testTag("listen_mode_waveform")
                                .padding(vertical = NutsNewsTheme.spacing.xs),
                    )
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector =
                                when (uiState.playbackState) {
                                    ArticleListenPlaybackState.Reading ->
                                        Icons.Filled.GraphicEq

                                    ArticleListenPlaybackState.Paused ->
                                        Icons.Filled.Pause

                                    else -> Icons.Filled.VolumeUp
                                },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = NutsNewsTheme.colors.mutedText,
                        )
                        Text(
                            text =
                                when (uiState.playbackState) {
                                    ArticleListenPlaybackState.Reading ->
                                        "Tap waves to pause"

                                    ArticleListenPlaybackState.Paused ->
                                        "Paused — tap waves to resume"

                                    else -> uiState.shortStatusMessage
                                },
                            modifier = Modifier.weight(1f),
                            color = NutsNewsTheme.colors.mutedText,
                            style = NutsNewsTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = brief.estimatedReadTime,
                            color = NutsNewsTheme.colors.mutedText,
                            style = NutsNewsTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                ListenControlButton(
                    label = uiState.primaryButtonTitle,
                    primary = true,
                    modifier = Modifier.weight(1f),
                    icon =
                        if (
                            uiState.playbackState ==
                            ArticleListenPlaybackState.Reading
                        ) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                    onClick = onToggle,
                )
                if (uiState.isActive) {
                    ListenControlButton(
                        label = "Stop",
                        primary = false,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Stop,
                        onClick = onStop,
                    )
                }
            }
            DetailInfoCard(
                label = "What you’ll hear",
                compact = false,
                modifier = Modifier.testTag("listen_mode_preview"),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                ) {
                    script.segments.forEachIndexed { index, segment ->
                        val isCurrent = uiState.currentSegmentIndex == index
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(
                                            NutsNewsTheme.dimensions.controlCornerRadius,
                                        ),
                                    ).background(
                                        if (isCurrent) {
                                            NutsNewsTheme.colors.badgeBackground
                                        } else {
                                            androidx.compose.ui.graphics.Color.Transparent
                                        },
                                    ).semantics {
                                        selected = isCurrent
                                    }.testTag("listen_mode_segment_${segment.id}")
                                    .padding(NutsNewsTheme.spacing.small),
                            horizontalArrangement =
                                Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .padding(top = 7.dp)
                                        .size(NutsNewsTheme.spacing.xs)
                                        .background(
                                            color =
                                                if (isCurrent) {
                                                    NutsNewsTheme.colors.accentHighlight
                                                } else {
                                                    NutsNewsTheme.colors.accent
                                                },
                                            shape = CircleShape,
                                        ),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement =
                                    Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
                            ) {
                                Text(
                                    text = segment.label,
                                    color =
                                        if (isCurrent) {
                                            NutsNewsTheme.colors.accent
                                        } else {
                                            NutsNewsTheme.colors.primaryText
                                        },
                                    style = NutsNewsTheme.typography.subheadline,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = segment.text,
                                    color = NutsNewsTheme.colors.secondaryText,
                                    style = NutsNewsTheme.typography.subheadline,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListenControlButton(
    label: String,
    primary: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius)
    Row(
        modifier =
            modifier
                .clip(shape)
                .then(
                    if (primary) {
                        Modifier.background(nutsNewsButtonGradient())
                    } else {
                        Modifier.background(NutsNewsTheme.colors.badgeBackground)
                    },
                ).clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).testTag(
                    if (primary) {
                        "listen_mode_primary"
                    } else {
                        "listen_mode_stop"
                    },
                ).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint =
                if (primary) {
                    NutsNewsTheme.colors.buttonText
                } else {
                    NutsNewsTheme.colors.primaryText
                },
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = NutsNewsTheme.spacing.xs),
            color =
                if (primary) {
                    NutsNewsTheme.colors.buttonText
                } else {
                    NutsNewsTheme.colors.primaryText
                },
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ListenWaveform(
    uiState: ArticleListenUiState,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "listen waveform")
    val animatedPhase by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = WaveTwoPi,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "listen waveform phase",
        )
    val isReading = uiState.playbackState == ArticleListenPlaybackState.Reading
    val isPaused = uiState.playbackState == ArticleListenPlaybackState.Paused
    val phase = if (isReading) animatedPhase else uiState.speechWaveSeed.toFloat()
    val level =
        when {
            isReading -> uiState.speechWaveLevel
            isPaused -> 0.22f
            else -> 0.18f
        }
    Canvas(modifier = modifier) {
        val barCount = 22
        val horizontalPadding = size.width * 0.08f
        val usableWidth = size.width - (horizontalPadding * 2)
        val step = usableWidth / (barCount - 1)
        val strokeWidth = (step * 0.34f).coerceAtLeast(3f)
        repeat(barCount) { index ->
            val texture =
                (
                    (
                        sin(
                            (index * uiState.speechWaveFrequency + phase).toDouble(),
                        ).toFloat() + 1f
                    ) / 2f
                )
                    .coerceIn(0f, 1f)
            val envelope = 1f - kotlin.math.abs((index - (barCount - 1) / 2f)) / barCount
            val barHeight =
                size.height *
                    (0.16f + (texture * level * 0.66f * envelope))
            val x = horizontalPadding + (index * step)
            drawLine(
                color =
                    if (index % 3 == 0) {
                        palette.accentHighlight
                    } else {
                        palette.accent
                    },
                start = Offset(x, (size.height - barHeight) / 2f),
                end = Offset(x, (size.height + barHeight) / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
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
    noteDraft: String,
    hasSavedNote: Boolean,
    isNoteLoading: Boolean,
    noteStatusMessage: String?,
    onNoteDraftChanged: (String) -> Unit,
    onSaveNote: () -> Unit,
    onClearNote: () -> Unit,
    reflection: StoryReflection?,
    isReflectionLoading: Boolean,
    reflectionStatusMessage: String?,
    onReflectionSelected: (StoryReflectionReaction) -> Unit,
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
        RegularArticleReflection(
            reflection = reflection,
            isLoading = isReflectionLoading,
            statusMessage = reflectionStatusMessage,
            onSelected = onReflectionSelected,
        )
        ArticleDetailSummary(
            summary = article.summary,
            compact = false,
        )
        ArticleNoteSection(
            draft = noteDraft,
            hasSavedNote = hasSavedNote,
            isLoading = isNoteLoading,
            statusMessage = noteStatusMessage,
            compact = false,
            onDraftChanged = onNoteDraftChanged,
            onSave = onSaveNote,
            onClear = onClearNote,
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
    noteDraft: String,
    hasSavedNote: Boolean,
    isNoteLoading: Boolean,
    noteStatusMessage: String?,
    onNoteDraftChanged: (String) -> Unit,
    onSaveNote: () -> Unit,
    onClearNote: () -> Unit,
    reflection: StoryReflection?,
    isReflectionLoading: Boolean,
    reflectionStatusMessage: String?,
    onReflectionSelected: (StoryReflectionReaction) -> Unit,
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
                CompactArticleReflection(
                    selectedReaction = reflection?.reaction,
                    isLoading = isReflectionLoading,
                    onSelected = onReflectionSelected,
                )
                ArticleDetailSummary(
                    summary = article.summary,
                    compact = true,
                )
                ArticleNoteSection(
                    draft = noteDraft,
                    hasSavedNote = hasSavedNote,
                    isLoading = isNoteLoading,
                    statusMessage = noteStatusMessage,
                    compact = true,
                    onDraftChanged = onNoteDraftChanged,
                    onSave = onSaveNote,
                    onClear = onClearNote,
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
private fun RegularArticleReflection(
    reflection: StoryReflection?,
    isLoading: Boolean,
    statusMessage: String?,
    onSelected: (StoryReflectionReaction) -> Unit,
) {
    val selectedReaction = reflection?.reaction
    val title = selectedReaction?.savedTitle ?: "How did this story land?"
    val subtitle =
        if (reflection != null) {
            "You marked this story as ${
                reflection.reaction.title.lowercase(Locale.ROOT)
            } on ${reflectionDisplayDate(reflection)}."
        } else {
            "Tap a quick reaction to make this story part of your private " +
                "good-news habit. Saved only on this device."
        }
    val status =
        statusMessage
            ?: if (selectedReaction != null) {
                "Reflection saved privately on this device"
            } else {
                "No account needed — this stays on your Android device"
            }

    DetailInfoCard(
        label = "Daily Reflection",
        compact = false,
        modifier = Modifier.testTag("article_detail_reflection"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(NutsNewsTheme.colors.badgeBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = reflectionIcon(selectedReaction),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = NutsNewsTheme.colors.accentHighlight,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.testTag("article_detail_reflection_title"),
                        color = NutsNewsTheme.colors.primaryText,
                        style = NutsNewsTheme.typography.headline,
                    )
                    Text(
                        text = subtitle,
                        modifier = Modifier.testTag("article_detail_reflection_subtitle"),
                        color = NutsNewsTheme.colors.secondaryText,
                        style = NutsNewsTheme.typography.subheadline,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                StoryReflectionReaction.entries.forEach { reaction ->
                    ReflectionChoice(
                        reaction = reaction,
                        selected = reaction == selectedReaction,
                        enabled = !isLoading,
                        compact = false,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelected(reaction) },
                    )
                }
            }
            Text(
                text = status,
                modifier = Modifier.testTag("article_detail_reflection_status"),
                color = NutsNewsTheme.colors.mutedText,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CompactArticleReflection(
    selectedReaction: StoryReflectionReaction?,
    isLoading: Boolean,
    onSelected: (StoryReflectionReaction) -> Unit,
) {
    DetailInfoCard(
        label = "Reflection",
        compact = true,
        modifier = Modifier.testTag("article_detail_reflection_compact"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoryReflectionReaction.entries.forEach { reaction ->
                ReflectionChoice(
                    reaction = reaction,
                    selected = reaction == selectedReaction,
                    enabled = !isLoading,
                    compact = true,
                    onClick = { onSelected(reaction) },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReflectionChoice(
    reaction: StoryReflectionReaction,
    selected: Boolean,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape =
        if (compact) {
            CircleShape
        } else {
            RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius)
        }
    val textColor =
        if (selected) {
            NutsNewsTheme.colors.buttonText
        } else {
            NutsNewsTheme.colors.primaryText
        }
    val selectionModifier =
        if (selected) {
            Modifier.background(nutsNewsButtonGradient())
        } else {
            Modifier.background(NutsNewsTheme.colors.badgeBackground)
        }
    val contentModifier =
        if (compact) {
            Modifier.padding(
                horizontal = NutsNewsTheme.spacing.small,
                vertical = 8.dp,
            )
        } else {
            Modifier
                .fillMaxWidth()
                .height(74.dp)
                .padding(horizontal = NutsNewsTheme.spacing.xs)
        }

    if (compact) {
        Row(
            modifier =
                modifier
                    .graphicsLayer { alpha = if (enabled) 1f else 0.55f }
                    .clip(shape)
                    .then(selectionModifier)
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = onClick,
                    )
                    .testTag("article_detail_reflection_${reaction.id}")
                    .then(contentModifier),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReflectionChoiceContent(
                reaction = reaction,
                compact = true,
                textColor = textColor,
            )
        }
    } else {
        Column(
            modifier =
                modifier
                    .graphicsLayer { alpha = if (enabled) 1f else 0.55f }
                    .clip(shape)
                    .then(selectionModifier)
                    .border(
                        width = 1.dp,
                        color =
                            if (selected) {
                                NutsNewsTheme.colors.accentHighlight.copy(alpha = 0.85f)
                            } else {
                                NutsNewsTheme.colors.cardBorder
                            },
                        shape = shape,
                    )
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = onClick,
                    )
                    .testTag("article_detail_reflection_${reaction.id}")
                    .then(contentModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ReflectionChoiceContent(
                reaction = reaction,
                compact = false,
                textColor = textColor,
            )
        }
    }
}

@Composable
private fun ReflectionChoiceContent(
    reaction: StoryReflectionReaction,
    compact: Boolean,
    textColor: androidx.compose.ui.graphics.Color,
) {
    Icon(
        imageVector = reflectionIcon(reaction),
        contentDescription = null,
        modifier = Modifier.size(if (compact) 14.dp else 17.dp),
        tint = textColor,
    )
    Text(
        text = if (compact) reaction.shortTitle else reaction.title,
        modifier = if (compact) Modifier else Modifier.fillMaxWidth(),
        color = textColor,
        style = NutsNewsTheme.typography.caption,
        fontWeight = FontWeight.Bold,
        textAlign = if (compact) TextAlign.Start else TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun reflectionIcon(reaction: StoryReflectionReaction?): ImageVector =
    when (reaction) {
        StoryReflectionReaction.Smile -> Icons.Filled.SentimentSatisfiedAlt
        StoryReflectionReaction.Hope,
        null,
        -> Icons.Filled.AutoAwesome

        StoryReflectionReaction.Revisit -> Icons.Filled.Bookmark
    }

internal fun reflectionDisplayDate(
    reflection: StoryReflection,
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String =
    DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(reflection.createdAt.atZone(zoneId))

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
private fun ArticleNoteSection(
    draft: String,
    hasSavedNote: Boolean,
    isLoading: Boolean,
    statusMessage: String?,
    compact: Boolean,
    onDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val canClear = !isLoading && (draft.isNotBlank() || hasSavedNote)
    var displayedStatus by remember { mutableStateOf(statusMessage.orEmpty()) }
    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrEmpty()) displayedStatus = statusMessage
    }

    DetailInfoCard(
        label = "My Note",
        compact = compact,
        modifier = Modifier.testTag("article_detail_note"),
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    if (compact) {
                        NutsNewsTheme.spacing.xs
                    } else {
                        NutsNewsTheme.spacing.small
                    },
                ),
        ) {
            if (!compact) {
                Text(
                    text =
                        "Save a private thought, reminder, or reason this story " +
                            "made you smile.",
                    color = NutsNewsTheme.colors.mutedText,
                    style = NutsNewsTheme.typography.subheadline,
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(if (compact) 66.dp else 96.dp)
                        .clip(
                            RoundedCornerShape(
                                NutsNewsTheme.dimensions.controlCornerRadius,
                            ),
                        )
                        .background(NutsNewsTheme.colors.badgeBackground)
                        .testTag("article_detail_note_editor_frame"),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChanged,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                if (compact) {
                                    NutsNewsTheme.spacing.xs
                                } else {
                                    NutsNewsTheme.spacing.small
                                },
                            )
                            .testTag("article_detail_note_editor"),
                    enabled = !isLoading,
                    textStyle =
                        (
                            if (compact) {
                                NutsNewsTheme.typography.subheadline
                            } else {
                                NutsNewsTheme.typography.body
                            }
                        ).copy(color = NutsNewsTheme.colors.primaryText),
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    cursorBrush = SolidColor(NutsNewsTheme.colors.accent),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        if (compact) {
                            NutsNewsTheme.spacing.xs
                        } else {
                            NutsNewsTheme.spacing.small
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoteActionButton(
                    label = if (compact) "Save" else "Save note",
                    primary = true,
                    enabled = !isLoading,
                    compact = compact,
                    modifier = if (compact) Modifier else Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                        )
                    },
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSave()
                    },
                )
                NoteActionButton(
                    label = "Clear",
                    primary = false,
                    enabled = canClear,
                    compact = compact,
                    modifier = if (compact) Modifier else Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                        )
                    },
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onClear()
                    },
                )
                if (compact) Spacer(modifier = Modifier.weight(1f))
            }
            AnimatedVisibility(
                visible = !statusMessage.isNullOrEmpty(),
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 },
                exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 2 },
            ) {
                Text(
                    text = displayedStatus,
                    modifier = Modifier.testTag("article_detail_note_status"),
                    color = NutsNewsTheme.colors.accent,
                    style = NutsNewsTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun NoteActionButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius)
    Row(
        modifier =
            modifier
                .graphicsLayer { alpha = if (enabled) 1f else 0.55f }
                .clip(shape)
                .then(
                    if (primary) {
                        Modifier.background(nutsNewsButtonGradient())
                    } else {
                        Modifier.background(NutsNewsTheme.colors.badgeBackground)
                    },
                )
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .testTag(
                    if (primary) {
                        "article_detail_note_save"
                    } else {
                        "article_detail_note_clear"
                    },
                )
                .padding(
                    horizontal =
                        if (compact) {
                            NutsNewsTheme.spacing.small
                        } else {
                            NutsNewsTheme.spacing.medium
                        },
                    vertical = if (compact) 7.dp else 11.dp,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            text = label,
            modifier = Modifier.padding(start = NutsNewsTheme.spacing.xs),
            color =
                if (primary) {
                    NutsNewsTheme.colors.buttonText
                } else {
                    NutsNewsTheme.colors.primaryText
                },
            style =
                if (compact) {
                    NutsNewsTheme.typography.caption
                } else {
                    NutsNewsTheme.typography.subheadline
                },
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
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
private const val ListenAutoStartDelayMillis = 180L
private const val WaveTwoPi = 6.2831855f
