package com.nutsnews.app.feature.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.nutsnews.app.designsystem.NutsNewsTheme
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w393dp-h852dp")
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstPageHasAccessibleProgressAndSkipWorksAfterAdvancing() {
        val completionCount = AtomicInteger()
        setScreen(onComplete = completionCount::incrementAndGet)

        composeRule
            .onNode(
                hasTestTag("onboarding_heading_categories") and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
            ).assertIsDisplayed()
        composeRule
            .onNodeWithTag("onboarding_progress")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Step 1 of 3",
                ),
            )
        composeRule.onNodeWithTag("onboarding_skip").assertIsDisplayed()

        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule
            .onNodeWithTag("onboarding_heading_favorites")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_skip").performClick()

        assertEquals(1, completionCount.get())
    }

    @Test
    fun nextBackAndGetStartedFollowDeterministicPageOrder() {
        val completionCount = AtomicInteger()
        setScreen(onComplete = completionCount::incrementAndGet)

        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule.onNodeWithTag("onboarding_back").performClick()
        composeRule
            .onNodeWithTag("onboarding_heading_categories")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule
            .onNodeWithTag("onboarding_heading_reading")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_get_started").performClick()

        assertEquals(1, completionCount.get())
    }

    @Test
    fun processRecreationKeepsTheIntermediatePage() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                OnboardingScreen(onComplete = {})
            }
        }

        composeRule.onNodeWithTag("onboarding_next").performClick()
        composeRule
            .onNodeWithTag("onboarding_heading_favorites")
            .performScrollTo()
            .assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule
            .onNodeWithTag("onboarding_heading_favorites")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("onboarding_progress")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Step 2 of 3",
                ),
            )
    }

    @Test
    fun largeTextKeepsFinalActionsReachable() {
        setScreen(initialPage = 2, fontScale = 2f)

        composeRule.onNodeWithTag("onboarding_skip").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_get_started").assertIsDisplayed()
        composeRule
            .onNodeWithTag("onboarding_heading_reading")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setScreen(
        initialPage: Int = 0,
        fontScale: Float = 1f,
        onComplete: () -> Unit = {},
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
                NutsNewsTheme(updateSystemBars = false) {
                    OnboardingScreen(
                        onComplete = onComplete,
                        initialPage = initialPage,
                    )
                }
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w800dp-h1280dp")
class OnboardingTabletScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabletLayoutShowsBoundedWalkthroughAndActions() {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                OnboardingScreen(onComplete = {}, initialPage = 1)
            }
        }

        composeRule.onNodeWithTag("adaptive_pane").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_skip").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_back").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_next").assertIsDisplayed()
        composeRule
            .onNodeWithTag("onboarding_heading_favorites")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
