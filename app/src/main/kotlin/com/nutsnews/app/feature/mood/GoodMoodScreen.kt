package com.nutsnews.app.feature.mood

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.article.GoodMood
import com.nutsnews.app.data.article.GoodMoodRecommendationEngine
import com.nutsnews.app.data.article.GoodMoodRecommendations
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsPoliteAnnouncement
import com.nutsnews.app.designsystem.nutsNewsButtonGradient
import java.net.URI

@Composable
fun GoodMoodScreen(
    articles: List<Article>,
    savedStoryIds: Set<StoryId>,
    hapticsEnabled: Boolean,
    onToggleSaved: (Article) -> Unit,
    onSaveHaptic: () -> Boolean,
    onOpenArticle: (Article) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMoodId by rememberSaveable { mutableStateOf(GoodMood.Default.id) }
    val selectedMood = GoodMood.fromId(selectedMoodId)
    val recommendations =
        remember(articles, selectedMood) {
            GoodMoodRecommendationEngine.recommendations(
                articles = articles,
                mood = selectedMood,
            )
        }
    val toggleSaved: (Article) -> Unit = { article ->
        performGoodMoodSaveHaptic(
            enabled = hapticsEnabled,
            performer = onSaveHaptic,
        )
        onToggleSaved(article)
    }

    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("good_mood_screen"),
    ) {
        NutsNewsAdaptivePane {
            GoodMoodContent(
                selectedMood = selectedMood,
                recommendations = recommendations,
                savedStoryIds = savedStoryIds,
                onMoodSelected = { mood -> selectedMoodId = mood.id },
                onToggleSaved = toggleSaved,
                onOpenArticle = onOpenArticle,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun GoodMoodContent(
    selectedMood: GoodMood,
    recommendations: GoodMoodRecommendations,
    savedStoryIds: Set<StoryId>,
    onMoodSelected: (GoodMood) -> Unit,
    onToggleSaved: (Article) -> Unit,
    onOpenArticle: (Article) -> Unit,
    onClose: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 142.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("good_mood_list"),
        contentPadding = PaddingValues(NutsNewsTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        item(
            key = "header",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            GoodMoodHeader(onClose)
        }
        items(
            items = GoodMood.entries,
            key = GoodMood::id,
        ) { mood ->
            GoodMoodChoice(
                mood = mood,
                isSelected = mood == selectedMood,
                onSelected = { onMoodSelected(mood) },
            )
        }
        recommendations.featuredArticle?.let { featuredArticle ->
            item(
                key = "featured-title",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                GoodMoodSectionTitle("Best match")
            }
            item(
                key = "featured-${featuredArticle.stableId.value}",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                GoodMoodFeaturedCard(
                    article = featuredArticle,
                    selectedMood = selectedMood,
                    isSaved = featuredArticle.stableId in savedStoryIds,
                    onOpen = { onOpenArticle(featuredArticle) },
                    onToggleSaved = { onToggleSaved(featuredArticle) },
                )
            }
        } ?: item(
            key = "empty",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            GoodMoodEmptyState()
        }
        if (recommendations.remainingArticles.isNotEmpty()) {
            item(
                key = "remaining-title",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                GoodMoodSectionTitle("More ${selectedMood.title.lowercase()} stories")
            }
            items(
                items = recommendations.remainingArticles,
                key = { article -> article.stableId.value },
                span = { GridItemSpan(maxLineSpan) },
            ) { article ->
                GoodMoodResultRow(
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
private fun GoodMoodHeader(onClose: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = NutsNewsTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        ) {
            Text(
                text = "Good Mood",
                modifier = Modifier.nutsNewsHeading(),
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
                    "Pick the feeling you want and NutsNews will match " +
                        "a calm story for you.",
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.subheadline,
            )
        }
        Surface(
            modifier =
                Modifier
                    .size(48.dp)
                    .testTag("good_mood_close"),
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
                    contentDescription = "Close Good Mood",
                    modifier = Modifier.size(16.dp),
                    tint = NutsNewsTheme.colors.accentHighlight,
                )
            }
        }
    }
}

@Composable
private fun GoodMoodChoice(
    mood: GoodMood,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    val palette = NutsNewsTheme.colors
    val shape = RoundedCornerShape(NutsNewsTheme.radii.medium)
    val scale by
        animateFloatAsState(
            targetValue =
                if (isSelected && !NutsNewsTheme.reducedMotion) {
                    1.02f
                } else {
                    1f
                },
            animationSpec =
                if (NutsNewsTheme.reducedMotion) {
                    snap()
                } else {
                    spring(dampingRatio = 0.84f, stiffness = 480f)
                },
            label = "${mood.title} mood selection",
        )
    val foreground = if (isSelected) palette.buttonText else palette.primaryText
    val subtitle =
        if (isSelected) {
            palette.buttonText
        } else {
            palette.secondaryText
        }
    val backgroundModifier =
        if (isSelected) {
            Modifier.background(nutsNewsButtonGradient())
        } else {
            Modifier.background(palette.cardBackgroundStrong)
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(scale)
                .shadow(
                    elevation = if (isSelected) 18.dp else 0.dp,
                    shape = shape,
                    ambientColor = palette.accentGlow,
                    spotColor = palette.accentGlow,
                ).clip(shape)
                .then(backgroundModifier)
                .border(
                    width = NutsNewsTheme.borders.hairline,
                    color = if (isSelected) Color.Transparent else palette.cardBorder,
                    shape = shape,
                ).selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onSelected,
                ).testTag("good_mood_choice_${mood.id}")
                .padding(NutsNewsTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = mood.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = foreground,
            )
            Text(
                text = mood.title,
                color = foreground,
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Text(
            text = mood.subtitle,
            modifier = Modifier.heightIn(min = 38.dp),
            color = subtitle,
            style = NutsNewsTheme.typography.caption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GoodMoodSectionTitle(title: String) {
    Text(
        text = title,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = NutsNewsTheme.spacing.xs)
                .nutsNewsHeading(),
        color = NutsNewsTheme.colors.primaryText,
        style = NutsNewsTheme.typography.subheadline,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun GoodMoodFeaturedCard(
    article: Article,
    selectedMood: GoodMood,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    val palette = NutsNewsTheme.colors
    val shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("good_mood_featured_${article.stableId.value}"),
        shape = shape,
        color = palette.cardBackgroundStrong,
        border = BorderStroke(NutsNewsTheme.borders.hairline, palette.cardBorder),
    ) {
        Column {
            GoodMoodFeaturedThumbnail(article)
            Column(
                modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = selectedMood.icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = palette.accentHighlight,
                    )
                    Text(
                        text = "${selectedMood.title} pick",
                        modifier = Modifier.testTag("good_mood_featured_label"),
                        color = palette.accentHighlight,
                        style = NutsNewsTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = article.title,
                    color = palette.primaryText,
                    style = NutsNewsTheme.typography.headline,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.summary.isNotBlank()) {
                    Text(
                        text = article.summary,
                        color = palette.secondaryText,
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
                                .heightIn(min = 48.dp)
                                .clip(CircleShape)
                                .background(nutsNewsButtonGradient())
                                .testTag("good_mood_featured_open"),
                        shape = CircleShape,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                            ),
                    ) {
                        Text(
                            text = "Open story",
                            color = palette.buttonText,
                            style = NutsNewsTheme.typography.subheadline,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    GoodMoodSaveButton(
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
private fun GoodMoodFeaturedThumbnail(article: Article) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(NutsNewsTheme.colors.badgeBackground)
                .testTag("good_mood_featured_thumbnail"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        article.thumbnailUrl?.let { thumbnailUrl ->
            AsyncImage(
                model = thumbnailUrl.toString(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun GoodMoodResultRow(
    article: Article,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("good_mood_result_${article.stableId.value}"),
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
            GoodMoodResultThumbnail(article)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
            ) {
                Text(
                    text = article.source,
                    modifier =
                        Modifier.testTag(
                            "good_mood_result_source_${article.stableId.value}",
                        ),
                    color = NutsNewsTheme.colors.accentText,
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
                    GoodMoodSaveButton(
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
private fun GoodMoodResultThumbnail(article: Article) {
    val shape = RoundedCornerShape(NutsNewsTheme.radii.small)
    Box(
        modifier =
            Modifier
                .size(width = 96.dp, height = 72.dp)
                .clip(shape)
                .background(NutsNewsTheme.colors.badgeBackground)
                .testTag("good_mood_result_thumbnail_${article.stableId.value}"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Newspaper,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        article.thumbnailUrl?.let { thumbnailUrl ->
            AsyncImage(
                model = thumbnailUrl.toString(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun GoodMoodSaveButton(
    article: Article,
    isSaved: Boolean,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onToggleSaved: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .size(size.coerceAtLeast(48.dp))
                .semantics {
                    stateDescription = if (isSaved) "Saved" else "Not saved"
                }
                .testTag("good_mood_save_${article.stableId.value}"),
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
private fun GoodMoodEmptyState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = NutsNewsTheme.spacing.xl)
                .nutsNewsPoliteAnnouncement()
                .testTag("good_mood_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Text(
            text = "No mood matches yet",
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
        )
        Text(
            text =
                "Load a few stories on the home screen, then come back " +
                    "for a personalized pick.",
            modifier = Modifier.padding(horizontal = NutsNewsTheme.spacing.large),
            color = NutsNewsTheme.colors.secondaryText,
            style = NutsNewsTheme.typography.subheadline,
            textAlign = TextAlign.Center,
        )
    }
}

internal fun performGoodMoodSaveHaptic(
    enabled: Boolean,
    performer: () -> Boolean,
): Boolean {
    if (!enabled) return false
    return runCatching(performer).getOrDefault(false)
}

private val GoodMood.icon: ImageVector
    get() =
        when (this) {
            GoodMood.Calm -> Icons.Filled.Eco
            GoodMood.Hopeful -> Icons.Filled.Favorite
            GoodMood.Inspired -> Icons.Filled.Star
            GoodMood.Curious -> Icons.Filled.Search
        }

@Preview(showBackground = true)
@Composable
private fun GoodMoodPreview() {
    val articles =
        listOf(
            moodPreviewArticle(
                id = "hope",
                title = "Neighbors bring hope to a recovering community",
                categories = listOf("Community"),
            ),
            moodPreviewArticle(
                id = "kind",
                title = "Volunteers help a rescue team reunite a family",
                categories = listOf("Uplifting"),
            ),
        )
    NutsNewsTheme(updateSystemBars = false) {
        GoodMoodScreen(
            articles = articles,
            savedStoryIds = emptySet(),
            hapticsEnabled = true,
            onToggleSaved = {},
            onSaveHaptic = { true },
            onOpenArticle = {},
            onClose = {},
        )
    }
}

private fun moodPreviewArticle(
    id: String,
    title: String,
    categories: List<String>,
): Article =
    Article(
        id = id,
        title = title,
        summary = "A bright local update gives everyone a reason to feel good.",
        originalUrl = URI("https://example.com/$id"),
        source = "Good News Daily",
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = URI("https://example.com/$id.jpg"),
        categories = categories,
    )
