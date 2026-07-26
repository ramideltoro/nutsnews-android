package com.nutsnews.app.data.article

import com.nutsnews.app.core.model.ArticlesResponse
import com.nutsnews.app.core.network.HttpRequest
import com.nutsnews.app.core.network.HttpResponse
import com.nutsnews.app.core.network.HttpTransport
import com.nutsnews.app.data.network.OkHttpTransport
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
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
    ): ArticlesResponse {
        val request = articleRequest(page = page, category = category)
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
