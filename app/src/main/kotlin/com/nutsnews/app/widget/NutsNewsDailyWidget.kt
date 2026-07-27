package com.nutsnews.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.nutsnews.app.MainActivity
import com.nutsnews.app.NutsNewsApplication
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NutsNewsDailyWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode =
        SizeMode.Responsive(NutsNewsWidgetSizes.Supported)

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val initialData = loadWidgetData(context)
        updateAppWidgetState(context, id) { preferences ->
            preferences.writeWidgetData(initialData)
        }
        provideContent {
            NutsNewsWidgetContent(
                data = currentState<Preferences>().readWidgetData() ?: initialData,
                size = LocalSize.current,
            )
        }
    }
}

@Composable
internal fun NutsNewsWidgetContent(
    data: WidgetData,
    size: DpSize,
    openAppAction: Action = actionStartActivity<MainActivity>(),
    refreshAction: Action = actionRunCallback<RefreshNutsNewsWidgetAction>(),
) {
    val sizeClass = NutsNewsWidgetSizes.classify(size)
    val palette = NutsNewsWidgetPalettes.forTheme(data.theme)
    val padding =
        when (sizeClass) {
            NutsNewsWidgetSizeClass.Small -> 14.dp
            NutsNewsWidgetSizeClass.Medium -> 16.dp
            NutsNewsWidgetSizeClass.Large -> 18.dp
        }
    val spacing =
        when (sizeClass) {
            NutsNewsWidgetSizeClass.Small -> 8.dp
            NutsNewsWidgetSizeClass.Medium -> 10.dp
            NutsNewsWidgetSizeClass.Large -> 12.dp
        }

    Scaffold(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(palette.background)
                .appWidgetBackground()
                .cornerRadius(26.dp)
                .clickable(openAppAction)
                .semantics {
                    testTag = "nutsnews_widget_root"
                    contentDescription = "Open NutsNews: ${data.article.title}"
                },
        backgroundColor = palette.background,
    ) {
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            WidgetHeader(
                palette = palette,
                symbol = palette.symbol,
                refreshAction = refreshAction,
            )
            Spacer(GlanceModifier.height(spacing))
            Text(
                text = data.article.title,
                modifier =
                    GlanceModifier.semantics {
                        testTag = "widget_title"
                    },
                style =
                    TextStyle(
                        color = palette.primaryText,
                        fontSize =
                            when (sizeClass) {
                                NutsNewsWidgetSizeClass.Small -> 17.sp
                                NutsNewsWidgetSizeClass.Medium -> 18.sp
                                NutsNewsWidgetSizeClass.Large -> 22.sp
                            },
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines =
                    when (sizeClass) {
                        NutsNewsWidgetSizeClass.Small -> 4
                        NutsNewsWidgetSizeClass.Medium -> 3
                        NutsNewsWidgetSizeClass.Large -> 4
                    },
            )

            if (sizeClass != NutsNewsWidgetSizeClass.Small) {
                Spacer(GlanceModifier.height(spacing))
                Text(
                    text = data.article.summary,
                    modifier =
                        GlanceModifier.semantics {
                            testTag = "widget_summary"
                        },
                    style =
                        TextStyle(
                            color = palette.secondaryText,
                            fontSize =
                                if (sizeClass == NutsNewsWidgetSizeClass.Large) {
                                    14.sp
                                } else {
                                    12.sp
                                },
                            fontWeight = FontWeight.Medium,
                        ),
                    maxLines =
                        if (
                            sizeClass == NutsNewsWidgetSizeClass.Large &&
                            data.showStatsOnLargeWidget
                        ) {
                            2
                        } else {
                            4
                        },
                )
            }

            if (
                sizeClass == NutsNewsWidgetSizeClass.Large &&
                data.showStatsOnLargeWidget
            ) {
                Spacer(GlanceModifier.height(spacing))
                WidgetStatsPanel(
                    stats = data.stats,
                    palette = palette,
                    availableWidth = (size.width - (padding * 2)).coerceAtLeast(0.dp),
                )
            }

            Spacer(GlanceModifier.defaultWeight())
            WidgetFooter(
                mood = data.article.mood,
                source = data.article.source,
                palette = palette,
            )
        }
    }
}

@Composable
private fun WidgetHeader(
    palette: NutsNewsWidgetPalette,
    symbol: String,
    refreshAction: Action,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = symbol,
            style =
                TextStyle(
                    color = palette.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = "↻",
            modifier =
                GlanceModifier
                    .size(32.dp)
                    .clickable(refreshAction)
                    .semantics {
                        testTag = "widget_refresh"
                        contentDescription = "Refresh NutsNews widget"
                    },
            style =
                TextStyle(
                    color = palette.accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}

@Composable
private fun WidgetStatsPanel(
    stats: WidgetStats,
    palette: NutsNewsWidgetPalette,
    availableWidth: Dp,
) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .cornerRadius(18.dp)
                .background(palette.card)
                .padding(12.dp)
                .semantics {
                    testTag = "widget_stats"
                    contentDescription =
                        "Today’s calm reset ${stats.progressText}, " +
                            "${stats.currentStreak} day streak, " +
                            "${stats.totalStoryCount} stories"
                },
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Today’s calm reset",
                style =
                    TextStyle(
                        color = palette.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = stats.progressText,
                modifier =
                    GlanceModifier.semantics {
                        testTag = "widget_progress_text"
                    },
                style =
                    TextStyle(
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
        Spacer(GlanceModifier.height(9.dp))
        WidgetProgressBar(
            progress = stats.progressFraction,
            palette = palette,
            availableWidth = (availableWidth - 24.dp).coerceAtLeast(0.dp),
        )
        Spacer(GlanceModifier.height(9.dp))
        Row {
            WidgetStatPill(
                title = "Streak",
                value = stats.currentStreak.toString(),
                palette = palette,
            )
            Spacer(GlanceModifier.width(8.dp))
            WidgetStatPill(
                title = "Stories",
                value = stats.totalStoryCount.toString(),
                palette = palette,
            )
        }
    }
}

@Composable
private fun WidgetProgressBar(
    progress: Float,
    palette: NutsNewsWidgetPalette,
    availableWidth: Dp,
) {
    Box(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .height(7.dp)
                .cornerRadius(20.dp)
                .background(palette.border)
                .semantics {
                    testTag = "widget_progress"
                    contentDescription = "${(progress * 100).toInt()} percent complete"
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                GlanceModifier
                    .width(availableWidth * progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .cornerRadius(20.dp)
                    .background(palette.accent),
        ) {}
    }
}

@Composable
private fun WidgetStatPill(
    title: String,
    value: String,
    palette: NutsNewsWidgetPalette,
) {
    Row(
        modifier =
            GlanceModifier
                .cornerRadius(30.dp)
                .background(palette.accent)
                .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style =
                TextStyle(
                    color = palette.buttonText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
        )
        Spacer(GlanceModifier.width(5.dp))
        Text(
            text = title,
            style =
                TextStyle(
                    color = palette.buttonText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}

@Composable
private fun WidgetFooter(
    mood: String,
    source: String,
    palette: NutsNewsWidgetPalette,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mood,
            modifier =
                GlanceModifier
                    .cornerRadius(30.dp)
                    .background(palette.accent)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .semantics {
                        testTag = "widget_mood"
                    },
            style =
                TextStyle(
                    color = palette.buttonText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            maxLines = 1,
        )
        if (source.isNotBlank()) {
            Spacer(GlanceModifier.width(7.dp))
            Text(
                text = source,
                modifier =
                    GlanceModifier.semantics {
                        testTag = "widget_source"
                    },
                style =
                    TextStyle(
                        color = palette.secondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
            )
        }
    }
}

class RefreshNutsNewsWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        NutsNewsWidgetUpdater.updateAll(
            context = context,
            forceRefresh = true,
        )
    }
}

internal object NutsNewsWidgetUpdater {
    suspend fun updateAll(
        context: Context,
        forceRefresh: Boolean,
    ) {
        val widget = NutsNewsDailyWidget()
        val glanceIds =
            GlanceAppWidgetManager(context)
                .getGlanceIds(NutsNewsDailyWidget::class.java)
        if (glanceIds.isEmpty()) return

        val data = loadWidgetData(context, forceRefresh)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { preferences ->
                preferences.writeWidgetData(data)
            }
            widget.update(context, glanceId)
        }
    }
}

private suspend fun loadWidgetData(
    context: Context,
    forceRefresh: Boolean = false,
): WidgetData =
    try {
        withContext(Dispatchers.IO) {
            widgetDataProvider(context).load(forceRefresh)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        WidgetData.Placeholder.copy(
            article = WidgetArticle.Fallback,
            articleStatus = WidgetArticleStatus.Fallback,
            theme = NutsNewsAppTheme.Amber,
            stats = WidgetStats.Empty,
            refreshedAt = Instant.now(),
        )
    }

private fun widgetDataProvider(context: Context): WidgetDataProvider =
    (context.applicationContext as NutsNewsApplication)
        .container
        .widgetDataProvider
