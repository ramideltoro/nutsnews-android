package com.nutsnews.app

import android.app.Application
import com.nutsnews.app.data.article.DiskArticleResponseCache
import com.nutsnews.app.data.database.NutsNewsDatabase
import com.nutsnews.app.data.preferences.DataStoreUserPreferencesRepository
import com.nutsnews.app.data.preferences.nutsNewsPreferencesDataStore
import com.nutsnews.app.data.story.RoomSavedStoryRepository
import com.nutsnews.app.data.story.RoomStoryNoteRepository
import com.nutsnews.app.di.AppContainer
import com.nutsnews.app.di.DefaultAppContainer

class NutsNewsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = NutsNewsDatabase.create(this)
        container =
            DefaultAppContainer(
                savedStoryRepository =
                    RoomSavedStoryRepository(database.savedStoryDao()),
                storyNoteRepository =
                    RoomStoryNoteRepository(database.storyNoteDao()),
                responseCache =
                    DiskArticleResponseCache(
                        cacheDir
                            .toPath()
                            .resolve(DiskArticleResponseCache.DirectoryName),
                    ),
                userPreferencesRepository =
                    DataStoreUserPreferencesRepository(nutsNewsPreferencesDataStore),
            )
    }
}
