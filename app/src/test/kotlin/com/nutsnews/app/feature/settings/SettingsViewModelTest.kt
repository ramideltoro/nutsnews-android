package com.nutsnews.app.feature.settings

import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.widget.WidgetRefreshRequester
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

    @Test
    fun themeSelectionPersistsAndIsIgnoredWhenAlreadySelected() =
        runTest(mainDispatcher) {
            val preferences =
                InMemoryUserPreferencesRepository(
                    UserPreferences(theme = NutsNewsAppTheme.Amber),
                )
            var widgetRefreshCount = 0
            val viewModel =
                SettingsViewModel(
                    userPreferencesRepository = preferences,
                    widgetRefreshRequester =
                        WidgetRefreshRequester {
                            widgetRefreshCount += 1
                            true
                        },
                )
            viewModel.uiState.first { state -> !state.isLoading }

            viewModel.selectTheme(NutsNewsAppTheme.Friday)

            val selected =
                viewModel.uiState.first { state ->
                    state.theme == NutsNewsAppTheme.Friday
                }
            assertEquals(NutsNewsAppTheme.Friday, selected.theme)
            assertEquals(
                NutsNewsAppTheme.Friday,
                preferences.preferences.first().theme,
            )
            assertEquals(1, widgetRefreshCount)

            viewModel.selectTheme(NutsNewsAppTheme.Friday)
            mainDispatcher.scheduler.advanceUntilIdle()
            assertEquals(
                NutsNewsAppTheme.Friday,
                preferences.preferences.first().theme,
            )
            assertEquals(1, widgetRefreshCount)
        }

    @Test
    fun hapticsAndWidgetTogglesPersistAcrossViewModelsAndRefreshOnlyWidget() =
        runTest(mainDispatcher) {
            val preferences = InMemoryUserPreferencesRepository()
            var widgetRefreshCount = 0
            val viewModel =
                SettingsViewModel(
                    userPreferencesRepository = preferences,
                    widgetRefreshRequester =
                        WidgetRefreshRequester {
                            widgetRefreshCount += 1
                            true
                        },
                )
            viewModel.uiState.first { state -> !state.isLoading }

            viewModel.setHapticsEnabled(false)
            val hapticsOff =
                viewModel.uiState.first { state -> !state.hapticsEnabled }
            assertEquals("Off", hapticsOff.hapticsSubtitle)
            assertEquals(0, widgetRefreshCount)

            viewModel.setShowStatsOnLargeWidget(false)
            val widgetOff =
                viewModel.uiState.first { state -> !state.showStatsOnLargeWidget }
            assertEquals("Large stats off", widgetOff.widgetSubtitle)
            assertEquals(1, widgetRefreshCount)

            val restarted =
                SettingsViewModel(preferences)
                    .uiState
                    .first { state -> !state.isLoading }
            assertEquals(false, restarted.hapticsEnabled)
            assertEquals(false, restarted.showStatsOnLargeWidget)
        }
}
