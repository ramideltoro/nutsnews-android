package com.nutsnews.app.data.article

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.Duration
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

interface ArticleResponseCache {
    suspend fun read(
        key: String,
        maxAge: Duration?,
    ): String?

    suspend fun write(
        key: String,
        response: String,
    )

    suspend fun remove(key: String)
}

object EmptyArticleResponseCache : ArticleResponseCache {
    override suspend fun read(
        key: String,
        maxAge: Duration?,
    ): String? = null

    override suspend fun write(
        key: String,
        response: String,
    ) = Unit

    override suspend fun remove(key: String) = Unit
}

class DiskArticleResponseCache(
    private val directory: Path,
    private val clock: Clock = Clock.systemUTC(),
) : ArticleResponseCache {
    private val mutex = Mutex()

    override suspend fun read(
        key: String,
        maxAge: Duration?,
    ): String? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val file = cacheFile(key)
                val envelope =
                    try {
                        decodeEnvelope(
                            Files
                                .readAllBytes(file)
                                .toString(StandardCharsets.UTF_8),
                        )
                    } catch (_: IOException) {
                        return@withLock null
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        deleteQuietly(file)
                        return@withLock null
                    }

                if (maxAge != null) {
                    val ageMillis = clock.millis() - envelope.cachedAtEpochMillis
                    if (ageMillis > maxAge.toMillis()) {
                        return@withLock null
                    }
                }

                envelope.response
            }
        }

    override suspend fun write(
        key: String,
        response: String,
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                var temporaryFile: Path? = null
                try {
                    Files.createDirectories(directory)
                    val target = cacheFile(key)
                    temporaryFile = Files.createTempFile(directory, "cache-", ".tmp")
                    Files.write(
                        temporaryFile,
                        encodeEnvelope(
                            CacheEnvelope(
                                cachedAtEpochMillis = clock.millis(),
                                response = response,
                            ),
                        ).toByteArray(StandardCharsets.UTF_8),
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                    )
                    try {
                        Files.move(
                            temporaryFile,
                            target,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(
                            temporaryFile,
                            target,
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    }
                    temporaryFile = null
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // A cache write must never block access to fresh stories.
                } finally {
                    temporaryFile?.let(::deleteQuietly)
                }
            }
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                deleteQuietly(cacheFile(key))
            }
        }
    }

    internal fun cacheFile(key: String): Path {
        val safeName =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(key.toByteArray(StandardCharsets.UTF_8))
        return directory.resolve("$safeName.json")
    }

    private fun encodeEnvelope(envelope: CacheEnvelope): String =
        buildJsonObject {
            put("cachedAtEpochMillis", envelope.cachedAtEpochMillis)
            put(
                "data",
                Base64
                    .getEncoder()
                    .encodeToString(envelope.response.toByteArray(StandardCharsets.UTF_8)),
            )
        }.toString()

    private fun decodeEnvelope(value: String): CacheEnvelope {
        val objectValue = Json.parseToJsonElement(value).jsonObject
        val cachedAt =
            objectValue["cachedAtEpochMillis"]
                ?.jsonPrimitive
                ?.longOrNull
                ?: error("Missing cache timestamp.")
        val encodedData =
            objectValue["data"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?: error("Missing cache data.")
        val response =
            Base64
                .getDecoder()
                .decode(encodedData)
                .toString(StandardCharsets.UTF_8)

        return CacheEnvelope(
            cachedAtEpochMillis = cachedAt,
            response = response,
        )
    }

    private fun deleteQuietly(file: Path) {
        try {
            Files.deleteIfExists(file)
        } catch (_: Exception) {
            // Cache cleanup is best effort.
        }
    }

    private data class CacheEnvelope(
        val cachedAtEpochMillis: Long,
        val response: String,
    )

    companion object {
        const val DirectoryName = "NutsNewsArticleResponses"
    }
}
