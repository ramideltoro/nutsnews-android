package com.nutsnews.app.core.network

import java.net.URI

data class HttpRequest(
    val url: URI,
    val headers: Map<String, String> = emptyMap(),
)

data class HttpResponse(
    val statusCode: Int,
    val body: String?,
)

fun interface HttpTransport {
    suspend fun execute(request: HttpRequest): HttpResponse
}
