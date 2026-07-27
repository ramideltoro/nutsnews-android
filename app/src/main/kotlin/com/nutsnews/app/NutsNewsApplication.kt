package com.nutsnews.app

import android.app.Application
import com.nutsnews.app.data.article.DiskArticleResponseCache
import com.nutsnews.app.data.database.NutsNewsDatabase
import com.nutsnews.app.data.preferences.DataStoreUserPreferencesRepository
import com.nutsnews.app.data.preferences.nutsNewsPreferencesDataStore
import com.nutsnews.app.data.story.RoomReadingStatsRepository
import com.nutsnews.app.data.story.RoomSavedStoryRepository
import com.nutsnews.app.data.story.RoomStoryNoteRepository
import com.nutsnews.app.data.story.RoomStoryReflectionRepository
import com.nutsnews.app.di.AppContainer
import com.nutsnews.app.di.DefaultAppContainer
import com.nutsnews.app.reminder.AndroidDailyReminderManager
import com.nutsnews.app.widget.AndroidWidgetRefreshRequester

class NutsNewsApplication : Application() {
    lateinit var container: AppContainer
        internal set

    override fun onCreate() {
        super.onCreate()
        val database = NutsNewsDatabase.create(this)
        val dailyReminderManager =
            AndroidDailyReminderManager.create(this).also { manager ->
                manager.createNotificationChannel()
            }
        container =
            DefaultAppContainer(
                readingStatsRepository =
                    RoomReadingStatsRepository(database.readingActivityDao()),
                savedStoryRepository =
                    RoomSavedStoryRepository(database.savedStoryDao()),
                storyNoteRepository =
                    RoomStoryNoteRepository(database.storyNoteDao()),
                storyReflectionRepository =
                    RoomStoryReflectionRepository(database.storyReflectionDao()),
                responseCache =
                    DiskArticleResponseCache(
                        cacheDir
                            .toPath()
                            .resolve(DiskArticleResponseCache.DirectoryName),
                    ),
                userPreferencesRepository =
                    DataStoreUserPreferencesRepository(nutsNewsPreferencesDataStore),
                dailyReminderManager = dailyReminderManager,
                widgetRefreshRequester = AndroidWidgetRefreshRequester(this),
            )
    }
}
