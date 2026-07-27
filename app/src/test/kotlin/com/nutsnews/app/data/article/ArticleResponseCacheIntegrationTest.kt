package com.nutsnews.app.data.article

import com.nutsnews.app.data.network.OkHttpTransport
import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArticleResponseCacheIntegrationTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    @JvmField
    @Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun feedUsesFifteenMinuteFreshnessAndNormalizedCategoryKey() =
        runBlocking {
            val fixture = fixture("articles-camel.json")
            serverRule.server.enqueue(MockResponse(body = fixture))

            val first = client().fetchArticles(page = 0, category = " Science ")
            clock.advance(Duration.ofMinutes(14).plusSeconds(59))
            val fresh = client().fetchArticles(page = 0, category = "science")

            assertEquals(first, fresh)
            assertEquals(1, serverRule.server.requestCount)

            clock.advance(Duration.ofSeconds(2))
            serverRule.server.enqueue(MockResponse(body = fixture("articles-snake.json")))
            val refreshed = client().fetchArticles(page = 0, category = "SCIENCE")

            assertEquals("42", refreshed.articles.first().id)
            assertEquals(2, serverRule.server.requestCount)
        }

    @Test
    fun searchUsesFiveMinuteFreshnessAndNormalizedQueryKey() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = fixture("articles-camel.json")))

            val first = client().searchArticles(query = "Good News", page = 0, limit = 20)
            clock.advance(Duration.ofMinutes(4).plusSeconds(59))
            val fresh = client().searchArticles(query = " good   news ", page = 0, limit = 20)

            assertEquals(first, fresh)
            assertEquals(1, serverRule.server.requestCount)

            clock.advance(Duration.ofSeconds(2))
            serverRule.server.enqueue(MockResponse(body = fixture("articles-snake.json")))
            val refreshed = client().searchArticles(query = "GOOD NEWS", page = 0, limit = 20)

            assertEquals("42", refreshed.articles.first().id)
            assertEquals(2, serverRule.server.requestCount)
        }

    @Test
    fun forcedRefreshBypassesFreshDataAndReplacesIt() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = fixture("articles-camel.json")))
            client().fetchArticles()

            serverRule.server.enqueue(MockResponse(body = fixture("articles-snake.json")))
            val forced =
                client().fetchArticles(
                    fetchPolicy = NutsNewsFetchPolicy.ReloadIgnoringCache,
                )
            val cached = client().fetchArticles()

            assertEquals("42", forced.articles.first().id)
            assertEquals(forced, cached)
            assertEquals(2, serverRule.server.requestCount)
        }

    @Test
    fun staleFeedFallsBackAfterNetworkFailure() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = fixture("articles-camel.json")))
            val expected = client().fetchFeedPage()
            assertEquals(ArticleFetchSource.Network, expected.source)
            val freshCache = client().fetchFeedPage()
            assertEquals(expected.response, freshCache.response)
            assertEquals(ArticleFetchSource.FreshCache, freshCache.source)
            clock.advance(Duration.ofMinutes(16))
            serverRule.server.enqueue(MockResponse(code = 503, body = """{"error":"offline"}"""))

            val offline = client().fetchFeedPage()

            assertEquals(expected.response, offline.response)
            assertEquals(ArticleFetchSource.StaleCache, offline.source)
            assertEquals(true, offline.isStale)
            assertEquals(2, serverRule.server.requestCount)
        }

    @Test
    fun offlineProcessRestartReopensDiskCachedFeedAndSearch() =
        runBlocking {
            val directory =
                temporaryFolder.root
                    .toPath()
                    .resolve("restart-cache")
            val endpoints =
                NutsNewsEndpoints(
                    articles = serverRule.server.url("/api/articles").toString(),
                    archiveSearch = serverRule.server.url("/api/search").toString(),
                )
            serverRule.server.enqueue(MockResponse(body = fixture("articles-camel.json")))
            serverRule.server.enqueue(MockResponse(body = fixture("articles-snake.json")))
            val online =
                NutsNewsApiClient(
                    endpoints = endpoints,
                    transport = OkHttpTransport(),
                    responseCache = DiskArticleResponseCache(directory, clock),
                )
            val expectedFeed = online.fetchFeedPage()
            val expectedSearch = online.searchArticles(query = "community")
            clock.advance(Duration.ofMinutes(20))

            val restartedOffline =
                NutsNewsApiClient(
                    endpoints = endpoints,
                    transport = {
                        throw IOException("Device is offline after process restart.")
                    },
                    responseCache = DiskArticleResponseCache(directory, clock),
                )

            val restoredFeed = restartedOffline.fetchFeedPage()
            val restoredSearch = restartedOffline.searchArticles(query = "community")

            assertEquals(ArticleFetchSource.StaleCache, restoredFeed.source)
            assertEquals(expectedFeed.response, restoredFeed.response)
            assertEquals(expectedSearch, restoredSearch)
            assertEquals(2, serverRule.server.requestCount)
        }

    @Test
    fun forcedSearchRefreshStillFallsBackToLastKnownGoodData() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = fixture("articles-snake.json")))
            val expected = client().searchArticles(query = "community")
            serverRule.server.enqueue(MockResponse(code = 500, body = """{"error":"offline"}"""))

            val offline =
                client().searchArticles(
                    query = "COMMUNITY",
                    fetchPolicy = NutsNewsFetchPolicy.ReloadIgnoringCache,
                )

            assertEquals(expected, offline)
            assertEquals(2, serverRule.server.requestCount)
        }

    @Test
    fun corruptCachedResponseIsEvictedBeforeFreshNetworkDecode() =
        runBlocking {
            val key = NutsNewsApiClient.articleCacheKey(page = 0, category = null)
            cache.write(key, "{")
            serverRule.server.enqueue(MockResponse(body = fixture("articles-camel.json")))

            val response = client().fetchArticles()

            assertEquals("story-camel", response.articles.first().id)
            assertEquals(1, serverRule.server.requestCount)
            assertEquals(
                fixture("articles-camel.json"),
                cache.read(key, maxAge = null),
            )
        }

    @Test
    fun corruptCacheCannotMaskAnOfflineFailure() =
        runBlocking {
            val key = NutsNewsApiClient.articleCacheKey(page = 3, category = "Science")
            cache.write(key, "{")
            serverRule.server.enqueue(MockResponse(code = 502, body = """{"error":"offline"}"""))

            assertFailsWith<NutsNewsApiException.HttpStatus> {
                client().fetchArticles(page = 3, category = "science")
            }
            assertEquals(null, cache.read(key, maxAge = null))
        }

    @Test
    fun cacheKeysMatchIosNormalization() {
        assertEquals(
            "articles:v1:page=-1:category=all",
            NutsNewsApiClient.articleCacheKey(page = -1, category = " \n "),
        )
        assertEquals(
            "articles:v1:page=2:category=science & nature",
            NutsNewsApiClient.articleCacheKey(page = 2, category = " Science & Nature "),
        )

        val request =
            ArchiveSearchRequest.create(
                query = "  GOOD   News ",
                page = -4,
                limit = 99,
            )
        assertEquals(
            "search:v1:q=good news:page=0:limit=50",
            NutsNewsApiClient.searchCacheKey(request),
        )
        assertEquals(Duration.ofMinutes(15), NutsNewsApiClient.FeedFreshness)
        assertEquals(Duration.ofMinutes(5), NutsNewsApiClient.SearchFreshness)
    }

    private val clock = MutableClock(Instant.parse("2026-07-26T12:00:00Z"))
    private val cache: DiskArticleResponseCache by lazy {
        DiskArticleResponseCache(
            directory =
                temporaryFolder.root
                    .toPath()
                    .resolve(DiskArticleResponseCache.DirectoryName),
            clock = clock,
        )
    }

    private fun client(): NutsNewsApiClient =
        NutsNewsApiClient(
            endpoints =
                NutsNewsEndpoints(
                    articles = serverRule.server.url("/api/articles").toString(),
                    archiveSearch = serverRule.server.url("/api/search").toString(),
                ),
            transport = OkHttpTransport(),
            responseCache = cache,
        )

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name")) {
            "Missing fixture: $name"
        }.readText()
}
