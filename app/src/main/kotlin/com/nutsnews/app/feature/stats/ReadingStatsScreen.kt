package com.nutsnews.app.feature.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutsnews.app.core.model.ReadingStatsDay
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsPoliteAnnouncement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReadingStatsScreen(
    uiState: ReadingStatsUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("reading_stats_screen"),
    ) {
        NutsNewsAdaptivePane {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                ReadingStatsTopBar(onClose = onClose)
                if (uiState.isLoading) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .nutsNewsPoliteAnnouncement()
                                .testTag("reading_stats_loading"),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = NutsNewsTheme.colors.accent,
                        )
                    }
                } else {
                    ReadingStatsContent(uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun ReadingStatsTopBar(onClose: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = NutsNewsTheme.spacing.small),
    ) {
        TextButton(
            onClick = onClose,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .testTag("reading_stats_close"),
        ) {
            Text(
                text = "Close",
                color = NutsNewsTheme.colors.accentText,
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = "Reading Stats",
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .nutsNewsHeading(),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ReadingStatsContent(uiState: ReadingStatsUiState) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("reading_stats_list"),
        contentPadding = PaddingValues(NutsNewsTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        item(key = "header") {
            ReadingStatsHeaderCard()
        }
        item(key = "today") {
            ReadingStatsTodayCard(uiState = uiState)
        }
        item(key = "week") {
            ReadingStatsWeeklyChart(uiState = uiState)
        }
        item(key = "totals") {
            ReadingStatsTotals(uiState = uiState)
        }
    }
}

@Composable
private fun ReadingStatsHeaderCard() {
    StatsSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("reading_stats_header"),
        strong = true,
        borderWidth = NutsNewsTheme.borders.emphasized,
    ) {
        Row(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius),
                color = NutsNewsTheme.colors.badgeBackground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = NutsNewsTheme.colors.accentHighlight,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            ) {
                Text(
                    text = "Your positive-news rhythm",
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.headline,
                )
                Text(
                    text =
                        "Private on-device stats from the stories you open, " +
                            "favorite, and note.",
                    color = NutsNewsTheme.colors.secondaryText,
                    style = NutsNewsTheme.typography.subheadline,
                )
            }
        }
    }
}

@Composable
private fun ReadingStatsTodayCard(uiState: ReadingStatsUiState) {
    StatsSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("reading_stats_today"),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today",
                    modifier =
                        Modifier
                            .weight(1f)
                            .nutsNewsHeading(),
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.headline,
                )
                Text(
                    text = uiState.todayProgressLabel,
                    modifier = Modifier.testTag("reading_stats_goal_label"),
                    color = NutsNewsTheme.colors.accentText,
                    style = NutsNewsTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { uiState.goalProgress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .testTag("reading_stats_goal_progress"),
                color = NutsNewsTheme.colors.accent,
                trackColor = NutsNewsTheme.colors.badgeBackground,
            )
            Text(
                text = uiState.todayMessage,
                modifier =
                    Modifier
                        .nutsNewsPoliteAnnouncement()
                        .testTag("reading_stats_today_message"),
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.subheadline,
            )
        }
    }
}

@Composable
private fun ReadingStatsWeeklyChart(uiState: ReadingStatsUiState) {
    StatsSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("reading_stats_weekly_chart"),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        ) {
            Text(
                text = "Last 7 days",
                modifier = Modifier.nutsNewsHeading(),
                color = NutsNewsTheme.colors.primaryText,
                style = NutsNewsTheme.typography.headline,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                verticalAlignment = Alignment.Bottom,
            ) {
                uiState.recentDays.forEach { day ->
                    ReadingStatsDayBar(
                        day = day,
                        maximumCount = uiState.maxRecentDayCount,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingStatsDayBar(
    day: ReadingStatsDay,
    maximumCount: Int,
    modifier: Modifier = Modifier,
) {
    val barHeight =
        (82f * day.storyCount / maximumCount)
            .coerceAtLeast(12f)
            .dp
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .testTag("reading_stats_day_${day.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = day.storyCount.toString(),
            color =
                if (day.storyCount > 0) {
                    NutsNewsTheme.colors.accent
                } else {
                    NutsNewsTheme.colors.mutedText
                },
            style = NutsNewsTheme.typography.caption2,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(NutsNewsTheme.spacing.xs))
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .testTag("reading_stats_bar_${day.id}"),
            shape = RoundedCornerShape(NutsNewsTheme.radii.xs),
            color =
                if (day.storyCount > 0) {
                    NutsNewsTheme.colors.accentHighlight.copy(alpha = 0.85f)
                } else {
                    NutsNewsTheme.colors.badgeBackground
                },
            border =
                BorderStroke(
                    0.75.dp,
                    NutsNewsTheme.colors.cardBorder,
                ),
        ) {}
        Spacer(modifier = Modifier.height(NutsNewsTheme.spacing.xs))
        Text(
            text = weekdayFormatter.format(day.date),
            color = NutsNewsTheme.colors.mutedText,
            style = NutsNewsTheme.typography.caption2,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun ReadingStatsTotals(uiState: ReadingStatsUiState) {
    val tiles =
        listOf(
            StatsTileData(
                title = "Streak",
                value = uiState.currentStreak.toString(),
                subtitle = if (uiState.currentStreak == 1) "day" else "days",
                icon = Icons.Filled.LocalFireDepartment,
            ),
            StatsTileData(
                title = "Opened",
                value = uiState.totalUniqueStoryCount.toString(),
                subtitle = "stories",
                icon = Icons.Filled.Newspaper,
            ),
            StatsTileData(
                title = "Favorites",
                value = uiState.savedStoryCount.toString(),
                subtitle = "library",
                icon = Icons.Filled.Favorite,
            ),
            StatsTileData(
                title = "Notes",
                value = uiState.noteCount.toString(),
                subtitle = "private",
                icon = Icons.AutoMirrored.Filled.Note,
            ),
            StatsTileData(
                title = "Originals",
                value = uiState.originalOpensTodayCount.toString(),
                subtitle = "today",
                icon = Icons.Filled.Language,
            ),
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("reading_stats_totals"),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        tiles.chunked(2).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                rowTiles.forEach { tile ->
                    ReadingStatsTile(
                        data = tile,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowTiles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReadingStatsTile(
    data: StatsTileData,
    modifier: Modifier = Modifier,
) {
    StatsSurface(
        modifier =
            modifier
                .heightIn(min = 144.dp)
                .testTag("reading_stats_tile_${data.title.lowercase(Locale.ROOT)}"),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = NutsNewsTheme.colors.accentHighlight,
            )
            Text(
                text = data.value,
                modifier =
                    Modifier.testTag(
                        "reading_stats_value_${data.title.lowercase(Locale.ROOT)}",
                    ),
                color = NutsNewsTheme.colors.primaryText,
                style =
                    NutsNewsTheme.typography.metric.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Text(
                text = data.title,
                color = NutsNewsTheme.colors.accentText,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = data.subtitle,
                color = NutsNewsTheme.colors.mutedText,
                style = NutsNewsTheme.typography.caption2,
            )
        }
    }
}

@Composable
private fun StatsSurface(
    modifier: Modifier,
    strong: Boolean = false,
    borderWidth: androidx.compose.ui.unit.Dp = NutsNewsTheme.borders.hairline,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color =
            if (strong) {
                NutsNewsTheme.colors.cardBackgroundStrong
            } else {
                NutsNewsTheme.colors.cardBackground
            },
        border = BorderStroke(borderWidth, NutsNewsTheme.colors.cardBorder),
        content = content,
    )
}

private data class StatsTileData(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val weekdayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

@Preview(showBackground = true)
@Composable
private fun ReadingStatsPreview() {
    val today = LocalDate.of(2026, 7, 26)
    NutsNewsTheme(updateSystemBars = false) {
        ReadingStatsScreen(
            uiState =
                ReadingStatsUiState(
                    isLoading = false,
                    todayStoryCount = 3,
                    dailyGoal = 3,
                    currentStreak = 4,
                    totalUniqueStoryCount = 18,
                    savedStoryCount = 7,
                    noteCount = 2,
                    originalOpensTodayCount = 1,
                    recentDays =
                        (6 downTo 0).map { daysAgo ->
                            ReadingStatsDay(
                                date = today.minusDays(daysAgo.toLong()),
                                storyCount = (6 - daysAgo) % 4,
                            )
                        },
                ),
            onClose = {},
        )
    }
}
