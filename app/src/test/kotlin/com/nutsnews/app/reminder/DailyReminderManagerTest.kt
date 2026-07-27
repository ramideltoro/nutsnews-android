package com.nutsnews.app.reminder

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class DailyReminderManagerTest {
    @Test
    fun schedulesEachSupportedTimeAsTheNextInexactDailyAlarm() {
        val now =
            ZonedDateTime.of(
                2026,
                7,
                26,
                7,
                30,
                0,
                0,
                ZoneId.of("America/New_York"),
            )

        listOf(8, 15, 20).forEach { hour ->
            val alarm = RecordingAlarmGateway()
            val manager =
                AndroidDailyReminderManager(
                    alarmGateway = alarm,
                    notificationGateway = RecordingNotificationGateway(),
                    permissionGate = MutablePermissionGate(canPost = true),
                    now = { now },
                )

            val result = assertIs<ReminderScheduleResult.Scheduled>(manager.schedule(hour))
            val trigger = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(result.triggerAtMillis),
                now.zone,
            )

            assertEquals(now.toLocalDate(), trigger.toLocalDate())
            assertEquals(hour, trigger.hour)
            assertEquals(0, trigger.minute)
            assertEquals(result.triggerAtMillis, alarm.scheduledAtMillis)
            assertEquals(1, alarm.cancelCount)
        }
    }

    @Test
    fun passedTimeMovesToTomorrowAndInvalidHourUsesMorningDefault() {
        val now =
            ZonedDateTime.of(
                2026,
                7,
                26,
                20,
                0,
                0,
                0,
                ZoneId.of("America/New_York"),
            )
        val alarm = RecordingAlarmGateway()
        val manager =
            AndroidDailyReminderManager(
                alarmGateway = alarm,
                notificationGateway = RecordingNotificationGateway(),
                permissionGate = MutablePermissionGate(canPost = true),
                now = { now },
            )

        val result = assertIs<ReminderScheduleResult.Scheduled>(manager.schedule(11))
        val trigger = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(result.triggerAtMillis),
            now.zone,
        )

        assertEquals(now.toLocalDate().plusDays(1), trigger.toLocalDate())
        assertEquals(8, trigger.hour)
    }

    @Test
    fun deniedPermissionCancelsAnyAlarmAndDoesNotScheduleOrDeliver() {
        val alarm = RecordingAlarmGateway()
        val notifications = RecordingNotificationGateway()
        val permission = MutablePermissionGate(canPost = false, requiresRuntime = true)
        val manager =
            AndroidDailyReminderManager(
                alarmGateway = alarm,
                notificationGateway = notifications,
                permissionGate = permission,
            )

        assertEquals(ReminderScheduleResult.PermissionDenied, manager.schedule(8))
        assertEquals(1, alarm.cancelCount)
        assertNull(alarm.scheduledAtMillis)
        assertFalse(manager.deliverReminder())
        assertEquals(0, notifications.postCount)

        permission.canPost = true
        assertTrue(manager.deliverReminder())
        assertEquals(1, notifications.postCount)
    }

    @Test
    fun repeatedLifecycleRestoreSchedulingReplacesTheSingleReminder() {
        val alarm = RecordingAlarmGateway()
        val now =
            ZonedDateTime.of(
                2026,
                7,
                26,
                7,
                30,
                0,
                0,
                ZoneId.of("America/New_York"),
            )
        val manager =
            AndroidDailyReminderManager(
                alarmGateway = alarm,
                notificationGateway = RecordingNotificationGateway(),
                permissionGate = MutablePermissionGate(canPost = true),
                now = { now },
            )

        val first = assertIs<ReminderScheduleResult.Scheduled>(manager.schedule(8))
        val restored = assertIs<ReminderScheduleResult.Scheduled>(manager.schedule(8))

        assertEquals(first.triggerAtMillis, restored.triggerAtMillis)
        assertEquals(2, alarm.cancelCount)
        assertEquals(2, alarm.scheduleCount)
        assertEquals(restored.triggerAtMillis, alarm.scheduledAtMillis)
    }

    @Test
    fun channelAndCancellationDelegateToTheirPlatformGateways() {
        val alarm = RecordingAlarmGateway()
        val notifications = RecordingNotificationGateway()
        val manager =
            AndroidDailyReminderManager(
                alarmGateway = alarm,
                notificationGateway = notifications,
                permissionGate = MutablePermissionGate(canPost = true),
            )

        manager.createNotificationChannel()
        manager.cancel()

        assertEquals(1, notifications.channelCount)
        assertEquals(1, alarm.cancelCount)
    }
}

internal class RecordingAlarmGateway : ReminderAlarmGateway {
    var scheduledAtMillis: Long? = null
    var cancelCount = 0
    var scheduleCount = 0

    override fun schedule(triggerAtMillis: Long) {
        scheduleCount += 1
        scheduledAtMillis = triggerAtMillis
    }

    override fun cancel() {
        cancelCount += 1
        scheduledAtMillis = null
    }
}

internal class RecordingNotificationGateway : ReminderNotificationGateway {
    var channelCount = 0
    var postCount = 0

    override fun createChannel() {
        channelCount += 1
    }

    override fun postReminder() {
        postCount += 1
    }
}

internal class MutablePermissionGate(
    var canPost: Boolean,
    var requiresRuntime: Boolean = false,
) : ReminderPermissionGate {
    override val canPostNotifications: Boolean
        get() = canPost
    override val requiresRuntimePermission: Boolean
        get() = requiresRuntime
}
