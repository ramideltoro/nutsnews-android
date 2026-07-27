package com.nutsnews.app.feature.article

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.nutsnews.app.core.model.Article
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class OriginalStoryLauncherTest {
    private val colors =
        OriginalStoryColors(
            toolbar = 0xFF332211.toInt(),
            navigationBar = 0xFF110900.toInt(),
            isDark = true,
        )

    @Test
    fun validWebUrlOpensFullScreenCustomTab() {
        var launchedUri: Uri? = null
        var launchedIntent: CustomTabsIntent? = null
        val launcher =
            launcher(
                customTabStarter = { intent, uri ->
                    launchedIntent = intent
                    launchedUri = uri
                },
            )

        val result = launcher.open(article("https://example.com/good"), colors)

        assertEquals(OriginalStoryOpenResult.OpenedCustomTab, result)
        assertTrue(result.didOpen)
        assertEquals("https://example.com/good", launchedUri.toString())
        assertEquals(
            colors.toolbar,
            launchedIntent
                ?.intent
                ?.let {
                    CustomTabsIntent.getColorSchemeParams(
                        it,
                        CustomTabsIntent.COLOR_SCHEME_DARK,
                    )
                }?.toolbarColor,
        )
        assertEquals(
            colors.navigationBar,
            launchedIntent
                ?.intent
                ?.let {
                    CustomTabsIntent.getColorSchemeParams(
                        it,
                        CustomTabsIntent.COLOR_SCHEME_DARK,
                    )
                }?.navigationBarColor,
        )
        assertEquals(
            Int.MAX_VALUE,
            launchedIntent
                ?.intent
                ?.getIntExtra(CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_BREAKPOINT_DP, -1),
        )
    }

    @Test
    fun absentAndInvalidUrlsNeverLaunchABrowser() {
        var launchCount = 0
        val launcher =
            launcher(
                customTabStarter = { _, _ -> launchCount += 1 },
                fallbackStarter = { launchCount += 1 },
            )

        assertEquals(
            OriginalStoryOpenResult.MissingUrl,
            launcher.open(article(null), colors),
        )
        assertEquals(
            OriginalStoryOpenResult.InvalidUrl,
            launcher.open(article("ftp://example.com/story"), colors),
        )
        assertEquals(0, launchCount)
    }

    @Test
    fun missingCustomTabProviderUsesExternalBrowserFallback() {
        var fallbackIntent: Intent? = null
        val launcher =
            launcher(
                customTabStarter = { _, _ -> throw ActivityNotFoundException() },
                fallbackStarter = { fallbackIntent = it },
            )

        val result = launcher.open(article("https://example.com/fallback"), colors)

        assertEquals(OriginalStoryOpenResult.OpenedFallbackBrowser, result)
        assertTrue(result.didOpen)
        assertEquals(Intent.ACTION_VIEW, fallbackIntent?.action)
        assertEquals("https://example.com/fallback", fallbackIntent?.data.toString())
    }

    @Test
    fun unavailableAndFailedBrowsersReturnSafeResults() {
        val unavailable =
            launcher(
                customTabStarter = { _, _ -> throw ActivityNotFoundException() },
                fallbackStarter = { throw ActivityNotFoundException() },
            )
        val failed =
            launcher(
                customTabStarter = { _, _ -> throw SecurityException() },
                fallbackStarter = { throw SecurityException() },
            )

        assertEquals(
            OriginalStoryOpenResult.BrowserUnavailable,
            unavailable.open(article("https://example.com/unavailable"), colors),
        )
        assertEquals(
            OriginalStoryOpenResult.Failed,
            failed.open(article("https://example.com/failed"), colors),
        )
        assertFalse(
            failed.open(article("https://example.com/failed"), colors).didOpen,
        )
    }

    private fun launcher(
        customTabStarter: (CustomTabsIntent, Uri) -> Unit,
        fallbackStarter: (Intent) -> Unit = {},
    ) = AndroidOriginalStoryLauncher(
        context = RuntimeEnvironment.getApplication(),
        customTabStarter = customTabStarter,
        fallbackStarter = fallbackStarter,
    )
}

private fun article(url: String?): Article =
    Article(
        id = "browser-test",
        title = "A positive story",
        summary = "Good things happened.",
        originalUrl = url?.let(::URI),
        source = "NutsNews",
        publishedAt = null,
        createdAt = null,
        thumbnailUrl = null,
        categories = emptyList(),
    )
