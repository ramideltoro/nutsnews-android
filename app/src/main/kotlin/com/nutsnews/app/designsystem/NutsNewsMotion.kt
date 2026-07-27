package com.nutsnews.app.designsystem

/**
 * Frozen motion values from `ramideltoro/nutsnews-ios@972dda3`.
 *
 * Keeping the M01–M13 contract in one place prevents small timing changes in one feature from
 * drifting away from the source app. Compose animations still honor Android's animator-duration
 * scale; callers replace decorative motion with a snap when [NutsNewsTheme.reducedMotion] is true.
 */
object NutsNewsMotion {
    // M01 — staged startup splash.
    const val SplashInitialDelayMillis = 500L
    const val SplashStageDelayMillis = 500L
    const val SplashVisibleHoldMillis = 1_000L
    const val SplashElementMillis = 350
    const val SplashContentRevealMillis = 450

    // M02/M03 — route and live-theme transitions.
    const val RouteFadeMillis = 250
    const val ThemeChangeMillis = 250
    const val ThemeGlowMillis = 1_000
    const val ThemeGlowResetMillis = 1_050L

    // M04 — feed-card scroll entrance.
    const val FeedEntranceMillis = 320
    const val FeedEntranceStartAlpha = 0.22f
    const val FeedEntranceStartScale = 0.96f
    const val FeedEntranceOffsetDp = 18f

    // M05/M06 — spring damping preserved from the Swift source.
    const val DashboardSpringDamping = 0.82f
    const val MoodSpringDamping = 0.84f

    // M07 — read, settings, source, and similar navigation actions.
    const val ActionGlowMillis = 1_000
    const val ActionOpenDelayMillis = 160L
    const val ActionGlowResetMillis = 1_050L
    const val ActionGlowRadiusDp = 22f

    // M08/M09 — like and unlike feedback.
    const val LikeGlowInMillis = 180
    const val LikeActiveWindowMillis = 1_000L
    const val LikeSettleMillis = 350
    const val UnlikeMillis = 250
    const val CelebrationTravelMillis = 2_000
    const val CelebrationClearMillis = 2_150L
    const val CelebrationParticleCount = 18

    // M10 — note/reflection confirmation.
    const val StatusEnterMillis = 200
    const val StatusHoldMillis = 1_800L
    const val StatusExitMillis = 250
    const val ReflectionGlowMillis = 900

    // M11 — Listen Mode.
    const val ListenReadingTransitionMillis = 160
    const val ListenPausedTransitionMillis = 180
    const val ListenAutoStartDelayMillis = 180L
    const val ListenWaveformCycleMillis = 900
    const val ListenWaveformBarCount = 28

    // M12 — positive share card.
    const val ShareGlowMillis = 1_000
    const val ShareCreatingResetMillis = 800L

    // M13 is behavior rather than visual timing: soft like impact at iOS intensity 0.85.
    const val LikeHapticIntensity = 0.85f
}
