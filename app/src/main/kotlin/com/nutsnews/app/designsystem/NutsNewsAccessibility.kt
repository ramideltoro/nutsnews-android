package com.nutsnews.app.designsystem

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import kotlin.math.max
import kotlin.math.min

/**
 * Keeps custom, iOS-shaped controls at Android's minimum accessible touch size.
 *
 * Material controls already apply this policy. NutsNews uses this modifier on
 * bespoke chips, cards, and text buttons that do not receive it automatically.
 */
fun Modifier.nutsNewsMinimumTouchTarget(): Modifier =
    minimumInteractiveComponentSize()

/** Marks a dynamic status so TalkBack announces updates without interrupting speech. */
fun Modifier.nutsNewsPoliteAnnouncement(): Modifier =
    semantics {
        liveRegion = LiveRegionMode.Polite
    }

/** Exposes a visual section title as a heading for screen-reader navigation. */
fun Modifier.nutsNewsHeading(): Modifier =
    semantics {
        heading()
    }

/** Gives a full-screen or modal destination a stable accessibility pane name. */
fun Modifier.nutsNewsPane(title: String): Modifier =
    semantics {
        paneTitle = title
        isTraversalGroup = true
    }

/**
 * Observes Android's animator-duration accessibility setting.
 *
 * Compose animation primitives already consume this setting. Exposing it to
 * feature code also lets NutsNews skip decorative delays and infinite motion.
 */
@Composable
internal fun rememberNutsNewsReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reducedMotion by
        remember(resolver) {
            mutableStateOf(resolver.animationsAreDisabled())
        }
    DisposableEffect(resolver) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reducedMotion = resolver.animationsAreDisabled()
                }
            }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }
    return reducedMotion
}

private fun android.content.ContentResolver.animationsAreDisabled(): Boolean =
    Settings.Global.getFloat(
        this,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f

/**
 * WCAG contrast ratio after compositing translucent foregrounds over the
 * supplied opaque background.
 */
internal fun nutsNewsContrastRatio(
    foreground: Color,
    background: Color,
): Float {
    val opaqueBackground =
        if (background.alpha < 1f) {
            background.compositeOver(Color.Black)
        } else {
            background
        }
    val opaqueForeground =
        if (foreground.alpha < 1f) {
            foreground.compositeOver(opaqueBackground)
        } else {
            foreground
        }
    val lighter = max(opaqueForeground.luminance(), opaqueBackground.luminance())
    val darker = min(opaqueForeground.luminance(), opaqueBackground.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
