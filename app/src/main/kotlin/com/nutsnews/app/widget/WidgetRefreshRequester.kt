package com.nutsnews.app.widget

import android.content.Context
import android.content.Intent

fun interface WidgetRefreshRequester {
    fun requestRefresh(): Boolean
}

object NoOpWidgetRefreshRequester : WidgetRefreshRequester {
    override fun requestRefresh(): Boolean = false
}

class AndroidWidgetRefreshRequester(
    context: Context,
) : WidgetRefreshRequester {
    private val applicationContext = context.applicationContext

    override fun requestRefresh(): Boolean =
        runCatching {
            applicationContext.sendBroadcast(
                Intent(NutsNewsWidgetContract.ActionRefresh)
                    .setClass(applicationContext, NutsNewsWidgetReceiver::class.java),
            )
            true
        }.getOrDefault(false)
}

object NutsNewsWidgetContract {
    const val ActionRefresh = "com.nutsnews.app.action.REFRESH_WIDGET"
}
