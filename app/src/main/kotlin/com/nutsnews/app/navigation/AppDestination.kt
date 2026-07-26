package com.nutsnews.app.navigation

import com.nutsnews.app.core.model.StoryId

sealed interface AppDestination {
    data object Startup : AppDestination

    data object Onboarding : AppDestination

    data object Feed : AppDestination

    data class ArticleDetail(val storyId: StoryId) : AppDestination

    data object SavedStories : AppDestination

    data object ArchiveSearch : AppDestination

    data object GoodMood : AppDestination

    data object DailyDigest : AppDestination

    data object ReadingStats : AppDestination

    data object Settings : AppDestination

    data object ThemePicker : AppDestination

    data object Help : AppDestination
}
