package com.nutsnews.app.data.network

import com.nutsnews.app.core.network.HttpRequest
import com.nutsnews.app.core.network.HttpResponse
import com.nutsnews.app.core.network.HttpTransport
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync

class OkHttpTransport(
    private val client: OkHttpClient = sharedClient,
) : HttpTransport {
    override suspend fun execute(request: HttpRequest): HttpResponse {
        val okHttpRequest =
            Request
                .Builder()
                .url(request.url.toString())
                .apply {
                    request.headers.forEach { (name, value) ->
                        header(name, value)
                    }
                }
                .get()
                .build()

        return client.newCall(okHttpRequest).executeAsync().use { response ->
            HttpResponse(
                statusCode = response.code,
                body = response.body.string(),
            )
        }
    }

    companion object {
        const val DefaultTimeoutSeconds = 20L

        internal fun createDefaultClient(): OkHttpClient =
            OkHttpClient
                .Builder()
                .callTimeout(DefaultTimeoutSeconds, TimeUnit.SECONDS)
                .connectTimeout(DefaultTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(DefaultTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(DefaultTimeoutSeconds, TimeUnit.SECONDS)
                .build()

        private val sharedClient: OkHttpClient by lazy(::createDefaultClient)
    }
}
