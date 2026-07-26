package com.nutsnews.app.di

import com.nutsnews.app.data.article.ArticleResponseCache
import com.nutsnews.app.data.article.EmptyArticleResponseCache
import com.nutsnews.app.data.article.NutsNewsApiClient
import com.nutsnews.app.navigation.AppNavigator
import com.nutsnews.app.navigation.DefaultAppNavigator

interface AppContainer {
    val navigator: AppNavigator
    val articleApiClient: NutsNewsApiClient
}

class DefaultAppContainer(
    private val responseCache: ArticleResponseCache = EmptyArticleResponseCache,
) : AppContainer {
    override val navigator: AppNavigator by lazy {
        DefaultAppNavigator()
    }

    override val articleApiClient: NutsNewsApiClient by lazy {
        NutsNewsApiClient(responseCache = responseCache)
    }
}
