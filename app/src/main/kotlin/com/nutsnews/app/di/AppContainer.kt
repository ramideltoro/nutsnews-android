package com.nutsnews.app.di

import com.nutsnews.app.data.article.NutsNewsApiClient
import com.nutsnews.app.navigation.AppNavigator
import com.nutsnews.app.navigation.DefaultAppNavigator

interface AppContainer {
    val navigator: AppNavigator
    val articleApiClient: NutsNewsApiClient
}

class DefaultAppContainer : AppContainer {
    override val navigator: AppNavigator by lazy {
        DefaultAppNavigator()
    }

    override val articleApiClient: NutsNewsApiClient by lazy {
        NutsNewsApiClient()
    }
}
