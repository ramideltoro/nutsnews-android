package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class GoodMoodRecommendationEngineTest {
    @Test
    fun definitionsMatchIosWithAndroidEquivalentIcons() {
        assertEquals(
            listOf(
                "calm|Calm|gentle, peaceful stories|eco|calm,peace,quiet,mindful,garden,nature,wellness,healing,gentle,sleep,walk|wellness,lifestyle,nature,travel",
                "hopeful|Hopeful|kindness and recovery|favorite|hope,kind,rescue,recover,community,help,volunteer,support,reunited,donate,neighbors|community,uplifting,animals,human-interest",
                "inspired|Inspired|people doing big things|star|inspire,achievement,award,record,student,teacher,artist,athlete,first,dream,success|achievement,inspiring,culture,education",
                "curious|Curious|science, animals, culture|travel_explore|science,animal,space,discovery,research,museum,culture,history,nature,ocean,rare|science,animals,culture,travel",
            ),
            GoodMood.entries.map { mood ->
                "${mood.id}|${mood.title}|${mood.subtitle}|${mood.androidIconName}|${
                    mood.keywords.joinToString(",")
                }|${mood.categoryKeywords.joinToString(",")}"
            },
        )
        assertEquals(GoodMood.Hopeful, GoodMood.Default)
        assertEquals(GoodMood.Hopeful, GoodMood.fromId("unknown"))
    }

    @Test
    fun scoreAppliesEachCategoryAndFieldWeightOncePerKeyword() {
        val article =
            article(
                title = "Science briefing",
                summary = "A rare discovery, rare indeed",
                source = "Research Weekly",
                categories = listOf("Science and culture", "Animals"),
            )

        assertEquals(
            19,
            GoodMoodRecommendationEngine.score(article, GoodMood.Curious),
        )
    }

    @Test
    fun positiveMatchesChooseFeaturedThenSortRemainingByScoreAndDisplayDate() {
        val lowerScoreNewerDate =
            article(
                id = "lower",
                title = "Hope arrives",
                publishedAt = "z-date",
            )
        val higherScore =
            article(
                id = "higher",
                title = "Kind rescue",
                publishedAt = "a-date",
            )
        val tiedOlderDisplayDate =
            article(
                id = "tie-old",
                title = "Hope grows",
                publishedAt = "a-date",
            )
        val unmatched =
            article(
                id = "unmatched",
                title = "Ordinary report",
                publishedAt = "zz-date",
            )

        val result =
            GoodMoodRecommendationEngine.recommendations(
                articles =
                    listOf(
                        lowerScoreNewerDate,
                        unmatched,
                        tiedOlderDisplayDate,
                        higherScore,
                    ),
                mood = GoodMood.Hopeful,
            )

        assertEquals(higherScore, result.featuredArticle)
        assertEquals(
            listOf(lowerScoreNewerDate, tiedOlderDisplayDate),
            result.remainingArticles,
        )
        assertEquals(
            listOf(higherScore, lowerScoreNewerDate, tiedOlderDisplayDate),
            result.rankedArticles,
        )
    }

    @Test
    fun positiveMatchesAreCappedAtSixteen() {
        val matches =
            (0 until 18).map { index ->
                article(
                    id = "match-$index",
                    title = "Hope $index",
                    publishedAt = "date-${index.toString().padStart(2, '0')}",
                )
            }

        val result =
            GoodMoodRecommendationEngine.recommendations(matches, GoodMood.Hopeful)

        assertEquals(16, result.rankedArticles.size)
        assertEquals("match-17", result.featuredArticle?.id)
        assertEquals("match-2", result.remainingArticles.last().id)
    }

    @Test
    fun noPositiveMatchFallsBackToFirstTwelveSafeArticlesInFeedOrder() {
        val unsafeBlank = article(id = "blank", title = " \n ")
        val unsafeThumbnail =
            article(id = "no-image", title = "No image").copy(thumbnailUrl = null)
        val safe =
            (0 until 14).map { index ->
                article(id = "safe-$index", title = "Ordinary report $index")
            }

        val result =
            GoodMoodRecommendationEngine.recommendations(
                articles = listOf(unsafeBlank, unsafeThumbnail) + safe,
                mood = GoodMood.Calm,
            )

        assertEquals(safe.take(12), result.rankedArticles)
        assertEquals(safe.first(), result.featuredArticle)
        assertEquals(safe.drop(1).take(11), result.remainingArticles)
    }

    @Test
    fun noSafeArticlesReturnsAnEmptyRecommendation() {
        val result =
            GoodMoodRecommendationEngine.recommendations(
                articles =
                    listOf(
                        article(id = "blank", title = " "),
                        article(id = "no-image", title = "No image").copy(thumbnailUrl = null),
                    ),
                mood = GoodMood.Inspired,
            )

        assertNull(result.featuredArticle)
        assertEquals(emptyList(), result.remainingArticles)
        assertEquals(emptyList(), result.rankedArticles)
    }

    @Test
    fun everyMoodSelectsItsExclusiveFrozenKeywordFixture() {
        val expectedByMood =
            mapOf(
                GoodMood.Calm to article(id = "calm", title = "A peaceful garden"),
                GoodMood.Hopeful to article(id = "hopeful", title = "A kind rescue"),
                GoodMood.Inspired to article(id = "inspired", title = "A record award"),
                GoodMood.Curious to article(id = "curious", title = "A science discovery"),
            )
        val neutral = article(id = "neutral", title = "Ordinary report")
        val corpus = expectedByMood.values.toList() + neutral

        GoodMood.entries.forEach { mood ->
            val recommendations =
                GoodMoodRecommendationEngine.recommendations(corpus, mood)

            assertEquals(expectedByMood.getValue(mood), recommendations.featuredArticle)
            assertEquals(
                listOf(expectedByMood.getValue(mood)),
                recommendations.rankedArticles,
            )
        }
    }

    private fun article(
        id: String = "story",
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
