package com.nutsnews.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.feature.article.ArticleDetailScreen
import com.nutsnews.app.feature.article.ArticleDetailViewModel
import com.nutsnews.app.feature.article.AndroidOriginalStoryLauncher
import com.nutsnews.app.feature.article.AndroidArticleSpeechEngine
import com.nutsnews.app.feature.article.ArticleListenController
import com.nutsnews.app.feature.article.UnavailableArticleDetailScreen
import com.nutsnews.app.feature.bootstrap.BootstrapUiState
import com.nutsnews.app.feature.bootstrap.BootstrapViewModel
import com.nutsnews.app.feature.feed.ArticleCardInteractionViewModel
import com.nutsnews.app.feature.feed.ArticleFeedContent
import com.nutsnews.app.feature.feed.ArticleFeedViewModel
import com.nutsnews.app.feature.feed.FeedScreen
import com.nutsnews.app.feature.home.HomeDashboard
import com.nutsnews.app.feature.home.HomeDashboardViewModel
import com.nutsnews.app.feature.personalization.PersonalizationMode
import com.nutsnews.app.feature.personalization.PersonalizationScreen
import com.nutsnews.app.feature.personalization.PersonalizationViewModel
import com.nutsnews.app.feature.splash.StartupSplash
import com.nutsnews.app.feature.splash.StartupSplashTiming
import com.nutsnews.app.feature.splash.StartupSplashUiState
import com.nutsnews.app.feature.splash.StartupSplashViewModel
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppNavigator
import com.nutsnews.app.reminder.DailyReminderContract
import com.nutsnews.app.reminder.ReminderScheduleResult

class MainActivity : ComponentActivity() {
    private val articleListenController by lazy {
        ArticleListenController(AndroidArticleSpeechEngine(applicationContext))
    }

    private val originalStoryLauncher by lazy {
        AndroidOriginalStoryLauncher(this)
    }

    private val appNavigator: AppNavigator
        get() = (application as NutsNewsApplication).container.navigator

    private val dailyReminderManager
        get() = (application as NutsNewsApplication).container.dailyReminderManager

    private val bootstrapViewModel: BootstrapViewModel by viewModels {
        BootstrapViewModel.Factory(
            navigator = appNavigator,
            userPreferencesRepository =
                (application as NutsNewsApplication).container.userPreferencesRepository,
        )
    }

    private val startupSplashViewModel: StartupSplashViewModel by viewModels()

    private val articleFeedViewModel: ArticleFeedViewModel by viewModels {
        ArticleFeedViewModel.Factory(
            articleSource =
                (application as NutsNewsApplication).container.articleApiClient,
        )
    }

    private val articleCardInteractionViewModel: ArticleCardInteractionViewModel by viewModels {
        val container = (application as NutsNewsApplication).container
        ArticleCardInteractionViewModel.Factory(
            savedStoryRepository = container.savedStoryRepository,
            userPreferencesRepository = container.userPreferencesRepository,
        )
    }

    private val articleDetailViewModel: ArticleDetailViewModel by viewModels {
        val container = (application as NutsNewsApplication).container
        ArticleDetailViewModel.Factory(
            savedStoryRepository = container.savedStoryRepository,
            readingStatsRepository = container.readingStatsRepository,
            storyNoteRepository = container.storyNoteRepository,
            storyReflectionRepository = container.storyReflectionRepository,
        )
    }

    private val personalizationViewModel: PersonalizationViewModel by viewModels {
        PersonalizationViewModel.Factory(
            userPreferencesRepository =
                (application as NutsNewsApplication).container.userPreferencesRepository,
        )
    }

    private val homeDashboardViewModel: HomeDashboardViewModel by viewModels {
        val container = (application as NutsNewsApplication).container
        HomeDashboardViewModel.Factory(
            userPreferencesRepository = container.userPreferencesRepository,
            readingStatsRepository = container.readingStatsRepository,
            savedStoryRepository = container.savedStoryRepository,
            storyNoteRepository = container.storyNoteRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NutsNews)
        super.onCreate(savedInstanceState)
        appNavigator.restoreState(savedInstanceState?.getString(NavigationStateKey))
        enableEdgeToEdge()
        setContent {
            val uiState by bootstrapViewModel.uiState.collectAsStateWithLifecycle()
            val splashUiState by
                startupSplashViewModel.uiState.collectAsStateWithLifecycle()
            val personalizationUiState by
                personalizationViewModel.uiState.collectAsStateWithLifecycle()
            val feedUiState by articleFeedViewModel.uiState.collectAsStateWithLifecycle()
            val articleCardInteractionUiState by
                articleCardInteractionViewModel.uiState.collectAsStateWithLifecycle()
            val articleDetailUiState by
                articleDetailViewModel.uiState.collectAsStateWithLifecycle()
            val articleListenUiState by
                articleListenController.uiState.collectAsStateWithLifecycle()
            val homeDashboardUiState by
                homeDashboardViewModel.uiState.collectAsStateWithLifecycle()
            val pendingPermissionSaveMode =
                remember { mutableStateOf<PersonalizationMode?>(null) }
            val savePersonalization: (PersonalizationMode) -> Unit = { mode ->
                val reminderEnabled =
                    personalizationViewModel.uiState.value.reminderEnabled
                val reminderHour =
                    personalizationViewModel.uiState.value.reminderHour
                personalizationViewModel.save(
                    onSaved = {
                        if (reminderEnabled) {
                            if (
                                dailyReminderManager.schedule(reminderHour) ==
                                ReminderScheduleResult.PermissionDenied
                            ) {
                                personalizationViewModel
                                    .onNotificationPermissionDenied()
                            }
                        } else {
                            dailyReminderManager.cancel()
                        }
                        if (mode == PersonalizationMode.Editor) {
                            bootstrapViewModel.onNavigateUp()
                        }
                    },
                )
            }
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { isGranted ->
                    val pendingMode =
                        pendingPermissionSaveMode.value
                            ?: return@rememberLauncherForActivityResult
                    pendingPermissionSaveMode.value = null
                    if (!isGranted || !dailyReminderManager.canPostNotifications) {
                        personalizationViewModel.onNotificationPermissionDenied()
                    }
                    savePersonalization(pendingMode)
                }
            NutsNewsApp(
                uiState = uiState,
                splashUiState = splashUiState,
                onNavigateUp = bootstrapViewModel::onNavigateUp,
                destinationContent = { destination ->
                    when (destination) {
                        AppDestination.Feed -> {
                            LaunchedEffect(Unit) {
                                articleFeedViewModel.loadInitialArticles()
                            }
                            FeedScreen(
                                uiState = feedUiState,
                                onDestinationSelected =
                                    bootstrapViewModel::onDestinationRequested,
                                onCategorySelected = articleFeedViewModel::applyCategory,
                            ) {
                                ArticleFeedContent(
                                    uiState = feedUiState,
                                    onRefresh = articleFeedViewModel::forceRefresh,
                                    onRetry = articleFeedViewModel::retry,
                                    onLoadMore = articleFeedViewModel::loadMoreIfNeeded,
                                    onOpenArticle = { article ->
                                        bootstrapViewModel.onDestinationRequested(
                                            AppDestination.ArticleDetail(article.stableId),
                                        )
                                    },
                                    likedStoryIds =
                                        articleCardInteractionUiState.likedStoryIds,
                                    hapticsEnabled =
                                        articleCardInteractionUiState.hapticsEnabled,
                                    onToggleLiked =
                                        articleCardInteractionViewModel::toggleLiked,
                                    dashboard = {
                                        HomeDashboard(
                                            uiState = homeDashboardUiState,
                                            articles = feedUiState.articles,
                                            isFeedLoading = feedUiState.isLoading,
                                            onTodayPicks = {
                                                bootstrapViewModel.onDestinationRequested(
                                                    AppDestination.DailyDigest,
                                                )
                                            },
                                            onGoodMood = {
                                                bootstrapViewModel.onDestinationRequested(
                                                    AppDestination.GoodMood,
                                                )
                                            },
                                            onReadingStats = {
                                                bootstrapViewModel.onDestinationRequested(
                                                    AppDestination.ReadingStats,
                                                )
                                            },
                                            onSavedStories = {
                                                bootstrapViewModel.onDestinationRequested(
                                                    AppDestination.SavedStories,
                                                )
                                            },
                                            onArchiveSearch = {
                                                bootstrapViewModel.onDestinationRequested(
                                                    AppDestination.ArchiveSearch,
                                                )
                                            },
                                            onPersonalize = {
                                                bootstrapViewModel.onDestinationRequested(
                                                    AppDestination.Personalization,
                                                )
                                            },
                                            onRefreshForYou = {
                                                articleFeedViewModel.refresh(forceReload = true)
                                            },
                                            onOpenArticle = { article ->
                                                bootstrapViewModel.onDestinationRequested(
                                                    AppDestination.ArticleDetail(article.stableId),
                                                )
                                            },
                                            scrollable = false,
                                        )
                                    },
                                )
                            }
                        }

                        is AppDestination.ArticleDetail -> {
                            val article =
                                feedUiState.articles.firstOrNull { candidate ->
                                    candidate.stableId == destination.storyId
                                } ?: articleCardInteractionUiState
                                    .savedArticlesById[destination.storyId]
                            if (article == null) {
                                UnavailableArticleDetailScreen(
                                    onClose = bootstrapViewModel::onNavigateUp,
                                )
                            } else {
                                ArticleDetailScreen(
                                    article = article,
                                    onClose = {
                                        articleListenController.stop()
                                        bootstrapViewModel.onNavigateUp()
                                    },
                                    isLiked =
                                        article.stableId in
                                            articleDetailUiState.likedStoryIds,
                                    onToggleLiked = articleDetailViewModel::toggleLiked,
                                    onArticleShown = articleDetailViewModel::onArticleShown,
                                    onOpenOriginalStory =
                                        originalStoryLauncher::open,
                                    onOriginalStoryOpened =
                                        articleDetailViewModel::onOriginalStoryOpened,
                                    noteDraft =
                                        articleDetailUiState.noteDraft.takeIf {
                                            articleDetailUiState.noteArticleId ==
                                                article.stableId
                                        }.orEmpty(),
                                    hasSavedNote =
                                        articleDetailUiState.noteArticleId ==
                                            article.stableId &&
                                            articleDetailUiState.hasSavedNote,
                                    isNoteLoading =
                                        articleDetailUiState.noteArticleId !=
                                            article.stableId ||
                                            articleDetailUiState.isNoteLoading,
                                    noteStatusMessage =
                                        articleDetailUiState.noteStatusMessage.takeIf {
                                            articleDetailUiState.noteArticleId ==
                                                article.stableId
                                        },
                                    onNoteDraftChanged = { draft ->
                                        articleDetailViewModel.onNoteDraftChanged(
                                            article = article,
                                            draft = draft,
                                        )
                                    },
                                    onSaveNote = {
                                        articleDetailViewModel.saveNote(article)
                                    },
                                    onClearNote = {
                                        articleDetailViewModel.clearNote(article)
                                    },
                                    reflection =
                                        articleDetailUiState.reflection.takeIf {
                                            articleDetailUiState.reflectionArticleId ==
                                                article.stableId
                                        },
                                    isReflectionLoading =
                                        articleDetailUiState.reflectionArticleId !=
                                            article.stableId ||
                                            articleDetailUiState.isReflectionLoading,
                                    reflectionStatusMessage =
                                        articleDetailUiState.reflectionStatusMessage.takeIf {
                                            articleDetailUiState.reflectionArticleId ==
                                                article.stableId
                                        },
                                    onReflectionSelected = { reaction ->
                                        articleDetailViewModel.saveReflection(
                                            article = article,
                                            reaction = reaction,
                                        )
                                    },
                                    listenUiState = articleListenUiState,
                                    onToggleListening = articleListenController::toggle,
                                    onStopListening = articleListenController::stop,
                                )
                            }
                        }

                        AppDestination.Onboarding,
                        AppDestination.Personalization,
                        -> {
                            val mode =
                                if (destination == AppDestination.Onboarding) {
                                    PersonalizationMode.FirstRun
                                } else {
                                    PersonalizationMode.Editor
                                }
                            PersonalizationScreen(
                                uiState = personalizationUiState,
                                mode = mode,
                                onTopicToggled = personalizationViewModel::onTopicToggled,
                                onMoodSelected = personalizationViewModel::onMoodSelected,
                                onDailyGoalChanged =
                                    personalizationViewModel::onDailyGoalChanged,
                                onReminderEnabledChanged =
                                    personalizationViewModel::onReminderEnabledChanged,
                                onReminderHourSelected =
                                    personalizationViewModel::onReminderHourSelected,
                                onSave = {
                                    when {
                                        personalizationUiState.reminderEnabled &&
                                            dailyReminderManager.requiresRuntimePermission -> {
                                            pendingPermissionSaveMode.value = mode
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS,
                                            )
                                        }

                                        personalizationUiState.reminderEnabled &&
                                            !dailyReminderManager.canPostNotifications -> {
                                            personalizationViewModel
                                                .onNotificationPermissionDenied()
                                            savePersonalization(mode)
                                        }

                                        else -> savePersonalization(mode)
                                    }
                                },
                                onClose = {
                                    personalizationViewModel.discardChanges()
                                    bootstrapViewModel.onNavigateUp()
                                },
                            )
                        }

                        else -> NutsNewsDestinationPlaceholder(destination)
                    }
                },
            )
        }
        handleLaunchIntent(intent)
    }

    override fun onStop() {
        articleListenController.stop()
        super.onStop()
    }

    override fun onDestroy() {
        articleListenController.shutdown()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(NavigationStateKey, appNavigator.saveState())
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val NavigationStateKey = "nutsnews.navigation.backStack"
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.action != DailyReminderContract.ActionOpenDailyDigest) return
        intent.action = null
        bootstrapViewModel.onDailyReminderNotificationOpened()
    }
}

@Composable
internal fun NutsNewsApp(
    uiState: BootstrapUiState,
    splashUiState: StartupSplashUiState,
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Boolean = { false },
    destinationContent: @Composable (AppDestination) -> Unit = {
        NutsNewsDestinationPlaceholder(it)
    },
) {
    NutsNewsTheme(theme = uiState.theme) {
        BackHandler(
            enabled = !splashUiState.isShowingSplash && uiState.canNavigateUp,
        ) {
            onNavigateUp()
        }

        val contentProgress by
            animateFloatAsState(
                targetValue = if (splashUiState.isShowingSplash) 0f else 1f,
                animationSpec =
                    tween(
                        durationMillis = StartupSplashTiming.ContentTransitionMillis,
                        easing = FastOutSlowInEasing,
                    ),
                label = "Startup content transition",
            )

        Box(modifier = modifier.fillMaxSize()) {
            key(uiState.destination) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = contentProgress
                                scaleX = 0.99f + (0.01f * contentProgress)
                                scaleY = 0.99f + (0.01f * contentProgress)
                            },
                ) {
                    destinationContent(uiState.destination)
                }
            }

            AnimatedVisibility(
                visible = splashUiState.isShowingSplash,
                enter = EnterTransition.None,
                exit =
                    fadeOut(
                        animationSpec =
                            tween(
                                durationMillis = StartupSplashTiming.ContentTransitionMillis,
                                easing = FastOutSlowInEasing,
                            ),
                    ),
            ) {
                StartupSplash(uiState = splashUiState)
            }
        }
    }
}

@Composable
private fun NutsNewsDestinationPlaceholder(destination: AppDestination) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(destination.shellTitle)
    }
}

@Preview(showBackground = true)
@Composable
private fun NutsNewsAppPreview() {
    NutsNewsApp(
        uiState = BootstrapUiState(),
        splashUiState = StartupSplashUiState(),
    )
}
