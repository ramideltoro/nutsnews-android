package com.nutsnews.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nutsnews.app.NutsNewsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailyReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != DailyReminderContract.ActionDeliverReminder) return

        val application = context.applicationContext as NutsNewsApplication
        application.container.dailyReminderManager.deliverReminder()
        restoreNextAlarm(application)
    }
}

class DailyReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action !in SupportedActions) return
        restoreNextAlarm(context.applicationContext as NutsNewsApplication)
    }

    companion object {
        val SupportedActions: Set<String> =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
    }
}

private fun BroadcastReceiver.restoreNextAlarm(
    application: NutsNewsApplication,
) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            ReminderRestorer(
                userPreferencesRepository =
                    application.container.userPreferencesRepository,
                dailyReminderManager = application.container.dailyReminderManager,
            ).restore()
        } finally {
            pendingResult.finish()
        }
    }
}
