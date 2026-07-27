package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Compact, Bundle-safe article serialization for [androidx.lifecycle.SavedStateHandle].
 *
 * Network response caching remains the durable offline source of truth. This snapshot only keeps
 * the currently visible content available while Android recreates the task.
 */
internal object ArticleStateCodec {
    fun encode(
        response: ArticlesResponse,
        maxArticles: Int = MaximumSavedArticles,
    ): String =
        buildJsonObject {
            val savedArticles = response.articles.take(maxArticles.coerceAtLeast(0))
            put(
                "articles",
                buildJsonArray {
                    savedArticles.forEach { article ->
                        add(article.toJson())
                    }
                },
            )
            val savedNextPage =
                response.nextPage.takeIf {
                    savedArticles.size == response.articles.size
                }
            if (savedNextPage == null) {
                put("nextPage", JsonNull)
            } else {
                put("nextPage", savedNextPage)
            }
        }.toString()

    fun decodeOrNull(value: String?): ArticlesResponse? =
        value
            ?.let { encoded ->
                runCatching {
                    ArticleJsonDecoder.decodeResponse(encoded)
                }.getOrNull()
            }

    private fun Article.toJson() =
        buildJsonObject {
            put("id", id)
            put("title", title)
            put("summary", summary)
            originalUrl?.let { put("originalUrl", it.toString()) }
            put("source", source)
            publishedAt?.let { put("publishedAt", it) }
            createdAt?.let { put("createdAt", it) }
            thumbnailUrl?.let { put("thumbnailUrl", it.toString()) }
            put(
                "categories",
                buildJsonArray {
                    categories.forEach { category ->
                        add(JsonPrimitive(category))
                    }
                },
            )
        }

    private const val MaximumSavedArticles = 100
}
