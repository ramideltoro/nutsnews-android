package com.nutsnews.app.data.story

import androidx.room.Room
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.data.database.NutsNewsDatabase
import com.nutsnews.app.data.database.SavedStoryEntity
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
class RoomSavedStoryRepositoryTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "saved-story-repository-test.db"
    private val clock = MutableSavedStoryClock(Instant.parse("2026-07-26T12:00:00Z"))

    private lateinit var database: NutsNewsDatabase
    private lateinit var repository: RoomSavedStoryRepository

    @Before
    fun createDatabase() {
        context.deleteDatabase(databaseName)
        database = NutsNewsDatabase.create(context, databaseName)
        repository = RoomSavedStoryRepository(database.savedStoryDao(), clock)
    }

    @After
    fun closeDatabase() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
        context.deleteDatabase(databaseName)
    }

    @Test
    fun likingStoresTheFullStoryAndPublishesLikedStateAndCount() =
        runBlocking {
            val article = article("story-1", "https://nutsnews.com/story-1")

            repository.setLiked(article, isLiked = true)

            assertTrue(repository.isLiked(article.stableId))
            assertTrue(repository.observeIsLiked(article.stableId).first())
            assertEquals(1, repository.count.first())
            val savedStory = repository.stories.first().single()
            assertEquals(article.stableId, savedStory.id)
            assertEquals(
                article.copy(id = article.stableId.value),
                savedStory.article,
            )
            assertEquals(clock.instant(), savedStory.savedAt)
        }

    @Test
    fun unlikingRemovesTheSavedSnapshot() =
        runBlocking {
            val article = article("story-1", "https://nutsnews.com/story-1")
            repository.setLiked(article, isLiked = true)

            repository.setLiked(article, isLiked = false)

            assertFalse(repository.isLiked(article.stableId))
            assertFalse(repository.observeIsLiked(article.stableId).first())
            assertEquals(0, repository.count.first())
            assertEquals(emptyList(), repository.stories.first())
        }

    @Test
    fun duplicateStableIdsReplaceMetadataAndSavedTimeWithoutAddingRows() =
        runBlocking {
            val original = article("api-old", "https://nutsnews.com/shared")
            repository.save(original)
            clock.advance(Duration.ofMinutes(5))
            val refreshed =
                original.copy(
                    id = "api-new",
                    title = "Updated hopeful story",
                    summary = "Updated summary",
                    categories = listOf("Science"),
                )

            repository.save(refreshed)

            assertEquals(1, repository.count.first())
            val savedStory = repository.stories.first().single()
            assertEquals("Updated hopeful story", savedStory.article.title)
            assertEquals("Updated summary", savedStory.article.summary)
            assertEquals(listOf("Science"), savedStory.article.categories)
            assertEquals(clock.instant(), savedStory.savedAt)
        }

    @Test
    fun storiesAreOrderedByNewestSavedDate() =
        runBlocking {
            val first = article("first", "https://nutsnews.com/first")
            val second = article("second", "https://nutsnews.com/second")
            repository.save(first)
            clock.advance(Duration.ofSeconds(1))
            repository.save(second)

            assertEquals(
                listOf(second.stableId, first.stableId),
                repository.stories.first().map { it.id },
            )
        }

    @Test
    fun fullStorySnapshotRestoresAfterDatabaseReopen() =
        runBlocking {
            val article = article("story-1", "https://nutsnews.com/story-1")
            repository.save(article)
            database.close()

            database = NutsNewsDatabase.create(context, databaseName)
            repository = RoomSavedStoryRepository(database.savedStoryDao(), clock)

            val restored = repository.stories.first().single()
            assertEquals(article.stableId, restored.id)
            assertEquals(article.copy(id = article.stableId.value), restored.article)
            assertEquals(clock.instant(), restored.savedAt)
        }

    @Test
    fun malformedStoredUrlsAreIgnoredWithoutDiscardingTheSavedStory() =
        runBlocking {
            database.savedStoryDao().upsert(
                SavedStoryEntity(
                    stableArticleId = "malformed-story",
                    apiId = "api-malformed",
                    title = "Still readable",
                    summary = "The local snapshot remains available.",
                    originalUrl = "https://bad url",
                    source = "NutsNews",
                    publishedAt = null,
                    createdAt = null,
                    thumbnailUrl = "https://bad image",
                    categories = listOf("Community"),
                    savedAtEpochMillis = clock.millis(),
                ),
            )

            val restored = savedStories().single()
            assertEquals("Still readable", restored.article.title)
            assertEquals(null, restored.article.originalUrl)
            assertEquals(null, restored.article.thumbnailUrl)
            assertEquals(clock.instant(), restored.savedAt)
        }

    private suspend fun savedStories() = repository.stories.first()

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
            categories = listOf("Community", "Kindness"),
        )
}

private class MutableSavedStoryClock(
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
