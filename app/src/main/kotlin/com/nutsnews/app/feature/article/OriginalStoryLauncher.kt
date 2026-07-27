package com.nutsnews.app.feature.article

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Immutable
import com.nutsnews.app.core.model.Article
import java.util.Locale

@Immutable
data class OriginalStoryColors(
    val toolbar: Int,
    val navigationBar: Int,
    val isDark: Boolean,
)

sealed interface OriginalStoryOpenResult {
    data object OpenedCustomTab : OriginalStoryOpenResult

    data object OpenedFallbackBrowser : OriginalStoryOpenResult

    data object MissingUrl : OriginalStoryOpenResult

    data object InvalidUrl : OriginalStoryOpenResult

    data object BrowserUnavailable : OriginalStoryOpenResult

    data object Failed : OriginalStoryOpenResult

    val didOpen: Boolean
        get() = this is OpenedCustomTab || this is OpenedFallbackBrowser
}

fun interface OriginalStoryLauncher {
    fun open(
        article: Article,
        colors: OriginalStoryColors,
    ): OriginalStoryOpenResult
}

class AndroidOriginalStoryLauncher(
    private val context: Context,
    private val customTabStarter: (CustomTabsIntent, Uri) -> Unit = { intent, uri ->
        intent.launchUrl(context, uri)
    },
    private val fallbackStarter: (Intent) -> Unit = context::startActivity,
) : OriginalStoryLauncher {
    override fun open(
        article: Article,
        colors: OriginalStoryColors,
    ): OriginalStoryOpenResult {
        val originalUrl = article.originalUrl ?: return OriginalStoryOpenResult.MissingUrl
        val scheme = originalUrl.scheme?.lowercase(Locale.ROOT)
        if (
            (scheme != "http" && scheme != "https") ||
            originalUrl.host.isNullOrBlank()
        ) {
            return OriginalStoryOpenResult.InvalidUrl
        }
        val uri = Uri.parse(originalUrl.toASCIIString())
        val colorParams =
            CustomTabColorSchemeParams
                .Builder()
                .setToolbarColor(colors.toolbar)
                .setNavigationBarColor(colors.navigationBar)
                .build()
        val customTab =
            CustomTabsIntent
                .Builder()
                .setDefaultColorSchemeParams(colorParams)
                .setColorScheme(
                    if (colors.isDark) {
                        CustomTabsIntent.COLOR_SCHEME_DARK
                    } else {
                        CustomTabsIntent.COLOR_SCHEME_LIGHT
                    },
                )
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setUrlBarHidingEnabled(true)
                .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_START)
                .setActivitySideSheetBreakpointDp(Int.MAX_VALUE)
                .build()

        return try {
            customTabStarter(customTab, uri)
            OriginalStoryOpenResult.OpenedCustomTab
        } catch (customTabFailure: Exception) {
            val fallback =
                Intent(Intent.ACTION_VIEW, uri)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .apply {
                        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            try {
                fallbackStarter(fallback)
                OriginalStoryOpenResult.OpenedFallbackBrowser
            } catch (_: ActivityNotFoundException) {
                if (customTabFailure is ActivityNotFoundException) {
                    OriginalStoryOpenResult.BrowserUnavailable
                } else {
                    OriginalStoryOpenResult.Failed
                }
            } catch (_: Exception) {
                OriginalStoryOpenResult.Failed
            }
        }
    }
}
