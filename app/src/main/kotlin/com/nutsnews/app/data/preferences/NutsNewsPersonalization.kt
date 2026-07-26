package com.nutsnews.app.data.preferences

import com.nutsnews.app.core.model.Article
import java.util.Locale

data class TopicPreference(
    val id: String,
    val title: String,
    val iconName: String,
    val keywords: List<String>,
)

data class MoodPreference(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val keywords: List<String>,
)

object NutsNewsPersonalization {
    val DefaultTopicIds: Set<String> = setOf("community", "science", "animals")
    const val DefaultMoodId = "calm"

    val topics: List<TopicPreference> =
        listOf(
            TopicPreference(
                id = "animals",
                title = "Animals",
                iconName = "pawprint.fill",
                keywords =
                    listOf(
                        "animal",
                        "animals",
                        "dog",
                        "cat",
                        "wildlife",
                        "bird",
                        "rescue",
                        "pet",
                        "zoo",
                        "habitat",
                        "species",
                    ),
            ),
            TopicPreference(
                id = "science",
                title = "Science",
                iconName = "atom",
                keywords =
                    listOf(
                        "science",
                        "space",
                        "nasa",
                        "research",
                        "discovery",
                        "breakthrough",
                        "technology",
                        "study",
                        "innovation",
                        "climate",
                    ),
            ),
            TopicPreference(
                id = "community",
                title = "Community",
                iconName = "person.3.fill",
                keywords =
                    listOf(
                        "community",
                        "neighbors",
                        "volunteer",
                        "local",
                        "school",
                        "family",
                        "kindness",
                        "help",
                        "support",
                        "together",
                    ),
            ),
            TopicPreference(
                id = "wellness",
                title = "Wellness",
                iconName = "leaf.fill",
                keywords =
                    listOf(
                        "wellness",
                        "health",
                        "mental",
                        "calm",
                        "mindful",
                        "fitness",
                        "healing",
                        "therapy",
                        "garden",
                        "peace",
                    ),
            ),
            TopicPreference(
                id = "achievements",
                title = "Achievements",
                iconName = "trophy.fill",
                keywords =
                    listOf(
                        "achievement",
                        "record",
                        "milestone",
                        "award",
                        "graduate",
                        "winner",
                        "success",
                        "first",
                        "goal",
                        "champion",
                    ),
            ),
            TopicPreference(
                id = "travel",
                title = "Travel",
                iconName = "airplane.departure",
                keywords =
                    listOf(
                        "travel",
                        "park",
                        "trail",
                        "beach",
                        "city",
                        "island",
                        "museum",
                        "journey",
                        "tour",
                        "destination",
                    ),
            ),
            TopicPreference(
                id = "culture",
                title = "Culture",
                iconName = "theatermasks.fill",
                keywords =
                    listOf(
                        "culture",
                        "art",
                        "music",
                        "film",
                        "book",
                        "artist",
                        "museum",
                        "dance",
                        "festival",
                        "creative",
                    ),
            ),
            TopicPreference(
                id = "nature",
                title = "Nature",
                iconName = "tree.fill",
                keywords =
                    listOf(
                        "nature",
                        "forest",
                        "tree",
                        "river",
                        "ocean",
                        "garden",
                        "wildlife",
                        "conservation",
                        "restore",
                        "environment",
                    ),
            ),
        )

    val moods: List<MoodPreference> =
        listOf(
            MoodPreference(
                id = "calm",
                title = "Calm",
                subtitle = "Soft, peaceful stories",
                iconName = "sun.horizon.fill",
                keywords =
                    listOf(
                        "calm",
                        "peace",
                        "garden",
                        "nature",
                        "healing",
                        "wellness",
                        "quiet",
                        "gentle",
                        "restored",
                        "beautiful",
                    ),
            ),
            MoodPreference(
                id = "hopeful",
                title = "Hopeful",
                subtitle = "Progress and kindness",
                iconName = "sparkles",
                keywords =
                    listOf(
                        "hope",
                        "progress",
                        "kindness",
                        "help",
                        "support",
                        "community",
                        "volunteer",
                        "improve",
                        "restore",
                        "future",
                    ),
            ),
            MoodPreference(
                id = "inspired",
                title = "Inspired",
                subtitle = "People doing amazing things",
                iconName = "bolt.heart.fill",
                keywords =
                    listOf(
                        "inspire",
                        "achievement",
                        "record",
                        "award",
                        "first",
                        "goal",
                        "winner",
                        "dream",
                        "success",
                        "milestone",
                    ),
            ),
            MoodPreference(
                id = "curious",
                title = "Curious",
                subtitle = "Science, culture, and discovery",
                iconName = "lightbulb.fill",
                keywords =
                    listOf(
                        "science",
                        "discovery",
                        "research",
                        "space",
                        "museum",
                        "technology",
                        "innovation",
                        "study",
                        "ancient",
                        "reveals",
                    ),
            ),
        )

    val topicIds: Set<String> = topics.mapTo(linkedSetOf(), TopicPreference::id)
    val moodIds: Set<String> = moods.mapTo(linkedSetOf(), MoodPreference::id)

    fun selectedTopicIds(topicIds: Set<String>): Set<String> =
        topicIds
            .intersect(this.topicIds)
            .ifEmpty { DefaultTopicIds }

    fun mood(moodId: String): MoodPreference =
        moods.firstOrNull { mood -> mood.id == moodId }
            ?: moods.first { mood -> mood.id == DefaultMoodId }

    fun topicTitles(topicIds: Set<String>): List<String> {
        val selectedIds = selectedTopicIds(topicIds)
        return topics
            .filter { topic -> topic.id in selectedIds }
            .map(TopicPreference::title)
    }

    fun personalizedArticles(
        articles: List<Article>,
        preferences: UserPreferences,
        limit: Int = 3,
    ): List<Article> =
        personalizedArticles(
            articles = articles,
            selectedTopicIds = preferences.selectedTopicIds,
            selectedMoodId = preferences.selectedMoodId,
            limit = limit,
        )

    fun personalizedArticles(
        articles: List<Article>,
        selectedTopicIds: Set<String>,
        selectedMoodId: String,
        limit: Int = 3,
    ): List<Article> {
        val sanitizedTopicIds =
            NutsNewsPersonalization.selectedTopicIds(selectedTopicIds)
        val selectedTopics =
            topics.filter { topic ->
                topic.id in sanitizedTopicIds
            }
        val selectedMood = mood(selectedMoodId)
        val scoredArticles =
            articles.map { article ->
                ScoredArticle(
                    article = article,
                    score =
                        personalizationScore(
                            article = article,
                            topics = selectedTopics,
                            mood = selectedMood,
                        ),
                )
            }
        val positiveMatches =
            scoredArticles
                .filter { scoredArticle -> scoredArticle.score > 0 }
                .sortedWith(
                    compareByDescending<ScoredArticle> { scoredArticle -> scoredArticle.score }
                        .thenBy { scoredArticle -> scoredArticle.article.title },
                ).map(ScoredArticle::article)
        val safeLimit = limit.coerceAtLeast(0)
        if (positiveMatches.size >= safeLimit) {
            return positiveMatches.take(safeLimit)
        }

        val remainingArticles =
            scoredArticles
                .filter { scoredArticle -> scoredArticle.score <= 0 }
                .map(ScoredArticle::article)
        return (positiveMatches + remainingArticles).take(safeLimit)
    }

    fun personalizationSummary(preferences: UserPreferences): String =
        personalizationSummary(
            selectedTopicIds = preferences.selectedTopicIds,
            selectedMoodId = preferences.selectedMoodId,
        )

    fun personalizationSummary(
        selectedTopicIds: Set<String>,
        selectedMoodId: String,
    ): String {
        val topicTitles =
            topicTitles(selectedTopicIds)
                .take(3)
                .joinToString(", ")
        val moodTitle = mood(selectedMoodId).title
        return "For You is tuned for $topicTitles with a ${
            moodTitle.lowercase(Locale.ROOT)
        } feel."
    }

    internal fun personalizationScore(
        article: Article,
        topics: List<TopicPreference>,
        mood: MoodPreference,
    ): Int {
        val searchableText =
            (
                listOf(article.title, article.summary, article.source) +
                    article.categories
            ).joinToString(" ")
                .lowercase(Locale.ROOT)
        var score = 0
        topics.forEach { topic ->
            if (
                article.categories.any { category ->
                    category.contains(topic.title, ignoreCase = true)
                }
            ) {
                score += 4
            }
            score += topic.keywords.count(searchableText::contains) * 2
        }
        score += mood.keywords.count(searchableText::contains)
        return score
    }

    private data class ScoredArticle(
        val article: Article,
        val score: Int,
    )
}
