package com.nutsnews.app.data.story

import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.data.database.NutsNewsDatabase
import com.nutsnews.app.data.database.StoryReflectionEntity
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
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
class RoomStoryReflectionRepositoryTest {
    private val context = RuntimeEnvironment.getApplication()
    private val databaseName = "story-reflection-repository-test.db"
    private val clock =
        MutableStoryReflectionClock(Instant.parse("2026-07-26T12:00:00Z"))

    private lateinit var database: NutsNewsDatabase
    private lateinit var repository: RoomStoryReflectionRepository

    @Before
    fun createDatabase() {
        context.deleteDatabase(databaseName)
        database = NutsNewsDatabase.create(context, databaseName)
        repository = RoomStoryReflectionRepository(database.storyReflectionDao(), clock)
    }

    @After
    fun closeDatabase() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
        context.deleteDatabase(databaseName)
    }

    @Test
    fun everyReactionPersistsWithCurrentStoryMetadataAndTimestamp() =
        runBlocking {
            StoryReflectionReaction.entries.forEachIndexed { index, reaction ->
                val article =
                    article(
                        id = "api-$index",
                        originalUrl = "https://nutsnews.com/story/$index",
                    )

                repository.setReaction(article, reaction)

                val reflection = repository.findReflection(article)
                requireNotNull(reflection)
                assertEquals(article.stableId, reflection.articleId)
                assertEquals(article.title, reflection.articleTitle)
                assertEquals(article.source, reflection.articleSource)
                assertEquals(reaction, reflection.reaction)
                assertEquals(clock.instant(), reflection.createdAt)
                clock.advance(Duration.ofSeconds(1))
            }

            assertEquals(StoryReflectionReaction.entries.size, repository.count.first())
        }

    @Test
    fun replacingReactionUpdatesOneRecordWithoutDuplication() =
        runBlocking {
            val article = article("api-42", "https://nutsnews.com/story")
            repository.setReaction(article, StoryReflectionReaction.Smile)
            val firstTimestamp = clock.instant()
            clock.advance(Duration.ofMinutes(3))

            val updatedArticle =
                article.copy(
                    title = "An even more hopeful story",
                    source = "Community News",
                )
            repository.setReaction(updatedArticle, StoryReflectionReaction.Hope)

            val reflection = repository.observeReflection(article).first()
            requireNotNull(reflection)
            assertEquals(StoryReflectionReaction.Hope, reflection.reaction)
            assertEquals(updatedArticle.title, reflection.articleTitle)
            assertEquals(updatedArticle.source, reflection.articleSource)
            assertEquals(clock.instant(), reflection.createdAt)
            assertEquals(1, repository.count.first())
            check(reflection.createdAt > firstTimestamp)
        }

    @Test
    fun legacyApiIdLookupMigratesToStableIdOnReplacement() =
        runBlocking {
            val article = article(" api-42 ", "https://nutsnews.com/story")
            database.storyReflectionDao().upsert(
                StoryReflectionEntity(
                    stableArticleId = "api-42",
                    legacyArticleId = null,
                    articleTitle = "Legacy title",
                    articleSource = "Legacy source",
                    reactionId = "revisit",
                    createdAtEpochMillis = 1,
                ),
            )

            assertEquals(
                StoryReflectionReaction.Revisit,
                repository.findReflection(article)?.reaction,
            )

            repository.setReaction(article, StoryReflectionReaction.Smile)

            val migrated = repository.findReflection(article)
            requireNotNull(migrated)
            assertEquals(article.stableId, migrated.articleId)
            assertEquals(StoryReflectionReaction.Smile, migrated.reaction)
            assertEquals(
                null,
                database
                    .storyReflectionDao()
                    .findReflection("api-42", legacyArticleId = null),
            )
            assertEquals(1, repository.count.first())
        }

    @Test
    fun aggregateCountAndSelectionsRestoreAfterDatabaseReopen() =
        runBlocking {
            val first = article("first", "https://nutsnews.com/first")
            val second = article("second", "https://nutsnews.com/second")
            repository.setReaction(first, StoryReflectionReaction.Hope)
            repository.setReaction(second, StoryReflectionReaction.Revisit)
            assertEquals(2, repository.count.first())
            database.close()

            database = NutsNewsDatabase.create(context, databaseName)
            repository =
                RoomStoryReflectionRepository(database.storyReflectionDao(), clock)

            assertEquals(2, repository.count.first())
            assertEquals(
                StoryReflectionReaction.Hope,
                repository.findReflection(first)?.reaction,
            )
            assertEquals(
                StoryReflectionReaction.Revisit,
                repository.findReflection(second)?.reaction,
            )
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

private class MutableStoryReflectionClock(
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
