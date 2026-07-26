package com.nutsnews.app.data.preferences

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val hasCompletedOnboarding: Flow<Boolean>

    suspend fun setOnboardingCompleted(completed: Boolean)
}
