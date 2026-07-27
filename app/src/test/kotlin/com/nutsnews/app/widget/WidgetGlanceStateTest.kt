package com.nutsnews.app.widget

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class WidgetGlanceStateTest {
    @Test
    fun widgetDataRoundTripsThroughObservableGlancePreferences() {
        val expected =
            WidgetData(
                article =
                    WidgetArticle(
                        storyId = "story-42",
                        title = "A school opens a community garden",
                        summary = "Neighbors helped students create a green space.",
                        source = "NutsNews",
                        mood = "Community",
                    ),
                articleStatus = WidgetArticleStatus.Stale,
                theme = NutsNewsAppTheme.Bambi,
                stats =
                    WidgetStats(
                        todayCount = 2,
                        dailyGoal = 4,
                        currentStreak = 7,
                        totalStoryCount = 31,
                    ),
                showStatsOnLargeWidget = false,
                refreshedAt = Instant.parse("2026-07-26T20:15:30Z"),
            )
        val preferences = mutablePreferencesOf()

        preferences.writeWidgetData(expected)

        assertEquals(expected, preferences.readWidgetData())
    }

    @Test
    fun uninitializedGlanceStateDoesNotReplaceTheInitialWidgetPayload() {
        assertNull(emptyPreferences().readWidgetData())
    }
}
