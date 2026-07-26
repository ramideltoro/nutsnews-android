package com.nutsnews.app.feature.bootstrap

import androidx.compose.runtime.Immutable
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppPresentation

@Immutable
data class BootstrapUiState(
    val destination: AppDestination = AppDestination.Startup,
    val canNavigateUp: Boolean = false,
    val presentation: AppPresentation = destination.presentation,
    val returnDestination: AppDestination? = null,
    val theme: NutsNewsAppTheme = NutsNewsAppTheme.Default,
)
