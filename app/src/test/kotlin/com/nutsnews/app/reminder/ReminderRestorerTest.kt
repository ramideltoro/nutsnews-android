package com.nutsnews.app.reminder

import com.nutsnews.app.data.preferences.InMemoryUserPreferencesRepository
import com.nutsnews.app.data.preferences.ReminderConfiguration
import com.nutsnews.app.data.preferences.UserPreferences
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ReminderRestorerTest {
    @Test
    fun enabledPreferenceRestoresTheSelectedHour() =
        runBlocking {
            val manager = RecordingDailyReminderManager()
            val restorer =
                ReminderRestorer(
                    userPreferencesRepository =
                        InMemoryUserPreferencesRepository(
                            UserPreferences(
                                reminder =
                                    ReminderConfiguration(
                                        enabled = true,
                                        hour = 20,
                                    ),
                            ),
                        ),
                    dailyReminderManager = manager,
                )

            assertIs<ReminderScheduleResult.Scheduled>(restorer.restore())
            assertEquals(listOf(20), manager.scheduledHours)
            assertEquals(0, manager.cancelCount)
        }

    @Test
    fun disabledPreferenceCancelsAnyExistingAlarm() =
        runBlocking {
            val manager = RecordingDailyReminderManager()
            val restorer =
                ReminderRestorer(
                    userPreferencesRepository = InMemoryUserPreferencesRepository(),
                    dailyReminderManager = manager,
                )

            assertNull(restorer.restore())
            assertEquals(emptyList(), manager.scheduledHours)
            assertEquals(1, manager.cancelCount)
        }
}

internal class RecordingDailyReminderManager(
    override var canPostNotifications: Boolean = true,
    override var requiresRuntimePermission: Boolean = false,
) : DailyReminderManager {
    val scheduledHours = mutableListOf<Int>()
    var cancelCount = 0
    var deliveryCount = 0
    var channelCount = 0

    override fun createNotificationChannel() {
        channelCount += 1
    }

    override fun schedule(hour: Int): ReminderScheduleResult {
        scheduledHours += hour
        return if (canPostNotifications) {
            ReminderScheduleResult.Scheduled(hour.toLong())
        } else {
            ReminderScheduleResult.PermissionDenied
        }
    }

    override fun cancel() {
        cancelCount += 1
    }

    override fun deliverReminder(): Boolean {
        if (!canPostNotifications) return false
        deliveryCount += 1
        return true
    }
}
