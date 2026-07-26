package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import java.text.Collator
import java.util.Locale

data class DigestCategoryCount(
    val label: String,
    val count: Int,
)

data class DailyDigest(
    val storyCount: Int,
    val uniqueSourceCount: Int,
    val savedStoryCount: Int,
    val categoryCounts: List<DigestCategoryCount>,
    val featuredArticle: Article?,
    val quickReadArticle: Article?,
    val worthSavingArticle: Article?,
    val remainingArticles: List<Article>,
    val rankedArticles: List<Article>,
)

object DailyDigestEngine {
    private val PositiveKeywords =
        listOf(
            "good",
            "kind",
            "hope",
            "uplifting",
            "community",
            "rescue",
            "inspire",
            "student",
            "teacher",
            "science",
            "animal",
            "nature",
            "garden",
            "healing",
            "achievement",
            "volunteer",
            "reunited",
        )
    private val PositiveCategoryKeywords =
        listOf(
            "uplifting",
            "community",
            "wellness",
            "achievement",
            "animals",
            "science",
        )

    private const val MaximumDigestArticles = 24
    private const val MaximumCategoryCounts = 8
    private const val MaximumRemainingArticles = 10
    private const val MaximumQuickReadCharacters = 260

    fun digest(
        articles: List<Article>,
        savedStories: List<SavedStory>,
        locale: Locale = Locale.getDefault(),
    ): DailyDigest =
        digest(
            articles = articles,
            savedStoryIds = savedStories.mapTo(linkedSetOf(), SavedStory::id),
            locale = locale,
        )

    fun digest(
        articles: List<Article>,
        savedStoryIds: Set<StoryId> = emptySet(),
        locale: Locale = Locale.getDefault(),
    ): DailyDigest {
        val digestArticles =
            articles
                .filter { article ->
                    article.title.trim().isNotEmpty() && article.thumbnailUrl != null
                }.take(MaximumDigestArticles)
        val rankedArticles =
            digestArticles.sortedWith(
                compareByDescending<Article>(::score)
                    .thenByDescending(Article::displayDate),
            )
        val featuredArticle = rankedArticles.firstOrNull()
        val quickReadArticle =
            rankedArticles.firstOrNull { article ->
                article.summary.length <= MaximumQuickReadCharacters &&
                    article.id != featuredArticle?.id
            } ?: rankedArticles.drop(1).firstOrNull()
        val worthSavingArticle =
            rankedArticles.firstOrNull { article ->
                article.stableId !in savedStoryIds &&
                    article.id != featuredArticle?.id
            } ?: rankedArticles.drop(1).firstOrNull()
        val remainingArticles =
            rankedArticles
                .filter { article ->
                    article.id != featuredArticle?.id &&
                        article.id != quickReadArticle?.id
                }.take(MaximumRemainingArticles)

        return DailyDigest(
            storyCount = digestArticles.size,
            uniqueSourceCount =
                digestArticles
                    .map { article -> article.source.trim() }
                    .filter(String::isNotEmpty)
                    .toSet()
                    .size,
            savedStoryCount = savedStoryIds.size,
            categoryCounts = categoryCounts(digestArticles, locale),
            featuredArticle = featuredArticle,
            quickReadArticle = quickReadArticle,
            worthSavingArticle = worthSavingArticle,
            remainingArticles = remainingArticles,
            rankedArticles = rankedArticles,
        )
    }

    internal fun score(article: Article): Int {
        val title = article.title.lowercase(Locale.ROOT)
        val summary = article.summary.lowercase(Locale.ROOT)
        val categories =
            article.categories.map { category -> category.lowercase(Locale.ROOT) }
        var score = 0

        categories.forEach { category ->
            if (
                PositiveCategoryKeywords.any { categoryKeyword ->
                    category.contains(categoryKeyword)
                }
            ) {
                score += 5
            }
        }
        PositiveKeywords.forEach { keyword ->
            if (title.contains(keyword)) {
                score += 4
            }
            if (summary.contains(keyword)) {
                score += 2
            }
        }
        if (article.thumbnailUrl != null) {
            score += 2
        }
        if (article.summary.trim().isNotEmpty()) {
            score += 1
        }
        return score
    }

    private fun categoryCounts(
        articles: List<Article>,
        locale: Locale,
    ): List<DigestCategoryCount> {
        val counts = linkedMapOf<String, Int>()
        articles
            .flatMap(Article::categories)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { category ->
                counts[category] = (counts[category] ?: 0) + 1
            }
        val labelCollator =
            Collator.getInstance(locale).apply {
                strength = Collator.SECONDARY
            }
        return counts
            .map { (label, count) ->
                DigestCategoryCount(label = label, count = count)
            }.sortedWith { left, right ->
                if (left.count != right.count) {
                    right.count.compareTo(left.count)
                } else {
                    labelCollator.compare(left.label, right.label)
                }
            }.take(MaximumCategoryCounts)
    }
}
