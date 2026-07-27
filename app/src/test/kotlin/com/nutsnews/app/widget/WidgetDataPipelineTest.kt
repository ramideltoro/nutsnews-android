package com.nutsnews.app.widget

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.article.ArticleFetchResult
import com.nutsnews.app.data.article.ArticleFetchSource
import com.nutsnews.app.data.article.DiskArticleResponseCache
import com.nutsnews.app.data.article.FeedArticleSource
import com.nutsnews.app.data.article.NutsNewsFetchPolicy
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import java.io.IOException
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WidgetDataPipelineTest {
    @JvmField
    @Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun freshArticleIsNormalizedCachedAndCombinedWithCurrentWidgetState() =
        runTest {
            val source =
                FakeWidgetArticleSource(
                    result =
                        widgetFetchResult(
                            article =
                                widgetArticle(
                                    summary = " ",
                                    source = "  Good News Network  ",
                                    categories = listOf("  Community  "),
                                ),
                            source = ArticleFetchSource.Network,
                        ),
                )
            val store = InMemoryWidgetArticleStore()
            val preferences =
                InMemoryUserPreferencesRepository(
                    UserPreferences(
                        theme = NutsNewsAppTheme.Foxy,
                        dailyGoal = 2,
                        showStatsOnLargeWidget = false,
                    ),
                )
            val stats =
                FakeWidgetReadingStatsRepository(
                    ReadingStats(
                        todayStoryCount = 3,
                        originalOpensTodayCount = 1,
                        totalUniqueStoryCount = 9,
                        currentStreak = 4,
                        recentDays = emptyList(),
                    ),
                )
            val pipeline =
                widgetPipeline(
                    source = source,
                    store = store,
                    preferences = preferences,
                    stats = stats,
                )

            val data = pipeline.load(forceRefresh = true)

            assertEquals(WidgetArticleStatus.Current, data.articleStatus)
            assertEquals("A neighborhood planted a new garden", data.article.title)
            assertEquals(WidgetArticle.DefaultSummary, data.article.summary)
            assertEquals("Good News Network", data.article.source)
            assertEquals("Community", data.article.mood)
            assertEquals(data.article, store.article)
            assertEquals(NutsNewsFetchPolicy.ReloadIgnoringCache, source.lastFetchPolicy)
            assertEquals(NutsNewsAppTheme.Foxy, data.theme)
            assertEquals(3, data.stats.todayCount)
            assertEquals(2, data.stats.dailyGoal)
            assertEquals("2/2", data.stats.progressText)
            assertEquals(1f, data.stats.progressFraction)
            assertEquals(4, data.stats.currentStreak)
            assertEquals(9, data.stats.totalStoryCount)
            assertFalse(data.showStatsOnLargeWidget)
            assertEquals(FixedInstant, data.refreshedAt)
        }

    @Test
    fun offlineRequestUsesTheLastNormalizedArticleAsStaleFallback() =
        runTest {
            val cached =
                WidgetArticle(
                    storyId = "cached-story",
                    title = "Neighbors reopen a beloved park",
                    summary = "A cached positive story.",
                    source = "NutsNews",
                    mood = "Community",
                )
            val pipeline =
                widgetPipeline(
                    source =
                        FakeWidgetArticleSource(
                            failure = IOException("offline"),
                        ),
                    store = InMemoryWidgetArticleStore(cached),
                )

            val data = pipeline.load()

            assertEquals(WidgetArticleStatus.Stale, data.articleStatus)
            assertEquals(cached, data.article)
        }

    @Test
    fun upstreamStaleResponseRemainsMarkedStaleAndIsNotRecachedAsFresh() =
        runTest {
            val store = InMemoryWidgetArticleStore()
            val staleArticle = widgetArticle(title = "A cached API story")
            val pipeline =
                widgetPipeline(
                    source =
                        FakeWidgetArticleSource(
                            result =
                                widgetFetchResult(
                                    article = staleArticle,
                                    source = ArticleFetchSource.StaleCache,
                                ),
                        ),
                    store = store,
                )

            val data = pipeline.load()

            assertEquals(WidgetArticleStatus.Stale, data.articleStatus)
            assertEquals(staleArticle.title, data.article.title)
            assertEquals(0, store.writeCount)
        }

    @Test
    fun emptyResponseUsesDeterministicRuntimeFallbackAndPreviewPlaceholder() =
        runTest {
            val pipeline =
                widgetPipeline(
                    source =
                        FakeWidgetArticleSource(
                            result =
                                ArticleFetchResult(
                                    response =
                                        ArticlesResponse(
                                            articles = emptyList(),
                                            nextPage = null,
                                        ),
                                    source = ArticleFetchSource.Network,
                                ),
                        ),
                )

            val data = pipeline.load()

            assertEquals(WidgetArticleStatus.Fallback, data.articleStatus)
            assertSame(WidgetArticle.Fallback, data.article)
            assertEquals(
                "Open NutsNews for today’s positive story",
                data.article.title,
            )
            assertEquals(WidgetData.Placeholder, pipeline.placeholder())
            assertEquals(
                "Your daily good-news reset is ready",
                pipeline.placeholder().article.title,
            )
        }

    @Test
    fun corruptNormalizedArticleCacheIsEvictedBeforeFallback() =
        runTest {
            val responseCache =
                DiskArticleResponseCache(
                    directory = temporaryFolder.root.toPath(),
                    clock = FixedClock,
                )
            responseCache.write(
                key = ResponseCacheWidgetArticleStore.CacheKey,
                response = """{"schemaVersion":1,"title":null}""",
            )
            val pipeline =
                widgetPipeline(
                    source =
                        FakeWidgetArticleSource(
                            failure = IOException("offline"),
                        ),
                    store = ResponseCacheWidgetArticleStore(responseCache),
                )

            val data = pipeline.load()

            assertEquals(WidgetArticleStatus.Fallback, data.articleStatus)
            assertEquals(WidgetArticle.Fallback, data.article)
            assertNull(
                responseCache.read(
                    key = ResponseCacheWidgetArticleStore.CacheKey,
                    maxAge = null,
                ),
            )
        }

    @Test
    fun normalizedArticleCacheRoundTripsCompleteUnicodeContent() =
        runTest {
            val responseCache =
                DiskArticleResponseCache(
                    directory = temporaryFolder.root.toPath(),
                    clock = FixedClock,
                )
            val store = ResponseCacheWidgetArticleStore(responseCache)
            val article =
                WidgetArticle(
                    storyId = "https://example.com/mañana",
                    title = "Neighbors share alegría 🌱",
                    summary = "A brighter local moment.",
                    source = "NutsNews",
                    mood = "Community",
                )

            store.write(article)

            assertEquals(article, store.read())
        }

    @Test
    fun preferenceAndReadingUpdatesAreReflectedWithoutRebuildingPipeline() =
        runTest {
            val preferences = InMemoryUserPreferencesRepository()
            val stats = FakeWidgetReadingStatsRepository(EmptyStats)
            val pipeline =
                widgetPipeline(
                    source =
                        FakeWidgetArticleSource(
                            result = widgetFetchResult(widgetArticle()),
                        ),
                    preferences = preferences,
                    stats = stats,
                )

            val initial = pipeline.load()
            assertEquals(NutsNewsAppTheme.Amber, initial.theme)
            assertEquals(3, initial.stats.dailyGoal)
            assertEquals(0, initial.stats.todayCount)
            assertTrue(initial.showStatsOnLargeWidget)

            preferences.updatePreferences { current ->
                current.copy(
                    theme = NutsNewsAppTheme.Sakura,
                    dailyGoal = 5,
                    showStatsOnLargeWidget = false,
                )
            }
            stats.state.value =
                EmptyStats.copy(
                    todayStoryCount = 2,
                    totalUniqueStoryCount = 12,
                    currentStreak = 6,
                )

            val updated = pipeline.load()
            assertEquals(NutsNewsAppTheme.Sakura, updated.theme)
            assertEquals(5, updated.stats.dailyGoal)
            assertEquals(2, updated.stats.todayCount)
            assertEquals("2/5", updated.stats.progressText)
            assertEquals(0.4f, updated.stats.progressFraction)
            assertEquals(6, updated.stats.currentStreak)
            assertEquals(12, updated.stats.totalStoryCount)
            assertFalse(updated.showStatsOnLargeWidget)
        }

    private fun widgetPipeline(
        source: FeedArticleSource,
        store: WidgetArticleStore = InMemoryWidgetArticleStore(),
        preferences: InMemoryUserPreferencesRepository =
            InMemoryUserPreferencesRepository(),
        stats: FakeWidgetReadingStatsRepository =
            FakeWidgetReadingStatsRepository(EmptyStats),
    ): DefaultWidgetDataPipeline =
        DefaultWidgetDataPipeline(
            articleSource = source,
            articleStore = store,
            userPreferencesRepository = preferences,
            readingStatsRepository = stats,
            clock = FixedClock,
        )
}

private class FakeWidgetArticleSource(
    private val result: ArticleFetchResult? = null,
    private val failure: Exception? = null,
) : FeedArticleSource {
    var lastFetchPolicy: NutsNewsFetchPolicy? = null

    override suspend fun fetchFeedPage(
        page: Int,
        category: String?,
        fetchPolicy: NutsNewsFetchPolicy,
    ): ArticleFetchResult {
        assertEquals(0, page)
        assertNull(category)
        lastFetchPolicy = fetchPolicy
        failure?.let { throw it }
        return checkNotNull(result)
    }
}

private class InMemoryWidgetArticleStore(
    var article: WidgetArticle? = null,
) : WidgetArticleStore {
    var writeCount = 0

    override suspend fun read(): WidgetArticle? = article

    override suspend fun write(article: WidgetArticle) {
        writeCount += 1
        this.article = article
    }
}

private class FakeWidgetReadingStatsRepository(
    initialStats: ReadingStats,
) : ReadingStatsRepository {
    val state = MutableStateFlow(initialStats)

    override fun observeStats(recentDayCount: Int): Flow<ReadingStats> = state

    override suspend fun recordStoryOpen(article: Article) = Unit

    override suspend fun recordOriginalStoryOpen() = Unit

    override suspend fun lastOpenedAt(storyId: StoryId): Instant? = null
}

private fun widgetFetchResult(
    article: Article,
    source: ArticleFetchSource = ArticleFetchSource.Network,
): ArticleFetchResult =
    ArticleFetchResult(
        response =
            ArticlesResponse(
                articles = listOf(article),
                nextPage = 1,
            ),
        source = source,
    )

private fun widgetArticle(
    title: String = "A neighborhood planted a new garden",
    summary: String = "Residents created a welcoming green space.",
    source: String = "NutsNews",
    categories: List<String> = listOf("Community"),
): Article =
    Article(
        id = "widget-story",
        title = title,
        summary = summary,
        originalUrl = URI("https://example.com/widget-story"),
        source = source,
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = null,
        categories = categories,
    )

private val EmptyStats =
    ReadingStats(
        todayStoryCount = 0,
        originalOpensTodayCount = 0,
        totalUniqueStoryCount = 0,
        currentStreak = 0,
        recentDays = emptyList(),
    )

private val FixedInstant = Instant.parse("2026-07-26T12:00:00Z")
private val FixedClock = Clock.fixed(FixedInstant, ZoneOffset.UTC)
