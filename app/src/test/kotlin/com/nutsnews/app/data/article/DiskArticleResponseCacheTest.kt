package com.nutsnews.app.data.article

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DiskArticleResponseCacheTest {
    @JvmField
    @Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun freshAndStaleReadsUseTheInjectedClockWithoutDeletingExpiredData() =
        runBlocking {
            val clock = MutableClock(Instant.parse("2026-07-26T12:00:00Z"))
            val cache = cache(clock)
            val key = "articles:v1:page=0:category=all"

            cache.write(key, """{"articles":[{"id":"cached"}]}""")
            clock.advance(Duration.ofMinutes(15))
            assertEquals(
                """{"articles":[{"id":"cached"}]}""",
                cache.read(key, maxAge = NutsNewsApiClient.FeedFreshness),
            )

            clock.advance(Duration.ofMillis(1))
            assertNull(cache.read(key, maxAge = NutsNewsApiClient.FeedFreshness))
            assertEquals(
                """{"articles":[{"id":"cached"}]}""",
                cache.read(key, maxAge = null),
            )
            assertTrue(Files.exists(cache.cacheFile(key)))
        }

    @Test
    fun corruptEnvelopeIsEvicted() =
        runBlocking {
            val cache = cache(MutableClock(Instant.parse("2026-07-26T12:00:00Z")))
            val key = "articles:v1:page=1:category=all"
            val file = cache.cacheFile(key)
            Files.createDirectories(file.parent)
            Files.write(file, "{".toByteArray(StandardCharsets.UTF_8))

            assertNull(cache.read(key, maxAge = Duration.ofMinutes(15)))
            assertFalse(Files.exists(file))
        }

    @Test
    fun removeDeletesTheEnvelopeAndMissingEntriesAreHarmless() =
        runBlocking {
            val cache = cache(MutableClock(Instant.parse("2026-07-26T12:00:00Z")))
            val key = "search:v1:q=science:page=0:limit=20"

            cache.write(key, """{"articles":[]}""")
            assertTrue(Files.exists(cache.cacheFile(key)))
            cache.remove(key)
            cache.remove(key)

            assertFalse(Files.exists(cache.cacheFile(key)))
            assertNull(cache.read(key, maxAge = null))
        }

    @Test
    fun safeFilenameMatchesTheIosUrlSafeBase64Rule() {
        val cache = cache(MutableClock(Instant.parse("2026-07-26T12:00:00Z")))

        assertEquals(
            "YXJ0aWNsZXM6djE6cGFnZT0wOmNhdGVnb3J5PWFsbA.json",
            cache
                .cacheFile("articles:v1:page=0:category=all")
                .fileName
                .toString(),
        )
    }

    @Test
    fun diskFailuresNeverBlockCallers() =
        runBlocking {
            val regularFile = temporaryFolder.newFile("not-a-directory")
            val cache =
                DiskArticleResponseCache(
                    directory = regularFile.toPath().resolve("responses"),
                    clock = MutableClock(Instant.parse("2026-07-26T12:00:00Z")),
                )

            cache.write("articles:v1:page=0:category=all", """{"articles":[]}""")

            assertNull(cache.read("articles:v1:page=0:category=all", maxAge = null))
        }

    private fun cache(clock: Clock): DiskArticleResponseCache =
        DiskArticleResponseCache(
            directory =
                temporaryFolder.root
                    .toPath()
                    .resolve(DiskArticleResponseCache.DirectoryName),
            clock = clock,
        )
}

internal class MutableClock(
    initialInstant: Instant,
) : Clock() {
    @Volatile
    private var currentInstant = initialInstant

    fun advance(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = Clock.fixed(currentInstant, zone)

    override fun instant(): Instant = currentInstant
}
