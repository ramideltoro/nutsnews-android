package com.nutsnews.app.feature.splash

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutsnews.app.designsystem.NutsNewsMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class StartupSplashUiState(
    val stage: StartupSplashStage = StartupSplashStage.Waiting,
) {
    val isShowingSplash: Boolean
        get() = stage != StartupSplashStage.Complete

    val isIconVisible: Boolean
        get() =
            stage in
                setOf(
                    StartupSplashStage.IconVisible,
                    StartupSplashStage.TitleVisible,
                    StartupSplashStage.SubtitleVisible,
                )

    val isTitleVisible: Boolean
        get() =
            stage in
                setOf(
                    StartupSplashStage.TitleVisible,
                    StartupSplashStage.SubtitleVisible,
                    StartupSplashStage.IconHidden,
                )

    val isSubtitleVisible: Boolean
        get() =
            stage in
                setOf(
                    StartupSplashStage.SubtitleVisible,
                    StartupSplashStage.IconHidden,
                    StartupSplashStage.TitleHidden,
                )
}

enum class StartupSplashStage {
    Waiting,
    IconVisible,
    TitleVisible,
    SubtitleVisible,
    IconHidden,
    TitleHidden,
    SubtitleHidden,
    Complete,
}

internal object StartupSplashTiming {
    const val ElementAnimationMillis = NutsNewsMotion.SplashElementMillis
    const val ContentTransitionMillis = NutsNewsMotion.SplashContentRevealMillis

    const val InitialDelayMillis = NutsNewsMotion.SplashInitialDelayMillis
    const val StageDelayMillis = NutsNewsMotion.SplashStageDelayMillis
    const val FullyVisibleHoldMillis = NutsNewsMotion.SplashVisibleHoldMillis
}

internal fun interface StartupSplashPlayer {
    suspend fun play(onStage: (StartupSplashStage) -> Unit)
}

internal class IosParityStartupSplashPlayer(
    private val sleep: suspend (Long) -> Unit = { durationMillis ->
        delay(durationMillis)
    },
) : StartupSplashPlayer {
    override suspend fun play(onStage: (StartupSplashStage) -> Unit) {
        sleep(StartupSplashTiming.InitialDelayMillis)
        onStage(StartupSplashStage.IconVisible)

        sleep(StartupSplashTiming.StageDelayMillis)
        onStage(StartupSplashStage.TitleVisible)

        sleep(StartupSplashTiming.StageDelayMillis)
        onStage(StartupSplashStage.SubtitleVisible)

        sleep(StartupSplashTiming.FullyVisibleHoldMillis)
        onStage(StartupSplashStage.IconHidden)

        sleep(StartupSplashTiming.StageDelayMillis)
        onStage(StartupSplashStage.TitleHidden)

        sleep(StartupSplashTiming.StageDelayMillis)
        onStage(StartupSplashStage.SubtitleHidden)

        sleep(StartupSplashTiming.StageDelayMillis)
        onStage(StartupSplashStage.Complete)
    }
}

internal class StartupSplashViewModel(
    player: StartupSplashPlayer = IosParityStartupSplashPlayer(),
    playbackScope: CoroutineScope? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(StartupSplashUiState())
    val uiState: StateFlow<StartupSplashUiState> = mutableUiState.asStateFlow()

    init {
        (playbackScope ?: viewModelScope).launch {
            player.play { stage ->
                mutableUiState.value = StartupSplashUiState(stage)
            }
        }
    }
}
