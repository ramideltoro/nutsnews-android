package com.nutsnews.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nutsnews.app.R
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@Suppress("DEPRECATION")
class AndroidReminderPlatformTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    @Test
    fun alarmGatewayUsesAnInexactAllowWhileIdleAlarmAndCancelsIt() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val shadowAlarmManager = shadowOf(alarmManager)
        val gateway = AndroidReminderAlarmGateway(context)
        val triggerAtMillis = System.currentTimeMillis() + 60_000

        gateway.schedule(triggerAtMillis)

        val scheduledAlarm = assertNotNull(shadowAlarmManager.peekNextScheduledAlarm())
        assertEquals(AlarmManager.RTC_WAKEUP, scheduledAlarm.type)
        assertEquals(triggerAtMillis, scheduledAlarm.triggerAtTime)
        assertEquals(0, scheduledAlarm.interval)
        assertTrue(scheduledAlarm.allowWhileIdle)
        assertTrue(shadowOf(scheduledAlarm.operation).isImmutable)
        assertEquals(
            DailyReminderContract.ActionDeliverReminder,
            shadowOf(scheduledAlarm.operation).savedIntent.action,
        )

        gateway.cancel()

        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun notificationChannelAndContentMatchIosAndTapTargetsDailyDigest() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val gateway = AndroidReminderNotificationGateway(context)
        val shadowNotificationManager = shadowOf(notificationManager)
        notificationManager.cancelAll()

        gateway.createChannel()
        gateway.postReminder()

        val channel =
            assertNotNull(
                notificationManager.getNotificationChannel(DailyReminderContract.ChannelId),
            )
        assertEquals(
            context.getString(R.string.daily_reminder_channel_name),
            channel.name.toString(),
        )
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)

        val notification =
            assertNotNull(
                shadowNotificationManager.getNotification(
                    DailyReminderContract.NotificationId,
                ),
            )
        assertEquals(
            context.getString(R.string.daily_reminder_title),
            notification.extras.getString(Notification.EXTRA_TITLE),
        )
        assertEquals(
            context.getString(R.string.daily_reminder_body),
            notification.extras.getString(Notification.EXTRA_TEXT),
        )
        assertEquals(Notification.CATEGORY_REMINDER, notification.category)
        val pendingIntent = assertNotNull(notification.contentIntent)
        val shadowPendingIntent = shadowOf(pendingIntent)
        assertTrue(shadowPendingIntent.isActivity)
        assertTrue(shadowPendingIntent.isImmutable)
        assertEquals(
            DailyReminderContract.ActionOpenDailyDigest,
            shadowPendingIntent.savedIntent.action,
        )
    }

    @Test
    fun permissionGateDistinguishesRuntimeDenialAndSystemSettings() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()
        val shadowApplication = shadowOf(application)
        val shadowNotificationManager = shadowOf(notificationManager)
        shadowApplication.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowNotificationManager.setNotificationsEnabled(true)
        val gate = AndroidReminderPermissionGate(context)

        assertTrue(gate.requiresRuntimePermission)
        assertFalse(gate.canPostNotifications)

        shadowApplication.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(gate.requiresRuntimePermission)
        assertTrue(gate.canPostNotifications)

        shadowNotificationManager.setNotificationsEnabled(false)
        assertFalse(gate.canPostNotifications)
    }
}
