package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.core.network.HttpRequest
import com.nutsnews.app.core.network.HttpResponse
import com.nutsnews.app.core.network.HttpTransport
import com.nutsnews.app.data.network.OkHttpTransport
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class NutsNewsEndpoints(
    val articles: String,
    val archiveSearch: String,
) {
    companion object {
        val Production =
            NutsNewsEndpoints(
                articles = "https://www.nutsnews.com/api/articles",
                archiveSearch = "https://www.nutsnews.com/api/search",
            )
    }
}

sealed class NutsNewsApiException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause) {
    class InvalidUrl :
        NutsNewsApiException("The NutsNews API URL is invalid.")

    class InvalidResponse :
        NutsNewsApiException("The NutsNews API returned an invalid response.")

    class HttpStatus(
        val statusCode: Int,
    ) : NutsNewsApiException("The NutsNews API returned status code $statusCode.")

    class Timeout(
        cause: IOException,
    ) : NutsNewsApiException("The NutsNews API request timed out.", cause)

    class Network(
        cause: IOException,
    ) : NutsNewsApiException("NutsNews could not reach the article service.", cause)

    class Decoding(
        cause: ArticleDecodingException,
    ) : NutsNewsApiException("NutsNews could not read the article response.", cause)
}

enum class NutsNewsFetchPolicy {
    UseCache,
    ReloadIgnoringCache,
}

enum class ArticleFetchSource {
    Network,
    FreshCache,
    StaleCache,
}

data class ArticleFetchResult(
    val response: ArticlesResponse,
    val source: ArticleFetchSource,
) {
    val isStale: Boolean
        get() = source == ArticleFetchSource.StaleCache
}

interface FeedArticleSource {
    suspend fun fetchFeedPage(
        page: Int = 0,
        category: String? = null,
        fetchPolicy: NutsNewsFetchPolicy = NutsNewsFetchPolicy.UseCache,
    ): ArticleFetchResult
}

class NutsNewsApiClient(
    private val endpoints: NutsNewsEndpoints = NutsNewsEndpoints.Production,
    private val transport: HttpTransport = OkHttpTransport(),
    private val responseCache: ArticleResponseCache = EmptyArticleResponseCache,
) : FeedArticleSource,
    ArchiveArticleSearchSource {
    suspend fun fetchArticles(
        page: Int = 0,
        category: String? = null,
        fetchPolicy: NutsNewsFetchPolicy = NutsNewsFetchPolicy.UseCache,
    ): ArticlesResponse =
        fetchFeedPage(
            page = page,
            category = category,
            fetchPolicy = fetchPolicy,
        ).response

    override suspend fun fetchFeedPage(
        page: Int,
        category: String?,
        fetchPolicy: NutsNewsFetchPolicy,
    ): ArticleFetchResult {
        val request = articleRequest(page = page, category = category)
        return fetchWithCache(
            request = request,
            cacheKey = articleCacheKey(page = page, category = category),
            freshness = FeedFreshness,
            fetchPolicy = fetchPolicy,
        )
    }

    override suspend fun searchArticles(
        query: String,
        page: Int,
        limit: Int,
        fetchPolicy: NutsNewsFetchPolicy,
    ): ArticlesResponse {
        val request = ArchiveSearchRequest.create(query = query, page = page, limit = limit)
        if (!request.meetsMinimum) {
            return ArticlesResponse(articles = emptyList(), nextPage = null)
        }

        return fetchWithCache(
            request = searchRequest(request),
            cacheKey = searchCacheKey(request),
            freshness = SearchFreshness,
            fetchPolicy = fetchPolicy,
        ).response
    }

    fun searchOutcomes(
        query: String,
        page: Int = 0,
        limit: Int = ArchiveSearchRequest.DefaultPageSize,
        fetchPolicy: NutsNewsFetchPolicy = NutsNewsFetchPolicy.UseCache,
    ): Flow<ArchiveSearchOutcome> =
        flow {
            val request = ArchiveSearchRequest.create(query = query, page = page, limit = limit)
            if (!request.meetsMinimum) {
                emit(
                    ArchiveSearchOutcome.Empty(
                        query = request.query,
                        page = request.page,
                        limit = request.limit,
                        reason = ArchiveSearchOutcome.EmptyReason.QueryTooShort,
                    ),
                )
                return@flow
            }

            emit(
                ArchiveSearchOutcome.Loading(
                    query = request.query,
                    page = request.page,
                    limit = request.limit,
                ),
            )

            try {
                val response =
                    searchArticles(
                        query = request.query,
                        page = request.page,
                        limit = request.limit,
                        fetchPolicy = fetchPolicy,
                    )
                if (response.articles.isEmpty()) {
                    emit(
                        ArchiveSearchOutcome.Empty(
                            query = request.query,
                            page = request.page,
                            limit = request.limit,
                            reason = ArchiveSearchOutcome.EmptyReason.NoMatches,
                        ),
                    )
                } else {
                    emit(
                        ArchiveSearchOutcome.Page(
                            query = request.query,
                            page = request.page,
                            limit = request.limit,
                            articles = response.articles,
                            nextPage = response.nextPage,
                        ),
                    )
                }
            } catch (error: NutsNewsApiException) {
                emit(
                    ArchiveSearchOutcome.Failure(
                        query = request.query,
                        page = request.page,
                        limit = request.limit,
                        error = error,
                    ),
                )
            }
        }

    private fun articleRequest(
        page: Int,
        category: String?,
    ): HttpRequest {
        val baseUrl =
            endpoints.articles.toHttpUrlOrNull()
                ?: throw NutsNewsApiException.InvalidUrl()
        val url =
            baseUrl
                .newBuilder()
                .addQueryParameter("page", page.toString())
                .apply {
                    if (category != null && category.trim().isNotEmpty()) {
                        addQueryParameter("category", category)
                    }
                }
                .build()

        return HttpRequest(
            url = url.toUri(),
            headers = mapOf("Accept" to "application/json"),
        )
    }

    private fun searchRequest(request: ArchiveSearchRequest): HttpRequest {
        val baseUrl =
            endpoints.archiveSearch.toHttpUrlOrNull()
                ?: throw NutsNewsApiException.InvalidUrl()
        val url =
            baseUrl
                .newBuilder()
                .addQueryParameter("q", request.query)
                .addQueryParameter("page", request.page.toString())
                .addQueryParameter("limit", request.limit.toString())
                .build()

        return HttpRequest(
            url = url.toUri(),
            headers = mapOf("Accept" to "application/json"),
        )
    }

    private suspend fun fetchWithCache(
        request: HttpRequest,
        cacheKey: String,
        freshness: Duration,
        fetchPolicy: NutsNewsFetchPolicy,
    ): ArticleFetchResult {
        if (fetchPolicy == NutsNewsFetchPolicy.UseCache) {
            decodeCachedResponse(cacheKey = cacheKey, maxAge = freshness)?.let { response ->
                return ArticleFetchResult(
                    response = response,
                    source = ArticleFetchSource.FreshCache,
                )
            }
        }

        return try {
            val freshResponse = fetchFreshResponse(request)
            responseCache.write(key = cacheKey, response = freshResponse)
            ArticleFetchResult(
                response = decodeResponse(freshResponse),
                source = ArticleFetchSource.Network,
            )
        } catch (error: NutsNewsApiException) {
            decodeCachedResponse(cacheKey = cacheKey, maxAge = null)?.let { response ->
                return ArticleFetchResult(
                    response = response,
                    source = ArticleFetchSource.StaleCache,
                )
            }
            throw error
        }
    }

    private suspend fun decodeCachedResponse(
        cacheKey: String,
        maxAge: Duration?,
    ): ArticlesResponse? {
        val cachedResponse = responseCache.read(key = cacheKey, maxAge = maxAge) ?: return null
        return try {
            decodeResponse(cachedResponse)
        } catch (_: NutsNewsApiException.Decoding) {
            responseCache.remove(cacheKey)
            null
        }
    }

    private suspend fun fetchFreshResponse(request: HttpRequest): String {
        val response =
            try {
                transport.execute(request)
            } catch (error: SocketTimeoutException) {
                throw NutsNewsApiException.Timeout(error)
            } catch (error: InterruptedIOException) {
                throw NutsNewsApiException.Timeout(error)
            } catch (error: IOException) {
                throw NutsNewsApiException.Network(error)
            }

        validate(response)
        return response.body!!
    }

    private fun decodeResponse(response: String): ArticlesResponse =
        try {
            ArticleJsonDecoder.decodeResponse(response)
        } catch (error: ArticleDecodingException) {
            throw NutsNewsApiException.Decoding(error)
        }

    private fun validate(response: HttpResponse) {
        if (response.statusCode !in HttpStatusCodes) {
            throw NutsNewsApiException.InvalidResponse()
        }
        if (response.statusCode !in SuccessStatusCodes) {
            throw NutsNewsApiException.HttpStatus(response.statusCode)
        }
        if (response.body == null) {
            throw NutsNewsApiException.InvalidResponse()
        }
    }

    companion object {
        val FeedFreshness: Duration = Duration.ofMinutes(15)
        val SearchFreshness: Duration = Duration.ofMinutes(5)

        private val HttpStatusCodes = 100..599
        private val SuccessStatusCodes = 200..299

        internal fun articleCacheKey(
            page: Int,
            category: String?,
        ): String {
            val normalizedCategory = category?.trim()?.lowercase(Locale.ROOT)
            val categoryKey = normalizedCategory?.takeIf(String::isNotEmpty) ?: "all"
            return "articles:v1:page=$page:category=$categoryKey"
        }

        internal fun searchCacheKey(request: ArchiveSearchRequest): String =
            "search:v1:q=${request.query.trim().lowercase(Locale.ROOT)}" +
                ":page=${request.page}:limit=${request.limit}"
    }
}
