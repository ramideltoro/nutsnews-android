package com.nutsnews.app.parity

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import com.nutsnews.app.core.model.Article
import com.nutsnews.app.core.model.ReadingStatsDay
import com.nutsnews.app.core.model.SavedStory
import com.nutsnews.app.core.model.StoryReflection
import com.nutsnews.app.core.model.StoryReflectionReaction
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.NutsNewsThemePreview
import com.nutsnews.app.feature.article.ArticleDetailScreen
import com.nutsnews.app.feature.article.ArticleListenModeContent
import com.nutsnews.app.feature.article.ArticleListenPlaybackState
import com.nutsnews.app.feature.article.ArticleListenSegment
import com.nutsnews.app.feature.article.ArticleListenUiState
import com.nutsnews.app.feature.article.ArticleShareCardUiState
import com.nutsnews.app.feature.article.PositiveShareCardRenderer
import com.nutsnews.app.feature.article.UnavailableArticleDetailScreen
import com.nutsnews.app.feature.article.buildArticleListenScript
import com.nutsnews.app.feature.article.deriveArticleBrief
import com.nutsnews.app.feature.digest.DailyDigestScreen
import com.nutsnews.app.feature.feed.ArticleFeedContent
import com.nutsnews.app.feature.feed.ArticleFeedUiState
import com.nutsnews.app.feature.feed.FeedScreen
import com.nutsnews.app.feature.help.HelpFaqScreen
import com.nutsnews.app.feature.home.HomeDashboard
import com.nutsnews.app.feature.home.HomeDashboardUiState
import com.nutsnews.app.feature.mood.GoodMoodScreen
import com.nutsnews.app.feature.personalization.PersonalizationMode
import com.nutsnews.app.feature.personalization.PersonalizationScreen
import com.nutsnews.app.feature.personalization.PersonalizationUiState
import com.nutsnews.app.feature.saved.SavedStoriesScreen
import com.nutsnews.app.feature.saved.SavedStoriesUiState
import com.nutsnews.app.feature.search.ArchiveSearchScreen
import com.nutsnews.app.feature.search.ArchiveSearchUiState
import com.nutsnews.app.feature.settings.HapticsSettingsScreen
import com.nutsnews.app.feature.settings.SettingsScreen
import com.nutsnews.app.feature.settings.SettingsUiState
import com.nutsnews.app.feature.settings.WidgetSettingsScreen
import com.nutsnews.app.feature.splash.StartupSplash
import com.nutsnews.app.feature.splash.StartupSplashStage
import com.nutsnews.app.feature.splash.StartupSplashUiState
import com.nutsnews.app.feature.stats.ReadingStatsScreen
import com.nutsnews.app.feature.stats.ReadingStatsUiState
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

abstract class ComposeScreenshotGoldenContract {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    protected fun captureScenes(
        scenes: List<GoldenScene>,
        fontScale: Float = 1f,
    ) {
        require(scenes.isNotEmpty())
        composeRule.mainClock.autoAdvance = false

        scenes.forEach { scene ->
            composeRule.activity.runOnUiThread {
                composeRule.activity.setContent {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, fontScale),
                    ) {
                        NutsNewsTheme(
                            theme = scene.theme,
                            updateSystemBars = false,
                            reducedMotionOverride = true,
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                scene.content()
                            }
                        }
                    }
                }
            }
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.waitForIdle()
            ScreenshotGolden.assertMatches(scene.name, captureRoot())
        }
    }

    private fun captureRoot(): Bitmap =
        composeRule.runOnIdle {
            captureLargestWindow()
        }
}

data class GoldenScene(
    val name: String,
    val theme: NutsNewsAppTheme = NutsNewsAppTheme.Amber,
    val content: @Composable () -> Unit,
) {
    override fun toString(): String = name
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneScreenScreenshotGoldenTest(
    private val scene: GoldenScene,
) : ComposeScreenshotGoldenContract() {
    @Test
    fun approvedPhoneSceneMatches() {
        captureScenes(listOf(scene))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> =
            phoneGoldenScenes().map { scene -> arrayOf(scene) }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PositiveShareCardScreenshotGoldenTest {
    @Test
    fun deterministicShareCardMatches() {
        ScreenshotGolden.assertMatches(
            "phone_s12_share_card",
            PositiveShareCardRenderer.render(
                article = PrimaryArticle,
                whyGood =
                    "Neighbors turned a shared challenge into a welcoming place for everyone.",
                takeaway = "Small acts of care can grow into lasting community change.",
                moodLabel = "Hopeful",
            ),
        )
    }
}

private fun phoneGoldenScenes(): List<GoldenScene> {
    val brief = deriveArticleBrief(PrimaryArticle)
    return buildList {
        add(
            GoldenScene("phone_s01_splash") {
                StartupSplash(
                    StartupSplashUiState(StartupSplashStage.SubtitleVisible),
                )
            },
        )
        NutsNewsAppTheme.entries.forEach { theme ->
            add(
                GoldenScene(
                    name = "phone_s19_theme_${theme.rawValue.lowercase()}",
                    theme = theme,
                ) {
                    NutsNewsThemePreview(theme)
                },
            )
        }
        add(
            GoldenScene("phone_s02_personalization_loading") {
                Personalization(PersonalizationUiState(isLoading = true))
            },
        )
        add(
            GoldenScene("phone_s02_personalization_populated") {
                Personalization(
                    PersonalizationUiState(
                        isLoading = false,
                        selectedTopicIds = setOf("community", "science", "nature"),
                        selectedMoodId = "hopeful",
                        dailyGoal = 4,
                        reminderEnabled = true,
                        reminderHour = 20,
                    ),
                )
            },
        )
        add(GoldenScene("phone_s03_s04_feed_loading") {
            Feed(ArticleFeedUiState(isInitialLoading = true))
        })
        add(GoldenScene("phone_s03_s04_feed_empty") {
            Feed(
                ArticleFeedUiState(
                    availableCategories = Categories,
                    selectedCategory = "Science",
                ),
            )
        })
        add(GoldenScene("phone_s03_s04_feed_error") {
            Feed(
                ArticleFeedUiState(
                    availableCategories = Categories,
                    errorMessage = "NutsNews could not reach the feed.",
                ),
            )
        })
        add(GoldenScene("phone_s03_s04_s05_s06_feed_populated") {
            Feed(
                ArticleFeedUiState(
                    articles = Articles,
                    availableCategories = Categories,
                    nextPage = 2,
                ),
            )
        })
        add(GoldenScene("phone_s05_dashboard_loading") {
            Dashboard(HomeDashboardUiState(), emptyList(), true)
        })
        add(GoldenScene("phone_s05_dashboard_empty") {
            Dashboard(DashboardState, emptyList(), false)
        })
        add(GoldenScene("phone_s05_dashboard_populated") {
            Dashboard(DashboardState, Articles, false)
        })
        add(GoldenScene("phone_s07_s08_s09_s10_s12_article_populated") {
            ArticleDetailScreen(
                article = PrimaryArticle,
                onClose = {},
                heroImageModel = null,
                isLiked = true,
                noteDraft = "Remember this hopeful idea.",
                hasSavedNote = true,
                reflection = Reflection,
            )
        })
        add(GoldenScene("phone_s09_s11_article_listening") {
            ArticleDetailScreen(
                article = PrimaryArticle,
                onClose = {},
                heroImageModel = null,
                listenUiState = ListeningState,
            )
        })
        add(GoldenScene("phone_s12_article_share_error") {
            ArticleDetailScreen(
                article = PrimaryArticle,
                onClose = {},
                heroImageModel = null,
                shareCardUiState =
                    ArticleShareCardUiState(
                        failureMessage =
                            "The positive share card couldn’t be created. Please try again.",
                    ),
            )
        })
        add(GoldenScene("phone_s07_article_unavailable") {
            UnavailableArticleDetailScreen(onClose = {})
        })
        add(GoldenScene("phone_s11_listen_mode_sheet") {
            NutsNewsBackground {
                ArticleListenModeContent(
                    brief = brief,
                    script = buildArticleListenScript(PrimaryArticle, brief),
                    uiState = ListeningState,
                    onToggle = {},
                    onStop = {},
                    onDismiss = {},
                )
            }
        })
        add(GoldenScene("phone_s13_saved_loading") {
            SavedStories(SavedStoriesUiState(isLoading = true))
        })
        add(GoldenScene("phone_s13_saved_empty") {
            SavedStories(SavedStoriesUiState(isLoading = false))
        })
        add(GoldenScene("phone_s13_saved_populated") {
            SavedStories(
                SavedStoriesUiState(
                    isLoading = false,
                    stories = SavedStories,
                    filteredStories = SavedStories,
                ),
            )
        })
        add(GoldenScene("phone_s14_search_initial") {
            ArchiveSearch(ArchiveSearchUiState())
        })
        add(GoldenScene("phone_s14_search_loading") {
            ArchiveSearch(
                ArchiveSearchUiState(
                    query = "science",
                    searchedQuery = "science",
                    isSearching = true,
                    hasSearched = true,
                ),
            )
        })
        add(GoldenScene("phone_s14_search_empty") {
            ArchiveSearch(
                ArchiveSearchUiState(
                    query = "science",
                    searchedQuery = "science",
                    hasSearched = true,
                ),
            )
        })
        add(GoldenScene("phone_s14_search_error") {
            ArchiveSearch(
                ArchiveSearchUiState(
                    query = "science",
                    searchedQuery = "science",
                    hasSearched = true,
                    errorMessage = "NutsNews could not reach the archive.",
                    failedPage = 0,
                ),
            )
        })
        add(GoldenScene("phone_s14_search_populated") {
            ArchiveSearch(
                ArchiveSearchUiState(
                    query = "community",
                    searchedQuery = "community",
                    articles = Articles,
                    savedStoryIds = setOf(PrimaryArticle.stableId),
                    hasSearched = true,
                ),
            )
        })
        add(GoldenScene("phone_s15_mood_empty") { GoodMood(emptyList()) })
        add(GoldenScene("phone_s15_mood_populated") { GoodMood(Articles) })
        add(GoldenScene("phone_s16_digest_empty") { Digest(emptyList()) })
        add(GoldenScene("phone_s16_digest_populated") { Digest(Articles) })
        add(GoldenScene("phone_s17_stats_loading") {
            ReadingStatsScreen(ReadingStatsUiState(isLoading = true), {})
        })
        add(GoldenScene("phone_s17_stats_empty") {
            ReadingStatsScreen(ReadingStatsUiState(isLoading = false), {})
        })
        add(GoldenScene("phone_s17_stats_populated") {
            ReadingStatsScreen(ReadingStatsState, {})
        })
        add(GoldenScene("phone_s18_settings_loading") {
            Settings(SettingsUiState(isLoading = true))
        })
        add(GoldenScene("phone_s18_settings_populated") {
            Settings(
                SettingsUiState(
                    isLoading = false,
                    theme = NutsNewsAppTheme.Amber,
                    hapticsEnabled = true,
                    showStatsOnLargeWidget = true,
                ),
            )
        })
        add(GoldenScene("phone_s20_haptics") {
            HapticsSettingsScreen(true, {}, {}, {})
        })
        add(GoldenScene("phone_s21_widget_settings") {
            WidgetSettingsScreen(true, {}, {}, {})
        })
        add(GoldenScene("phone_s22_help") {
            HelpFaqScreen({}, {}, {}, {}, {}, {}, {}, {})
        })
        add(GoldenScene("phone_s23_notification_reminder_entry") {
            Personalization(
                PersonalizationUiState(
                    isLoading = false,
                    selectedTopicIds = setOf("community", "science"),
                    selectedMoodId = "calm",
                    reminderEnabled = true,
                    reminderHour = 8,
                    statusText =
                        "Notifications are off. Enable them in Android settings.",
                ),
            )
        })
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w800dp-h1280dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TabletScreenScreenshotGoldenTest : ComposeScreenshotGoldenContract() {
    @Test
    fun tabletFeedRemainsAdaptivelyBounded() {
        captureScenes(
            listOf(
                GoldenScene("tablet_s03_s05_s06_feed_populated") {
                    Feed(
                        ArticleFeedUiState(
                            articles = Articles,
                            availableCategories = Categories,
                        ),
                    )
                },
            ),
        )
    }

    @Test
    fun tabletDetailRemainsAdaptivelyBounded() {
        captureScenes(
            listOf(
                GoldenScene("tablet_s07_s08_s09_article_populated") {
                    ArticleDetailScreen(
                        article = PrimaryArticle,
                        onClose = {},
                        heroImageModel = null,
                        isLiked = true,
                    )
                },
            ),
        )
    }

    @Test
    fun tabletSettingsRemainAdaptivelyBounded() {
        captureScenes(
            listOf(
                GoldenScene("tablet_s18_settings_populated") {
                    Settings(
                        SettingsUiState(
                            isLoading = false,
                            theme = NutsNewsAppTheme.Amber,
                        ),
                    )
                },
            ),
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LargeTextScreenScreenshotGoldenTest : ComposeScreenshotGoldenContract() {
    @Test
    fun largeTextOnboardingStaysReadable() {
        captureScenes(
            scenes =
                listOf(
                    GoldenScene("large_text_s02_personalization") {
                        Personalization(
                            PersonalizationUiState(
                                isLoading = false,
                                selectedTopicIds = setOf("community", "science"),
                                selectedMoodId = "hopeful",
                                dailyGoal = 3,
                            ),
                        )
                    },
                ),
            fontScale = 1.5f,
        )
    }

    @Test
    fun largeTextFeedStaysReadable() {
        captureScenes(
            scenes =
                listOf(
                    GoldenScene("large_text_s03_s06_feed") {
                        Feed(
                            ArticleFeedUiState(
                                articles = Articles,
                                availableCategories = Categories,
                            ),
                        )
                    },
                ),
            fontScale = 1.5f,
        )
    }

    @Test
    fun largeTextDetailStaysReadable() {
        captureScenes(
            scenes =
                listOf(
                    GoldenScene("large_text_s07_s09_article") {
                        ArticleDetailScreen(
                            article = PrimaryArticle,
                            onClose = {},
                            heroImageModel = null,
                        )
                    },
                ),
            fontScale = 1.5f,
        )
    }
}

@Composable
private fun Personalization(uiState: PersonalizationUiState) {
    PersonalizationScreen(
        uiState = uiState,
        mode = PersonalizationMode.FirstRun,
        onTopicToggled = {},
        onMoodSelected = {},
        onDailyGoalChanged = {},
        onReminderEnabledChanged = {},
        onReminderHourSelected = {},
        onSave = {},
        onClose = {},
    )
}

@Composable
private fun Feed(uiState: ArticleFeedUiState) {
    FeedScreen(
        uiState = uiState,
        onDestinationSelected = {},
        onCategorySelected = {},
    ) {
        ArticleFeedContent(
            uiState = uiState,
            onRefresh = {},
            onRetry = {},
            onLoadMore = {},
            onOpenArticle = {},
            likedStoryIds = setOf(PrimaryArticle.stableId),
            dashboard = {
                Dashboard(
                    uiState = DashboardState,
                    articles = uiState.articles,
                    isFeedLoading = uiState.isInitialLoading,
                    scrollable = false,
                )
            },
        )
    }
}

@Composable
private fun Dashboard(
    uiState: HomeDashboardUiState,
    articles: List<Article>,
    isFeedLoading: Boolean,
    scrollable: Boolean = true,
) {
    HomeDashboard(
        uiState = uiState,
        articles = articles,
        isFeedLoading = isFeedLoading,
        onTodayPicks = {},
        onGoodMood = {},
        onReadingStats = {},
        onSavedStories = {},
        onArchiveSearch = {},
        onPersonalize = {},
        onRefreshForYou = {},
        onOpenArticle = {},
        scrollable = scrollable,
    )
}

@Composable
private fun SavedStories(uiState: SavedStoriesUiState) {
    SavedStoriesScreen(
        uiState = uiState,
        onQueryChanged = {},
        onOpenStory = {},
        onRemoveStory = {},
        onClose = {},
    )
}

@Composable
private fun ArchiveSearch(uiState: ArchiveSearchUiState) {
    ArchiveSearchScreen(
        uiState = uiState,
        onQueryChanged = {},
        onSubmitSearch = {},
        onClearSearch = {},
        onRetry = {},
        onLoadMore = {},
        onToggleSaved = {},
        onOpenArticle = {},
        onClose = {},
        requestInitialFocus = false,
    )
}

@Composable
private fun GoodMood(articles: List<Article>) {
    GoodMoodScreen(
        articles = articles,
        savedStoryIds = setOf(PrimaryArticle.stableId),
        hapticsEnabled = false,
        onToggleSaved = {},
        onSaveHaptic = { false },
        onOpenArticle = {},
        onClose = {},
    )
}

@Composable
private fun Digest(articles: List<Article>) {
    DailyDigestScreen(
        articles = articles,
        savedStoryIds = setOf(PrimaryArticle.stableId),
        hapticsEnabled = false,
        onToggleSaved = {},
        onSaveHaptic = { false },
        onOpenArticle = {},
        onClose = {},
    )
}

@Composable
private fun Settings(uiState: SettingsUiState) {
    SettingsScreen(
        uiState = uiState,
        onAppearance = {},
        onHaptics = {},
        onWidget = {},
        onGoHome = {},
    )
}

private val GoldenThumbnail =
    URI("android.resource://com.nutsnews.app/drawable/brand_icon")

private val PrimaryArticle =
    Article(
        id = "community-garden",
        title = "Neighbors turn an empty lot into a thriving community garden",
        summary =
            "Volunteers created a welcoming green space where families can learn and grow.",
        originalUrl = URI("https://example.com/community-garden"),
        source = "Good News Daily",
        publishedAt = "2026-07-26T12:00:00Z",
        createdAt = null,
        thumbnailUrl = GoldenThumbnail,
        categories = listOf("Community", "Nature"),
    )

private val Articles =
    listOf(
        PrimaryArticle,
        Article(
            id = "science-school",
            title = "Students build a solar lab for their neighborhood school",
            summary = "The hands-on project is making clean-energy lessons accessible.",
            originalUrl = URI("https://example.com/science-school"),
            source = "Bright Wire",
            publishedAt = "2026-07-25T12:00:00Z",
            createdAt = null,
            thumbnailUrl = GoldenThumbnail,
            categories = listOf("Science", "Achievements"),
        ),
        Article(
            id = "rescue-otters",
            title = "Rescued otters return to their restored coastal home",
            summary = "Care teams celebrated a gentle release beside the harbor.",
            originalUrl = URI("https://example.com/rescue-otters"),
            source = "Coastal Journal",
            publishedAt = "2026-07-24T12:00:00Z",
            createdAt = null,
            thumbnailUrl = GoldenThumbnail,
            categories = listOf("Animals", "Nature"),
        ),
    )

private val Categories = listOf("Community", "Science", "Animals", "Nature")

private val DashboardState =
    HomeDashboardUiState(
        isLoading = false,
        todayStoryCount = 2,
        dailyGoal = 3,
        currentStreak = 5,
        savedCount = 8,
        notesCount = 3,
        selectedTopicIds = setOf("community", "science", "nature"),
        selectedMoodId = "hopeful",
        reminderEnabled = true,
        reminderHour = 20,
    )

private val SavedStories =
    Articles.take(2).mapIndexed { index, article ->
        SavedStory(
            article = article,
            savedAt = Instant.parse("2026-07-${26 - index}T12:00:00Z"),
        )
    }

private val Reflection =
    StoryReflection(
        articleId = PrimaryArticle.stableId,
        articleTitle = PrimaryArticle.title,
        articleSource = PrimaryArticle.source,
        reaction = StoryReflectionReaction.Hope,
        createdAt = Instant.parse("2026-07-26T13:00:00Z"),
    )

private val ListeningState =
    ArticleListenUiState(
        playbackState = ArticleListenPlaybackState.Reading,
        statusMessage = "Reading why this is good news",
        segments =
            listOf(
                ArticleListenSegment(
                    id = "summary",
                    label = "Brief",
                    text = PrimaryArticle.summary,
                ),
                ArticleListenSegment(
                    id = "takeaway",
                    label = "Takeaway",
                    text = "Small acts of care can grow into lasting community change.",
                ),
            ),
        currentSegmentIndex = 0,
        speechWaveLevel = 0.55f,
        speechWaveFrequency = 1.1f,
        speechWaveSeed = 57,
        isEngineReady = true,
    )

private val ReadingStatsState =
    ReadingStatsUiState(
        isLoading = false,
        todayStoryCount = 2,
        dailyGoal = 3,
        currentStreak = 5,
        totalUniqueStoryCount = 24,
        savedStoryCount = 8,
        noteCount = 3,
        originalOpensTodayCount = 1,
        recentDays =
            (0L..6L).map { offset ->
                ReadingStatsDay(
                    date = LocalDate.of(2026, 7, 26).minusDays(6L - offset),
                    storyCount = listOf(1, 3, 2, 0, 4, 1, 2)[offset.toInt()],
                )
            },
    )
