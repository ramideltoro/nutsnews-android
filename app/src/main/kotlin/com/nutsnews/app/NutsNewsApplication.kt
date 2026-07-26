package com.nutsnews.app

import android.app.Application
import com.nutsnews.app.data.article.DiskArticleResponseCache
import com.nutsnews.app.di.AppContainer
import com.nutsnews.app.di.DefaultAppContainer

class NutsNewsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container =
            DefaultAppContainer(
                responseCache =
                    DiskArticleResponseCache(
                        cacheDir
                            .toPath()
                            .resolve(DiskArticleResponseCache.DirectoryName),
                    ),
            )
    }
}
