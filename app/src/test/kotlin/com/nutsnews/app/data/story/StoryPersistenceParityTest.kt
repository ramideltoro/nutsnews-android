package com.nutsnews.app.data.story

import androidx.room.Room
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.data.database.NutsNewsDatabase
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
class StoryPersistenceParityTest {
    private val clock = MutableParityClock(Instant.parse("2026-07-26T12:00:00Z"))
    private lateinit var database: NutsNewsDatabase
    private lateinit var savedStories: RoomSavedStoryRepository
    private lateinit var notes: RoomStoryNoteRepository
    private lateinit var reflections: RoomStoryReflectionRepository
    private lateinit var readingStats: RoomReadingStatsRepository

    @Before
    fun createRepositories() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    NutsNewsDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        savedStories = RoomSavedStoryRepository(database.savedStoryDao(), clock)
        notes = RoomStoryNoteRepository(database.storyNoteDao(), clock)
        reflections = RoomStoryReflectionRepository(database.storyReflectionDao(), clock)
        readingStats = RoomReadingStatsRepository(database.readingActivityDao(), clock)
    }

    @After
    fun closeDatabase() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun oneStableIdentityJoinsEveryUserOwnedRecordAcrossRouteShapes() =
        runBlocking {
            val feedArticle = routeArticle(id = "feed-api", title = "Original feed title")
            val searchArticle =
                routeArticle(
                    id = "search-api",
                    title = "Refreshed search title",
                    source = "Updated source",
                )

            savedStories.setLiked(feedArticle, isLiked = true)
            notes.setNote(feedArticle, " A private note. ")
            reflections.setReaction(feedArticle, StoryReflectionReaction.Smile)
            readingStats.recordStoryOpen(feedArticle)

            clock.advance(Duration.ofMinutes(5))
            savedStories.save(searchArticle)
            notes.setNote(searchArticle, "Updated private note.")
            reflections.setReaction(searchArticle, StoryReflectionReaction.Hope)
            readingStats.recordStoryOpen(searchArticle)

            assertEquals(feedArticle.stableId, searchArticle.stableId)
            assertTrue(savedStories.isLiked(searchArticle.stableId))
            assertEquals(1, savedStories.count.first())
            assertEquals("Refreshed search title", savedStories.stories.first().single().article.title)
            assertEquals(clock.instant(), savedStories.stories.first().single().savedAt)

            val note = notes.findNote(feedArticle)
            requireNotNull(note)
            assertEquals("Updated private note.", note.text)
            assertEquals(clock.instant(), note.updatedAt)
            assertEquals(1, notes.count.first())

            val reflection = reflections.findReflection(searchArticle)
            requireNotNull(reflection)
            assertEquals(StoryReflectionReaction.Hope, reflection.reaction)
            assertEquals("Refreshed search title", reflection.articleTitle)
            assertEquals(clock.instant(), reflection.createdAt)
            assertEquals(1, reflections.count.first())

            val stats = readingStats.observeStats().first()
            assertEquals(1, stats.todayStoryCount)
            assertEquals(1, stats.totalUniqueStoryCount)
            assertEquals(clock.instant(), readingStats.lastOpenedAt(feedArticle.stableId))
        }

    private fun routeArticle(
        id: String,
        title: String,
        source: String = "NutsNews",
    ): Article =
        Article(
            id = id,
            title = title,
            summary = "Good things happened.",
            originalUrl = URI("https://nutsnews.com/shared-story"),
            source = source,
            publishedAt = "2026-07-25T12:00:00Z",
            createdAt = "2026-07-25T12:01:00Z",
            thumbnailUrl = URI("https://nutsnews.com/shared-story.jpg"),
            categories = listOf("Community"),
        )
}

private class MutableParityClock(
    initialInstant: Instant,
) : Clock() {
    private var currentInstant = initialInstant

    fun advance(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = Clock.fixed(currentInstant, zone)

    override fun instant(): Instant = currentInstant
}
