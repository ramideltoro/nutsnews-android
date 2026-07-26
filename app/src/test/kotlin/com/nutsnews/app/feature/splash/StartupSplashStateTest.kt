package com.nutsnews.app.feature.splash

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

class StartupSplashStateTest {
    @Test
    fun playerMatchesTheFrozenIosStageOrderAndTiming() =
        runBlocking {
            var elapsedMillis = 0L
            val observedStages = mutableListOf<TimedStage>()
            val player =
                IosParityStartupSplashPlayer { durationMillis ->
                    elapsedMillis += durationMillis
                }

            player.play { stage ->
                observedStages += TimedStage(elapsedMillis, stage)
            }

            assertEquals(
                listOf(
                    TimedStage(500, StartupSplashStage.IconVisible),
                    TimedStage(1_000, StartupSplashStage.TitleVisible),
                    TimedStage(1_500, StartupSplashStage.SubtitleVisible),
                    TimedStage(2_500, StartupSplashStage.IconHidden),
                    TimedStage(3_000, StartupSplashStage.TitleHidden),
                    TimedStage(3_500, StartupSplashStage.SubtitleHidden),
                    TimedStage(4_000, StartupSplashStage.Complete),
                ),
                observedStages,
            )
        }

    @Test
    fun stageVisibilityMatchesTheIosElementSequence() {
        assertVisibility(
            stage = StartupSplashStage.Waiting,
            icon = false,
            title = false,
            subtitle = false,
        )
        assertVisibility(
            stage = StartupSplashStage.IconVisible,
            icon = true,
            title = false,
            subtitle = false,
        )
        assertVisibility(
            stage = StartupSplashStage.TitleVisible,
            icon = true,
            title = true,
            subtitle = false,
        )
        assertVisibility(
            stage = StartupSplashStage.SubtitleVisible,
            icon = true,
            title = true,
            subtitle = true,
        )
        assertVisibility(
            stage = StartupSplashStage.IconHidden,
            icon = false,
            title = true,
            subtitle = true,
        )
        assertVisibility(
            stage = StartupSplashStage.TitleHidden,
            icon = false,
            title = false,
            subtitle = true,
        )
        assertVisibility(
            stage = StartupSplashStage.SubtitleHidden,
            icon = false,
            title = false,
            subtitle = false,
        )

        val complete = StartupSplashUiState(StartupSplashStage.Complete)
        assertFalse(complete.isShowingSplash)
    }

    @Test
    fun retainedViewModelStartsTheSequenceOnlyOnce() {
        var playCount = 0
        val immediatePlayer =
            StartupSplashPlayer { onStage ->
                playCount += 1
                onStage(StartupSplashStage.Complete)
            }
        val viewModel =
            StartupSplashViewModel(
                player = immediatePlayer,
                playbackScope = CoroutineScope(Dispatchers.Unconfined),
            )

        repeat(3) {
            assertEquals(StartupSplashStage.Complete, viewModel.uiState.value.stage)
        }
        assertEquals(1, playCount)
    }

    private fun assertVisibility(
        stage: StartupSplashStage,
        icon: Boolean,
        title: Boolean,
        subtitle: Boolean,
    ) {
        val state = StartupSplashUiState(stage)

        assertTrue(state.isShowingSplash)
        assertEquals(icon, state.isIconVisible)
        assertEquals(title, state.isTitleVisible)
        assertEquals(subtitle, state.isSubtitleVisible)
    }

    private data class TimedStage(
        val elapsedMillis: Long,
        val stage: StartupSplashStage,
    )
}
