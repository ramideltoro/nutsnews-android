package com.nutsnews.app.widget

import java.io.File
import kotlin.test.assertContains
import org.junit.Test

class NutsNewsWidgetHostConfigTest {
    @Test
    fun manifestRegistersExportedGlanceReceiverAndRefreshActions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertContains(manifest, ".widget.NutsNewsWidgetReceiver")
        assertContains(manifest, "android:exported=\"true\"")
        assertContains(manifest, "android.appwidget.action.APPWIDGET_UPDATE")
        assertContains(manifest, NutsNewsWidgetContract.ActionRefresh)
        assertContains(manifest, "android.appwidget.provider")
        assertContains(manifest, "@xml/nutsnews_widget_info")
    }

    @Test
    fun providerSupportsResponsiveHomeScreenPlacementAndThreeHourUpdates() {
        val provider = File("src/main/res/xml/nutsnews_widget_info.xml").readText()

        assertContains(provider, "android:widgetCategory=\"home_screen\"")
        assertContains(provider, "android:resizeMode=\"horizontal|vertical\"")
        assertContains(provider, "android:minWidth=\"110dp\"")
        assertContains(provider, "android:minHeight=\"110dp\"")
        assertContains(provider, "android:targetCellWidth=\"2\"")
        assertContains(provider, "android:targetCellHeight=\"2\"")
        assertContains(provider, "android:updatePeriodMillis=\"10800000\"")
        assertContains(provider, "android:initialLayout=\"@layout/nutsnews_widget_loading\"")
        assertContains(provider, "android:previewLayout=\"@layout/nutsnews_widget_preview\"")
    }

    @Test
    fun pickerPreviewAndInitialLoadingLayoutsExposeBrandedUsefulStates() {
        val loading = File("src/main/res/layout/nutsnews_widget_loading.xml").readText()
        val preview = File("src/main/res/layout/nutsnews_widget_preview.xml").readText()

        assertContains(loading, "@string/widget_loading")
        assertContains(loading, "android:indeterminate=\"true\"")
        assertContains(loading, "@drawable/nutsnews_widget_preview_background")
        assertContains(preview, "@string/widget_preview_title")
        assertContains(preview, "@string/widget_preview_summary")
        assertContains(preview, "@string/widget_preview_mood")
        assertContains(preview, "@drawable/nutsnews_widget_preview_background")
    }
}
