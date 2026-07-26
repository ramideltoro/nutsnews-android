package com.nutsnews.app.reminder

import com.nutsnews.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class ReminderRestorer(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailyReminderManager: DailyReminderManager,
) {
    suspend fun restore(): ReminderScheduleResult? {
        val preferences = userPreferencesRepository.preferences.first()
        return if (preferences.reminder.enabled) {
            dailyReminderManager.schedule(preferences.reminder.hour)
        } else {
            dailyReminderManager.cancel()
            null
        }
    }
}
