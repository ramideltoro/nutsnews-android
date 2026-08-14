package com.nutsnews.app.widget

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.data.preferences.UserPreferenceDefaults
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import java.time.Instant

enum class WidgetArticleStatus {
    Current,
    Stale,
    Fallback,
    Placeholder,
}

data class WidgetArticle(
    val storyId: String,
    val title: String,
    val summary: String,
    val source: String,
    val mood: String,
) {
    companion object {
        const val DefaultSummary =
            "Open NutsNews for a calm, positive story picked to brighten your day."

        val Placeholder =
            WidgetArticle(
                storyId = "nutsnews-widget-placeholder",
                title = "Your daily good-news reset is ready",
                summary =
                    "A calm, positive story from NutsNews for a brighter moment today.",
                source = "",
                mood = "Daily Reset",
            )

        val Fallback =
            WidgetArticle(
                storyId = "nutsnews-widget-fallback",
                title = "Open NutsNews for today’s positive story",
                summary =
                    "Your good-news dashboard, favorite stories, mood picker, and " +
                        "daily reset are waiting.",
                source = "",
                mood = "Good News",
            )

        fun fromArticle(article: Article): WidgetArticle =
            WidgetArticle(
                storyId = article.stableId.value,
                title =
                    article.title
                        .trim()
                        .ifEmpty { "A good-news story is ready" },
                summary =
                    article.summary
                        .trim()
                        .ifEmpty { DefaultSummary },
                source = article.source.trim(),
                mood =
                    article.categories
                        .firstOrNull { category -> category.isNotBlank() }
                        ?.trim()
                        ?: Article.DefaultCategoryLabel,
            )
    }
}

data class WidgetStats(
    val todayCount: Int,
    val dailyGoal: Int,
    val currentStreak: Int,
    val totalStoryCount: Int,
) {
    val progressText: String
        get() = "${todayCount.coerceAtMost(dailyGoal)}/$dailyGoal"

    val progressFraction: Float
        get() = (todayCount.toFloat() / dailyGoal).coerceIn(0f, 1f)

    companion object {
        val Empty =
            WidgetStats(
                todayCount = 0,
                dailyGoal = UserPreferenceDefaults.DefaultDailyGoal,
                currentStreak = 0,
                totalStoryCount = 0,
            )

        fun from(
            readingStats: ReadingStats,
            dailyGoal: Int,
        ): WidgetStats =
            WidgetStats(
                todayCount = readingStats.todayStoryCount.coerceAtLeast(0),
                dailyGoal = UserPreferenceDefaults.sanitizeDailyGoal(dailyGoal),
                currentStreak = readingStats.currentStreak.coerceAtLeast(0),
                totalStoryCount = readingStats.totalUniqueStoryCount.coerceAtLeast(0),
            )
    }
}

data class WidgetData(
    val article: WidgetArticle,
    val articleStatus: WidgetArticleStatus,
    val theme: NutsNewsAppTheme,
    val stats: WidgetStats,
    val showStatsOnLargeWidget: Boolean,
    val refreshedAt: Instant,
) {
    companion object {
        val Placeholder =
            WidgetData(
                article = WidgetArticle.Placeholder,
                articleStatus = WidgetArticleStatus.Placeholder,
                theme = NutsNewsAppTheme.Amber,
                stats =
                    WidgetStats(
                        todayCount = 2,
                        dailyGoal = 3,
                        currentStreak = 4,
                        totalStoryCount = 18,
                    ),
                showStatsOnLargeWidget = true,
                refreshedAt = Instant.EPOCH,
            )
    }
}
