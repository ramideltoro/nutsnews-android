package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ArticlesResponse

interface ArchiveArticleSearchSource {
    suspend fun searchArticles(
        query: String,
        page: Int = 0,
        limit: Int = ArchiveSearchRequest.DefaultPageSize,
        fetchPolicy: NutsNewsFetchPolicy = NutsNewsFetchPolicy.UseCache,
    ): ArticlesResponse
}

class ArchiveSearchRequest private constructor(
    val query: String,
    val page: Int,
    val limit: Int,
    val meetsMinimum: Boolean,
) {
    companion object {
        const val DefaultPageSize = 20
        const val MinimumLimit = 1
        const val MaximumLimit = 50

        fun create(
            query: String,
            page: Int = 0,
            limit: Int = DefaultPageSize,
        ): ArchiveSearchRequest {
            val normalizedQuery = normalizeQuery(query)
            return ArchiveSearchRequest(
                query = normalizedQuery,
                page = page.coerceAtLeast(0),
                limit = limit.coerceIn(MinimumLimit, MaximumLimit),
                meetsMinimum = hasMinimumGraphemeCount(normalizedQuery),
            )
        }

        private fun normalizeQuery(value: String): String =
            buildString {
                var needsSpace = false
                value.trim().forEach { character ->
                    if (character.isWhitespace()) {
                        needsSpace = isNotEmpty()
                    } else {
                        if (needsSpace) append(' ')
                        append(character)
                        needsSpace = false
                    }
                }
            }

        private fun hasMinimumGraphemeCount(value: String): Boolean {
            var clusterCount = 0
            var index = 0
            var previousCodePoint = -1
            var joinsNextCodePoint = false
            var regionalIndicatorRun = 0

            while (index < value.length) {
                val codePoint = value.codePointAt(index)
                val extendsCluster = isGraphemeExtension(codePoint)
                val startsCluster =
                    when {
                        clusterCount == 0 -> true
                        previousCodePoint == CarriageReturn && codePoint == LineFeed -> false
                        codePoint == ZeroWidthJoiner -> false
                        joinsNextCodePoint -> false
                        extendsCluster -> false
                        isRegionalIndicator(codePoint) && regionalIndicatorRun % 2 == 1 -> false
                        else -> true
                    }

                if (startsCluster) {
                    clusterCount += 1
                    if (clusterCount >= MinimumSearchCharacters) return true
                }

                if (codePoint == ZeroWidthJoiner) {
                    joinsNextCodePoint = true
                } else if (!extendsCluster) {
                    joinsNextCodePoint = false
                }

                if (isRegionalIndicator(codePoint)) {
                    regionalIndicatorRun += 1
                } else if (!extendsCluster && codePoint != ZeroWidthJoiner) {
                    regionalIndicatorRun = 0
                }

                previousCodePoint = codePoint
                index += Character.charCount(codePoint)
            }

            return false
        }

        private fun isGraphemeExtension(codePoint: Int): Boolean =
            when (Character.getType(codePoint)) {
                Character.NON_SPACING_MARK.toInt(),
                Character.COMBINING_SPACING_MARK.toInt(),
                Character.ENCLOSING_MARK.toInt(),
                -> true
                else ->
                    codePoint in EmojiModifierRange ||
                        codePoint in VariationSelectorRange ||
                        codePoint in VariationSelectorSupplementRange ||
                        codePoint in EmojiTagRange ||
                        codePoint == ZeroWidthNonJoiner
            }

        private fun isRegionalIndicator(codePoint: Int): Boolean =
            codePoint in RegionalIndicatorRange

        private const val MinimumSearchCharacters = 2
        private const val CarriageReturn = 0x000D
        private const val LineFeed = 0x000A
        private const val ZeroWidthNonJoiner = 0x200C
        private const val ZeroWidthJoiner = 0x200D
        private val EmojiModifierRange = 0x1F3FB..0x1F3FF
        private val VariationSelectorRange = 0xFE00..0xFE0F
        private val VariationSelectorSupplementRange = 0xE0100..0xE01EF
        private val EmojiTagRange = 0xE0020..0xE007F
        private val RegionalIndicatorRange = 0x1F1E6..0x1F1FF
    }
}

sealed interface ArchiveSearchOutcome {
    val query: String
    val page: Int
    val limit: Int

    val isPagination: Boolean
        get() = page > 0

    data class Loading(
        override val query: String,
        override val page: Int,
        override val limit: Int,
    ) : ArchiveSearchOutcome

    data class Empty(
        override val query: String,
        override val page: Int,
        override val limit: Int,
        val reason: EmptyReason,
    ) : ArchiveSearchOutcome

    data class Page(
        override val query: String,
        override val page: Int,
        override val limit: Int,
        val articles: List<Article>,
        val nextPage: Int?,
    ) : ArchiveSearchOutcome {
        val canLoadMore: Boolean
            get() = nextPage != null
    }

    data class Failure(
        override val query: String,
        override val page: Int,
        override val limit: Int,
        val error: NutsNewsApiException,
    ) : ArchiveSearchOutcome

    enum class EmptyReason {
        QueryTooShort,
        NoMatches,
    }
}
