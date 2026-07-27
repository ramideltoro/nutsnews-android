package com.nutsnews.app.feature.article

import com.nutsnews.app.core.model.Article
import java.net.URI
import kotlin.test.assertEquals
import org.junit.Test

class ArticleBriefTest {
    @Test
    fun categoryFixturesMatchIosMoodWhyGoodAndTakeawayRules() {
        val fixtures =
            listOf(
                BriefFixture(
                    article = article(categories = listOf("Science", "Discovery")),
                    mood = "Curious",
                    whyGood =
                        "It highlights progress and curiosity, showing how discovery can " +
                            "make the world feel more hopeful.",
                    takeaway = "Progress is still happening, one discovery at a time.",
                ),
                BriefFixture(
                    article = article(title = "A first record becomes a milestone"),
                    mood = "Inspired",
                    whyGood =
                        "It celebrates effort, persistence, and a meaningful win that can " +
                            "leave readers feeling encouraged.",
                    takeaway = "Small steps can turn into a story worth celebrating.",
                ),
                BriefFixture(
                    article = article(source = "Community Kindness Journal"),
                    mood = "Hopeful",
                    whyGood =
                        "It shows people helping each other in a practical way, which is " +
                            "exactly the kind of local goodness NutsNews is built to surface.",
                    takeaway = "Good news often starts close to home.",
                ),
                BriefFixture(
                    article = article(categories = listOf("Animal Rescue")),
                    mood = "Calm",
                    whyGood =
                        "It gives readers a wholesome moment centered on care, protection, " +
                            "and the bond people share with animals.",
                    takeaway = "Care and compassion can travel farther than expected.",
                ),
                BriefFixture(
                    article = article(categories = listOf("Garden", "Nature")),
                    mood = "Calm",
                    whyGood =
                        "It offers a calmer kind of news moment, focused on wellbeing, " +
                            "restoration, and small positive changes.",
                    takeaway = "A quick reminder that the world still has soft spots.",
                ),
            )

        fixtures.forEach { fixture ->
            val brief = deriveArticleBrief(fixture.article)
            assertEquals(fixture.mood, brief.primaryMoodLabel)
            assertEquals(fixture.whyGood, brief.whyGoodNews)
            assertEquals(fixture.takeaway, brief.takeaway)
        }
    }

    @Test
    fun readingTimeRoundsUpAtOneHundredEightyWords() {
        val oneHundredEightyWords = List(180) { "good" }.joinToString(" ")
        val oneHundredEightyOneWords = "$oneHundredEightyWords news"

        assertEquals(
            "1 min native brief",
            deriveArticleBrief(article(title = oneHundredEightyWords, summary = ""))
                .estimatedReadTime,
        )
        assertEquals(
            "2 min native brief",
            deriveArticleBrief(article(title = oneHundredEightyOneWords, summary = ""))
                .estimatedReadTime,
        )
    }

    @Test
    fun whatHappenedUsesTrimmedSummaryThenFallsBackToTrimmedTitle() {
        assertEquals(
            "A complete summary.",
            deriveArticleBrief(article(summary = "  A complete summary. \n")).whatHappened,
        )
        assertEquals(
            "A title fallback",
            deriveArticleBrief(
                article(
                    title = "  A title fallback  ",
                    summary = " \n ",
                ),
            ).whatHappened,
        )
    }
}

private data class BriefFixture(
    val article: Article,
    val mood: String,
    val whyGood: String,
    val takeaway: String,
)

private fun article(
    title: String = "Neighbors make the day brighter",
    summary: String = "A practical idea created a hopeful result.",
    source: String = "NutsNews",
    categories: List<String> = emptyList(),
): Article =
    Article(
        id = "brief-fixture",
        title = title,
        summary = summary,
        originalUrl = URI("https://example.com/brief"),
        source = source,
        publishedAt = "Published today",
        createdAt = null,
        thumbnailUrl = null,
        categories = categories,
    )
