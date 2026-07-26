package com.nutsnews.app.data.article

import com.nutsnews.app.data.network.OkHttpTransport
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.junit.Rule
import org.junit.Test

class ArchiveSearchApiClientTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    @Test
    fun normalizesQueryAndClampsHighBounds() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = fixture("articles-camel.json")))

            val response =
                client().searchArticles(
                    query = " \n  red\t pandas   ",
                    page = -8,
                    limit = 99,
                )
            val request = serverRule.server.takeRequest()

            assertEquals(2, response.articles.size)
            assertEquals("/api/search?q=red%20pandas&page=0&limit=50", request.target)
            assertEquals("red pandas", request.url.queryParameter("q"))
            assertEquals("0", request.url.queryParameter("page"))
            assertEquals("50", request.url.queryParameter("limit"))
        }

    @Test
    fun clampsLowLimitAndPreservesRequestedPage() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = """{"articles":[],"next_page":7}"""))

            val response =
                client().searchArticles(
                    query = "science",
                    page = 6,
                    limit = 0,
                )
            val request = serverRule.server.takeRequest()

            assertEquals(7, response.nextPage)
            assertEquals("/api/search?q=science&page=6&limit=1", request.target)
        }

    @Test
    fun twoGraphemeMinimumReturnsEmptyWithoutNetwork() =
        runBlocking {
            val ascii = client().searchArticles(query = " a ")
            val emoji = client().searchArticles(query = "👨‍👩‍👧‍👦")

            assertEquals(emptyList(), ascii.articles)
            assertNull(ascii.nextPage)
            assertEquals(emptyList(), emoji.articles)
            assertEquals(0, serverRule.server.requestCount)
        }

    @Test
    fun loadingThenPageOutcomeExposesPagination() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = fixture("articles-snake.json")))

            val outcomes =
                client()
                    .searchOutcomes(query = "  kind   science ", page = 2, limit = 20)
                    .toList()

            val loading = assertIs<ArchiveSearchOutcome.Loading>(outcomes[0])
            assertEquals("kind science", loading.query)
            assertTrue(loading.isPagination)

            val page = assertIs<ArchiveSearchOutcome.Page>(outcomes[1])
            assertEquals(2, page.articles.size)
            assertEquals(3, page.nextPage)
            assertTrue(page.canLoadMore)
            assertTrue(page.isPagination)
        }

    @Test
    fun emptyOutcomesDistinguishShortQueriesAndNoMatches() =
        runBlocking {
            val shortOutcomes = client().searchOutcomes(query = "x").toList()

            val short = assertIs<ArchiveSearchOutcome.Empty>(shortOutcomes.single())
            assertEquals(ArchiveSearchOutcome.EmptyReason.QueryTooShort, short.reason)
            assertEquals(0, serverRule.server.requestCount)

            serverRule.server.enqueue(MockResponse(body = """{"articles":[],"nextPage":null}"""))
            val emptyOutcomes = client().searchOutcomes(query = "nothing").toList()

            assertIs<ArchiveSearchOutcome.Loading>(emptyOutcomes[0])
            val empty = assertIs<ArchiveSearchOutcome.Empty>(emptyOutcomes[1])
            assertEquals(ArchiveSearchOutcome.EmptyReason.NoMatches, empty.reason)
            assertFalse(empty.isPagination)
        }

    @Test
    fun serverFailureBecomesStructuredFailureOutcome() =
        runBlocking {
            serverRule.server.enqueue(
                MockResponse(code = 429, body = """{"error":"slow down"}"""),
            )

            val outcomes = client().searchOutcomes(query = "community").toList()

            assertIs<ArchiveSearchOutcome.Loading>(outcomes[0])
            val failure = assertIs<ArchiveSearchOutcome.Failure>(outcomes[1])
            val status = assertIs<NutsNewsApiException.HttpStatus>(failure.error)
            assertEquals(429, status.statusCode)
            assertFalse(failure.isPagination)
        }

    @Test
    fun malformedSearchResponseBecomesFailureOutcome() =
        runBlocking {
            serverRule.server.enqueue(MockResponse(body = "{"))

            val outcomes = client().searchOutcomes(query = "animals").toList()

            assertIs<ArchiveSearchOutcome.Loading>(outcomes[0])
            val failure = assertIs<ArchiveSearchOutcome.Failure>(outcomes[1])
            assertIs<NutsNewsApiException.Decoding>(failure.error)
            Unit
        }

    @Test
    fun queryHelpersMatchIosWhitespaceAndCharacterRules() {
        fun request(query: String): ArchiveSearchRequest =
            ArchiveSearchRequest.create(query = query)

        assertEquals(
            "good news today",
            request("\t good \n news\u2003today \r").query,
        )
        assertFalse(request("").meetsMinimum)
        assertFalse(request("é").meetsMinimum)
        assertFalse(request("e\u0301").meetsMinimum)
        assertFalse(request("👨‍👩‍👧‍👦").meetsMinimum)
        assertFalse(request("👍🏽").meetsMinimum)
        assertFalse(request("🇺🇸").meetsMinimum)
        assertTrue(request("éa").meetsMinimum)
        assertTrue(request("😀a").meetsMinimum)
        assertTrue(request("🇺🇸a").meetsMinimum)
    }

    private fun client(): NutsNewsApiClient =
        NutsNewsApiClient(
            endpoints =
                NutsNewsEndpoints(
                    articles = serverRule.server.url("/api/articles").toString(),
                    archiveSearch = serverRule.server.url("/api/search").toString(),
                ),
            transport = OkHttpTransport(),
        )

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name")) {
            "Missing fixture: $name"
        }.readText()
}
