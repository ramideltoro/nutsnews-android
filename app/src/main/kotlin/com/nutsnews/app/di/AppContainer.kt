package com.nutsnews.app.di

import com.nutsnews.app.navigation.AppNavigator
import com.nutsnews.app.navigation.DefaultAppNavigator

interface AppContainer {
    val navigator: AppNavigator
}

class DefaultAppContainer : AppContainer {
    override val navigator: AppNavigator by lazy {
        DefaultAppNavigator()
    }
}
