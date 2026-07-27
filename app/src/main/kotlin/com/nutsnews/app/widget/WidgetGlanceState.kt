package com.nutsnews.app.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import java.time.Instant

internal fun MutablePreferences.writeWidgetData(data: WidgetData) {
    this[WidgetStateKeys.Initialized] = true
    this[WidgetStateKeys.StoryId] = data.article.storyId
    this[WidgetStateKeys.Title] = data.article.title
    this[WidgetStateKeys.Summary] = data.article.summary
    this[WidgetStateKeys.Source] = data.article.source
    this[WidgetStateKeys.Mood] = data.article.mood
    this[WidgetStateKeys.ArticleStatus] = data.articleStatus.name
    this[WidgetStateKeys.Theme] = data.theme.rawValue
    this[WidgetStateKeys.TodayCount] = data.stats.todayCount
    this[WidgetStateKeys.DailyGoal] = data.stats.dailyGoal
    this[WidgetStateKeys.CurrentStreak] = data.stats.currentStreak
    this[WidgetStateKeys.TotalStoryCount] = data.stats.totalStoryCount
    this[WidgetStateKeys.ShowStats] = data.showStatsOnLargeWidget
    this[WidgetStateKeys.RefreshedAt] = data.refreshedAt.toEpochMilli()
}

internal fun Preferences.readWidgetData(): WidgetData? {
    if (this[WidgetStateKeys.Initialized] != true) return null

    val articleStatus =
        this[WidgetStateKeys.ArticleStatus]
            ?.let { rawStatus ->
                WidgetArticleStatus.entries.firstOrNull { status ->
                    status.name == rawStatus
                }
            }
            ?: WidgetArticleStatus.Fallback

    return WidgetData(
        article =
            WidgetArticle(
                storyId = this[WidgetStateKeys.StoryId].orEmpty(),
                title = this[WidgetStateKeys.Title] ?: WidgetArticle.Fallback.title,
                summary = this[WidgetStateKeys.Summary] ?: WidgetArticle.Fallback.summary,
                source = this[WidgetStateKeys.Source].orEmpty(),
                mood = this[WidgetStateKeys.Mood] ?: WidgetArticle.Fallback.mood,
            ),
        articleStatus = articleStatus,
        theme = NutsNewsAppTheme.fromStoredValue(this[WidgetStateKeys.Theme]),
        stats =
            WidgetStats(
                todayCount =
                    (this[WidgetStateKeys.TodayCount] ?: WidgetStats.Empty.todayCount)
                        .coerceAtLeast(0),
                dailyGoal =
                    (this[WidgetStateKeys.DailyGoal] ?: WidgetStats.Empty.dailyGoal)
                        .coerceAtLeast(1),
                currentStreak =
                    (this[WidgetStateKeys.CurrentStreak] ?: WidgetStats.Empty.currentStreak)
                        .coerceAtLeast(0),
                totalStoryCount =
                    (this[WidgetStateKeys.TotalStoryCount] ?: WidgetStats.Empty.totalStoryCount)
                        .coerceAtLeast(0),
            ),
        showStatsOnLargeWidget = this[WidgetStateKeys.ShowStats] ?: true,
        refreshedAt =
            Instant.ofEpochMilli(
                this[WidgetStateKeys.RefreshedAt] ?: Instant.EPOCH.toEpochMilli(),
            ),
    )
}

private object WidgetStateKeys {
    val Initialized = booleanPreferencesKey("nutsnews.widget.initialized")
    val StoryId = stringPreferencesKey("nutsnews.widget.article.storyId")
    val Title = stringPreferencesKey("nutsnews.widget.article.title")
    val Summary = stringPreferencesKey("nutsnews.widget.article.summary")
    val Source = stringPreferencesKey("nutsnews.widget.article.source")
    val Mood = stringPreferencesKey("nutsnews.widget.article.mood")
    val ArticleStatus = stringPreferencesKey("nutsnews.widget.article.status")
    val Theme = stringPreferencesKey("nutsnews.widget.theme")
    val TodayCount = intPreferencesKey("nutsnews.widget.stats.todayCount")
    val DailyGoal = intPreferencesKey("nutsnews.widget.stats.dailyGoal")
    val CurrentStreak = intPreferencesKey("nutsnews.widget.stats.currentStreak")
    val TotalStoryCount = intPreferencesKey("nutsnews.widget.stats.totalStoryCount")
    val ShowStats = booleanPreferencesKey("nutsnews.widget.stats.show")
    val RefreshedAt = longPreferencesKey("nutsnews.widget.refreshedAt")
}
