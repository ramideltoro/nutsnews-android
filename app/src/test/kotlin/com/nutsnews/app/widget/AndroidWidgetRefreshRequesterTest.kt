package com.nutsnews.app.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.test.core.app.ApplicationProvider
import com.nutsnews.app.BuildConfig
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidWidgetRefreshRequesterTest {
    @Test
    fun refreshImmediatelyBroadcastsSystemAndNutsNewsActionsToThisPackage() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val shadowApplication = shadowOf(application)
        val beforeCount = shadowApplication.broadcastIntents.size

        assertTrue(AndroidWidgetRefreshRequester(application).requestRefresh())

        val broadcasts = shadowApplication.broadcastIntents.drop(beforeCount)
        assertEquals(
            listOf(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                NutsNewsWidgetContract.ActionRefresh,
            ),
            broadcasts.map { intent -> intent.action },
        )
        assertTrue(
            broadcasts.all { intent ->
                intent.`package` == BuildConfig.APPLICATION_ID
            },
        )
    }

    @Test
    fun noOpRequesterMakesUnsupportedWidgetRefreshSafe() {
        assertEquals(false, NoOpWidgetRefreshRequester.requestRefresh())
    }
}
