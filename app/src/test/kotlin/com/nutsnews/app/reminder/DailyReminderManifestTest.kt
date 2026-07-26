package com.nutsnews.app.reminder

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class DailyReminderManifestTest {
    @Test
    fun manifestRegistersPermissionAndAllRestoreActionsWithoutExactAlarmAccess() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertContains(manifest, "android.permission.POST_NOTIFICATIONS")
        assertContains(manifest, "android.permission.RECEIVE_BOOT_COMPLETED")
        assertFalse(manifest.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertFalse(manifest.contains("android.permission.USE_EXACT_ALARM"))
        assertContains(manifest, ".reminder.DailyReminderAlarmReceiver")
        assertContains(manifest, ".reminder.DailyReminderRestoreReceiver")
        assertContains(manifest, "android.intent.action.BOOT_COMPLETED")
        assertContains(manifest, "android.intent.action.MY_PACKAGE_REPLACED")
        assertContains(manifest, "android.intent.action.TIME_SET")
        assertContains(manifest, "android.intent.action.TIMEZONE_CHANGED")
        assertEquals(
            setOf(
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.MY_PACKAGE_REPLACED",
                "android.intent.action.TIME_SET",
                "android.intent.action.TIMEZONE_CHANGED",
            ),
            DailyReminderRestoreReceiver.SupportedActions,
        )
    }
}
