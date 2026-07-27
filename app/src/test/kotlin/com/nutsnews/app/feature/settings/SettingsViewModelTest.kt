package com.nutsnews.app.feature.settings

import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesLiveThemeHapticsAndWidgetSubtitles() =
        runTest(mainDispatcher) {
            val preferences =
                InMemoryUserPreferencesRepository(
                    UserPreferences(
                        theme = NutsNewsAppTheme.Sakura,
                        hapticsEnabled = true,
                        showStatsOnLargeWidget = false,
                    ),
                )
            val viewModel = SettingsViewModel(preferences)

            assertEquals(true, viewModel.uiState.first().isLoading)
            val initial = viewModel.uiState.first { state -> !state.isLoading }
            assertEquals(NutsNewsAppTheme.Sakura, initial.theme)
            assertEquals("Sakura", initial.theme.title)
            assertEquals("On", initial.hapticsSubtitle)
            assertEquals("Large stats off", initial.widgetSubtitle)

            preferences.updatePreferences { current ->
                current.copy(
                    theme = NutsNewsAppTheme.Bambi,
                    hapticsEnabled = false,
                    showStatsOnLargeWidget = true,
                )
            }

            val updated =
                viewModel.uiState.first { state ->
                    state.theme == NutsNewsAppTheme.Bambi
                }
            assertEquals("Bambi", updated.theme.title)
            assertEquals("Off", updated.hapticsSubtitle)
            assertEquals("Large stats on", updated.widgetSubtitle)
        }
}
