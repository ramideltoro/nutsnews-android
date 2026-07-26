package com.nutsnews.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.feature.bootstrap.BootstrapUiState
import com.nutsnews.app.feature.bootstrap.BootstrapViewModel
import com.nutsnews.app.feature.personalization.PersonalizationMode
import com.nutsnews.app.feature.personalization.PersonalizationScreen
import com.nutsnews.app.feature.personalization.PersonalizationViewModel
import com.nutsnews.app.feature.splash.StartupSplash
import com.nutsnews.app.feature.splash.StartupSplashTiming
import com.nutsnews.app.feature.splash.StartupSplashUiState
import com.nutsnews.app.feature.splash.StartupSplashViewModel
import com.nutsnews.app.navigation.AppDestination
import com.nutsnews.app.navigation.AppNavigator
import com.nutsnews.app.reminder.DailyReminderContract
import com.nutsnews.app.reminder.ReminderScheduleResult

class MainActivity : ComponentActivity() {
    private val appNavigator: AppNavigator
        get() = (application as NutsNewsApplication).container.navigator

    private val dailyReminderManager
        get() = (application as NutsNewsApplication).container.dailyReminderManager

    private val bootstrapViewModel: BootstrapViewModel by viewModels {
        BootstrapViewModel.Factory(
            navigator = appNavigator,
            userPreferencesRepository =
                (application as NutsNewsApplication).container.userPreferencesRepository,
        )
    }

    private val startupSplashViewModel: StartupSplashViewModel by viewModels()

    private val personalizationViewModel: PersonalizationViewModel by viewModels {
        PersonalizationViewModel.Factory(
            userPreferencesRepository =
                (application as NutsNewsApplication).container.userPreferencesRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NutsNews)
        super.onCreate(savedInstanceState)
        appNavigator.restoreState(savedInstanceState?.getString(NavigationStateKey))
        enableEdgeToEdge()
        setContent {
            val uiState by bootstrapViewModel.uiState.collectAsStateWithLifecycle()
            val splashUiState by
                startupSplashViewModel.uiState.collectAsStateWithLifecycle()
            val personalizationUiState by
                personalizationViewModel.uiState.collectAsStateWithLifecycle()
            val pendingPermissionSaveMode =
                remember { mutableStateOf<PersonalizationMode?>(null) }
            val savePersonalization: (PersonalizationMode) -> Unit = { mode ->
                val reminderEnabled =
                    personalizationViewModel.uiState.value.reminderEnabled
                val reminderHour =
                    personalizationViewModel.uiState.value.reminderHour
                personalizationViewModel.save(
                    onSaved = {
                        if (reminderEnabled) {
                            if (
                                dailyReminderManager.schedule(reminderHour) ==
                                ReminderScheduleResult.PermissionDenied
                            ) {
                                personalizationViewModel
                                    .onNotificationPermissionDenied()
                            }
                        } else {
                            dailyReminderManager.cancel()
                        }
                        if (mode == PersonalizationMode.Editor) {
                            bootstrapViewModel.onNavigateUp()
                        }
                    },
                )
            }
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { isGranted ->
                    val pendingMode =
                        pendingPermissionSaveMode.value
                            ?: return@rememberLauncherForActivityResult
                    pendingPermissionSaveMode.value = null
                    if (!isGranted || !dailyReminderManager.canPostNotifications) {
                        personalizationViewModel.onNotificationPermissionDenied()
                    }
                    savePersonalization(pendingMode)
                }
            NutsNewsApp(
                uiState = uiState,
                splashUiState = splashUiState,
                onNavigateUp = bootstrapViewModel::onNavigateUp,
                destinationContent = { destination ->
                    when (destination) {
                        AppDestination.Onboarding,
                        AppDestination.Personalization,
                        -> {
                            val mode =
                                if (destination == AppDestination.Onboarding) {
                                    PersonalizationMode.FirstRun
                                } else {
                                    PersonalizationMode.Editor
                                }
                            PersonalizationScreen(
                                uiState = personalizationUiState,
                                mode = mode,
                                onTopicToggled = personalizationViewModel::onTopicToggled,
                                onMoodSelected = personalizationViewModel::onMoodSelected,
                                onDailyGoalChanged =
                                    personalizationViewModel::onDailyGoalChanged,
                                onReminderEnabledChanged =
                                    personalizationViewModel::onReminderEnabledChanged,
                                onReminderHourSelected =
                                    personalizationViewModel::onReminderHourSelected,
                                onSave = {
                                    when {
                                        personalizationUiState.reminderEnabled &&
                                            dailyReminderManager.requiresRuntimePermission -> {
                                            pendingPermissionSaveMode.value = mode
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS,
                                            )
                                        }

                                        personalizationUiState.reminderEnabled &&
                                            !dailyReminderManager.canPostNotifications -> {
                                            personalizationViewModel
                                                .onNotificationPermissionDenied()
                                            savePersonalization(mode)
                                        }

                                        else -> savePersonalization(mode)
                                    }
                                },
                                onClose = {
                                    personalizationViewModel.discardChanges()
                                    bootstrapViewModel.onNavigateUp()
                                },
                            )
                        }

                        else -> NutsNewsDestinationPlaceholder(destination)
                    }
                },
            )
        }
        handleLaunchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(NavigationStateKey, appNavigator.saveState())
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val NavigationStateKey = "nutsnews.navigation.backStack"
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.action != DailyReminderContract.ActionOpenDailyDigest) return
        intent.action = null
        bootstrapViewModel.onDailyReminderNotificationOpened()
    }
}

@Composable
internal fun NutsNewsApp(
    uiState: BootstrapUiState,
    splashUiState: StartupSplashUiState,
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Boolean = { false },
    destinationContent: @Composable (AppDestination) -> Unit = {
        NutsNewsDestinationPlaceholder(it)
    },
) {
    NutsNewsTheme(theme = uiState.theme) {
        BackHandler(
            enabled = !splashUiState.isShowingSplash && uiState.canNavigateUp,
        ) {
            onNavigateUp()
        }

        val contentProgress by
            animateFloatAsState(
                targetValue = if (splashUiState.isShowingSplash) 0f else 1f,
                animationSpec =
                    tween(
                        durationMillis = StartupSplashTiming.ContentTransitionMillis,
                        easing = FastOutSlowInEasing,
                    ),
                label = "Startup content transition",
            )

        Box(modifier = modifier.fillMaxSize()) {
            key(uiState.destination) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = contentProgress
                                scaleX = 0.99f + (0.01f * contentProgress)
                                scaleY = 0.99f + (0.01f * contentProgress)
                            },
                ) {
                    destinationContent(uiState.destination)
                }
            }

            AnimatedVisibility(
                visible = splashUiState.isShowingSplash,
                enter = EnterTransition.None,
                exit =
                    fadeOut(
                        animationSpec =
                            tween(
                                durationMillis = StartupSplashTiming.ContentTransitionMillis,
                                easing = FastOutSlowInEasing,
                            ),
                    ),
            ) {
                StartupSplash(uiState = splashUiState)
            }
        }
    }
}

@Composable
private fun NutsNewsDestinationPlaceholder(destination: AppDestination) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(destination.shellTitle)
    }
}

@Preview(showBackground = true)
@Composable
private fun NutsNewsAppPreview() {
    NutsNewsApp(
        uiState = BootstrapUiState(),
        splashUiState = StartupSplashUiState(),
    )
}
