package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import java.util.Locale

enum class GoodMood(
    val id: String,
    val title: String,
    val subtitle: String,
    val androidIconName: String,
    val keywords: List<String>,
    val categoryKeywords: List<String>,
) {
    Calm(
        id = "calm",
        title = "Calm",
        subtitle = "gentle, peaceful stories",
        androidIconName = "eco",
        keywords =
            listOf(
                "calm",
                "peace",
                "quiet",
                "mindful",
                "garden",
                "nature",
                "wellness",
                "healing",
                "gentle",
                "sleep",
                "walk",
            ),
        categoryKeywords = listOf("wellness", "lifestyle", "nature", "travel"),
    ),
    Hopeful(
        id = "hopeful",
        title = "Hopeful",
        subtitle = "kindness and recovery",
        androidIconName = "favorite",
        keywords =
            listOf(
                "hope",
                "kind",
                "rescue",
                "recover",
                "community",
                "help",
                "volunteer",
                "support",
                "reunited",
                "donate",
                "neighbors",
            ),
        categoryKeywords =
            listOf("community", "uplifting", "animals", "human-interest"),
    ),
    Inspired(
        id = "inspired",
        title = "Inspired",
        subtitle = "people doing big things",
        androidIconName = "star",
        keywords =
            listOf(
                "inspire",
                "achievement",
                "award",
                "record",
                "student",
                "teacher",
                "artist",
                "athlete",
                "first",
                "dream",
                "success",
            ),
        categoryKeywords =
            listOf("achievement", "inspiring", "culture", "education"),
    ),
    Curious(
        id = "curious",
        title = "Curious",
        subtitle = "science, animals, culture",
        androidIconName = "travel_explore",
        keywords =
            listOf(
                "science",
                "animal",
                "space",
                "discovery",
                "research",
                "museum",
                "culture",
                "history",
                "nature",
                "ocean",
                "rare",
            ),
        categoryKeywords = listOf("science", "animals", "culture", "travel"),
    ),
    ;

    companion object {
        val Default = Hopeful

        fun fromId(id: String): GoodMood =
            entries.firstOrNull { mood -> mood.id == id } ?: Default
    }
}

data class GoodMoodRecommendations(
    val featuredArticle: Article?,
    val remainingArticles: List<Article>,
) {
    val rankedArticles: List<Article>
        get() = listOfNotNull(featuredArticle) + remainingArticles
}

object GoodMoodRecommendationEngine {
    private const val MaximumMatchedResults = 16
    private const val MaximumFallbackResults = 12

    fun recommendations(
        articles: List<Article>,
        mood: GoodMood = GoodMood.Default,
    ): GoodMoodRecommendations {
        val safeArticles =
            articles.filter { article ->
                article.title.trim().isNotEmpty() && article.thumbnailUrl != null
            }
        val scoredArticles =
            safeArticles
                .map { article ->
                    ScoredArticle(
                        article = article,
                        score = score(article, mood),
                    )
                }.filter { scoredArticle -> scoredArticle.score > 0 }
                .sortedWith(
                    compareByDescending<ScoredArticle> { scoredArticle -> scoredArticle.score }
                        .thenByDescending { scoredArticle -> scoredArticle.article.displayDate },
                ).map(ScoredArticle::article)

        val rankedArticles =
            if (scoredArticles.isEmpty()) {
                safeArticles.take(MaximumFallbackResults)
            } else {
                scoredArticles.take(MaximumMatchedResults)
            }
        return GoodMoodRecommendations(
            featuredArticle = rankedArticles.firstOrNull(),
            remainingArticles = rankedArticles.drop(1),
        )
    }

    internal fun score(
        article: Article,
        mood: GoodMood,
    ): Int {
        val title = article.title.lowercase(Locale.ROOT)
        val summary = article.summary.lowercase(Locale.ROOT)
        val source = article.source.lowercase(Locale.ROOT)
        val categories = article.categories.map { category -> category.lowercase(Locale.ROOT) }
        var score = 0

        categories.forEach { category ->
            if (
                mood.categoryKeywords.any { categoryKeyword ->
                    category.contains(categoryKeyword)
                }
            ) {
                score += 5
            }
        }
        mood.keywords.forEach { keyword ->
            if (title.contains(keyword)) {
                score += 4
            }
            if (summary.contains(keyword)) {
                score += 2
            }
            if (source.contains(keyword)) {
                score += 1
            }
        }
        return score
    }

    private data class ScoredArticle(
        val article: Article,
        val score: Int,
    )
}
