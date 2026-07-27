package com.nutsnews.app.di

import com.nutsnews.app.data.article.ArticleResponseCache
import com.nutsnews.app.data.article.EmptyArticleResponseCache
import com.nutsnews.app.data.article.NutsNewsApiClient
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.data.story.ReadingStatsRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.data.story.StoryNoteRepository
import com.nutsnews.app.data.story.StoryReflectionRepository
import com.nutsnews.app.navigation.AppNavigator
import com.nutsnews.app.navigation.DefaultAppNavigator
import com.nutsnews.app.reminder.DailyReminderManager
import com.nutsnews.app.reminder.NoOpDailyReminderManager
import com.nutsnews.app.widget.DefaultWidgetDataPipeline
import com.nutsnews.app.widget.NoOpWidgetRefreshRequester
import com.nutsnews.app.widget.ResponseCacheWidgetArticleStore
import com.nutsnews.app.widget.WidgetDataProvider
import com.nutsnews.app.widget.WidgetRefreshRequester

interface AppContainer {
    val navigator: AppNavigator
    val articleApiClient: NutsNewsApiClient
    val userPreferencesRepository: UserPreferencesRepository
    val dailyReminderManager: DailyReminderManager
    val readingStatsRepository: ReadingStatsRepository
    val savedStoryRepository: SavedStoryRepository
    val storyNoteRepository: StoryNoteRepository
    val storyReflectionRepository: StoryReflectionRepository
    val widgetDataProvider: WidgetDataProvider
    val widgetRefreshRequester: WidgetRefreshRequester
}

class DefaultAppContainer(
    override val readingStatsRepository: ReadingStatsRepository,
    override val savedStoryRepository: SavedStoryRepository,
    override val storyNoteRepository: StoryNoteRepository,
    override val storyReflectionRepository: StoryReflectionRepository,
    private val responseCache: ArticleResponseCache = EmptyArticleResponseCache,
    override val userPreferencesRepository: UserPreferencesRepository =
        InMemoryUserPreferencesRepository(),
    override val dailyReminderManager: DailyReminderManager =
        NoOpDailyReminderManager,
    override val widgetRefreshRequester: WidgetRefreshRequester =
        NoOpWidgetRefreshRequester,
) : AppContainer {
    override val navigator: AppNavigator by lazy {
        DefaultAppNavigator()
    }

    override val articleApiClient: NutsNewsApiClient by lazy {
        NutsNewsApiClient(responseCache = responseCache)
    }

    override val widgetDataProvider: WidgetDataProvider by lazy {
        DefaultWidgetDataPipeline(
            articleSource = articleApiClient,
            articleStore = ResponseCacheWidgetArticleStore(responseCache),
            userPreferencesRepository = userPreferencesRepository,
            readingStatsRepository = readingStatsRepository,
        )
    }
}
