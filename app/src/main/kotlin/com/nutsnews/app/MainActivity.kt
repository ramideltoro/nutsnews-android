package com.nutsnews.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.feature.bootstrap.BootstrapUiState
import com.nutsnews.app.feature.bootstrap.BootstrapViewModel
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppNavigator

class MainActivity : ComponentActivity() {
    private val appNavigator: AppNavigator
        get() = (application as NutsNewsApplication).container.navigator

    private val bootstrapViewModel: BootstrapViewModel by viewModels {
        BootstrapViewModel.Factory(
            navigator = appNavigator,
            userPreferencesRepository =
                (application as NutsNewsApplication).container.userPreferencesRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NutsNews)
        super.onCreate(savedInstanceState)
        appNavigator.restoreState(savedInstanceState?.getString(NavigationStateKey))
        enableEdgeToEdge()
        setContent {
            val uiState by bootstrapViewModel.uiState.collectAsStateWithLifecycle()
            NutsNewsApp(
                uiState = uiState,
                onNavigateUp = bootstrapViewModel::onNavigateUp,
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(NavigationStateKey, appNavigator.saveState())
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val NavigationStateKey = "nutsnews.navigation.backStack"
    }
}

@Composable
internal fun NutsNewsApp(
    uiState: BootstrapUiState,
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Boolean = { false },
    destinationContent: @Composable (AppDestination) -> Unit = {
        NutsNewsDestinationPlaceholder(it)
    },
) {
    NutsNewsTheme {
        BackHandler(enabled = uiState.canNavigateUp) {
            onNavigateUp()
        }
        key(uiState.destination) {
            Surface(modifier = modifier.fillMaxSize()) {
                destinationContent(uiState.destination)
            }
        }
    }
}

@Composable
private fun NutsNewsDestinationPlaceholder(destination: AppDestination) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(destination.shellTitle)
    }
}

@Preview(showBackground = true)
@Composable
private fun NutsNewsAppPreview() {
    NutsNewsApp(uiState = BootstrapUiState())
}
