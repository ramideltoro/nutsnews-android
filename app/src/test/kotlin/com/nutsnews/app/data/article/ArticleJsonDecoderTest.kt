package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import java.net.URI
import java.time.ZoneId
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.Test

class ArticleJsonDecoderTest {
    @Test
    fun camelCaseFixturePreservesPrecedenceAndCleansCategories() {
        val response = ArticleJsonDecoder.decodeResponse(fixture("articles-camel.json"))

        assertEquals(2, response.nextPage)
        assertEquals(2, response.articles.size)

        val article = response.articles[0]
        assertEquals("story-camel", article.id)
        assertEquals("A bright story", article.title)
        assertEquals("Primary summary", article.summary)
        assertEquals(URI("https://example.com/camel"), article.originalUrl)
        assertEquals("Example Daily", article.source)
        assertEquals("2026-07-04T23:30:15.123Z", article.publishedAt)
        assertEquals("2026-07-05T00:00:00Z", article.createdAt)
        assertEquals(URI("https://images.example.com/thumbnail-camel.jpg"), article.thumbnailUrl)
        assertEquals(listOf("Community", "Science"), article.categories)

        val imageFallback = response.articles[1]
        assertEquals("Camel AI summary", imageFallback.summary)
        assertEquals("2026-06-01T12:00:00Z", imageFallback.createdAt)
        assertEquals(URI("https://images.example.com/image-camel.jpg"), imageFallback.thumbnailUrl)
        assertEquals(
            listOf("Kindness", "Animals", "Wellness", "Achievement", "Science"),
            imageFallback.categories,
        )
    }

    @Test
    fun snakeCaseFixtureSupportsNumericIdsAndFallbackFields() {
        val response = ArticleJsonDecoder.decodeResponse(fixture("articles-snake.json"))

        assertEquals(3, response.nextPage)

        val article = response.articles[0]
        assertEquals("42", article.id)
        assertEquals("Snake AI summary", article.summary)
        assertEquals(URI("https://example.com/snake"), article.originalUrl)
        assertEquals("2025-01-02T03:04:05Z", article.publishedAt)
        assertEquals("2025-01-03T03:04:05Z", article.createdAt)
        assertEquals(URI("https://images.example.com/thumbnail-snake.jpg"), article.thumbnailUrl)
        assertEquals(listOf("Uplifting"), article.categories)

        val imageFallback = response.articles[1]
        assertEquals("-9", imageFallback.id)
        assertEquals("2024-02-29T18:00:00.987654Z", imageFallback.createdAt)
        assertEquals(URI("https://images.example.com/image-snake.jpg"), imageFallback.thumbnailUrl)
        assertEquals(listOf(Article.DefaultCategoryLabel), imageFallback.categories)
    }

    @Test
    fun fallbackFixtureMatchesIosDefaultsAndStableIdentityPrecedence() {
        val articles = ArticleJsonDecoder.decodeResponse(fixture("articles-fallbacks.json")).articles

        assertEquals("https://example.com/original", articles[0].stableId.value)
        assertEquals("not-a-date", articles[0].displayDate(Locale.US, ZoneId.of("UTC")))

        assertEquals("api-only", articles[1].stableId.value)
        assertEquals(Article.RecentlyLabel, articles[1].displayDate(Locale.US, ZoneId.of("UTC")))
        assertEquals(listOf(Article.DefaultCategoryLabel), articles[1].categories)

        assertEquals("Fallback summary", articles[2].summary)
        assertEquals(Article.DefaultSourceLabel, articles[2].source)
        assertEquals(URI("https://example.com/snake-fallback"), articles[2].originalUrl)
        assertEquals(URI("https://images.example.com/fallback.jpg"), articles[2].thumbnailUrl)
        assertEquals("https://example.com/snake-fallback", articles[2].stableId.value)
        assertEquals(listOf(Article.DefaultCategoryLabel), articles[2].categories)

        assertEquals("", articles[3].id)
        assertEquals(Article.UntitledLabel, articles[3].title)
        assertEquals(Article.DefaultSourceLabel, articles[3].source)
        assertEquals("untitled story", articles[3].stableId.value)
        assertEquals(listOf("News", "SCIENCE"), articles[3].categories)

        assertEquals("", articles[4].id)
        assertNull(articles[4].originalUrl)
        assertEquals("fractional ids are unsupported", articles[4].stableId.value)
    }

    @Test
    fun datePresentationUsesPublishedThenCreatedAndLocalizedMediumStyle() {
        val camel = ArticleJsonDecoder.decodeResponse(fixture("articles-camel.json")).articles
        val snake = ArticleJsonDecoder.decodeResponse(fixture("articles-snake.json")).articles
        val fallback = ArticleJsonDecoder.decodeResponse(fixture("articles-fallbacks.json")).articles

        assertEquals(
            "Jul 4, 2026",
            camel[0].displayDate(locale = Locale.US, zoneId = ZoneId.of("UTC")),
        )
        assertEquals(
            "Jan 2, 2025",
            snake[0].displayDate(locale = Locale.US, zoneId = ZoneId.of("UTC")),
        )
        assertEquals(
            "Feb 29, 2024",
            snake[1].displayDate(locale = Locale.US, zoneId = ZoneId.of("UTC")),
        )
        assertEquals(
            "Jan 2, 2026",
            fallback[3].displayDate(locale = Locale.US, zoneId = ZoneId.of("UTC")),
        )
    }

    @Test
    fun responseFallbacksAndInvalidJsonAreDeterministic() {
        assertEquals(emptyList(), ArticleJsonDecoder.decodeResponse("{}").articles)
        assertEquals(
            emptyList(),
            ArticleJsonDecoder
                .decodeResponse("""{"articles":[{"id":"valid"},null]}""")
                .articles,
        )
        assertNull(ArticleJsonDecoder.decodeResponse("""{"nextPage":"2"}""").nextPage)
        assertEquals(
            4,
            ArticleJsonDecoder.decodeResponse("""{"nextPage":4,"next_page":5}""").nextPage,
        )
        assertEquals(
            Article.UntitledLabel,
            ArticleJsonDecoder.decodeArticle("""{"id":"single"}""").title,
        )
        assertFailsWith<ArticleDecodingException> {
            ArticleJsonDecoder.decodeResponse("[]")
        }
        assertFailsWith<ArticleDecodingException> {
            ArticleJsonDecoder.decodeResponse("{")
        }
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name")) {
            "Missing fixture: $name"
        }.readText()
}
