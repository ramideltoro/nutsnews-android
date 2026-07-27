package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import java.net.URI
import java.time.Instant
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class DailyDigestEngineTest {
    @Test
    fun emptyFeedReturnsZeroMetricsAndNoSelections() {
        val digest =
            DailyDigestEngine.digest(
                articles = emptyList(),
                savedStoryIds = setOf(StoryId("saved-1"), StoryId("saved-2")),
                locale = Locale.US,
            )

        assertEquals(0, digest.storyCount)
        assertEquals(0, digest.uniqueSourceCount)
        assertEquals(2, digest.savedStoryCount)
        assertEquals(emptyList(), digest.categoryCounts)
        assertNull(digest.featuredArticle)
        assertNull(digest.quickReadArticle)
        assertNull(digest.worthSavingArticle)
        assertEquals(emptyList(), digest.remainingArticles)
        assertEquals(emptyList(), digest.rankedArticles)
    }

    @Test
    fun oneStoryFeedProducesOnlyTheFeaturedSelection() {
        val story =
            article(
                id = "only",
                title = "A kind community",
                source = " Wire ",
                categories = listOf("Community", "Science"),
            )
        val digest =
            DailyDigestEngine.digest(
                articles =
                    listOf(
                        article(id = "blank", title = " "),
                        article(id = "no-image", title = "No image").copy(thumbnailUrl = null),
                        story,
                    ),
                savedStoryIds = setOf(story.stableId),
                locale = Locale.US,
            )

        assertEquals(1, digest.storyCount)
        assertEquals(1, digest.uniqueSourceCount)
        assertEquals(1, digest.savedStoryCount)
        assertEquals(
            listOf(
                DigestCategoryCount("Community", 1),
                DigestCategoryCount("Science", 1),
            ),
            digest.categoryCounts,
        )
        assertEquals(story, digest.featuredArticle)
        assertNull(digest.quickReadArticle)
        assertNull(digest.worthSavingArticle)
        assertEquals(emptyList(), digest.remainingArticles)
    }

    @Test
    fun smallFeedSelectsQuickReadAndWorthSavingWithoutRepeatingFeaturedInRows() {
        val featured =
            article(
                id = "featured",
                title = "Good kind hope in the community",
                summary = "A rescue and healing milestone.",
                publishedAt = "a-date",
            )
        val savedLongRead =
            article(
                id = "saved-long",
                title = "Science report",
                summary = "x".repeat(300),
                publishedAt = "z-date",
            )
        val quickUnsaved =
            article(
                id = "quick",
                title = "Nature update",
                summary = "A short garden story.",
                publishedAt = "m-date",
            )

        val digest =
            DailyDigestEngine.digest(
                articles = listOf(savedLongRead, quickUnsaved, featured),
                savedStories =
                    listOf(
                        SavedStory(featured, Instant.parse("2026-07-26T12:00:00Z")),
                        SavedStory(savedLongRead, Instant.parse("2026-07-26T12:01:00Z")),
                    ),
                locale = Locale.US,
            )

        assertEquals(2, digest.savedStoryCount)
        assertEquals(featured, digest.featuredArticle)
        assertEquals(quickUnsaved, digest.quickReadArticle)
        assertEquals(quickUnsaved, digest.worthSavingArticle)
        assertEquals(listOf(savedLongRead), digest.remainingArticles)
        assertEquals(
            emptySet(),
            digest.remainingArticles
                .map(Article::id)
                .intersect(setOf(featured.id, quickUnsaved.id)),
        )
    }

    @Test
    fun fullFeedCapsInputMetricsCategoriesAndRemainingRows() {
        val firstTwentyFour =
            (0 until 24).map { index ->
                article(
                    id = "story-$index",
                    title =
                        if (index == 5) {
                            "Good kind hope community rescue"
                        } else {
                            "Ordinary report $index"
                        },
                    source =
                        when (index % 3) {
                            0 -> " Wire "
                            1 -> "Wire"
                            else -> "wire"
                        },
                    publishedAt = "date-${index.toString().padStart(2, '0')}",
                    categories =
                        listOf(
                            when (index % 4) {
                                0 -> "Animals"
                                1 -> "Community"
                                2 -> "Science"
                                else -> "Wellness"
                            },
                            "Shared",
                        ),
                )
            }
        val ignoredHighScore =
            article(
                id = "ignored",
                title = "Good kind hope science animal achievement",
                categories = listOf("Science"),
            )
        val digest =
            DailyDigestEngine.digest(
                articles = firstTwentyFour + ignoredHighScore,
                savedStoryIds = setOf(StoryId("library-one"), StoryId("library-two")),
                locale = Locale.US,
            )

        assertEquals(24, digest.storyCount)
        assertEquals(2, digest.uniqueSourceCount)
        assertEquals(2, digest.savedStoryCount)
        assertEquals(
            listOf(
                DigestCategoryCount("Shared", 24),
                DigestCategoryCount("Animals", 6),
                DigestCategoryCount("Community", 6),
                DigestCategoryCount("Science", 6),
                DigestCategoryCount("Wellness", 6),
            ),
            digest.categoryCounts,
        )
        assertEquals("story-5", digest.featuredArticle?.id)
        assertEquals(10, digest.remainingArticles.size)
        check(digest.rankedArticles.none { article -> article.id == "ignored" })
        check(digest.remainingArticles.none { article -> article.id == digest.featuredArticle?.id })
        check(digest.remainingArticles.none { article -> article.id == digest.quickReadArticle?.id })
    }

    @Test
    fun scoringAndDisplayDateTieBreakMatchIosWeights() {
        val weighted =
            article(
                id = "weighted",
                title = "Good science",
                summary = "Kind hope",
                categories = listOf("Science and Animals", "Community"),
            )
        assertEquals(25, DailyDigestEngine.score(weighted))

        val olderDisplayDate =
            article(id = "older", title = "Ordinary A", publishedAt = "a-date")
        val newerDisplayDate =
            article(id = "newer", title = "Ordinary B", publishedAt = "z-date")
        val digest =
            DailyDigestEngine.digest(
                articles = listOf(olderDisplayDate, newerDisplayDate),
                locale = Locale.US,
            )

        assertEquals(newerDisplayDate, digest.featuredArticle)
        assertEquals(olderDisplayDate, digest.quickReadArticle)
        assertEquals(olderDisplayDate, digest.worthSavingArticle)
        assertEquals(emptyList(), digest.remainingArticles)
    }

    @Test
    fun quickReadBoundaryAndWorthSavingFallbackAreDeterministic() {
        val featured =
            article(
                id = "featured",
                title = "Good kind hope community rescue",
                summary = "A healing achievement.",
            )
        val exactlyQuick =
            article(
                id = "exactly-quick",
                title = "Ordinary A",
                summary = "x".repeat(260),
                publishedAt = "z-date",
            )
        val tooLong =
            article(
                id = "too-long",
                title = "Ordinary B",
                summary = "x".repeat(261),
                publishedAt = "a-date",
            )

        val digest =
            DailyDigestEngine.digest(
                articles = listOf(tooLong, exactlyQuick, featured),
                savedStoryIds =
                    setOf(
                        featured.stableId,
                        exactlyQuick.stableId,
                        tooLong.stableId,
                    ),
                locale = Locale.US,
            )

        assertEquals(featured, digest.featuredArticle)
        assertEquals(exactlyQuick, digest.quickReadArticle)
        assertEquals(exactlyQuick, digest.worthSavingArticle)
        assertEquals(listOf(tooLong), digest.remainingArticles)
    }

    private fun article(
        id: String,
        title: String,
        summary: String = "An ordinary update",
        source: String = "Wire",
        publishedAt: String? = "2026-07-25T12:00:00Z",
        categories: List<String> = listOf("General"),
    ): Article =
        Article(
            id = id,
            title = title,
            summary = summary,
            originalUrl = URI("https://nutsnews.com/$id"),
            source = source,
            publishedAt = publishedAt,
            createdAt = null,
            thumbnailUrl = URI("https://nutsnews.com/$id.jpg"),
            categories = categories,
        )
}
