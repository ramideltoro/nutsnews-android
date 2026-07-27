package com.nutsnews.app.widget

import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.data.article.ArticleFetchSource
import com.nutsnews.app.data.article.FeedArticleSource
import com.nutsnews.app.data.article.NutsNewsFetchPolicy
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.data.story.ReadingStatsRepository
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

interface WidgetDataProvider {
    suspend fun load(forceRefresh: Boolean = false): WidgetData

    fun placeholder(): WidgetData = WidgetData.Placeholder
}

class DefaultWidgetDataPipeline(
    private val articleSource: FeedArticleSource,
    private val articleStore: WidgetArticleStore,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val readingStatsRepository: ReadingStatsRepository,
    private val clock: Clock = Clock.systemUTC(),
) : WidgetDataProvider {
    override suspend fun load(forceRefresh: Boolean): WidgetData {
        val preferences =
            fallbackOnFailure(UserPreferences()) {
                userPreferencesRepository.preferences.first()
            }
        val readingStats =
            fallbackOnFailure(EmptyReadingStats) {
                readingStatsRepository.observeStats().first()
            }
        val resolvedArticle = resolveArticle(forceRefresh)

        return WidgetData(
            article = resolvedArticle.article,
            articleStatus = resolvedArticle.status,
            theme = preferences.theme,
            stats =
                WidgetStats.from(
                    readingStats = readingStats,
                    dailyGoal = preferences.dailyGoal,
                ),
            showStatsOnLargeWidget = preferences.showStatsOnLargeWidget,
            refreshedAt = clock.instant(),
        )
    }

    private suspend fun resolveArticle(forceRefresh: Boolean): ResolvedWidgetArticle =
        try {
            val result =
                articleSource.fetchFeedPage(
                    page = 0,
                    category = null,
                    fetchPolicy =
                        if (forceRefresh) {
                            NutsNewsFetchPolicy.ReloadIgnoringCache
                        } else {
                            NutsNewsFetchPolicy.UseCache
                        },
                )
            val article = result.response.articles.firstOrNull()
            if (article == null) {
                staleOrFallback()
            } else {
                val widgetArticle = WidgetArticle.fromArticle(article)
                val isStale = result.source == ArticleFetchSource.StaleCache
                if (!isStale) {
                    cacheWithoutBlocking(widgetArticle)
                }
                ResolvedWidgetArticle(
                    article = widgetArticle,
                    status =
                        if (isStale) {
                            WidgetArticleStatus.Stale
                        } else {
                            WidgetArticleStatus.Current
                        },
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            staleOrFallback()
        }

    private suspend fun staleOrFallback(): ResolvedWidgetArticle {
        val cachedArticle =
            try {
                articleStore.read()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        return if (cachedArticle == null) {
            ResolvedWidgetArticle(
                article = WidgetArticle.Fallback,
                status = WidgetArticleStatus.Fallback,
            )
        } else {
            ResolvedWidgetArticle(
                article = cachedArticle,
                status = WidgetArticleStatus.Stale,
            )
        }
    }

    private suspend fun cacheWithoutBlocking(article: WidgetArticle) {
        try {
            articleStore.write(article)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // A widget cache write must never hide a current story.
        }
    }

    private suspend fun <Value> fallbackOnFailure(
        fallback: Value,
        block: suspend () -> Value,
    ): Value =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            fallback
        }

    private data class ResolvedWidgetArticle(
        val article: WidgetArticle,
        val status: WidgetArticleStatus,
    )

    private companion object {
        val EmptyReadingStats =
            ReadingStats(
                todayStoryCount = 0,
                originalOpensTodayCount = 0,
                totalUniqueStoryCount = 0,
                currentStreak = 0,
                recentDays = emptyList(),
            )
    }
}
