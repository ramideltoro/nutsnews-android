package com.nutsnews.app.data.database

import android.os.Build
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
@Config(sdk = [26, 35], manifest = Config.NONE)
class NutsNewsDatabaseMigrationTest {
    @JvmField
    @Rule
    val migrationHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            NutsNewsDatabase::class.java,
        )

    @Test
    fun everyCommittedSchemaPreservesAllUserOwnedDataAndIdentityRules() {
        assertEquals(
            (1..NutsNewsDatabase.SchemaVersion).toList(),
            CommittedSchemaVersions,
        )

        CommittedSchemaVersions.forEach { schemaVersion ->
            val databaseName = "nutsnews-migration-v$schemaVersion-api${Build.VERSION.SDK_INT}"
            RuntimeEnvironment.getApplication().deleteDatabase(databaseName)
            migrationHelper.createDatabase(databaseName, schemaVersion).apply {
                insertVersionOneFixture()
                close()
            }

            migrationHelper
                .runMigrationsAndValidate(
                    databaseName,
                    NutsNewsDatabase.SchemaVersion,
                    true,
                    *NutsNewsDatabaseMigrations.all.toTypedArray(),
                ).close()

            val database =
                Room
                    .databaseBuilder(
                        RuntimeEnvironment.getApplication(),
                        NutsNewsDatabase::class.java,
                        databaseName,
                    ).allowMainThreadQueries()
                    .addMigrations(*NutsNewsDatabaseMigrations.all.toTypedArray())
                    .build()

            try {
                assertMigratedFixture(database)
            } finally {
                database.close()
                RuntimeEnvironment.getApplication().deleteDatabase(databaseName)
            }
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertVersionOneFixture() {
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
                'https://nutsnews.com/saved',
                'api-saved',
                'Saved title',
                'Saved summary',
                'https://nutsnews.com/saved',
                'NutsNews',
                '2026-07-25T12:00:00Z',
                '2026-07-25T12:01:00Z',
                'https://nutsnews.com/saved.jpg',
                '["Community","Science"]',
                1753444800100
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO story_notes (
                stable_article_id,
                legacy_article_id,
                article_title,
                text,
                updated_at
            ) VALUES (
                'api-note',
                NULL,
                'Legacy note title',
                'Preserve this private note.',
                1753444800200
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO story_reflections (
                stable_article_id,
                legacy_article_id,
                article_title,
                article_source,
                reaction_id,
                created_at
            ) VALUES (
                'api-reflection',
                NULL,
                'Legacy reflection title',
                'NutsNews',
                'hope',
                1753444800300
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT OR IGNORE INTO reading_story_opens (
                day_key,
                stable_article_id,
                opened_at
            ) VALUES (
                '2026-07-26',
                'https://nutsnews.com/saved',
                1753444800400
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT OR IGNORE INTO reading_story_opens (
                day_key,
                stable_article_id,
                opened_at
            ) VALUES (
                '2026-07-26',
                'https://nutsnews.com/saved',
                1753444899999
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO reading_story_opens (
                day_key,
                stable_article_id,
                opened_at
            ) VALUES (
                '2026-07-26',
                'api-second-story',
                1753444800500
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO reading_story_opens (
                day_key,
                stable_article_id,
                opened_at
            ) VALUES (
                '2026-07-25',
                'https://nutsnews.com/saved',
                1753358400600
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO original_story_opens (
                day_key,
                open_count,
                updated_at
            ) VALUES (
                '2026-07-26',
                3,
                1753444800700
            )
            """.trimIndent(),
        )
    }

    private fun assertMigratedFixture(database: NutsNewsDatabase) =
        runBlocking {
            val saved = database.savedStoryDao().observeStories().first().single()
            assertEquals("https://nutsnews.com/saved", saved.stableArticleId)
            assertEquals("api-saved", saved.apiId)
            assertEquals(listOf("Community", "Science"), saved.categories)
            assertEquals(1_753_444_800_100, saved.savedAtEpochMillis)

            val note =
                database
                    .storyNoteDao()
                    .findNote(
                        stableArticleId = "https://nutsnews.com/note",
                        legacyArticleId = "api-note",
                    )
            requireNotNull(note)
            assertEquals("api-note", note.stableArticleId)
            assertEquals("Preserve this private note.", note.text)
            assertEquals(1_753_444_800_200, note.updatedAtEpochMillis)

            val reflection =
                database
                    .storyReflectionDao()
                    .findReflection(
                        stableArticleId = "https://nutsnews.com/reflection",
                        legacyArticleId = "api-reflection",
                    )
            requireNotNull(reflection)
            assertEquals("api-reflection", reflection.stableArticleId)
            assertEquals("hope", reflection.reactionId)
            assertEquals(1_753_444_800_300, reflection.createdAtEpochMillis)

            val storyOpens =
                database
                    .readingActivityDao()
                    .observeStoryOpens("2026-07-25", "2026-07-26")
                    .first()
            assertEquals(3, storyOpens.size)
            assertEquals(2, database.readingActivityDao().observeStoryCount("2026-07-26").first())
            assertEquals(2, database.readingActivityDao().observeTotalUniqueStoryCount().first())
            assertEquals(
                1_753_444_800_400,
                storyOpens
                    .single {
                        it.dayKey == "2026-07-26" &&
                            it.stableArticleId == "https://nutsnews.com/saved"
                    }.openedAtEpochMillis,
            )

            val originalOpens =
                database.readingActivityDao().findOriginalStoryOpens("2026-07-26")
            requireNotNull(originalOpens)
            assertEquals(3, originalOpens.openCount)
            assertEquals(1_753_444_800_700, originalOpens.updatedAtEpochMillis)
        }

    private companion object {
        val CommittedSchemaVersions = listOf(1)
    }
}
