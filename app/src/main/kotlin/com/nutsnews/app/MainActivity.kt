package com.nutsnews.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.feature.bootstrap.BootstrapUiState
import com.nutsnews.app.feature.bootstrap.BootstrapViewModel

class MainActivity : ComponentActivity() {
    private val bootstrapViewModel: BootstrapViewModel by viewModels {
        BootstrapViewModel.Factory(
            navigator = (application as NutsNewsApplication).container.navigator,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by bootstrapViewModel.uiState.collectAsStateWithLifecycle()
            NutsNewsApp(uiState = uiState)
        }
    }
}

@Composable
internal fun NutsNewsApp(
    uiState: BootstrapUiState,
    modifier: Modifier = Modifier,
) {
    NutsNewsTheme {
        key(uiState.destination) {
            Surface(modifier = modifier.fillMaxSize()) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NutsNewsAppPreview() {
    NutsNewsApp(uiState = BootstrapUiState())
}
