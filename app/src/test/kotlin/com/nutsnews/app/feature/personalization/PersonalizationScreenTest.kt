package com.nutsnews.app.feature.personalization

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.ReminderConfiguration
import com.nutsnews.app.data.preferences.UserPreferences
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.widget.WidgetRefreshRequester
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PersonalizationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstRunShowsIosDefaultsAndPreventsRemovingTheLastTopic() {
        val viewModel =
            PersonalizationViewModel(InMemoryUserPreferencesRepository())
        setContent(viewModel)

        composeRule.waitUntil {
            !viewModel.uiState.value.isLoading
        }

        composeRule.onNodeWithText("Welcome").assertIsDisplayed()
        composeRule.onNodeWithTag("topic_animals").assertIsSelected()
        composeRule.onNodeWithTag("topic_science").assertIsSelected()
        composeRule.onNodeWithTag("topic_community").assertIsSelected()
        composeRule
            .onNodeWithTag("mood_calm")
            .performScrollTo()
            .assertIsSelected()
        composeRule
            .onNodeWithTag("goal_value")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("3 stories per day")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("topic_animals")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("topic_science")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("topic_animals").assertIsNotSelected()
        composeRule.onNodeWithTag("topic_science").assertIsNotSelected()
        composeRule.onNodeWithTag("topic_community").assertIsSelected()

        composeRule
            .onNodeWithTag("topic_community")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("topic_community").assertIsSelected()
        assertEquals(setOf("community"), viewModel.uiState.value.selectedTopicIds)
    }

    @Test
    fun editsCompleteFirstRunPersistAndReopenInEditor() {
        val repository = InMemoryUserPreferencesRepository()
        var activeViewModel by
            mutableStateOf(PersonalizationViewModel(repository))
        var mode by mutableStateOf(PersonalizationMode.FirstRun)
        val savedCount = AtomicInteger()

        composeRule.setContent {
            val uiState by activeViewModel.uiState.collectAsState()
            NutsNewsTheme(updateSystemBars = false) {
                PersonalizationScreen(
                    uiState = uiState,
                    mode = mode,
                    onTopicToggled = activeViewModel::onTopicToggled,
                    onMoodSelected = activeViewModel::onMoodSelected,
                    onDailyGoalChanged = activeViewModel::onDailyGoalChanged,
                    onReminderEnabledChanged =
                        activeViewModel::onReminderEnabledChanged,
                    onReminderHourSelected =
                        activeViewModel::onReminderHourSelected,
                    onSave = {
                        activeViewModel.save {
                            savedCount.incrementAndGet()
                        }
                    },
                    onClose = activeViewModel::discardChanges,
                )
            }
        }
        composeRule.waitUntil {
            !activeViewModel.uiState.value.isLoading
        }

        composeRule
            .onNodeWithTag("topic_nature")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("mood_curious")
            .performScrollTo()
            .performClick()
        repeat(2) {
            composeRule
                .onNodeWithTag("goal_increase")
                .performScrollTo()
                .performClick()
        }
        composeRule
            .onNodeWithTag("reminder_toggle")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("reminder_time_20")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("personalization_save")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            savedCount.get() == 1
        }
        val persisted =
            runBlocking {
                repository.preferences.first { preferences ->
                    preferences.hasCompletedOnboarding
                }
            }
        assertTrue("nature" in persisted.selectedTopicIds)
        assertEquals("curious", persisted.selectedMoodId)
        assertEquals(5, persisted.dailyGoal)
        assertEquals(
            ReminderConfiguration(enabled = true, hour = 20),
            persisted.reminder,
        )

        val reopenedViewModel = PersonalizationViewModel(repository)
        composeRule.runOnIdle {
            activeViewModel = reopenedViewModel
            mode = PersonalizationMode.Editor
        }
        composeRule.waitUntil {
            !reopenedViewModel.uiState.value.isLoading
        }

        composeRule.onNodeWithText("Personalize").assertIsDisplayed()
        composeRule
            .onNodeWithTag("topic_nature")
            .performScrollTo()
            .assertIsSelected()
        composeRule
            .onNodeWithTag("mood_curious")
            .performScrollTo()
            .assertIsSelected()
        composeRule
            .onNodeWithTag("goal_value")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("5 stories per day").assertIsDisplayed()
        composeRule
            .onNodeWithTag("reminder_time_20")
            .performScrollTo()
            .assertIsSelected()
        composeRule
            .onNodeWithText("Save personalization")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun closingEditorDiscardsItsDraftAndKeepsPersistedValues() {
        val repository =
            InMemoryUserPreferencesRepository(
                UserPreferences(
                    hasCompletedOnboarding = true,
                    selectedTopicIds = setOf("nature"),
                    selectedMoodId = "hopeful",
                    dailyGoal = 4,
                ),
            )
        val viewModel = PersonalizationViewModel(repository)
        val closeCount = AtomicInteger()
        setContent(
            viewModel = viewModel,
            mode = PersonalizationMode.Editor,
            onClose = {
                viewModel.discardChanges()
                closeCount.incrementAndGet()
            },
        )
        composeRule.waitUntil {
            !viewModel.uiState.value.isLoading
        }

        composeRule
            .onNodeWithTag("goal_decrease")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("topic_wellness")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithText("3 stories per day")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("personalization_close")
            .performClick()

        assertEquals(1, closeCount.get())
        assertEquals(3, viewModel.uiState.value.dailyGoal)
        assertEquals(setOf("nature"), viewModel.uiState.value.selectedTopicIds)
        assertEquals(
            3,
            runBlocking { repository.preferences.first().dailyGoal },
        )
        assertEquals(
            setOf("nature"),
            runBlocking { repository.preferences.first().selectedTopicIds },
        )
    }

    @Test
    fun dailyGoalEditsAndCompletedSavesRequestWidgetRefreshes() {
        val repository = InMemoryUserPreferencesRepository()
        val refreshCount = AtomicInteger()
        val viewModel =
            PersonalizationViewModel(
                userPreferencesRepository = repository,
                widgetRefreshRequester =
                    WidgetRefreshRequester {
                        refreshCount.incrementAndGet()
                        true
                    },
            )
        setContent(viewModel)
        composeRule.waitUntil {
            !viewModel.uiState.value.isLoading
        }

        viewModel.onDailyGoalChanged(4)
        composeRule.waitUntil {
            runBlocking { repository.preferences.first().dailyGoal == 4 }
        }
        assertEquals(1, refreshCount.get())

        viewModel.onMoodSelected("hopeful")
        composeRule.waitUntil {
            runBlocking { repository.preferences.first().selectedMoodId == "hopeful" }
        }
        assertEquals(1, refreshCount.get())

        viewModel.save()
        composeRule.waitUntil {
            !viewModel.uiState.value.isSaving
        }
        assertEquals(2, refreshCount.get())
    }

    private fun setContent(
        viewModel: PersonalizationViewModel,
        mode: PersonalizationMode = PersonalizationMode.FirstRun,
        onClose: () -> Unit = viewModel::discardChanges,
    ) {
        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsState()
            NutsNewsTheme(updateSystemBars = false) {
                PersonalizationScreen(
                    uiState = uiState,
                    mode = mode,
                    onTopicToggled = viewModel::onTopicToggled,
                    onMoodSelected = viewModel::onMoodSelected,
                    onDailyGoalChanged = viewModel::onDailyGoalChanged,
                    onReminderEnabledChanged = viewModel::onReminderEnabledChanged,
                    onReminderHourSelected = viewModel::onReminderHourSelected,
                    onSave = viewModel::save,
                    onClose = onClose,
                )
            }
        }
    }
}
