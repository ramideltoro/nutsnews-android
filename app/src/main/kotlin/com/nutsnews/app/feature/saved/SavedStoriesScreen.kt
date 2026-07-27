package com.nutsnews.app.feature.saved

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsPoliteAnnouncement
import com.nutsnews.app.designsystem.nutsNewsButtonGradient
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SavedStoriesScreen(
    uiState: SavedStoriesUiState,
    onQueryChanged: (String) -> Unit,
    onOpenStory: (SavedStory) -> Unit,
    onRemoveStory: (SavedStory) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("saved_stories_screen"),
    ) {
        NutsNewsAdaptivePane {
            Column(modifier = Modifier.fillMaxSize()) {
                SavedStoriesTopBar(onClose = onClose)
                when {
                    uiState.isLoading -> SavedStoriesLoading()
                    uiState.stories.isEmpty() -> EmptySavedStories()
                    else ->
                        SavedStoriesLibrary(
                            uiState = uiState,
                            onQueryChanged = onQueryChanged,
                            onOpenStory = onOpenStory,
                            onRemoveStory = onRemoveStory,
                        )
                }
            }
        }
    }
}

@Composable
private fun SavedStoriesTopBar(onClose: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = NutsNewsTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(64.dp))
        Text(
            text = "Saved Stories",
            modifier =
                Modifier
                    .weight(1f)
                    .nutsNewsHeading(),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onClose,
            modifier =
                Modifier
                    .width(64.dp)
                    .testTag("saved_stories_close"),
            contentPadding = PaddingValues(0.dp),
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = NutsNewsTheme.colors.accent,
                ),
        ) {
            Text(
                text = "Done",
                style = NutsNewsTheme.typography.callout,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SavedStoriesLoading() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .nutsNewsPoliteAnnouncement()
                .testTag("saved_stories_loading"),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = NutsNewsTheme.colors.accent)
    }
}

@Composable
private fun EmptySavedStories() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(NutsNewsTheme.spacing.medium)
                .nutsNewsPoliteAnnouncement()
                .testTag("saved_stories_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Text(
            text = "No saved stories yet",
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.medium),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.title3,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text =
                "Tap the heart on any story to build your own calm, " +
                    "positive reading list.",
            modifier =
                Modifier.padding(
                    top = NutsNewsTheme.spacing.small,
                    start = NutsNewsTheme.spacing.large,
                    end = NutsNewsTheme.spacing.large,
                ),
            color = NutsNewsTheme.colors.secondaryText,
            style = NutsNewsTheme.typography.subheadline,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SavedStoriesLibrary(
    uiState: SavedStoriesUiState,
    onQueryChanged: (String) -> Unit,
    onOpenStory: (SavedStory) -> Unit,
    onRemoveStory: (SavedStory) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .animateContentSize()
                .testTag("saved_stories_list"),
        contentPadding =
            PaddingValues(
                start = NutsNewsTheme.spacing.medium,
                top = NutsNewsTheme.spacing.medium,
                end = NutsNewsTheme.spacing.medium,
                bottom = NutsNewsTheme.spacing.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        item(key = "search") {
            SavedStoriesSearch(
                query = uiState.query,
                onQueryChanged = onQueryChanged,
            )
        }
        item(key = "stats") {
            SavedStoriesStats(savedCount = uiState.savedCount)
        }
        if (uiState.filteredStories.isEmpty()) {
            item(key = "empty-search") {
                EmptySavedStoriesSearch()
            }
        } else {
            items(
                items = uiState.filteredStories,
                key = { story -> story.id.value },
            ) { story ->
                SavedStoryRow(
                    story = story,
                    onOpenStory = onOpenStory,
                    onRemoveStory = onRemoveStory,
                )
            }
        }
    }
}

@Composable
private fun SavedStoriesSearch(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    val palette = NutsNewsTheme.colors
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("saved_stories_search"),
        placeholder = {
            Text(
                text = "Search saved stories",
                color = palette.mutedText,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = palette.accent,
            )
        },
        trailingIcon =
            if (query.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onQueryChanged("") },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear saved story search",
                            tint = palette.secondaryText,
                        )
                    }
                }
            } else {
                null
            },
        singleLine = true,
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius),
        textStyle =
            NutsNewsTheme.typography.body.copy(
                color = palette.primaryText,
            ),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.cardBorder,
                focusedContainerColor = palette.cardBackgroundStrong,
                unfocusedContainerColor = palette.cardBackgroundStrong,
                cursorColor = palette.accent,
            ),
    )
}

@Composable
private fun SavedStoriesStats(savedCount: Int) {
    val palette = NutsNewsTheme.colors
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("saved_stories_stats"),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = palette.cardBackgroundStrong,
        border = BorderStroke(NutsNewsTheme.borders.hairline, palette.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(palette.badgeBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = palette.accentHighlight,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            ) {
                Text(
                    text = "Your good-news library",
                    color = palette.primaryText,
                    style = NutsNewsTheme.typography.headline,
                )
                Text(
                    text =
                        if (savedCount == 1) {
                            "1 story saved on this device"
                        } else {
                            "$savedCount stories saved on this device"
                        },
                    color = palette.secondaryText,
                    style = NutsNewsTheme.typography.subheadline,
                )
            }
        }
    }
}

@Composable
private fun EmptySavedStoriesSearch() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = NutsNewsTheme.spacing.large,
                    vertical = NutsNewsTheme.spacing.xl,
                ).nutsNewsPoliteAnnouncement()
                .testTag("saved_stories_empty_search"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Text(
            text = "No saved stories found",
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.title3,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Try searching by title, summary, source, or category.",
            color = NutsNewsTheme.colors.secondaryText,
            style = NutsNewsTheme.typography.subheadline,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SavedStoryRow(
    story: SavedStory,
    onOpenStory: (SavedStory) -> Unit,
    onRemoveStory: (SavedStory) -> Unit,
) {
    val palette = NutsNewsTheme.colors
    val cardShape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = NutsNewsTheme.shadows.cardBlurRadius,
                    shape = cardShape,
                    ambientColor = palette.accentGlow,
                    spotColor = palette.accentGlow,
                ).testTag("saved_story_${story.id.value}"),
        shape = cardShape,
        color = palette.cardBackgroundStrong,
        border = BorderStroke(NutsNewsTheme.borders.hairline, palette.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        ) {
            SavedStoryThumbnail(story)
            SavedStoryCategories(story.article.categories)
            Text(
                text = story.article.title,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .nutsNewsHeading()
                        .testTag("saved_story_title"),
                color = palette.primaryText,
                style = NutsNewsTheme.typography.cardTitle,
            )
            if (story.article.summary.isNotBlank()) {
                Text(
                    text = story.article.summary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("saved_story_summary"),
                    color = palette.secondaryText,
                    style = NutsNewsTheme.typography.subheadline,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SavedStoryFooter(
                story = story,
                onOpenStory = onOpenStory,
                onRemoveStory = onRemoveStory,
            )
        }
    }
}

@Composable
private fun SavedStoryThumbnail(story: SavedStory) {
    val palette = NutsNewsTheme.colors
    val imageShape = RoundedCornerShape(NutsNewsTheme.dimensions.imageCornerRadius)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(SavedStoryThumbnailAspectRatio)
                .clip(imageShape)
                .background(palette.badgeBackground)
                .testTag("saved_story_thumbnail"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Filled.Newspaper,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = palette.accent,
            )
            Text(
                text = "NutsNews",
                color = palette.secondaryText,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
            )
        }
        story.article.thumbnailUrl?.let { thumbnailUrl ->
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
private fun SavedStoryCategories(categories: List<String>) {
    if (categories.isEmpty()) return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("saved_story_categories"),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
    ) {
        categories.take(MaximumVisibleCategories).forEachIndexed { index, category ->
            Surface(
                modifier = Modifier.testTag("saved_story_category_$index"),
                shape = CircleShape,
                color = NutsNewsTheme.colors.badgeBackground,
                border =
                    BorderStroke(
                        NutsNewsTheme.borders.hairline,
                        NutsNewsTheme.colors.cardBorder,
                    ),
            ) {
                Text(
                    text = category,
                    modifier =
                        Modifier.padding(
                            horizontal = NutsNewsTheme.spacing.small,
                            vertical = NutsNewsTheme.spacing.xs,
                        ),
                    color = NutsNewsTheme.colors.accentHighlight,
                    style = NutsNewsTheme.typography.caption2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SavedStoryFooter(
    story: SavedStory,
    onOpenStory: (SavedStory) -> Unit,
    onRemoveStory: (SavedStory) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = "Saved ${story.savedDateText()}",
                modifier = Modifier.testTag("saved_story_date"),
                color = NutsNewsTheme.colors.mutedText,
                style = NutsNewsTheme.typography.caption,
            )
            Text(
                text = story.article.source,
                modifier = Modifier.testTag("saved_story_source"),
                color = NutsNewsTheme.colors.accentText,
                style = NutsNewsTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = { onOpenStory(story) },
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(nutsNewsButtonGradient())
                    .testTag("saved_story_open_${story.id.value}"),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        ) {
            Text(
                text = "Open",
                color = NutsNewsTheme.colors.buttonText,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Surface(
            modifier =
            Modifier
                    .size(48.dp)
                    .testTag("saved_story_remove_${story.id.value}"),
            onClick = { onRemoveStory(story) },
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
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove saved story",
                    modifier = Modifier.size(16.dp),
                    tint = NutsNewsTheme.colors.accentHighlight,
                )
            }
        }
    }
}

internal fun SavedStory.savedDateText(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String =
    DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(savedAt.atZone(zoneId))

private const val SavedStoryThumbnailAspectRatio = 3f / 2f
private const val MaximumVisibleCategories = 5

@Preview(showBackground = true)
@Composable
private fun SavedStoriesPreview() {
    val article =
        Article(
            id = "saved-preview",
            title = "Neighbors turn an empty lot into a thriving community garden",
            summary = "A volunteer-led project is bringing fresh food and joy to the block.",
            originalUrl = URI("https://example.com/garden"),
            source = "Community Daily",
            publishedAt = "2026-07-25T12:00:00Z",
            createdAt = null,
            thumbnailUrl = null,
            categories = listOf("Community", "Environment"),
        )
    NutsNewsTheme(updateSystemBars = false) {
        SavedStoriesScreen(
            uiState =
                SavedStoriesUiState(
                    isLoading = false,
                    stories = listOf(SavedStory(article, Instant.parse("2026-07-26T12:00:00Z"))),
                    filteredStories =
                        listOf(SavedStory(article, Instant.parse("2026-07-26T12:00:00Z"))),
                ),
            onQueryChanged = {},
            onOpenStory = {},
            onRemoveStory = {},
            onClose = {},
        )
    }
}
