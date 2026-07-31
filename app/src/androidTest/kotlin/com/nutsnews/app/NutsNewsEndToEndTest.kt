package com.nutsnews.app

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStats
import com.nutsnews.app.core.model.ReadingStatsDay
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryId
import com.nutsnews.app.core.model.StoryNote
import com.nutsnews.app.core.model.StoryReflection
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.core.network.HttpResponse
import com.nutsnews.app.data.article.NutsNewsApiClient
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.data.story.StoryNoteRepository
import com.nutsnews.app.data.story.StoryReflectionRepository
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.di.AppContainer
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppNavigator
import com.nutsnews.app.navigation.DefaultAppNavigator
import com.nutsnews.app.reminder.DailyReminderManager
import com.nutsnews.app.reminder.ReminderScheduleResult
import com.nutsnews.app.widget.WidgetData
import com.nutsnews.app.widget.WidgetDataProvider
import com.nutsnews.app.widget.WidgetRefreshRequester
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
class NutsNewsEndToEndTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private var activityScenario: ActivityScenario<MainActivity>? = null
    private var originalAnimatorDurationScale = "1"

    @Before
    fun disableDeviceAnimations() {
        originalAnimatorDurationScale =
            runShellCommand("settings get global animator_duration_scale")
                .trim()
                .takeUnless { value -> value.isEmpty() || value == "null" }
                ?: "1"
        setAnimatorDurationScale("0")
    }

    @After
    fun closeActivityAndRestoreAnimations() {
        activityScenario?.close()
        activityScenario = null
        setAnimatorDurationScale(originalAnimatorDurationScale)
    }

    @Test
    fun onboardingFeedAndArticleUserDataJourney() {
        val fixture = launchJourney(hasCompletedOnboarding = false)

        waitForTag("personalization_screen")
        composeRule
            .onNodeWithTag("topic_nature")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("mood_curious")
            .performScrollTo()
            .performClick()
        repeat(2) {
            composeRule
                .onNodeWithTag("goal_increase")
                .performScrollTo()
                .performClick()
        }
        composeRule
            .onNodeWithTag("reminder_toggle")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("reminder_time_20")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("personalization_save")
            .performScrollTo()
            .performClick()

        waitForTag("feed_article_list")
        composeRule.waitUntil(timeoutMillis = UiTimeoutMillis) {
            runBlocking {
                fixture.preferencesRepository.preferences
                    .first()
                    .hasCompletedOnboarding
            }
        }
        val preferences =
            runBlocking { fixture.preferencesRepository.preferences.first() }
        assertTrue("nature" in preferences.selectedTopicIds)
        assertEquals("curious", preferences.selectedMoodId)
        assertEquals(5, preferences.dailyGoal)
        assertEquals(listOf(20), fixture.reminderManager.scheduledHours)

        composeRule
            .onNodeWithTag("feed_category_science")
            .performClick()
            .assertIsSelected()
        scrollFeedTo(ScienceCardTag)
        composeRule
            .onNode(
                hasTestTag("article_like_story") and
                    hasAnyAncestor(hasTestTag(ScienceCardTag)),
                useUnmergedTree = true,
            ).performClick()
        composeRule.waitUntil(timeoutMillis = UiTimeoutMillis) {
            runBlocking { fixture.savedStories.isLiked(ScienceStoryId) }
        }
        composeRule
            .onNode(
                hasTestTag("article_read_story") and
                    hasAnyAncestor(hasTestTag(ScienceCardTag)),
                useUnmergedTree = true,
            ).performClick()

        waitForTag("article_detail")
        composeRule
            .onNodeWithTag("article_detail_like")
            .assertContentDescriptionEquals("Liked")
        composeRule
            .onNodeWithTag("article_detail_note_editor")
            .performScrollTo()
            .performClick()
            .performTextInput("Keep this hopeful discovery.")
        composeRule
            .onNodeWithTag("article_detail_note_save")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("article_detail_reflection_hope")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = UiTimeoutMillis) {
            runBlocking {
                fixture.notes.findNote(ScienceArticle)?.text ==
                    "Keep this hopeful discovery." &&
                    fixture.reflections.findReflection(ScienceArticle)?.reaction ==
                    StoryReflectionReaction.Hope
            }
        }
        pressSystemBack()
        waitForTag("feed_article_list")

        assertTrue(runBlocking { fixture.savedStories.isLiked(ScienceStoryId) })
        assertEquals(
            "Keep this hopeful discovery.",
            runBlocking { fixture.notes.findNote(ScienceArticle) }?.text,
        )
        assertEquals(
            StoryReflectionReaction.Hope,
            runBlocking { fixture.reflections.findReflection(ScienceArticle) }?.reaction,
        )
        assertNotNull(runBlocking { fixture.readingStats.lastOpenedAt(ScienceStoryId) })
    }

    @Test
    fun discoverySettingsHelpAndBackNavigationJourney() {
        val fixture = launchJourney(hasCompletedOnboarding = true)
        waitForTag("feed_article_list")

        openFeedDestination(AppDestination.ArchiveSearch)
        waitForTag("archive_search_screen")
        composeRule
            .onNodeWithTag("archive_search_query")
            .performTextInput("science")
        composeRule.onNodeWithTag("archive_search_submit").performClick()
        waitForTag("archive_search_results")
        composeRule
            .onNodeWithTag("archive_search_results")
            .performScrollToNode(
                hasTestTag("archive_search_result_${ScienceStoryId.value}"),
            )
        composeRule
            .onNodeWithTag("archive_search_save_${ScienceStoryId.value}")
            .performClick()
        composeRule.waitUntil(timeoutMillis = UiTimeoutMillis) {
            runBlocking { fixture.savedStories.isLiked(ScienceStoryId) }
        }
        composeRule
            .onNodeWithTag("archive_search_result_${ScienceStoryId.value}")
            .performClick()
        waitForTag("article_detail")
        pressSystemBack()
        waitForTag("archive_search_screen")
        composeRule.onNodeWithTag("archive_search_close").performClick()
        waitForTag("feed_article_list")

        openFeedDestination(AppDestination.SavedStories)
        waitForTag("saved_stories_screen")
        composeRule.waitUntil(timeoutMillis = UiTimeoutMillis) {
            composeRule
                .onAllNodesWithTag("saved_story_${ScienceStoryId.value}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("saved_stories_close").performClick()
        waitForTag("feed_article_list")

        openFeedDestination(AppDestination.GoodMood)
        waitForTag("good_mood_screen")
        composeRule
            .onNodeWithTag("good_mood_list")
            .performScrollToNode(hasTestTag("good_mood_choice_curious"))
        composeRule
            .onNodeWithTag("good_mood_choice_curious")
            .performClick()
        composeRule
            .onNodeWithTag("good_mood_list")
            .performScrollToNode(hasTestTag("good_mood_featured_label"))
        composeRule.onNodeWithTag("good_mood_featured_label").assertIsDisplayed()
        composeRule.onNodeWithTag("good_mood_close").performClick()
        waitForTag("feed_article_list")

        openFeedDestination(AppDestination.DailyDigest)
        waitForTag("daily_digest_screen")
        composeRule.onNodeWithTag("daily_digest_metric_stories").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_digest_close").performClick()
        waitForTag("feed_article_list")

        openFeedDestination(AppDestination.ReadingStats)
        waitForTag("reading_stats_screen")
        composeRule.onNodeWithTag("reading_stats_today").assertIsDisplayed()
        composeRule.onNodeWithTag("reading_stats_close").performClick()
        waitForTag("feed_article_list")

        openFeedDestination(AppDestination.Settings)
        waitForTag("settings_screen")
        composeRule.onNodeWithTag("settings_row_theme").performClick()
        waitForTag("theme_settings_screen")
        composeRule
            .onNodeWithTag("theme_settings_list")
            .performScrollToNode(hasTestTag("theme_option_sanJuan"))
        composeRule.onNodeWithTag("theme_option_sanJuan").performClick()
        composeRule.waitUntil(timeoutMillis = UiTimeoutMillis) {
            runBlocking {
                fixture.preferencesRepository.preferences.first().theme ==
                    NutsNewsAppTheme.Foxy
            }
        }
        pressSystemBack()
        waitForTag("settings_screen")

        composeRule.onNodeWithTag("settings_row_haptics").performClick()
        waitForTag("haptics_settings_screen")
        composeRule
            .onNodeWithTag("haptics_settings_switch")
            .assertIsOn()
            .performClick()
            .assertIsOff()
        pressSystemBack()
        waitForTag("settings_screen")

        composeRule.onNodeWithTag("settings_row_contact").performClick()
        waitForTag("contact_screen")
        composeRule.onNodeWithText("rami.deltoro@nutsnews.com").assertIsDisplayed()
        composeRule.onNodeWithTag("contact_back").performClick()
        waitForTag("settings_screen")

        composeRule.onNodeWithTag("settings_row_widget").performClick()
        waitForTag("widget_settings_screen")
        composeRule
            .onNodeWithTag("widget_settings_switch")
            .assertIsOn()
            .performClick()
            .assertIsOff()
        composeRule.onNodeWithTag("preference_settings_home").performClick()
        waitForTag("feed_article_list")

        openFeedDestination(AppDestination.Help)
        waitForTag("help_screen")
        composeRule
            .onNodeWithTag("help_list")
            .performScrollToNode(hasText("What is NutsNews for?"))
        composeRule.onNodeWithText("What is NutsNews for?").assertIsDisplayed()
        composeRule
            .onNodeWithTag("help_list")
            .performScrollToNode(hasTestTag("help_action_today_picks"))
        composeRule.onNodeWithTag("help_action_today_picks").performClick()
        waitForTag("daily_digest_screen")
        pressSystemBack()
        waitForTag("help_screen")
        composeRule.onNodeWithTag("help_close").performClick()
        waitForTag("feed_article_list")

        val finalPreferences =
            runBlocking { fixture.preferencesRepository.preferences.first() }
        assertEquals(NutsNewsAppTheme.Foxy, finalPreferences.theme)
        assertEquals(false, finalPreferences.hapticsEnabled)
        assertEquals(false, finalPreferences.showStatsOnLargeWidget)
    }

    private fun launchJourney(hasCompletedOnboarding: Boolean): JourneyAppContainer {
        val fixture = JourneyAppContainer(hasCompletedOnboarding)
        ApplicationProvider
            .getApplicationContext<NutsNewsApplication>()
            .container = fixture
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        return fixture
    }

    private fun openFeedDestination(destination: AppDestination) {
        composeRule.onNodeWithTag("feed_menu_button").performClick()
        composeRule
            .onNodeWithTag("feed_menu_${destination.route}")
            .performClick()
    }

    private fun scrollFeedTo(tag: String) {
        composeRule
            .onNodeWithTag("feed_article_list")
            .performScrollToNode(hasTestTag(tag))
    }

    private fun pressSystemBack() {
        requireNotNull(activityScenario).onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = UiTimeoutMillis) {
            composeRule
                .onAllNodesWithTag(tag)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun setAnimatorDurationScale(value: String) {
        runShellCommand("settings put global animator_duration_scale $value")
    }

    private fun runShellCommand(command: String): String {
        val descriptor =
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .executeShellCommand(command)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            return stream.readBytes().decodeToString()
        }
    }

    private companion object {
        const val UiTimeoutMillis = 10_000L
        const val ScienceCardTag = "feed_story_journey-science"
        val ScienceStoryId = StoryId("https://nutsnews.com/science")
    }
}

private class JourneyAppContainer(
    hasCompletedOnboarding: Boolean,
) : AppContainer {
    override val navigator: AppNavigator = DefaultAppNavigator()
    override val userPreferencesRepository =
        InMemoryUserPreferencesRepository(
            UserPreferences(hasCompletedOnboarding = hasCompletedOnboarding),
        )
    val preferencesRepository: InMemoryUserPreferencesRepository
        get() = userPreferencesRepository

    val savedStories = JourneySavedStoryRepository()
    override val savedStoryRepository: SavedStoryRepository = savedStories

    val notes = JourneyStoryNoteRepository()
    override val storyNoteRepository: StoryNoteRepository = notes

    val reflections = JourneyStoryReflectionRepository()
    override val storyReflectionRepository: StoryReflectionRepository = reflections

    val readingStats = JourneyReadingStatsRepository()
    override val readingStatsRepository: ReadingStatsRepository = readingStats

    val reminderManager = JourneyReminderManager()
    override val dailyReminderManager: DailyReminderManager = reminderManager

    override val articleApiClient =
        NutsNewsApiClient(
            transport = {
                HttpResponse(
                    statusCode = 200,
                    body = JourneyArticleResponse,
                )
            },
        )
    override val widgetDataProvider: WidgetDataProvider =
        object : WidgetDataProvider {
            override suspend fun load(forceRefresh: Boolean): WidgetData =
                WidgetData.Placeholder
        }
    override val widgetRefreshRequester = WidgetRefreshRequester { true }
}

private class JourneySavedStoryRepository : SavedStoryRepository {
    private val mutableStories = MutableStateFlow<List<SavedStory>>(emptyList())

    override val stories: Flow<List<SavedStory>> = mutableStories
    override val count: Flow<Int> = mutableStories.map { stories -> stories.size }

    override fun observeIsLiked(storyId: StoryId): Flow<Boolean> =
        mutableStories
            .map { stories -> stories.any { story -> story.id == storyId } }
            .distinctUntilChanged()

    override suspend fun isLiked(storyId: StoryId): Boolean =
        mutableStories.value.any { story -> story.id == storyId }

    override suspend fun setLiked(
        article: Article,
        isLiked: Boolean,
    ) {
        if (isLiked) save(article) else remove(article.stableId)
    }

    override suspend fun save(article: Article) {
        mutableStories.update { stories ->
            listOf(SavedStory(article, JourneyInstant)) +
                stories.filterNot { story -> story.id == article.stableId }
        }
    }

    override suspend fun remove(storyId: StoryId) {
        mutableStories.update { stories ->
            stories.filterNot { story -> story.id == storyId }
        }
    }
}

private class JourneyStoryNoteRepository : StoryNoteRepository {
    private val mutableNotes = MutableStateFlow<Map<StoryId, StoryNote>>(emptyMap())

    override val count: Flow<Int> = mutableNotes.map { notes -> notes.size }

    override fun observeNote(article: Article): Flow<StoryNote?> =
        mutableNotes
            .map { notes -> notes[article.stableId] ?: notes[StoryId(article.id.trim())] }
            .distinctUntilChanged()

    override suspend fun findNote(article: Article): StoryNote? =
        mutableNotes.value[article.stableId] ?: mutableNotes.value[StoryId(article.id.trim())]

    override suspend fun setNote(
        article: Article,
        text: String,
    ) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) {
            clearNote(article)
            return
        }
        mutableNotes.update { notes ->
            notes - StoryId(article.id.trim()) +
                (
                    article.stableId to
                        StoryNote(
                            articleId = article.stableId,
                            articleTitle = article.title,
                            text = cleaned,
                            updatedAt = JourneyInstant,
                        )
                )
        }
    }

    override suspend fun clearNote(article: Article) {
        mutableNotes.update { notes ->
            notes - article.stableId - StoryId(article.id.trim())
        }
    }
}

private class JourneyStoryReflectionRepository : StoryReflectionRepository {
    private val mutableReflections =
        MutableStateFlow<Map<StoryId, StoryReflection>>(emptyMap())

    override val count: Flow<Int> =
        mutableReflections.map { reflections -> reflections.size }

    override fun observeReflection(article: Article): Flow<StoryReflection?> =
        mutableReflections
            .map { reflections ->
                reflections[article.stableId] ?: reflections[StoryId(article.id.trim())]
            }.distinctUntilChanged()

    override suspend fun findReflection(article: Article): StoryReflection? =
        mutableReflections.value[article.stableId]
            ?: mutableReflections.value[StoryId(article.id.trim())]

    override suspend fun setReaction(
        article: Article,
        reaction: StoryReflectionReaction,
    ) {
        mutableReflections.update { reflections ->
            reflections - StoryId(article.id.trim()) +
                (
                    article.stableId to
                        StoryReflection(
                            articleId = article.stableId,
                            articleTitle = article.title,
                            articleSource = article.source,
                            reaction = reaction,
                            createdAt = JourneyInstant,
                        )
                )
        }
    }
}

private class JourneyReadingStatsRepository : ReadingStatsRepository {
    private val mutableStats =
        MutableStateFlow(
            ReadingStats(
                todayStoryCount = 2,
                originalOpensTodayCount = 1,
                totalUniqueStoryCount = 2,
                currentStreak = 2,
                recentDays =
                    (20..26).map { day ->
                        ReadingStatsDay(
                            date = LocalDate.of(2026, 7, day),
                            storyCount = if (day >= 25) 2 else 0,
                        )
                    },
            ),
        )
    private val openedStories = mutableSetOf<StoryId>()
    private val lastOpened = mutableMapOf<StoryId, Instant>()

    override fun observeStats(recentDayCount: Int): Flow<ReadingStats> = mutableStats

    override suspend fun recordStoryOpen(article: Article) {
        lastOpened[article.stableId] = JourneyInstant
        if (openedStories.add(article.stableId)) {
            mutableStats.update { stats ->
                stats.copy(
                    todayStoryCount = stats.todayStoryCount + 1,
                    totalUniqueStoryCount = stats.totalUniqueStoryCount + 1,
                    recentDays =
                        stats.recentDays.map { day ->
                            if (day.date == LocalDate.of(2026, 7, 26)) {
                                day.copy(storyCount = day.storyCount + 1)
                            } else {
                                day
                            }
                        },
                )
            }
        }
    }

    override suspend fun recordOriginalStoryOpen() {
        mutableStats.update { stats ->
            stats.copy(originalOpensTodayCount = stats.originalOpensTodayCount + 1)
        }
    }

    override suspend fun lastOpenedAt(storyId: StoryId): Instant? = lastOpened[storyId]
}

private class JourneyReminderManager : DailyReminderManager {
    val scheduledHours = mutableListOf<Int>()

    override val canPostNotifications: Boolean = true
    override val requiresRuntimePermission: Boolean = false

    override fun createNotificationChannel() = Unit

    override fun schedule(hour: Int): ReminderScheduleResult {
        scheduledHours += hour
        return ReminderScheduleResult.Scheduled(JourneyInstant.toEpochMilli())
    }

    override fun cancel() {
        scheduledHours.clear()
    }

    override fun deliverReminder(): Boolean = true
}

private val ScienceArticle =
    Article(
        id = "journey-science",
        title = "Science discovery protects rare animals",
        summary = "Researchers and neighbors created a hopeful new wildlife habitat.",
        originalUrl = java.net.URI("https://nutsnews.com/science"),
        source = "NutsNews Science",
        publishedAt = "2026-07-26T11:00:00Z",
        createdAt = "2026-07-26T11:01:00Z",
        thumbnailUrl = java.net.URI("file:///android_asset/journey-science.jpg"),
        categories = listOf("Science", "Animals"),
    )

private val JourneyInstant = Instant.parse("2026-07-26T12:00:00Z")

private val JourneyArticleResponse =
    """
    {
      "articles": [
        {
          "id": "journey-science",
          "title": "Science discovery protects rare animals",
          "summary": "Researchers and neighbors created a hopeful new wildlife habitat.",
          "originalUrl": "https://nutsnews.com/science",
          "source": "NutsNews Science",
          "publishedAt": "2026-07-26T11:00:00Z",
          "createdAt": "2026-07-26T11:01:00Z",
          "thumbnailUrl": "file:///android_asset/journey-science.jpg",
          "categories": ["Science", "Animals"]
        },
        {
          "id": "journey-community",
          "title": "Kind community rescue brings hope",
          "summary": "Volunteers helped a local family begin again.",
          "originalUrl": "https://nutsnews.com/community",
          "source": "NutsNews Community",
          "publishedAt": "2026-07-26T10:00:00Z",
          "thumbnailUrl": "file:///android_asset/journey-community.jpg",
          "categories": ["Community"]
        },
        {
          "id": "journey-calm",
          "title": "Peaceful garden restores a neighborhood",
          "summary": "A gentle nature project gave neighbors a quiet place to gather.",
          "originalUrl": "https://nutsnews.com/calm",
          "source": "NutsNews Nature",
          "publishedAt": "2026-07-26T09:00:00Z",
          "thumbnailUrl": "file:///android_asset/journey-calm.jpg",
          "categories": ["Nature", "Wellness"]
        }
      ],
      "nextPage": null
    }
    """.trimIndent()
