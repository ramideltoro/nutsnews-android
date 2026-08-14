package com.nutsnews.app.feature.feed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.NutsNewsMotion
import com.nutsnews.app.navigation.AppDestination
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FeedScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun headerMenuMatchesIosOrderIconsAndNavigatesEveryDestination() {
        val navigated = mutableListOf<AppDestination>()
        setContent(
            onDestinationSelected = navigated::add,
        )

        composeRule.onNodeWithTag("feed_header").assertIsDisplayed()
        composeRule.onNodeWithText("NutsNews").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open menu").assertIsDisplayed()
        assertEquals(
            listOf(
                "Help & F.A.Q.",
                "Today’s Picks",
                "Good Mood",
                "Reading Stats",
                "Favorites",
                "Search",
                "Personalize",
                "Settings",
            ),
            FeedMenuEntries.map(FeedMenuEntry::label),
        )
        assertEquals(
            listOf(
                AppDestination.Help,
                AppDestination.DailyDigest,
                AppDestination.GoodMood,
                AppDestination.ReadingStats,
                AppDestination.SavedStories,
                AppDestination.ArchiveSearch,
                AppDestination.Personalization,
                AppDestination.Settings,
            ),
            FeedMenuEntries.map(FeedMenuEntry::destination),
        )
        assertEquals(
            listOf(
                Icons.AutoMirrored.Filled.Help,
                Icons.Filled.Newspaper,
                Icons.Filled.AutoAwesome,
                Icons.Filled.BarChart,
                Icons.Filled.Favorite,
                Icons.Filled.Search,
                Icons.Filled.Tune,
                Icons.Filled.Settings,
            ),
            FeedMenuEntries.map(FeedMenuEntry::icon),
        )
        assertEquals(
            listOf(false, true, false, false, false, false, false, false),
            FeedMenuEntries.map(FeedMenuEntry::hasDividerBefore),
        )

        FeedMenuEntries.forEachIndexed { index, entry ->
            composeRule.onNodeWithTag("feed_menu_button").performClick()
            composeRule
                .onNodeWithTag("feed_menu_${entry.destination.route}")
                .assertIsDisplayed()
                .performClick()
            if (entry.destination == AppDestination.Settings) {
                composeRule.mainClock.advanceTimeBy(NutsNewsMotion.ActionOpenDelayMillis + 16L)
            }
            assertEquals(entry.destination, navigated[index])
        }
    }

    @Test
    fun categoryChipsScrollSelectAndMatchCaseInsensitively() {
        var selectedCategory by mutableStateOf<String?>("SCIENCE")
        val categories =
            listOf(
                "Animals",
                "Science",
                "Community",
                "Wellness",
                "Achievements",
                "Travel",
                "Culture",
                "Nature",
            )
        setContent(
            uiState =
                ArticleFeedUiState(
                    availableCategories = categories,
                    selectedCategory = selectedCategory,
                ),
            onCategorySelected = { selectedCategory = it },
            selectedCategory = { selectedCategory },
            categories = categories,
        )

        composeRule.onNodeWithTag("feed_category_all").assertIsNotSelected()
        composeRule.onNodeWithTag("feed_category_science").assertIsSelected()
        composeRule
            .onNodeWithTag("feed_category_row")
            .performScrollToNode(hasTestTag("feed_category_nature"))
        composeRule
            .onNodeWithTag("feed_category_nature")
            .assertIsDisplayed()
            .performClick()
            .assertIsSelected()
        composeRule
            .onNodeWithTag("feed_category_row")
            .performScrollToNode(hasTestTag("feed_category_science"))
        composeRule.onNodeWithTag("feed_category_science").assertIsNotSelected()
        assertEquals("Nature", selectedCategory)

        composeRule
            .onNodeWithTag("feed_category_row")
            .performScrollToNode(hasTestTag("feed_category_all"))
        composeRule
            .onNodeWithTag("feed_category_all")
            .performClick()
            .assertIsSelected()
        assertEquals(null, selectedCategory)
    }

    @Test
    fun renderedHeaderMenuAndSelectedChipProduceDistinctScreenshots() {
        var selectedCategory by mutableStateOf<String?>(null)
        val categories = listOf("Animals", "Science", "Community")
        setContent(
            uiState =
                ArticleFeedUiState(
                    availableCategories = categories,
                    selectedCategory = selectedCategory,
                ),
            onCategorySelected = { selectedCategory = it },
            selectedCategory = { selectedCategory },
            categories = categories,
        )

        val initialHeader =
            captureWindowBitmaps()
                .maxBy { image -> image.width * image.height }
        assertTrue(sampledColorCount(initialHeader) >= 6)
        val initialFingerprint = sampledFingerprint(initialHeader)

        composeRule.onNodeWithTag("feed_menu_button").performClick()
        composeRule.onNodeWithTag("feed_menu_popup").assertIsDisplayed()
        val menuWindows = captureWindowBitmaps()
        assertTrue(menuWindows.size >= 2)
        val menuScreenshot =
            menuWindows.first { image ->
                sampledFingerprint(image) != initialFingerprint
            }
        assertTrue(sampledColorCount(menuScreenshot) >= 3)
        composeRule
            .onNodeWithTag("feed_menu_${AppDestination.Help.route}")
            .performClick()

        composeRule
            .onNodeWithTag("feed_category_science")
            .performClick()
            .assertIsSelected()
        val selectedHeader =
            captureWindowBitmaps()
                .maxBy { image -> image.width * image.height }

        assertNotEquals(initialFingerprint, sampledFingerprint(selectedHeader))
    }

    private fun captureWindowBitmaps(): List<Bitmap> =
        composeRule.runOnIdle {
            val windowManagerClass = Class.forName("android.view.WindowManagerGlobal")
            val instance =
                windowManagerClass
                    .getDeclaredMethod("getInstance")
                    .invoke(null)
            val viewsField =
                windowManagerClass
                    .getDeclaredField("mViews")
                    .apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val views = viewsField.get(instance) as List<View>

            views
                .filter { view -> view.width > 0 && view.height > 0 && view.isShown }
                .map { view ->
                    Bitmap
                        .createBitmap(
                            view.width,
                            view.height,
                            Bitmap.Config.ARGB_8888,
                        ).also { bitmap ->
                            view.draw(Canvas(bitmap))
                        }
                }
        }

    private fun setContent(
        uiState: ArticleFeedUiState =
            ArticleFeedUiState(
                availableCategories =
                    listOf(
                        "Animals",
                        "Science",
                        "Community",
                    ),
            ),
        onDestinationSelected: (AppDestination) -> Unit = {},
        onCategorySelected: (String?) -> Unit = {},
        selectedCategory: (() -> String?)? = null,
        categories: List<String>? = null,
    ) {
        composeRule.setContent {
            NutsNewsTheme(updateSystemBars = false) {
                FeedScreen(
                    uiState =
                        if (selectedCategory == null || categories == null) {
                            uiState
                        } else {
                            uiState.copy(
                                availableCategories = categories,
                                selectedCategory = selectedCategory(),
                            )
                        },
                    onDestinationSelected = onDestinationSelected,
                    onCategorySelected = onCategorySelected,
                )
            }
        }
    }
}

private fun sampledColorCount(image: Bitmap): Int {
    val xStep = max(1, image.width / 40)
    val yStep = max(1, image.height / 40)
    return buildSet {
        for (y in 0 until image.height step yStep) {
            for (x in 0 until image.width step xStep) {
                add(image.getPixel(x, y))
            }
        }
    }.size
}

private fun sampledFingerprint(image: Bitmap): Long {
    val xStep = max(1, image.width / 40)
    val yStep = max(1, image.height / 40)
    var fingerprint = 1_125_899_906_842_597L
    for (y in 0 until image.height step yStep) {
        for (x in 0 until image.width step xStep) {
            fingerprint = (fingerprint * 31) + image.getPixel(x, y)
        }
    }
    return fingerprint
}
