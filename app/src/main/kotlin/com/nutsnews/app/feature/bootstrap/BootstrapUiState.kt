package com.nutsnews.app.feature.bootstrap

import androidx.compose.runtime.Immutable
import com.nutsnews.app.navigation.AppDestination

@Immutable
data class BootstrapUiState(
    val destination: AppDestination = AppDestination.Startup,
    val canNavigateUp: Boolean = false,
)
