package com.nutsnews.app.data.article

import com.nutsnews.app.core.network.HttpResponse
import com.nutsnews.app.data.network.OkHttpTransport
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test

class NutsNewsApiClientTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    @Test
    fun successDecodesFixtureAndSendsGetWithPage() =
        runBlocking {
            serverRule.server.enqueue(
                MockResponse(body = fixture("articles-camel.json")),
            )

            val response = client().fetchArticles(page = 4)
            val request = serverRule.server.takeRequest()

            assertEquals(2, response.articles.size)
            assertEquals(2, response.nextPage)
            assertEquals("GET", request.method)
            assertEquals("/api/articles?page=4", request.target)
            assertEquals("application/json", request.headers["Accept"])
        }

    @Test
    fun categoryQueryPreservesIosValueAndUsesUrlEncoding() =
        runBlocking {
            serverRule.server.enqueue(
                MockResponse(body = """{"articles":[],"nextPage":null}"""),
            )

            client().fetchArticles(page = -2, category = " Science & Nature ")
            val request = serverRule.server.takeRequest()

            assertEquals("-2", request.url.queryParameter("page"))
            assertEquals(" Science & Nature ", request.url.queryParameter("category"))
            assertEquals(
                "/api/articles?page=-2&category=%20Science%20%26%20Nature%20",
                request.target,
            )
        }

    @Test
    fun blankCategoryIsOmitted() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = """{"articles":[]}"""))

            client().fetchArticles(category = " \n ")
            val request = serverRule.server.takeRequest()

            assertEquals("0", request.url.queryParameter("page"))
            assertNull(request.url.queryParameter("category"))
        }

    @Test
    fun nonSuccessStatusMapsToStructuredStatusError() =
        runBlocking {
            serverRule.server.enqueue(
                MockResponse(code = 503, body = """{"error":"unavailable"}"""),
            )

            val error =
                assertFailsWith<NutsNewsApiException.HttpStatus> {
                    client().fetchArticles()
                }

            assertEquals(503, error.statusCode)
            assertEquals("The NutsNews API returned status code 503.", error.message)
        }

    @Test
    fun delayedMockResponseMapsCallTimeout() =
        runBlocking {
            serverRule.server.enqueue(
                MockResponse
                    .Builder()
                    .body("""{"articles":[]}""")
                    .bodyDelay(1, TimeUnit.SECONDS)
                    .build(),
            )
            val timedClient =
                OkHttpClient
                    .Builder()
                    .callTimeout(100, TimeUnit.MILLISECONDS)
                    .build()

            val error =
                assertFailsWith<NutsNewsApiException.Timeout> {
                    client(transport = OkHttpTransport(timedClient)).fetchArticles()
                }

            assertIs<IOException>(error.cause)
            Unit
        }

    @Test
    fun cancellingInFlightRequestPropagatesWithoutMappingItToTimeout() =
        runBlocking {
            serverRule.server.enqueue(
                MockResponse
                    .Builder()
                    .body("""{"articles":[{"id":"too-late"}]}""")
                    .bodyDelay(30, TimeUnit.SECONDS)
                    .build(),
            )
            val httpClient =
                OkHttpClient
                    .Builder()
                    .callTimeout(30, TimeUnit.SECONDS)
                    .build()
            val pending =
                async(Dispatchers.IO) {
                    client(transport = OkHttpTransport(httpClient))
                        .fetchArticles(fetchPolicy = NutsNewsFetchPolicy.ReloadIgnoringCache)
                }

            val request = serverRule.server.takeRequest()
            pending.cancel()

            assertEquals("/api/articles?page=0", request.target)
            assertFailsWith<CancellationException> { pending.await() }
            assertTrue(pending.isCancelled)
        }

    @Test
    fun malformedResponseMapsToStructuredDecodingError() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = "{"))

            val error =
                assertFailsWith<NutsNewsApiException.Decoding> {
                    client().fetchArticles()
                }

            assertIs<ArticleDecodingException>(error.cause)
            Unit
        }

    @Test
    fun transportFailuresAndInvalidResponsesAreMapped() =
        runBlocking {
            val timeout =
                assertFailsWith<NutsNewsApiException.Timeout> {
                    client(transport = { throw SocketTimeoutException("test") }).fetchArticles()
                }
            assertIs<SocketTimeoutException>(timeout.cause)

            val network =
                assertFailsWith<NutsNewsApiException.Network> {
                    client(transport = { throw IOException("test") }).fetchArticles()
                }
            assertIs<IOException>(network.cause)

            assertFailsWith<NutsNewsApiException.InvalidResponse> {
                client(transport = { HttpResponse(statusCode = 200, body = null) }).fetchArticles()
            }
            assertFailsWith<NutsNewsApiException.InvalidResponse> {
                client(transport = { HttpResponse(statusCode = 0, body = "") }).fetchArticles()
            }
            Unit
        }

    @Test
    fun productionEndpointsAndDefaultTimeoutRemainCentralized() {
        assertEquals(
            "https://www.nutsnews.com/api/articles",
            NutsNewsEndpoints.Production.articles,
        )
        assertEquals(
            "https://www.nutsnews.com/api/search",
            NutsNewsEndpoints.Production.archiveSearch,
        )

        val client = OkHttpTransport.createDefaultClient()
        val expectedMillis = TimeUnit.SECONDS.toMillis(OkHttpTransport.DefaultTimeoutSeconds)
        assertEquals(expectedMillis, client.callTimeoutMillis.toLong())
        assertEquals(expectedMillis, client.connectTimeoutMillis.toLong())
        assertEquals(expectedMillis, client.readTimeoutMillis.toLong())
        assertEquals(expectedMillis, client.writeTimeoutMillis.toLong())
    }

    @Test
    fun invalidEndpointMapsBeforeTransport() =
        runBlocking {
            assertFailsWith<NutsNewsApiException.InvalidUrl> {
                NutsNewsApiClient(
                    endpoints =
                        NutsNewsEndpoints(
                            articles = "not a URL",
                            archiveSearch = NutsNewsEndpoints.Production.archiveSearch,
                        ),
                    transport = { error("Transport should not run.") },
                ).fetchArticles()
            }
            Unit
        }

    private fun client(
        transport: com.nutsnews.app.core.network.HttpTransport = OkHttpTransport(),
    ): NutsNewsApiClient =
        NutsNewsApiClient(
            endpoints =
                NutsNewsEndpoints(
                    articles = serverRule.server.url("/api/articles").toString(),
                    archiveSearch = serverRule.server.url("/api/search").toString(),
                ),
            transport = transport,
        )

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name")) {
            "Missing fixture: $name"
        }.readText()
}
