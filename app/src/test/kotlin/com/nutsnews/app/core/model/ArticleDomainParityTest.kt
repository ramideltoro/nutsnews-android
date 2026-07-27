package com.nutsnews.app.core.model

import java.net.URI
import java.time.ZoneId
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.Test

class ArticleDomainParityTest {
    @Test
    fun stableIdentityUsesOriginalUrlThenTrimmedApiIdThenRootLowercaseTitle() {
        val article = article()

        assertEquals(
            "https://nutsnews.com/stories/shared",
            article
                .copy(
                    id = " api-id ",
                    title = "Fallback title",
                    originalUrl = URI("https://nutsnews.com/stories/shared"),
                ).stableId
                .value,
        )
        assertEquals(
            "api-id",
            article
                .copy(
                    id = " api-id ",
                    title = "Fallback title",
                    originalUrl = null,
                ).stableId
                .value,
        )
        assertEquals(
            "i\u0307yi\u0307 haber",
            article
                .copy(
                    id = " \n ",
                    title = " İYİ HABER ",
                    originalUrl = null,
                ).stableId
                .value,
        )
    }

    @Test
    fun displayDateUsesPublishedPrecedenceCallerZoneAndRawMalformedFallback() {
        val instant = "2026-07-04T23:30:15.123Z"
        val article =
            article().copy(
                publishedAt = instant,
                createdAt = "2026-01-01T00:00:00Z",
            )

        assertEquals(
            "Jul 4, 2026",
            article.displayDate(
                locale = Locale.US,
                zoneId = ZoneId.of("America/Los_Angeles"),
            ),
        )
        assertEquals(
            "Jul 5, 2026",
            article.displayDate(
                locale = Locale.US,
                zoneId = ZoneId.of("Asia/Tokyo"),
            ),
        )
        assertEquals(
            "Jan 1, 2026",
            article
                .copy(publishedAt = null)
                .displayDate(locale = Locale.US, zoneId = ZoneId.of("UTC")),
        )
        assertEquals(
            "not-a-date",
            article
                .copy(publishedAt = "not-a-date")
                .displayDate(locale = Locale.US, zoneId = ZoneId.of("UTC")),
        )
        assertEquals(
            Article.RecentlyLabel,
            article
                .copy(publishedAt = null, createdAt = null)
                .displayDate(locale = Locale.US, zoneId = ZoneId.of("UTC")),
        )
    }

    private fun article(): Article =
        Article(
            id = "story",
            title = "A hopeful story",
            summary = "Good things happened.",
            originalUrl = URI("https://nutsnews.com/story"),
            source = "NutsNews",
            publishedAt = "2026-07-25T12:00:00Z",
            createdAt = "2026-07-25T12:01:00Z",
            thumbnailUrl = URI("https://nutsnews.com/story.jpg"),
            categories = listOf("Community"),
        )
}
