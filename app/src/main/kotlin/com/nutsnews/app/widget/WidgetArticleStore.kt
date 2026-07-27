package com.nutsnews.app.widget

import com.nutsnews.app.data.article.ArticleResponseCache
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

interface WidgetArticleStore {
    suspend fun read(): WidgetArticle?

    suspend fun write(article: WidgetArticle)
}

object EmptyWidgetArticleStore : WidgetArticleStore {
    override suspend fun read(): WidgetArticle? = null

    override suspend fun write(article: WidgetArticle) = Unit
}

class ResponseCacheWidgetArticleStore(
    private val responseCache: ArticleResponseCache,
) : WidgetArticleStore {
    override suspend fun read(): WidgetArticle? {
        val rawValue =
            responseCache.read(
                key = CacheKey,
                maxAge = null,
            ) ?: return null

        return try {
            WidgetArticleJsonCodec.decode(rawValue)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            responseCache.remove(CacheKey)
            null
        }
    }

    override suspend fun write(article: WidgetArticle) {
        responseCache.write(
            key = CacheKey,
            response = WidgetArticleJsonCodec.encode(article),
        )
    }

    companion object {
        internal const val CacheKey = "widget:article:v1"
    }
}

private object WidgetArticleJsonCodec {
    private const val SchemaVersion = 1

    fun encode(article: WidgetArticle): String =
        buildJsonObject {
            put("schemaVersion", SchemaVersion)
            put("storyId", article.storyId)
            put("title", article.title)
            put("summary", article.summary)
            put("source", article.source)
            put("mood", article.mood)
        }.toString()

    fun decode(rawValue: String): WidgetArticle {
        val objectValue = Json.parseToJsonElement(rawValue).jsonObject
        check(
            objectValue["schemaVersion"]
                ?.jsonPrimitive
                ?.intOrNull == SchemaVersion,
        ) {
            "Unsupported widget article cache schema."
        }
        return WidgetArticle(
            storyId = objectValue.requiredString("storyId"),
            title = objectValue.requiredString("title"),
            summary = objectValue.requiredString("summary"),
            source = objectValue.requiredString("source"),
            mood = objectValue.requiredString("mood"),
        ).also { article ->
            check(article.storyId.isNotBlank() && article.title.isNotBlank()) {
                "Widget article cache is missing required content."
            }
        }
    }

    private fun JsonObject.requiredString(key: String): String =
        this[key]
            ?.jsonPrimitive
            ?.contentOrNull
            ?: error("Widget article cache is missing $key.")
}
