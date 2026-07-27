package com.nutsnews.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.nutsnews.app.feature.bootstrap.BootstrapUiState
import com.nutsnews.app.feature.splash.StartupSplashStage
import com.nutsnews.app.feature.splash.StartupSplashUiState
import com.nutsnews.app.navigation.AppDestination
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class NutsNewsAccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun destinationPaneIsNamedAndSplashIsSkippedWhenReducedMotionIsEnabled() {
        composeRule.setContent {
            NutsNewsApp(
                uiState = BootstrapUiState(destination = AppDestination.Feed),
                splashUiState = StartupSplashUiState(StartupSplashStage.Waiting),
                reducedMotionOverride = true,
                destinationContent = {
                    Text(
                        text = "Feed destination",
                        modifier = Modifier.testTag("accessible_destination"),
                    )
                },
            )
        }

        composeRule.onNodeWithTag("accessible_destination").assertIsDisplayed()
        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "NutsNews",
                ),
            ).assertIsDisplayed()
        composeRule
            .onAllNodesWithContentDescription("NutsNews chestnuts")
            .assertCountEquals(0)
    }

    @Test
    fun visibleSplashOwnsAccessibilityFocusAndHidesBackgroundDestination() {
        composeRule.setContent {
            NutsNewsApp(
                uiState = BootstrapUiState(destination = AppDestination.Feed),
                splashUiState =
                    StartupSplashUiState(
                        StartupSplashStage.SubtitleVisible,
                    ),
                reducedMotionOverride = false,
                destinationContent = {
                    Text(
                        text = "Feed destination",
                        modifier = Modifier.testTag("background_destination"),
                    )
                },
            )
        }

        composeRule
            .onNodeWithContentDescription("NutsNews chestnuts")
            .assertIsDisplayed()
        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "Starting NutsNews",
                ),
            ).assertIsDisplayed()
        composeRule.onAllNodesWithTag("background_destination").assertCountEquals(0)
    }
}
