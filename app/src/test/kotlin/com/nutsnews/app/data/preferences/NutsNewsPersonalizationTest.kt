package com.nutsnews.app.data.preferences

import com.nutsnews.app.core.model.Article
import java.net.URI
import kotlin.test.assertEquals
import org.junit.Test

class NutsNewsPersonalizationTest {
    @Test
    fun topicCatalogAndDefaultsMatchTheFrozenIosDefinitions() {
        assertEquals(
            listOf(
                "animals|Animals|pawprint.fill|animal,animals,dog,cat,wildlife,bird,rescue,pet,zoo,habitat,species",
                "science|Science|atom|science,space,nasa,research,discovery,breakthrough,technology,study,innovation,climate",
                "community|Community|person.3.fill|community,neighbors,volunteer,local,school,family,kindness,help,support,together",
                "wellness|Wellness|leaf.fill|wellness,health,mental,calm,mindful,fitness,healing,therapy,garden,peace",
                "achievements|Achievements|trophy.fill|achievement,record,milestone,award,graduate,winner,success,first,goal,champion",
                "travel|Travel|airplane.departure|travel,park,trail,beach,city,island,museum,journey,tour,destination",
                "culture|Culture|theatermasks.fill|culture,art,music,film,book,artist,museum,dance,festival,creative",
                "nature|Nature|tree.fill|nature,forest,tree,river,ocean,garden,wildlife,conservation,restore,environment",
            ),
            NutsNewsPersonalization.topics.map { topic ->
                "${topic.id}|${topic.title}|${topic.iconName}|${topic.keywords.joinToString(",")}"
            },
        )
        assertEquals(
            setOf("community", "science", "animals"),
            NutsNewsPersonalization.DefaultTopicIds,
        )
        assertEquals(
            NutsNewsPersonalization.topics.mapTo(linkedSetOf(), TopicPreference::id),
            NutsNewsPersonalization.topicIds,
        )
    }

    @Test
    fun moodCatalogAndDefaultMatchTheFrozenIosDefinitions() {
        assertEquals(
            listOf(
                "calm|Calm|Soft, peaceful stories|sun.horizon.fill|calm,peace,garden,nature,healing,wellness,quiet,gentle,restored,beautiful",
                "hopeful|Hopeful|Progress and kindness|sparkles|hope,progress,kindness,help,support,community,volunteer,improve,restore,future",
                "inspired|Inspired|People doing amazing things|bolt.heart.fill|inspire,achievement,record,award,first,goal,winner,dream,success,milestone",
                "curious|Curious|Science, culture, and discovery|lightbulb.fill|science,discovery,research,space,museum,technology,innovation,study,ancient,reveals",
            ),
            NutsNewsPersonalization.moods.map { mood ->
                "${mood.id}|${mood.title}|${mood.subtitle}|${mood.iconName}|${
                    mood.keywords.joinToString(",")
                }"
            },
        )
        assertEquals("calm", NutsNewsPersonalization.DefaultMoodId)
        assertEquals(
            NutsNewsPersonalization.moods.mapTo(linkedSetOf(), MoodPreference::id),
            NutsNewsPersonalization.moodIds,
        )
    }

    @Test
    fun scoreUsesCategoryBonusAndIosKeywordWeights() {
        val science = NutsNewsPersonalization.topics.single { it.id == "science" }
        val calm = NutsNewsPersonalization.moods.single { it.id == "calm" }

        assertEquals(
            6,
            NutsNewsPersonalization.personalizationScore(
                article = article(title = "A", categories = listOf("SCIENCE & TECH")),
                topics = listOf(science),
                mood = calm,
            ),
        )
        assertEquals(
            2,
            NutsNewsPersonalization.personalizationScore(
                article = article(title = "B", summary = "A science update"),
                topics = listOf(science),
                mood = calm,
            ),
        )
        assertEquals(
            2,
            NutsNewsPersonalization.personalizationScore(
                article = article(title = "C", summary = "A calm and peaceful update"),
                topics = listOf(science),
                mood = calm,
            ),
        )
    }

    @Test
    fun rankingUsesScoreThenTitleAndFillsFromFeedOrderWithinLimit() {
        val fallbackFirst = article(title = "Plain D", id = "d")
        val zuluMatch = article(title = "Zulu", summary = "A new record", id = "z")
        val fallbackSecond = article(title = "Plain C", id = "c")
        val alphaMatch = article(title = "Alpha", summary = "A deserving winner", id = "a")
        val feed = listOf(fallbackFirst, zuluMatch, fallbackSecond, alphaMatch)

        assertEquals(
            listOf(alphaMatch, zuluMatch, fallbackFirst),
            NutsNewsPersonalization.personalizedArticles(
                articles = feed,
                selectedTopicIds = setOf("achievements"),
                selectedMoodId = "calm",
            ),
        )
        assertEquals(
            listOf(alphaMatch),
            NutsNewsPersonalization.personalizedArticles(
                articles = feed,
                selectedTopicIds = setOf("achievements"),
                selectedMoodId = "calm",
                limit = 1,
            ),
        )
        assertEquals(
            emptyList(),
            NutsNewsPersonalization.personalizedArticles(
                articles = feed,
                selectedTopicIds = setOf("achievements"),
                selectedMoodId = "calm",
                limit = 0,
            ),
        )
        assertEquals(
            listOf(alphaMatch, zuluMatch, fallbackFirst, fallbackSecond),
            NutsNewsPersonalization.personalizedArticles(
                articles = feed,
                selectedTopicIds = setOf("achievements"),
                selectedMoodId = "calm",
                limit = 99,
            ),
        )
    }

    @Test
    fun invalidSelectionsUseDefaultsAndSummaryKeepsCatalogOrder() {
        assertEquals(
            NutsNewsPersonalization.DefaultTopicIds,
            NutsNewsPersonalization.selectedTopicIds(setOf("unknown")),
        )
        assertEquals("calm", NutsNewsPersonalization.mood("unknown").id)
        assertEquals(
            "For You is tuned for Animals, Science, Community with a calm feel.",
            NutsNewsPersonalization.personalizationSummary(
                selectedTopicIds = emptySet(),
                selectedMoodId = "unknown",
            ),
        )
        assertEquals(
            "For You is tuned for Animals, Community, Travel with a hopeful feel.",
            NutsNewsPersonalization.personalizationSummary(
                selectedTopicIds = setOf("culture", "travel", "community", "animals"),
                selectedMoodId = "hopeful",
            ),
        )
    }

    private fun article(
        title: String,
        summary: String = "An ordinary update",
        id: String = title,
        categories: List<String> = listOf("General"),
    ): Article =
        Article(
            id = id,
            title = title,
            summary = summary,
            originalUrl = URI("https://nutsnews.com/$id"),
            source = "Wire",
            publishedAt = "2026-07-25T12:00:00Z",
            createdAt = "2026-07-25T12:01:00Z",
            thumbnailUrl = null,
            categories = categories,
        )
}
