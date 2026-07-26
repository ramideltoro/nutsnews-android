package com.nutsnews.app.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class NutsNewsDatabaseTest {
    private lateinit var database: NutsNewsDatabase

    @Before
    fun createDatabase() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    NutsNewsDatabase::class.java,
                ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun databaseCreatesAllDaoBoundariesAndObservableTables() =
        runBlocking {
            val story =
                SavedStoryEntity(
                    stableArticleId = "https://nutsnews.com/story",
                    apiId = "api-42",
                    title = "A hopeful story",
                    summary = "Good things happened.",
                    originalUrl = "https://nutsnews.com/story",
                    source = "NutsNews",
                    publishedAt = "2026-07-25T12:00:00Z",
                    createdAt = "2026-07-25T12:01:00Z",
                    thumbnailUrl = "https://nutsnews.com/story.jpg",
                    categories = listOf("Community", "Science"),
                    savedAtEpochMillis = 1_753_444_800_000,
                )
            database.savedStoryDao().upsert(story)
            assertEquals(listOf(story), database.savedStoryDao().observeStories().first())
            assertEquals(1, database.savedStoryDao().observeCount().first())

            val note =
                StoryNoteEntity(
                    stableArticleId = story.stableArticleId,
                    legacyArticleId = story.apiId,
                    articleTitle = story.title,
                    text = "Remember this.",
                    updatedAtEpochMillis = 1_753_444_900_000,
                )
            database.storyNoteDao().upsert(note)
            assertEquals(
                note,
                database
                    .storyNoteDao()
                    .observeNote(story.stableArticleId, story.apiId)
                    .first(),
            )

            val reflection =
                StoryReflectionEntity(
                    stableArticleId = story.stableArticleId,
                    legacyArticleId = story.apiId,
                    articleTitle = story.title,
                    articleSource = story.source,
                    reactionId = "hope",
                    createdAtEpochMillis = 1_753_445_000_000,
                )
            database.storyReflectionDao().upsert(reflection)
            assertEquals(
                reflection,
                database
                    .storyReflectionDao()
                    .observeReflection(story.stableArticleId, story.apiId)
                    .first(),
            )

            val open =
                ReadingStoryOpenEntity(
                    dayKey = "2026-07-26",
                    stableArticleId = story.stableArticleId,
                    openedAtEpochMillis = 1_753_445_100_000,
                )
            database.readingActivityDao().insertStoryOpen(open)
            assertEquals(
                listOf(open),
                database
                    .readingActivityDao()
                    .observeStoryOpens("2026-07-20", "2026-07-26")
                    .first(),
            )

            val originalOpens =
                OriginalStoryOpenEntity(
                    dayKey = "2026-07-26",
                    openCount = 2,
                    updatedAtEpochMillis = 1_753_445_200_000,
                )
            database.readingActivityDao().upsertOriginalStoryOpens(originalOpens)
            assertEquals(
                originalOpens,
                database.readingActivityDao().observeOriginalStoryOpens("2026-07-26").first(),
            )
        }

    @Test
    fun compositeReadingKeyDeduplicatesSameStoryOnTheSameDay() =
        runBlocking {
            val first =
                ReadingStoryOpenEntity(
                    dayKey = "2026-07-26",
                    stableArticleId = "story",
                    openedAtEpochMillis = 10,
                )
            val duplicate = first.copy(openedAtEpochMillis = 20)

            database.readingActivityDao().insertStoryOpen(first)
            database.readingActivityDao().insertStoryOpen(duplicate)

            assertEquals(1, database.readingActivityDao().observeStoryCount("2026-07-26").first())
            assertEquals(1, database.readingActivityDao().observeTotalUniqueStoryCount().first())
        }

    @Test
    fun legacyIdsRemainQueryableWithoutOverridingStableRecords() =
        runBlocking {
            val legacyNote =
                StoryNoteEntity(
                    stableArticleId = "api-42",
                    legacyArticleId = null,
                    articleTitle = "Legacy",
                    text = "Old note",
                    updatedAtEpochMillis = 1,
                )
            database.storyNoteDao().upsert(legacyNote)
            assertEquals(
                legacyNote,
                database
                    .storyNoteDao()
                    .observeNote("https://nutsnews.com/story", "api-42")
                    .first(),
            )

            val stableNote =
                legacyNote.copy(
                    stableArticleId = "https://nutsnews.com/story",
                    legacyArticleId = "api-42",
                    text = "Current note",
                    updatedAtEpochMillis = 2,
                )
            database.storyNoteDao().upsert(stableNote)
            assertEquals(
                stableNote,
                database
                    .storyNoteDao()
                    .observeNote(stableNote.stableArticleId, "api-42")
                    .first(),
            )
        }

    @Test
    fun categoryConverterRoundTripsAndRecoversMalformedValues() {
        val converter = ArticleCategoriesConverter()

        assertEquals(
            listOf("Animals", "Science"),
            converter.decode(converter.encode(listOf("Animals", "Science"))),
        )
        assertEquals(emptyList(), converter.decode("{"))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class NutsNewsDatabaseMigrationTest {
    @JvmField
    @Rule
    val migrationHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            NutsNewsDatabase::class.java,
        )

    @Test
    fun versionOneSchemaPassesTheMigrationHarness() {
        val databaseName = "nutsnews-migration-test"
        migrationHelper.createDatabase(databaseName, NutsNewsDatabase.SchemaVersion).apply {
            execSQL(
                """
                INSERT INTO saved_stories (
                    stable_article_id,
                    api_id,
                    title,
                    summary,
                    original_url,
                    source,
                    published_at,
                    created_at,
                    thumbnail_url,
                    categories,
                    saved_at
                ) VALUES (
                    'stable-id',
                    'api-id',
                    'Title',
                    'Summary',
                    NULL,
                    'Source',
                    NULL,
                    NULL,
                    NULL,
                    '[]',
                    100
                )
                """.trimIndent(),
            )
            close()
        }

        val migrated =
            migrationHelper.runMigrationsAndValidate(
                databaseName,
                NutsNewsDatabase.SchemaVersion,
                true,
                *NutsNewsDatabaseMigrations.all.toTypedArray(),
            )
        migrated.query("SELECT saved_at FROM saved_stories WHERE stable_article_id = 'stable-id'")
            .use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(100, cursor.getLong(0))
            }
        migrated.close()
    }
}
