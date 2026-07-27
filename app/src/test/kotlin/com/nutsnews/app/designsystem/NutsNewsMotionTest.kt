package com.nutsnews.app.designsystem

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class NutsNewsMotionTest {
    @Test
    fun frozenIosMotionContractKeepsAllThirteenParityGroups() {
        assertEquals(
            listOf(500L, 500L, 1_000L),
            listOf(
                NutsNewsMotion.SplashInitialDelayMillis,
                NutsNewsMotion.SplashStageDelayMillis,
                NutsNewsMotion.SplashVisibleHoldMillis,
            ),
        )
        assertEquals(
            listOf(350, 450),
            listOf(
                NutsNewsMotion.SplashElementMillis,
                NutsNewsMotion.SplashContentRevealMillis,
            ),
        )
        assertEquals(250, NutsNewsMotion.RouteFadeMillis)
        assertEquals(250, NutsNewsMotion.ThemeChangeMillis)
        assertEquals(1_000, NutsNewsMotion.ThemeGlowMillis)
        assertEquals(1_050L, NutsNewsMotion.ThemeGlowResetMillis)
        assertEquals(320, NutsNewsMotion.FeedEntranceMillis)
        assertEquals(0.22f, NutsNewsMotion.FeedEntranceStartAlpha)
        assertEquals(0.96f, NutsNewsMotion.FeedEntranceStartScale)
        assertEquals(18f, NutsNewsMotion.FeedEntranceOffsetDp)
        assertEquals(0.82f, NutsNewsMotion.DashboardSpringDamping)
        assertEquals(0.84f, NutsNewsMotion.MoodSpringDamping)
        assertEquals(1_000, NutsNewsMotion.ActionGlowMillis)
        assertEquals(160L, NutsNewsMotion.ActionOpenDelayMillis)
        assertEquals(1_050L, NutsNewsMotion.ActionGlowResetMillis)
        assertEquals(22f, NutsNewsMotion.ActionGlowRadiusDp)
        assertEquals(180, NutsNewsMotion.LikeGlowInMillis)
        assertEquals(1_000L, NutsNewsMotion.LikeActiveWindowMillis)
        assertEquals(350, NutsNewsMotion.LikeSettleMillis)
        assertEquals(250, NutsNewsMotion.UnlikeMillis)
        assertEquals(2_000, NutsNewsMotion.CelebrationTravelMillis)
        assertEquals(2_150L, NutsNewsMotion.CelebrationClearMillis)
        assertEquals(18, NutsNewsMotion.CelebrationParticleCount)
        assertEquals(200, NutsNewsMotion.StatusEnterMillis)
        assertEquals(1_800L, NutsNewsMotion.StatusHoldMillis)
        assertEquals(250, NutsNewsMotion.StatusExitMillis)
        assertEquals(900, NutsNewsMotion.ReflectionGlowMillis)
        assertEquals(160, NutsNewsMotion.ListenReadingTransitionMillis)
        assertEquals(180, NutsNewsMotion.ListenPausedTransitionMillis)
        assertEquals(180L, NutsNewsMotion.ListenAutoStartDelayMillis)
        assertEquals(900, NutsNewsMotion.ListenWaveformCycleMillis)
        assertEquals(28, NutsNewsMotion.ListenWaveformBarCount)
        assertEquals(1_000, NutsNewsMotion.ShareGlowMillis)
        assertEquals(800L, NutsNewsMotion.ShareCreatingResetMillis)
        assertEquals(0.85f, NutsNewsMotion.LikeHapticIntensity)
    }

    @Test
    fun stagedCleanupNeverEndsBeforeItsVisibleMotion() {
        assertTrue(
            NutsNewsMotion.ThemeGlowResetMillis >=
                NutsNewsMotion.ThemeGlowMillis,
        )
        assertTrue(
            NutsNewsMotion.ActionGlowResetMillis >=
                NutsNewsMotion.ActionGlowMillis,
        )
        assertTrue(
            NutsNewsMotion.LikeActiveWindowMillis >=
                NutsNewsMotion.LikeGlowInMillis,
        )
        assertTrue(
            NutsNewsMotion.CelebrationClearMillis >=
                NutsNewsMotion.CelebrationTravelMillis,
        )
    }
}
