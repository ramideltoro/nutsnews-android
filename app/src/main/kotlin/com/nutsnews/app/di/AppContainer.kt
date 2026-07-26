package com.nutsnews.app.di

import com.nutsnews.app.data.article.ArticleResponseCache
import com.nutsnews.app.data.article.EmptyArticleResponseCache
import com.nutsnews.app.data.article.NutsNewsApiClient
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferencesRepository
import com.nutsnews.app.data.story.SavedStoryRepository
import com.nutsnews.app.navigation.AppNavigator
import com.nutsnews.app.navigation.DefaultAppNavigator

interface AppContainer {
    val navigator: AppNavigator
    val articleApiClient: NutsNewsApiClient
    val userPreferencesRepository: UserPreferencesRepository
    val savedStoryRepository: SavedStoryRepository
}

class DefaultAppContainer(
    override val savedStoryRepository: SavedStoryRepository,
    private val responseCache: ArticleResponseCache = EmptyArticleResponseCache,
    override val userPreferencesRepository: UserPreferencesRepository =
        InMemoryUserPreferencesRepository(),
) : AppContainer {
    override val navigator: AppNavigator by lazy {
        DefaultAppNavigator()
    }

    override val articleApiClient: NutsNewsApiClient by lazy {
        NutsNewsApiClient(responseCache = responseCache)
    }
}
