package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

class ArticleDecodingException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

object ArticleJsonDecoder {
    private val json = Json

    fun decodeArticle(value: String): Article =
        try {
            json.parseToJsonElement(value).requireObject().toArticle()
        } catch (error: SerializationException) {
            throw ArticleDecodingException("NutsNews could not read the article.", error)
        }

    fun decodeResponse(value: String): ArticlesResponse =
        try {
            val root = json.parseToJsonElement(value).requireObject()
            val articles = root.articleList()
            val nextPage =
                root.integer("nextPage")
                    ?: root.integer("next_page")

            ArticlesResponse(
                articles = articles,
                nextPage = nextPage,
            )
        } catch (error: SerializationException) {
            throw ArticleDecodingException("NutsNews could not read the article response.", error)
        }

    private fun JsonObject.articleList(): List<Article> {
        val elements = this["articles"] as? JsonArray ?: return emptyList()
        val objects = elements.map { it as? JsonObject ?: return emptyList() }
        return objects.map { it.toArticle() }
    }

    private fun JsonObject.toArticle(): Article =
        Article(
            id = articleId(),
            title = nonBlankString("title") ?: Article.UntitledLabel,
            summary = nonBlankString("summary", "aiSummary", "ai_summary").orEmpty(),
            originalUrl = url("originalUrl", "original_url"),
            source = nonBlankString("source") ?: Article.DefaultSourceLabel,
            publishedAt = nonBlankString("publishedAt", "published_at"),
            createdAt =
                nonBlankString(
                    "publishedOnSiteAt",
                    "published_on_site_at",
                    "createdAt",
                    "created_at",
                ),
            thumbnailUrl =
                url(
                    "thumbnailUrl",
                    "thumbnail_url",
                    "imageUrl",
                    "image_url",
                ),
            categories = categoryLabels(),
        )

    private fun JsonObject.articleId(): String {
        val primitive = this["id"] as? JsonPrimitive ?: return ""
        if (primitive is JsonNull) return ""
        if (primitive.isString) return primitive.content
        return primitive.longOrNull?.toString().orEmpty()
    }

    private fun JsonObject.categoryLabels(): List<String> {
        val categoryArray = this["categories"] as? JsonArray
        if (categoryArray != null && categoryArray.all { it is JsonPrimitive && it.isString }) {
            return cleanCategoryLabels(categoryArray.map { (it as JsonPrimitive).content })
        }

        val categoryString = nonBlankString("categories", "category")
        if (categoryString != null) {
            return cleanCategoryLabels(categoryString.split(*categoryDelimiters))
        }

        return listOf(Article.DefaultCategoryLabel)
    }

    private fun cleanCategoryLabels(labels: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        val cleaned =
            labels.mapNotNull { label ->
                val trimmed = label.trim()
                val lookupKey = trimmed.lowercase(Locale.ROOT)
                trimmed.takeIf { it.isNotEmpty() && seen.add(lookupKey) }
            }

        return cleaned.ifEmpty { listOf(Article.DefaultCategoryLabel) }
    }

    private fun JsonObject.nonBlankString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            val primitive = this[key] as? JsonPrimitive
            primitive
                ?.takeIf(JsonPrimitive::isString)
                ?.contentOrNull
                ?.trim()
                ?.takeIf(String::isNotEmpty)
        }

    private fun JsonObject.url(vararg keys: String): URI? {
        val value = nonBlankString(*keys) ?: return null
        return try {
            URI(value)
        } catch (_: URISyntaxException) {
            null
        }
    }

    private fun JsonObject.integer(key: String): Int? =
        (this[key] as? JsonPrimitive)
            ?.takeUnless(JsonPrimitive::isString)
            ?.intOrNull

    private fun JsonElement.requireObject(): JsonObject =
        this as? JsonObject
            ?: throw ArticleDecodingException("Expected a JSON object.")

    private val categoryDelimiters = charArrayOf('|', ',', ';', '/')
}
