package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.data.database.NutsNewsDatabase
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class RoomReadingStatsRepositoryTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "reading-stats-repository-test.db"
    private val clock =
        MutableReadingStatsClock(
            initialInstant = Instant.parse("2026-07-26T12:00:00Z"),
            clockZone = ZoneId.of("America/New_York"),
        )

    private lateinit var database: NutsNewsDatabase
    private lateinit var repository: RoomReadingStatsRepository

    @Before
    fun createDatabase() {
        context.deleteDatabase(databaseName)
        database = NutsNewsDatabase.create(context, databaseName)
        repository = RoomReadingStatsRepository(database.readingActivityDao(), clock)
    }

    @After
    fun closeDatabase() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
        context.deleteDatabase(databaseName)
    }

    @Test
    fun duplicateSameDayStoryOpenStaysUniqueAndRefreshesLastOpen() =
        runBlocking {
            val article = article("api-42", "https://nutsnews.com/story")
            repository.recordStoryOpen(article)
            val firstOpen = clock.instant()
            clock.advance(Duration.ofHours(2))

            repository.recordStoryOpen(article.copy(id = "updated-api-id"))

            val stats = repository.observeStats().first()
            assertEquals(1, stats.todayStoryCount)
            assertEquals(1, stats.totalUniqueStoryCount)
            assertEquals(1, stats.currentStreak)
            assertEquals(clock.instant(), repository.lastOpenedAt(article.stableId))
            check(repository.lastOpenedAt(article.stableId)!! > firstOpen)
        }

    @Test
    fun dayChangesRetainDailyCountsAndDeduplicateAllTimeTotals() =
        runBlocking {
            val first = article("first", "https://nutsnews.com/first")
            val second = article("second", "https://nutsnews.com/second")
            repository.recordStoryOpen(first)
            clock.advance(Duration.ofDays(1))
            repository.recordStoryOpen(first)
            repository.recordStoryOpen(second)

            val stats = repository.observeStats().first()
            assertEquals(2, stats.todayStoryCount)
            assertEquals(2, stats.totalUniqueStoryCount)
            assertEquals(2, stats.currentStreak)
            assertEquals(listOf(1, 2), stats.recentDays.takeLast(2).map { it.storyCount })
        }

    @Test
    fun streakStopsAtBreakAndRecentDayBoundsIncludeZeroDays() =
        runBlocking {
            val article = article("story", "https://nutsnews.com/story")
            repository.recordStoryOpen(article)
            clock.advance(Duration.ofDays(2))
            repository.recordStoryOpen(article.copy(id = "story-again"))
            clock.advance(Duration.ofDays(1))
            repository.recordStoryOpen(article.copy(id = "story-today"))

            val stats = repository.observeStats().first()
            assertEquals(2, stats.currentStreak)
            assertEquals(7, stats.recentDays.size)
            assertEquals(
                listOf(0, 0, 0, 1, 0, 1, 1),
                stats.recentDays.map { it.storyCount },
            )
            assertEquals(1, repository.observeStats(recentDayCount = 0).first().recentDays.size)
            assertEquals(30, repository.observeStats(recentDayCount = 100).first().recentDays.size)
        }

    @Test
    fun originalStoryOpensIncrementPerLocalDayAcrossUtcBoundary() =
        runBlocking {
            clock.setInstant(Instant.parse("2026-07-27T02:00:00Z"))
            repository.recordOriginalStoryOpen()
            repository.recordOriginalStoryOpen()
            assertEquals(2, repository.observeStats().first().originalOpensTodayCount)

            clock.advance(Duration.ofHours(3))
            assertEquals(0, repository.observeStats().first().originalOpensTodayCount)
            repository.recordOriginalStoryOpen()
            assertEquals(1, repository.observeStats().first().originalOpensTodayCount)
        }

    @Test
    fun readingActivityRestoresAfterDatabaseReopen() =
        runBlocking {
            val article = article("story", "https://nutsnews.com/story")
            repository.recordStoryOpen(article)
            repository.recordOriginalStoryOpen()
            database.close()

            database = NutsNewsDatabase.create(context, databaseName)
            repository = RoomReadingStatsRepository(database.readingActivityDao(), clock)

            val stats = repository.observeStats().first()
            assertEquals(1, stats.todayStoryCount)
            assertEquals(1, stats.totalUniqueStoryCount)
            assertEquals(1, stats.originalOpensTodayCount)
            assertEquals(clock.instant(), repository.lastOpenedAt(article.stableId))
        }

    private fun article(
        id: String,
        originalUrl: String?,
    ): Article =
        Article(
            id = id,
            title = "A hopeful story",
            summary = "Good things happened.",
            originalUrl = originalUrl?.let(::URI),
            source = "NutsNews",
            publishedAt = "2026-07-25T12:00:00Z",
            createdAt = "2026-07-25T12:01:00Z",
            thumbnailUrl = URI("https://nutsnews.com/story.jpg"),
            categories = listOf("Community"),
        )
}

private class MutableReadingStatsClock(
    initialInstant: Instant,
    private val clockZone: ZoneId,
) : Clock() {
    private var currentInstant = initialInstant

    fun advance(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }

    fun setInstant(instant: Instant) {
        currentInstant = instant
    }

    override fun getZone(): ZoneId = clockZone

    override fun withZone(zone: ZoneId): Clock =
        MutableReadingStatsClock(currentInstant, zone)

    override fun instant(): Instant = currentInstant
}
