package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class ArticleStateCodecTest {
    @Test
    fun savedStateRoundTripPreservesArticleFieldsAndPagination() {
        val article =
            Article(
                id = "story-ñ",
                title = "Neighbors reopen a community garden 🌱",
                summary = "The restored space is free for every family.",
                originalUrl = URI("https://example.com/garden?day=mañana"),
                source = "Community Wire",
                publishedAt = "2026-07-26T12:00:00Z",
                createdAt = "2026-07-26T12:01:00Z",
                thumbnailUrl = URI("https://example.com/garden.jpg"),
                categories = listOf("Community", "Creativity"),
            )
        val response = ArticlesResponse(listOf(article), nextPage = 3)

        assertEquals(
            response,
            ArticleStateCodec.decodeOrNull(ArticleStateCodec.encode(response)),
        )
        assertNull(ArticleStateCodec.decodeOrNull("{"))
    }

    @Test
    fun savedStateSnapshotIsBoundedAndDisablesInconsistentPagination() {
        val articles =
            (1..101).map { index ->
                Article(
                    id = "story-$index",
                    title = "Story $index",
                    summary = "A hopeful update.",
                    originalUrl = URI("https://example.com/$index"),
                    source = "NutsNews",
                    publishedAt = null,
                    createdAt = null,
                    thumbnailUrl = null,
                    categories = listOf("Community"),
                )
            }

        val restored =
            requireNotNull(
                ArticleStateCodec.decodeOrNull(
                    ArticleStateCodec.encode(
                        ArticlesResponse(articles, nextPage = 6),
                    ),
                ),
            )

        assertEquals(100, restored.articles.size)
        assertNull(restored.nextPage)
    }
}
