package com.nutsnews.app.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NutsNewsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NutsNewsDailyWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != NutsNewsWidgetContract.ActionRefresh) {
            super.onReceive(context, intent)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                NutsNewsWidgetUpdater.updateAll(
                    context = context.applicationContext,
                    forceRefresh = false,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
