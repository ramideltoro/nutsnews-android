package com.nutsnews.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreUserPreferencesRepositoryTest {
    @JvmField
    @Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun missingValuesUseIosCompatibleDefaults() =
        runBlocking {
            val fixture = fixture("defaults")

            assertEquals(UserPreferences(), fixture.repository.preferences.first())
            assertFalse(fixture.repository.hasCompletedOnboarding.first())
            assertEquals(
                setOf("animals", "community", "science"),
                fixture.repository.preferences.first().selectedTopicIds,
            )

            fixture.close()
        }

    @Test
    fun updatesPersistEveryOnboardingAndSettingsPreference() =
        runBlocking {
            val file = preferenceFile("persisted")
            val firstFixture = fixture(file)

            firstFixture.repository.setOnboardingCompleted(true)
            firstFixture.repository.setSelectedTopics(setOf("travel", "nature"))
            firstFixture.repository.setSelectedMood("curious")
            firstFixture.repository.setDailyGoal(5)
            firstFixture.repository.setReminderConfiguration(
                ReminderConfiguration(enabled = true, hour = 20),
            )
            firstFixture.repository.setTheme(NutsNewsAppTheme.Friday)
            firstFixture.repository.setHapticsEnabled(false)
            firstFixture.repository.setShowStatsOnLargeWidget(false)
            firstFixture.close()

            val reopenedFixture = fixture(file)
            assertEquals(
                UserPreferences(
                    hasCompletedOnboarding = true,
                    selectedTopicIds = setOf("nature", "travel"),
                    selectedMoodId = "curious",
                    dailyGoal = 5,
                    reminder = ReminderConfiguration(enabled = true, hour = 20),
                    theme = NutsNewsAppTheme.Friday,
                    hapticsEnabled = false,
                    showStatsOnLargeWidget = false,
                ),
                reopenedFixture.repository.preferences.first(),
            )
            reopenedFixture.close()
        }

    @Test
    fun publicUpdatesSanitizeInvalidSelectionsBeforeWriting() =
        runBlocking {
            val fixture = fixture("sanitized-updates")

            fixture.repository.updatePreferences {
                it.copy(
                    selectedTopicIds = setOf("not-a-topic"),
                    selectedMoodId = "not-a-mood",
                    dailyGoal = 99,
                    reminder = ReminderConfiguration(enabled = true, hour = 11),
                )
            }

            val preferences = fixture.repository.preferences.first()
            assertEquals(UserPreferenceDefaults.DefaultTopicIds, preferences.selectedTopicIds)
            assertEquals(UserPreferenceDefaults.DefaultMoodId, preferences.selectedMoodId)
            assertEquals(5, preferences.dailyGoal)
            assertEquals(
                ReminderConfiguration(enabled = true, hour = 8),
                preferences.reminder,
            )
            fixture.close()
        }

    @Test
    fun invalidStoredValuesRecoverWithoutDiscardingValidSelections() =
        runBlocking {
            val fixture = fixture("invalid-values")
            fixture.dataStore.edit { preferences ->
                preferences[DataStoreUserPreferencesRepository.Keys.SelectedTopics] =
                    " unknown, science, bad "
                preferences[DataStoreUserPreferencesRepository.Keys.SelectedMood] = "energized"
                preferences[DataStoreUserPreferencesRepository.Keys.DailyGoal] = -4
                preferences[DataStoreUserPreferencesRepository.Keys.ReminderEnabled] = true
                preferences[DataStoreUserPreferencesRepository.Keys.ReminderHour] = 24
                preferences[DataStoreUserPreferencesRepository.Keys.SelectedTheme] = "missing"
            }

            val preferences = fixture.repository.preferences.first()
            assertEquals(setOf("science"), preferences.selectedTopicIds)
            assertEquals("calm", preferences.selectedMoodId)
            assertEquals(1, preferences.dailyGoal)
            assertEquals(ReminderConfiguration(enabled = true, hour = 8), preferences.reminder)
            assertEquals(NutsNewsAppTheme.Amber, preferences.theme)
            fixture.close()
        }

    @Test
    fun whollyInvalidStoredTopicsRecoverToTheDefaultSet() =
        runBlocking {
            val fixture = fixture("invalid-topics")
            fixture.dataStore.edit { preferences ->
                preferences[DataStoreUserPreferencesRepository.Keys.SelectedTopics] =
                    "unknown,also-unknown"
            }

            assertEquals(
                UserPreferenceDefaults.DefaultTopicIds,
                fixture.repository.preferences.first().selectedTopicIds,
            )
            fixture.close()
        }

    @Test
    fun corruptedDataStoreFileIsReplacedWithDefaults() =
        runBlocking {
            val file = preferenceFile("corrupt")
            Files.write(file.toPath(), byteArrayOf(0x01, 0x02, 0x03, 0x04))
            val fixture =
                fixture(
                    file = file,
                    corruptionHandler =
                        ReplaceFileCorruptionHandler {
                            emptyPreferences()
                        },
                )

            assertEquals(UserPreferences(), fixture.repository.preferences.first())
            assertTrue(file.exists())
            fixture.close()
        }

    private fun fixture(name: String): Fixture = fixture(preferenceFile(name))

    private fun fixture(
        file: File,
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    ): Fixture {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        val dataStore =
            PreferenceDataStoreFactory.create(
                corruptionHandler = corruptionHandler,
                scope = scope,
                produceFile = { file },
            )
        return Fixture(
            dataStore = dataStore,
            repository = DataStoreUserPreferencesRepository(dataStore),
            job = job,
        )
    }

    private fun preferenceFile(name: String): File =
        temporaryFolder.root
            .resolve("$name.preferences_pb")

    private data class Fixture(
        val dataStore: DataStore<Preferences>,
        val repository: DataStoreUserPreferencesRepository,
        val job: Job,
    ) {
        suspend fun close() {
            job.cancelAndJoin()
        }
    }
}
