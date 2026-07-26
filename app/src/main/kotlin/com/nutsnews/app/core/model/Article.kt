package com.nutsnews.app.core.model

import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val originalUrl: URI?,
    val source: String,
    val publishedAt: String?,
    val createdAt: String?,
    val thumbnailUrl: URI?,
    val categories: List<String>,
) {
    val stableId: StoryId
        get() =
            StoryId(
                originalUrl
                    ?.toString()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: id.trim().takeIf(String::isNotEmpty)
                    ?: title.trim().lowercase(Locale.ROOT),
            )

    val displayDate: String
        get() = displayDate()

    fun displayDate(
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val rawDate = publishedAt ?: createdAt ?: return RecentlyLabel
        val instant =
            try {
                Instant.parse(rawDate)
            } catch (_: DateTimeParseException) {
                return rawDate
            }

        return DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(instant.atZone(zoneId))
    }

    companion object {
        const val UntitledLabel = "Untitled story"
        const val DefaultSourceLabel = "NutsNews"
        const val DefaultCategoryLabel = "Uplifting"
        const val RecentlyLabel = "Recently"
    }
}

data class ArticlesResponse(
    val articles: List<Article>,
    val nextPage: Int?,
)
