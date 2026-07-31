package com.nutsnews.app.feature.contact

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nutsnews.app.designsystem.NutsNewsTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContactUsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displaysTheNewsPolicyContactDetails() {
        setScreen()

        composeRule.onNodeWithTag("contact_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Contact us").assertIsDisplayed()
        composeRule.onNodeWithText(NutsNewsContactDetails.EMAIL).assertIsDisplayed()
        composeRule.onNodeWithText(NutsNewsContactDetails.CONTACT_URL).assertIsDisplayed()
    }

    @Test
    fun contactAndNavigationActionsRemainAvailable() {
        var emailCount = 0
        var websiteCount = 0
        var backCount = 0
        var homeCount = 0
        setScreen(
            onEmail = { emailCount += 1 },
            onOpenContactPage = { websiteCount += 1 },
            onBack = { backCount += 1 },
            onGoHome = { homeCount += 1 },
        )

        composeRule.onNodeWithTag("contact_email_action").performClick()
        composeRule.onNodeWithTag("contact_website_action").performClick()
        composeRule.onNodeWithContentDescription("Back to settings").performClick()
        composeRule.onNodeWithContentDescription("Go home").performClick()

        assertEquals(1, emailCount)
        assertEquals(1, websiteCount)
        assertEquals(1, backCount)
        assertEquals(1, homeCount)
    }

    private fun setScreen(
        onEmail: () -> Unit = {},
        onOpenContactPage: () -> Unit = {},
        onBack: () -> Unit = {},
        onGoHome: () -> Unit = {},
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                ContactUsScreen(
                    onEmail = onEmail,
                    onOpenContactPage = onOpenContactPage,
                    onBack = onBack,
                    onGoHome = onGoHome,
                )
            }
        }
    }
}
