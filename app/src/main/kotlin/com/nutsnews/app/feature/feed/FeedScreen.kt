package com.nutsnews.app.feature.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.LocalNutsNewsWindowInfo
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsMotion
import com.nutsnews.app.designsystem.NutsNewsPalettes
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsButtonGradient
import com.nutsnews.app.navigation.AppDestination
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    uiState: ArticleFeedUiState,
    onDestinationSelected: (AppDestination) -> Unit,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    NutsNewsBackground(modifier = modifier.fillMaxSize()) {
        NutsNewsAdaptivePane(
            maximumWidth = LocalNutsNewsWindowInfo.current.immersiveContentMaximumWidth,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                FeedHeader(
                    availableCategories = uiState.availableCategories,
                    selectedCategory = uiState.selectedCategory,
                    onDestinationSelected = onDestinationSelected,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier.statusBarsPadding(),
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    content = content,
                )
            }
        }
    }
}

@Composable
internal fun FeedHeader(
    availableCategories: List<String>,
    selectedCategory: String?,
    onDestinationSelected: (AppDestination) -> Unit,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val spacing = NutsNewsTheme.spacing
    val density = LocalDensity.current
    val titleShadowOffset = with(density) { spacing.xxs.toPx() }
    val titleShadowBlur = with(density) { spacing.small.toPx() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("feed_header")
                .padding(bottom = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinimumTouchTarget)
                    .padding(horizontal = spacing.medium)
                    .padding(top = spacing.small),
        ) {
            FeedOverflowMenu(
                onDestinationSelected = onDestinationSelected,
                modifier = Modifier.align(Alignment.CenterStart),
            )

            Text(
                text = "NutsNews",
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .nutsNewsHeading(),
                color = palette.accentHighlight,
                style =
                    NutsNewsTheme.typography.brandTitle.copy(
                        letterSpacing = 1.8.sp,
                        shadow =
                            Shadow(
                                color = palette.accentGlow,
                                offset = Offset(0f, titleShadowOffset),
                                blurRadius = titleShadowBlur,
                            ),
                    ),
            )
        }

        CategoryFilterRow(
            availableCategories = availableCategories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
        )
    }

    HorizontalDivider(
        thickness = NutsNewsTheme.borders.hairline,
        color = palette.cardBorder,
    )
}

@Composable
private fun FeedOverflowMenu(
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var settingsPending by remember { mutableStateOf(false) }
    val palette = NutsNewsTheme.colors
    val reducedMotion = NutsNewsTheme.reducedMotion
    val settingsGlow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val openDestination: (AppDestination) -> Unit = { destination ->
        if (
            destination != AppDestination.Settings ||
            reducedMotion
        ) {
            expanded = false
            onDestinationSelected(destination)
        } else if (!settingsPending) {
            settingsPending = true
            scope.launch {
                settingsGlow.snapTo(1f)
                launch {
                    settingsGlow.animateTo(
                        targetValue = 0f,
                        animationSpec =
                            tween(
                                durationMillis = NutsNewsMotion.ActionGlowMillis,
                                easing = FastOutSlowInEasing,
                            ),
                    )
                }
                delay(NutsNewsMotion.ActionOpenDelayMillis)
                expanded = false
                onDestinationSelected(destination)
                delay(
                    NutsNewsMotion.ActionGlowResetMillis -
                        NutsNewsMotion.ActionOpenDelayMillis,
                )
                settingsGlow.snapTo(0f)
                settingsPending = false
            }
        }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .size(MinimumTouchTarget)
                    .testTag("feed_menu_button"),
        ) {
            Surface(
                modifier = Modifier.size(width = 38.dp, height = 34.dp),
                shape = CircleShape,
                color = palette.badgeBackground,
                border =
                    androidx.compose.foundation.BorderStroke(
                        NutsNewsTheme.borders.hairline,
                        palette.cardBorder,
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open menu",
                        modifier = Modifier.size(20.dp),
                        tint = palette.accentHighlight,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("feed_menu_popup"),
        ) {
            FeedMenuEntries.forEach { entry ->
                if (entry.hasDividerBefore) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                DropdownMenuItem(
                    modifier = Modifier.testTag("feed_menu_${entry.destination.route}"),
                    text = { Text(entry.label) },
                    onClick = {
                        openDestination(entry.destination)
                    },
                    leadingIcon = {
                        if (entry.destination == AppDestination.Settings) {
                            val glow = settingsGlow.value
                            Box(
                                modifier =
                                    Modifier
                                        .size(32.dp)
                                        .shadow(
                                            elevation =
                                                (
                                                    NutsNewsMotion.ActionGlowRadiusDp *
                                                        glow
                                                ).dp,
                                            shape = CircleShape,
                                            ambientColor =
                                                palette.accentHighlight.copy(
                                                    alpha = glow * 0.72f,
                                                ),
                                            spotColor =
                                                palette.accentGlow.copy(
                                                    alpha = glow * 0.55f,
                                                ),
                                        ).graphicsLayer {
                                            scaleX = 1f + (glow * 0.035f)
                                            scaleY = 1f + (glow * 0.035f)
                                        }.testTag("feed_settings_glow"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        } else {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    availableCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
) {
    val categories = listOf<String?>(null) + availableCategories

    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("feed_category_row"),
        contentPadding = PaddingValues(horizontal = NutsNewsTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
    ) {
        itemsIndexed(
            items = categories,
            key = { _, category -> category?.lowercase(Locale.ROOT) ?: AllCategoryKey },
        ) { index, category ->
            val title = category ?: "All"
            CategoryChip(
                title = title,
                isSelected =
                    if (category == null) {
                        selectedCategory == null
                    } else {
                        selectedCategory.equals(category, ignoreCase = true)
                    },
                dotIndex = index,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    title: String,
    isSelected: Boolean,
    dotIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NutsNewsTheme.colors
    val shape = CircleShape
    val backgroundModifier =
        if (isSelected) {
            Modifier.background(nutsNewsButtonGradient(), shape)
        } else {
            Modifier.background(palette.badgeBackground, shape)
        }

    Row(
        modifier =
            modifier
                .heightIn(min = MinimumTouchTarget)
                .testTag(categoryTestTag(title))
                .clip(shape)
                .then(backgroundModifier)
                .border(
                    width = NutsNewsTheme.borders.hairline,
                    color =
                        if (isSelected) {
                            androidx.compose.ui.graphics.Color.Transparent
                        } else {
                            palette.cardBorder
                    },
                    shape = shape,
                )
                .selectable(
                    selected = isSelected,
                    onClick = onClick,
                    role = Role.RadioButton,
                )
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
                        color =
                            NutsNewsPalettes.categoryDotColor(
                                theme = NutsNewsTheme.appTheme,
                                index = dotIndex,
                                isSelected = isSelected,
                            ),
                        shape = CircleShape,
                    ),
        )
        Text(
            text = title,
            color = if (isSelected) palette.buttonText else palette.secondaryText,
            style = NutsNewsTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

internal data class FeedMenuEntry(
    val label: String,
    val destination: AppDestination,
    val icon: ImageVector,
    val hasDividerBefore: Boolean = false,
)

internal val FeedMenuEntries =
    listOf(
        FeedMenuEntry("Help & F.A.Q.", AppDestination.Help, Icons.AutoMirrored.Filled.Help),
        FeedMenuEntry(
            "Today’s Picks",
            AppDestination.DailyDigest,
            Icons.Filled.Newspaper,
            hasDividerBefore = true,
        ),
        FeedMenuEntry("Good Mood", AppDestination.GoodMood, Icons.Filled.AutoAwesome),
        FeedMenuEntry("Reading Stats", AppDestination.ReadingStats, Icons.Filled.BarChart),
        FeedMenuEntry("Favorites", AppDestination.SavedStories, Icons.Filled.Favorite),
        FeedMenuEntry("Search", AppDestination.ArchiveSearch, Icons.Filled.Search),
        FeedMenuEntry("Personalize", AppDestination.Personalization, Icons.Filled.Tune),
        FeedMenuEntry("Settings", AppDestination.Settings, Icons.Filled.Settings),
    )

private const val AllCategoryKey = "__all__"
private val MinimumTouchTarget = 48.dp

private fun categoryTestTag(title: String): String =
    "feed_category_${
        title
            .lowercase(Locale.ROOT)
            .map { character ->
                if (character.isLetterOrDigit()) character else '_'
            }
            .joinToString("")
    }"

@Preview(showBackground = true)
@Composable
private fun FeedScreenPreview() {
    NutsNewsTheme(updateSystemBars = false) {
        FeedScreen(
            uiState =
                ArticleFeedUiState(
                    availableCategories =
                        listOf(
                            "Animals",
                            "Science",
                            "Community",
                            "Wellness",
                        ),
                    selectedCategory = "Science",
                ),
            onDestinationSelected = {},
            onCategorySelected = {},
        )
    }
}
