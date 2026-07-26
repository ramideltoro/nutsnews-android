package com.nutsnews.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.nutsnews.app.MainActivity
import com.nutsnews.app.R
import com.nutsnews.app.data.preferences.UserPreferenceDefaults
import java.time.ZonedDateTime

interface DailyReminderManager {
    val canPostNotifications: Boolean
    val requiresRuntimePermission: Boolean

    fun createNotificationChannel()

    fun schedule(hour: Int): ReminderScheduleResult

    fun cancel()

    fun deliverReminder(): Boolean
}

sealed interface ReminderScheduleResult {
    data class Scheduled(
        val triggerAtMillis: Long,
    ) : ReminderScheduleResult

    data object PermissionDenied : ReminderScheduleResult
}

object NoOpDailyReminderManager : DailyReminderManager {
    override val canPostNotifications: Boolean = false
    override val requiresRuntimePermission: Boolean = false

    override fun createNotificationChannel() = Unit

    override fun schedule(hour: Int): ReminderScheduleResult =
        ReminderScheduleResult.PermissionDenied

    override fun cancel() = Unit

    override fun deliverReminder(): Boolean = false
}

internal class AndroidDailyReminderManager(
    private val alarmGateway: ReminderAlarmGateway,
    private val notificationGateway: ReminderNotificationGateway,
    private val permissionGate: ReminderPermissionGate,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) : DailyReminderManager {
    override val canPostNotifications: Boolean
        get() = permissionGate.canPostNotifications

    override val requiresRuntimePermission: Boolean
        get() = permissionGate.requiresRuntimePermission

    override fun createNotificationChannel() {
        notificationGateway.createChannel()
    }

    override fun schedule(hour: Int): ReminderScheduleResult {
        alarmGateway.cancel()
        if (!canPostNotifications) {
            return ReminderScheduleResult.PermissionDenied
        }

        val sanitizedHour =
            hour.takeIf(UserPreferenceDefaults.ValidReminderHours::contains)
                ?: UserPreferenceDefaults.DefaultReminderHour
        val triggerAtMillis =
            nextReminderDateTime(
                now = now(),
                hour = sanitizedHour,
            ).toInstant()
                .toEpochMilli()
        alarmGateway.schedule(triggerAtMillis)
        return ReminderScheduleResult.Scheduled(triggerAtMillis)
    }

    override fun cancel() {
        alarmGateway.cancel()
    }

    override fun deliverReminder(): Boolean {
        if (!canPostNotifications) return false
        return runCatching {
            notificationGateway.postReminder()
            true
        }.getOrDefault(false)
    }

    companion object {
        fun create(context: Context): AndroidDailyReminderManager {
            val applicationContext = context.applicationContext
            return AndroidDailyReminderManager(
                alarmGateway = AndroidReminderAlarmGateway(applicationContext),
                notificationGateway =
                    AndroidReminderNotificationGateway(applicationContext),
                permissionGate = AndroidReminderPermissionGate(applicationContext),
            )
        }
    }
}

internal fun nextReminderDateTime(
    now: ZonedDateTime,
    hour: Int,
): ZonedDateTime {
    val today =
        now.toLocalDate()
            .atTime(hour, 0)
            .atZone(now.zone)
    return if (today.isAfter(now)) today else today.plusDays(1)
}

internal interface ReminderAlarmGateway {
    fun schedule(triggerAtMillis: Long)

    fun cancel()
}

internal interface ReminderNotificationGateway {
    fun createChannel()

    fun postReminder()
}

internal interface ReminderPermissionGate {
    val canPostNotifications: Boolean
    val requiresRuntimePermission: Boolean
}

internal class AndroidReminderAlarmGateway(
    private val context: Context,
) : ReminderAlarmGateway {
    private val alarmManager =
        context.getSystemService(AlarmManager::class.java)

    override fun schedule(triggerAtMillis: Long) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            alarmPendingIntent(),
        )
    }

    override fun cancel() {
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            DailyReminderContract.AlarmRequestCode,
            Intent(context, DailyReminderAlarmReceiver::class.java).apply {
                action = DailyReminderContract.ActionDeliverReminder
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

internal class AndroidReminderNotificationGateway(
    private val context: Context,
) : ReminderNotificationGateway {
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    override fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                DailyReminderContract.ChannelId,
                context.getString(R.string.daily_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description =
                    context.getString(R.string.daily_reminder_channel_description)
            },
        )
    }

    override fun postReminder() {
        val openAppIntent =
            Intent(context, MainActivity::class.java).apply {
                action = DailyReminderContract.ActionOpenDailyDigest
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        val contentIntent =
            PendingIntent.getActivity(
                context,
                DailyReminderContract.ContentRequestCode,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            Notification.Builder(context, DailyReminderContract.ChannelId)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setColor(context.getColor(R.color.launcher_background))
                .setContentTitle(context.getString(R.string.daily_reminder_title))
                .setContentText(context.getString(R.string.daily_reminder_body))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()
        notificationManager.notify(
            DailyReminderContract.NotificationId,
            notification,
        )
    }
}

internal class AndroidReminderPermissionGate(
    private val context: Context,
) : ReminderPermissionGate {
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    override val requiresRuntimePermission: Boolean
        get() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED

    override val canPostNotifications: Boolean
        get() = !requiresRuntimePermission && notificationManager.areNotificationsEnabled()
}

object DailyReminderContract {
    const val ChannelId = "nutsnews.daily.good-news-reset"
    const val ActionDeliverReminder = "com.nutsnews.app.action.DELIVER_DAILY_REMINDER"
    const val ActionOpenDailyDigest = "com.nutsnews.app.action.OPEN_DAILY_DIGEST"
    const val NotificationId = 2_301
    const val AlarmRequestCode = 2_301
    const val ContentRequestCode = 2_302
}
