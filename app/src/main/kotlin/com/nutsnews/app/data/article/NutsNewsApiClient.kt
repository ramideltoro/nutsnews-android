package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.core.network.HttpRequest
import com.nutsnews.app.core.network.HttpResponse
import com.nutsnews.app.core.network.HttpTransport
import com.nutsnews.app.data.network.OkHttpTransport
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
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

class NutsNewsApiClient(
    private val endpoints: NutsNewsEndpoints = NutsNewsEndpoints.Production,
    private val transport: HttpTransport = OkHttpTransport(),
) {
    suspend fun fetchArticles(
        page: Int = 0,
        category: String? = null,
    ): ArticlesResponse =
        executeAndDecode(
            articleRequest(page = page, category = category),
        )

    suspend fun searchArticles(
        query: String,
        page: Int = 0,
        limit: Int = ArchiveSearchRequest.DefaultPageSize,
    ): ArticlesResponse {
        val request = ArchiveSearchRequest.create(query = query, page = page, limit = limit)
        if (!request.meetsMinimum) {
            return ArticlesResponse(articles = emptyList(), nextPage = null)
        }

        return executeAndDecode(searchRequest(request))
    }

    fun searchOutcomes(
        query: String,
        page: Int = 0,
        limit: Int = ArchiveSearchRequest.DefaultPageSize,
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
                val response = executeAndDecode(searchRequest(request))
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

    private suspend fun executeAndDecode(request: HttpRequest): ArticlesResponse {
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
        return try {
            ArticleJsonDecoder.decodeResponse(response.body!!)
        } catch (error: ArticleDecodingException) {
            throw NutsNewsApiException.Decoding(error)
        }
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
        private val HttpStatusCodes = 100..599
        private val SuccessStatusCodes = 200..299
    }
}
