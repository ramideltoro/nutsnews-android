package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.data.database.NutsNewsDatabase
import com.nutsnews.app.data.database.StoryNoteEntity
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
class RoomStoryNoteRepositoryTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "story-note-repository-test.db"
    private val clock = MutableStoryNoteClock(Instant.parse("2026-07-26T12:00:00Z"))

    private lateinit var database: NutsNewsDatabase
    private lateinit var repository: RoomStoryNoteRepository

    @Before
    fun createDatabase() {
        context.deleteDatabase(databaseName)
        database = NutsNewsDatabase.create(context, databaseName)
        repository = RoomStoryNoteRepository(database.storyNoteDao(), clock)
    }

    @After
    fun closeDatabase() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
        context.deleteDatabase(databaseName)
    }

    @Test
    fun createAndUpdateNormalizeTextAndPreserveCurrentTimestamp() =
        runBlocking {
            val article = article("api-42", "https://nutsnews.com/story")
            repository.setNote(article, " \n  Remember this kindness. \t")

            val created = repository.findNote(article)
            requireNotNull(created)
            assertEquals(article.stableId, created.articleId)
            assertEquals("Remember this kindness.", created.text)
            assertEquals(clock.instant(), created.updatedAt)

            clock.advance(Duration.ofMinutes(3))
            repository.setNote(article.copy(title = "Updated title"), "  Updated note  ")

            val updated = repository.observeNote(article).first()
            requireNotNull(updated)
            assertEquals("Updated title", updated.articleTitle)
            assertEquals("Updated note", updated.text)
            assertEquals(clock.instant(), updated.updatedAt)
            assertEquals(1, repository.count.first())
        }

    @Test
    fun legacyApiIdLookupMigratesToTheStableIdOnWrite() =
        runBlocking {
            val article = article("api-42", "https://nutsnews.com/story")
            val legacy =
                StoryNoteEntity(
                    stableArticleId = "api-42",
                    legacyArticleId = null,
                    articleTitle = article.title,
                    text = "Legacy note",
                    updatedAtEpochMillis = 1,
                )
            database.storyNoteDao().upsert(legacy)

            assertEquals("Legacy note", repository.findNote(article)?.text)

            repository.setNote(article, "Stable note")

            assertEquals(article.stableId, repository.findNote(article)?.articleId)
            assertEquals(
                null,
                database.storyNoteDao().findNote("api-42", legacyArticleId = null),
            )
            assertEquals(1, repository.count.first())
        }

    @Test
    fun stableRecordWinsWhenStableAndLegacyRowsBothExist() =
        runBlocking {
            val article = article("api-42", "https://nutsnews.com/story")
            database.storyNoteDao().upsert(
                StoryNoteEntity(
                    stableArticleId = "api-42",
                    legacyArticleId = null,
                    articleTitle = article.title,
                    text = "Legacy note",
                    updatedAtEpochMillis = 1,
                ),
            )
            database.storyNoteDao().upsert(
                StoryNoteEntity(
                    stableArticleId = article.stableId.value,
                    legacyArticleId = "api-42",
                    articleTitle = article.title,
                    text = "Stable note",
                    updatedAtEpochMillis = 2,
                ),
            )

            assertEquals("Stable note", repository.findNote(article)?.text)
        }

    @Test
    fun blankNoteDeletesStableAndLegacyRecords() =
        runBlocking {
            val article = article("api-42", "https://nutsnews.com/story")
            database.storyNoteDao().upsert(
                StoryNoteEntity(
                    stableArticleId = "api-42",
                    legacyArticleId = null,
                    articleTitle = article.title,
                    text = "Legacy note",
                    updatedAtEpochMillis = 1,
                ),
            )
            repository.setNote(article, "Stable note")
            database.storyNoteDao().upsert(
                StoryNoteEntity(
                    stableArticleId = "api-42",
                    legacyArticleId = null,
                    articleTitle = article.title,
                    text = "Legacy note",
                    updatedAtEpochMillis = 1,
                ),
            )

            repository.setNote(article, " \n\t ")

            assertNull(repository.findNote(article))
            assertEquals(0, repository.count.first())
        }

    @Test
    fun countsAndNotesRestoreAfterDatabaseReopen() =
        runBlocking {
            val first = article("first", "https://nutsnews.com/first")
            val second = article("second", "https://nutsnews.com/second")
            repository.setNote(first, "First note")
            clock.advance(Duration.ofSeconds(1))
            repository.setNote(second, "Second note")
            database.storyNoteDao().upsert(
                StoryNoteEntity(
                    stableArticleId = "blank-legacy-note",
                    legacyArticleId = null,
                    articleTitle = "Blank",
                    text = " \n\t ",
                    updatedAtEpochMillis = 1,
                ),
            )
            assertEquals(2, repository.count.first())
            database.close()

            database = NutsNewsDatabase.create(context, databaseName)
            repository = RoomStoryNoteRepository(database.storyNoteDao(), clock)

            assertEquals(2, repository.count.first())
            assertEquals("First note", repository.findNote(first)?.text)
            assertEquals("Second note", repository.findNote(second)?.text)
            repository.clearNote(first)
            assertEquals(1, repository.count.first())
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

private class MutableStoryNoteClock(
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
