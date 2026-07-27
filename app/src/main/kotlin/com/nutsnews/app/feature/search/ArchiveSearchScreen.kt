package com.nutsnews.app.feature.search

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsMinimumTouchTarget
import com.nutsnews.app.designsystem.nutsNewsPoliteAnnouncement
import com.nutsnews.app.designsystem.nutsNewsButtonGradient
import java.net.URI
import kotlinx.coroutines.delay

@Composable
fun ArchiveSearchScreen(
    uiState: ArchiveSearchUiState,
    onQueryChanged: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleSaved: (Article) -> Unit,
    onOpenArticle: (Article) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
) {
    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("archive_search_screen"),
    ) {
        NutsNewsAdaptivePane {
            Column(modifier = Modifier.fillMaxSize()) {
                ArchiveSearchHeader(onClose = onClose)
                ArchiveSearchControls(
                    uiState = uiState,
                    onQueryChanged = onQueryChanged,
                    onSubmitSearch = onSubmitSearch,
                    onClearSearch = onClearSearch,
                    requestInitialFocus = requestInitialFocus,
                )
                ArchiveSearchResults(
                    uiState = uiState,
                    onRetry = onRetry,
                    onLoadMore = onLoadMore,
                    onToggleSaved = onToggleSaved,
                    onOpenArticle = onOpenArticle,
                    listState = listState,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ArchiveSearchHeader(onClose: () -> Unit) {
    val palette = NutsNewsTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(palette.cardBackground)
                .statusBarsPadding()
                .padding(
                    start = NutsNewsTheme.spacing.medium,
                    top = NutsNewsTheme.spacing.medium,
                    end = NutsNewsTheme.spacing.medium,
                    bottom = NutsNewsTheme.spacing.small,
                ),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = "Search",
                modifier = Modifier.nutsNewsHeading(),
                color = palette.accentHighlight,
                style =
                    NutsNewsTheme.typography.title.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.2.sp,
                    ),
            )
            Text(
                text = "Find stories across the full NutsNews archive.",
                color = palette.secondaryText,
                style = NutsNewsTheme.typography.caption,
            )
        }
        Surface(
            modifier =
                Modifier
                    .size(48.dp)
                    .testTag("archive_search_close"),
            onClick = onClose,
            shape = CircleShape,
            color = palette.badgeBackground,
            border = BorderStroke(NutsNewsTheme.borders.hairline, palette.cardBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close search",
                    modifier = Modifier.size(16.dp),
                    tint = palette.accentHighlight,
                )
            }
        }
    }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(NutsNewsTheme.borders.hairline)
                .background(palette.cardBorder),
    )
}

@Composable
private fun ArchiveSearchControls(
    uiState: ArchiveSearchUiState,
    onQueryChanged: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearSearch: () -> Unit,
    requestInitialFocus: Boolean,
) {
    val palette = NutsNewsTheme.colors
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            delay(InitialFocusDelayMillis)
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChanged,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("archive_search_query"),
            placeholder = {
                Text(
                    text = "Search dogs, community, science...",
                    color = palette.mutedText,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = palette.accentSoft,
                )
            },
            trailingIcon =
                if (uiState.query.isNotEmpty()) {
                    {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = palette.secondaryText,
                            )
                        }
                    }
                } else {
                    null
                },
            supportingText =
                if (uiState.showShortQueryHint) {
                    {
                        Text(
                            text = "Type at least 2 characters to search.",
                            color = palette.secondaryText,
                            style = NutsNewsTheme.typography.caption,
                        )
                    }
                } else {
                    null
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
            shape = RoundedCornerShape(NutsNewsTheme.radii.medium),
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

        Button(
            onClick = onSubmitSearch,
            enabled = uiState.canSubmit,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .background(nutsNewsButtonGradient())
                    .testTag("archive_search_submit"),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = NutsNewsTheme.spacing.medium),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = palette.cardBorder,
                    disabledContentColor = palette.secondaryText,
                ),
        ) {
            if (uiState.isInitialLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = palette.buttonText,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(NutsNewsTheme.spacing.xs))
            }
            Text(
                text = "Search all NutsNews",
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArchiveSearchResults(
    uiState: ArchiveSearchUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleSaved: (Article) -> Unit,
    onOpenArticle: (Article) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isInitialLoading ->
            ArchiveSearchLoading(modifier)

        !uiState.hasSearched ->
            ArchiveSearchStartState(
                showShortQueryHint = uiState.showShortQueryHint,
                modifier = modifier,
            )

        uiState.articles.isEmpty() && uiState.errorMessage != null ->
            ArchiveSearchFailure(
                message = uiState.errorMessage,
                onRetry = onRetry,
                modifier = modifier,
            )

        uiState.articles.isEmpty() ->
            ArchiveSearchNoResults(modifier)

        else ->
            ArchiveSearchResultList(
                uiState = uiState,
                onRetry = onRetry,
                onLoadMore = onLoadMore,
                onToggleSaved = onToggleSaved,
                onOpenArticle = onOpenArticle,
                listState = listState,
                modifier = modifier,
            )
    }
}

@Composable
private fun ArchiveSearchResultList(
    uiState: ArchiveSearchUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleSaved: (Article) -> Unit,
    onOpenArticle: (Article) -> Unit,
    listState: LazyListState,
    modifier: Modifier,
) {
    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("archive_search_results"),
        contentPadding =
            PaddingValues(
                start = NutsNewsTheme.spacing.medium,
                end = NutsNewsTheme.spacing.medium,
                bottom = NutsNewsTheme.spacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        item(key = "header") {
            ArchiveSearchResultHeader(uiState)
        }
        items(
            items = uiState.articles,
            key = { article -> article.stableId.value },
        ) { article ->
            ArchiveSearchResultRow(
                article = article,
                isSaved = article.stableId in uiState.savedStoryIds,
                onToggleSaved = onToggleSaved,
                onOpenArticle = onOpenArticle,
            )
            if (article.stableId == uiState.articles.lastOrNull()?.stableId) {
                LaunchedEffect(
                    article.stableId,
                    uiState.nextPage,
                    uiState.canLoadMore,
                ) {
                    if (uiState.canLoadMore) onLoadMore()
                }
            }
        }
        if (uiState.isLoadingMore) {
            item(key = "loading-more") {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .padding(vertical = NutsNewsTheme.spacing.medium)
                            .size(24.dp)
                            .nutsNewsPoliteAnnouncement()
                            .testTag("archive_search_loading_more"),
                    color = NutsNewsTheme.colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        } else if (uiState.canLoadMore) {
            item(key = "load-more") {
                ArchiveSearchLoadMoreButton(onLoadMore)
            }
        }
        uiState.errorMessage?.let { message ->
            item(key = "page-error") {
                ArchiveSearchErrorBanner(
                    message = message,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun ArchiveSearchResultHeader(uiState: ArchiveSearchUiState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = NutsNewsTheme.spacing.xs)
                .testTag("archive_search_result_header"),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Results for “${uiState.searchedQuery}”",
            modifier =
                Modifier
                    .weight(1f)
                    .nutsNewsHeading(),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.subheadline,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            shape = CircleShape,
            color = NutsNewsTheme.colors.buttonGradient.first(),
        ) {
            Text(
                text = uiState.articles.size.toString(),
                modifier =
                    Modifier
                        .padding(
                            horizontal = NutsNewsTheme.spacing.small,
                            vertical = NutsNewsTheme.spacing.xxs,
                        ).testTag("archive_search_result_count"),
                color = NutsNewsTheme.colors.buttonText,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArchiveSearchResultRow(
    article: Article,
    isSaved: Boolean,
    onToggleSaved: (Article) -> Unit,
    onOpenArticle: (Article) -> Unit,
) {
    val palette = NutsNewsTheme.colors
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("archive_search_result_${article.stableId.value}"),
        onClick = { onOpenArticle(article) },
        shape = RoundedCornerShape(NutsNewsTheme.radii.medium),
        color = palette.cardBackgroundStrong,
        border = BorderStroke(NutsNewsTheme.borders.hairline, palette.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(NutsNewsTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            verticalAlignment = Alignment.Top,
        ) {
            ArchiveSearchThumbnail(article)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
            ) {
                ArchiveSearchMetadata(article)
                Text(
                    text = article.title,
                    color = palette.primaryText,
                    style = NutsNewsTheme.typography.subheadline,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.summary.isNotBlank()) {
                    Text(
                        text = article.summary,
                        color = palette.secondaryText,
                        style = NutsNewsTheme.typography.caption,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = NutsNewsTheme.spacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = article.displayDate,
                        modifier =
                            Modifier
                                .weight(1f)
                                .testTag("archive_search_result_date"),
                        color = palette.mutedText,
                        style = NutsNewsTheme.typography.caption2,
                        maxLines = 1,
                    )
                    ArchiveSearchSaveButton(
                        article = article,
                        isSaved = isSaved,
                        onToggleSaved = onToggleSaved,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveSearchThumbnail(article: Article) {
    val palette = NutsNewsTheme.colors
    val shape = RoundedCornerShape(NutsNewsTheme.radii.small)
    Box(
        modifier =
            Modifier
                .size(width = 94.dp, height = 70.dp)
                .clip(shape)
                .background(palette.badgeBackground)
                .testTag("archive_search_result_thumbnail"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Newspaper,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = palette.accent,
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
private fun ArchiveSearchMetadata(article: Article) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = article.source,
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .testTag("archive_search_result_source"),
            color = NutsNewsTheme.colors.accentText,
            style = NutsNewsTheme.typography.caption2,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        article.categories.firstOrNull()?.let { category ->
            Surface(
                shape = CircleShape,
                color = NutsNewsTheme.colors.badgeBackground,
            ) {
                Text(
                    text = category,
                    modifier =
                        Modifier
                            .padding(horizontal = NutsNewsTheme.spacing.xs, vertical = 3.dp)
                            .testTag("archive_search_result_category"),
                    color = NutsNewsTheme.colors.secondaryText,
                    style = NutsNewsTheme.typography.caption2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ArchiveSearchSaveButton(
    article: Article,
    isSaved: Boolean,
    onToggleSaved: (Article) -> Unit,
) {
    val palette = NutsNewsTheme.colors
    Surface(
        modifier =
            Modifier
                .nutsNewsMinimumTouchTarget()
                .semantics {
                    stateDescription = if (isSaved) "Saved" else "Not saved"
                }
                .testTag("archive_search_save_${article.stableId.value}"),
        onClick = { onToggleSaved(article) },
        shape = CircleShape,
        color =
            if (isSaved) {
                palette.buttonGradient.first()
            } else {
                palette.badgeBackground
            },
        border =
            if (isSaved) {
                null
            } else {
                BorderStroke(NutsNewsTheme.borders.hairline, palette.cardBorder)
            },
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = NutsNewsTheme.spacing.small,
                    vertical = NutsNewsTheme.spacing.xs,
                ),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                modifier = Modifier.size(14.dp),
                tint = if (isSaved) palette.buttonText else palette.accentHighlight,
            )
            Text(
                text = if (isSaved) "Saved" else "Save",
                color = if (isSaved) palette.buttonText else palette.accentHighlight,
                style = NutsNewsTheme.typography.caption2,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArchiveSearchLoadMoreButton(onLoadMore: () -> Unit) {
    Button(
        onClick = onLoadMore,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = NutsNewsTheme.spacing.small)
                .heightIn(min = 48.dp)
                .clip(CircleShape)
                .background(nutsNewsButtonGradient())
                .testTag("archive_search_load_more"),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    ) {
        Text(
            text = "Load more results",
            color = NutsNewsTheme.colors.buttonText,
            style = NutsNewsTheme.typography.subheadline,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ArchiveSearchLoading(modifier: Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .nutsNewsPoliteAnnouncement()
                .testTag("archive_search_loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = NutsNewsTheme.colors.accent)
        Text(
            text = "Searching good news...",
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.small),
            color = NutsNewsTheme.colors.secondaryText,
            style = NutsNewsTheme.typography.subheadline,
        )
    }
}

@Composable
private fun ArchiveSearchStartState(
    showShortQueryHint: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(NutsNewsTheme.spacing.medium)
                .testTag("archive_search_start"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.TravelExplore,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Text(
            text =
                if (showShortQueryHint) {
                    "Keep typing"
                } else {
                    "Search the archive"
                },
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.medium),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
        )
        Text(
            text =
                if (showShortQueryHint) {
                    "Archive searches need at least 2 characters."
                } else {
                    "Find uplifting stories by topic, source, or category."
                },
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
private fun ArchiveSearchNoResults(modifier: Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(NutsNewsTheme.spacing.medium)
                .nutsNewsPoliteAnnouncement()
                .testTag("archive_search_no_results"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Eco,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Text(
            text = "No matching stories yet",
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.medium),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
        )
        Text(
            text = "Try a broader search like “animals”, “community”, or “science”.",
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
private fun ArchiveSearchFailure(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(NutsNewsTheme.spacing.medium)
                .nutsNewsPoliteAnnouncement()
                .testTag("archive_search_failure"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Text(
            text = "Search is taking a breather",
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.medium),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
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
        ArchiveSearchRetryButton(
            onRetry = onRetry,
            modifier = Modifier.padding(top = NutsNewsTheme.spacing.medium),
        )
    }
}

@Composable
private fun ArchiveSearchErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .nutsNewsPoliteAnnouncement()
                .testTag("archive_search_page_error"),
        shape = RoundedCornerShape(NutsNewsTheme.radii.small),
        color = Color.Red.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(NutsNewsTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.caption,
            )
            ArchiveSearchRetryButton(onRetry = onRetry)
        }
    }
}

@Composable
private fun ArchiveSearchRetryButton(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .nutsNewsMinimumTouchTarget()
                .testTag("archive_search_retry"),
        onClick = onRetry,
        shape = CircleShape,
        color = NutsNewsTheme.colors.badgeBackground,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Text(
            text = "Retry",
            modifier =
                Modifier.padding(
                    horizontal = NutsNewsTheme.spacing.medium,
                    vertical = NutsNewsTheme.spacing.small,
                ),
            color = NutsNewsTheme.colors.accentHighlight,
            style = NutsNewsTheme.typography.caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val InitialFocusDelayMillis = 350L

@Preview(showBackground = true)
@Composable
private fun ArchiveSearchPreview() {
    val article =
        Article(
            id = "search-preview",
            title = "Community scientists share a hopeful discovery",
            summary = "The project brings neighbors together around a brighter future.",
            originalUrl = URI("https://example.com/search-preview"),
            source = "Good News Daily",
            publishedAt = "2026-07-26T12:00:00Z",
            createdAt = null,
            thumbnailUrl = null,
            categories = listOf("Science", "Community"),
        )
    NutsNewsTheme(updateSystemBars = false) {
        ArchiveSearchScreen(
            uiState =
                ArchiveSearchUiState(
                    query = "science",
                    searchedQuery = "science",
                    articles = listOf(article),
                    hasSearched = true,
                    nextPage = 1,
                ),
            onQueryChanged = {},
            onSubmitSearch = {},
            onClearSearch = {},
            onRetry = {},
            onLoadMore = {},
            onToggleSaved = {},
            onOpenArticle = {},
            onClose = {},
            requestInitialFocus = false,
        )
    }
}
