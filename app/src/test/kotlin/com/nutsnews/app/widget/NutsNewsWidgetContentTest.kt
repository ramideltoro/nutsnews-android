package com.nutsnews.app.widget

import android.app.Application
import androidx.glance.appwidget.testing.unit.assertHasRunCallbackClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.assertHasStartActivityClickAction
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.nutsnews.app.MainActivity
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsPalettes
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NutsNewsWidgetContentTest {
    @Test
    fun smallWidgetShowsCompactStoryWithLaunchAndRefreshActions() =
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            provideComposable {
                NutsNewsWidgetContent(
                    data = CurrentData,
                    size = NutsNewsWidgetSizes.Small,
                )
            }

            onNode(hasTestTag("widget_title")).assertExists()
            onNode(hasText(CurrentData.article.title)).assertExists()
            onNode(hasTestTag("widget_mood")).assertExists()
            onNode(hasTestTag("widget_summary")).assertDoesNotExist()
            onNode(hasTestTag("widget_stats")).assertDoesNotExist()
            onNode(hasTestTag("nutsnews_widget_root"))
                .assertHasStartActivityClickAction<MainActivity>()
            onNode(hasTestTag("widget_refresh"))
                .assertHasRunCallbackClickAction<RefreshNutsNewsWidgetAction>()
        }

    @Test
    fun mediumWidgetAddsSummaryWithoutLargeStatistics() =
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            provideComposable {
                NutsNewsWidgetContent(
                    data = CurrentData,
                    size = NutsNewsWidgetSizes.Medium,
                )
            }

            onNode(hasTestTag("widget_summary")).assertExists()
            onNode(hasText(CurrentData.article.summary)).assertExists()
            onNode(hasTestTag("widget_stats")).assertDoesNotExist()
        }

    @Test
    fun largeWidgetShowsOptionalProgressStreakAndStoryStatistics() =
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            provideComposable {
                NutsNewsWidgetContent(
                    data = CurrentData,
                    size = NutsNewsWidgetSizes.Large,
                )
            }

            onNode(hasTestTag("widget_summary")).assertExists()
            onNode(hasTestTag("widget_stats")).assertExists()
            onNode(hasTestTag("widget_progress")).assertExists()
            onNode(hasTestTag("widget_progress_text"))
                .assertHasText(CurrentData.stats.progressText)
            onNode(hasText("Streak")).assertExists()
            onNode(hasText("Stories")).assertExists()
        }

    @Test
    fun largeWidgetRespectsTheStatsVisibilityPreference() =
        runGlanceAppWidgetUnitTest {
            setContext(ApplicationProvider.getApplicationContext())
            provideComposable {
                NutsNewsWidgetContent(
                    data = CurrentData.copy(showStatsOnLargeWidget = false),
                    size = NutsNewsWidgetSizes.Large,
                )
            }

            onNode(hasTestTag("widget_summary")).assertExists()
            onNode(hasTestTag("widget_stats")).assertDoesNotExist()
        }

    @Test
    fun placeholderAndRuntimeFallbackRemainUsefulAtEverySize() {
        listOf(
            WidgetData.Placeholder to NutsNewsWidgetSizes.Small,
            WidgetData.Placeholder to NutsNewsWidgetSizes.Medium,
            fallbackData() to NutsNewsWidgetSizes.Large,
        ).forEach { (data, size) ->
            runGlanceAppWidgetUnitTest {
                setContext(ApplicationProvider.getApplicationContext())
                provideComposable {
                    NutsNewsWidgetContent(
                        data = data,
                        size = size,
                    )
                }

                onNode(hasText(data.article.title)).assertExists()
                onNode(hasTestTag("widget_mood")).assertExists()
            }
        }
    }

    @Test
    fun everyAppThemeUsesItsBrandedPaletteAndSymbol() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val symbols =
            NutsNewsAppTheme.entries.map { theme ->
                val widgetPalette = NutsNewsWidgetPalettes.forTheme(theme)
                val appPalette = NutsNewsPalettes.forTheme(theme)

                assertEquals(
                    appPalette.backgroundGradient.first(),
                    widgetPalette.background.getColor(context),
                )
                assertEquals(
                    appPalette.cardBackgroundStrong,
                    widgetPalette.card.getColor(context),
                )
                assertEquals(
                    appPalette.primaryText,
                    widgetPalette.primaryText.getColor(context),
                )
                assertEquals(
                    appPalette.accent,
                    widgetPalette.accent.getColor(context),
                )

                runGlanceAppWidgetUnitTest {
                    setContext(ApplicationProvider.getApplicationContext())
                    provideComposable {
                        NutsNewsWidgetContent(
                            data = CurrentData.copy(theme = theme),
                            size = NutsNewsWidgetSizes.Small,
                        )
                    }

                    onNode(hasText(widgetPalette.symbol)).assertExists()
                    onNode(hasText(CurrentData.article.title)).assertExists()
                }

                widgetPalette.symbol
            }

        assertEquals(NutsNewsAppTheme.entries.size, symbols.toSet().size)
    }

    @Test
    fun supportedSizesAndLauncherResizeBoundsClassifyResponsively() {
        assertEquals(
            setOf(
                NutsNewsWidgetSizes.Small,
                NutsNewsWidgetSizes.Medium,
                NutsNewsWidgetSizes.Large,
            ),
            NutsNewsWidgetSizes.Supported,
        )
        assertEquals(
            NutsNewsWidgetSizeClass.Small,
            NutsNewsWidgetSizes.classify(NutsNewsWidgetSizes.Small),
        )
        assertEquals(
            NutsNewsWidgetSizeClass.Medium,
            NutsNewsWidgetSizes.classify(NutsNewsWidgetSizes.Medium),
        )
        assertEquals(
            NutsNewsWidgetSizeClass.Large,
            NutsNewsWidgetSizes.classify(NutsNewsWidgetSizes.Large),
        )
    }

    private fun fallbackData(): WidgetData =
        CurrentData.copy(
            article = WidgetArticle.Fallback,
            articleStatus = WidgetArticleStatus.Fallback,
        )

    private companion object {
        val CurrentData =
            WidgetData(
                article =
                    WidgetArticle(
                        storyId = "test-story",
                        title = "Neighbors turn an empty lot into a community garden",
                        summary =
                            "Volunteers created a welcoming green space for everyone.",
                        source = "NutsNews",
                        mood = "Community",
                    ),
                articleStatus = WidgetArticleStatus.Current,
                theme = NutsNewsAppTheme.Amber,
                stats =
                    WidgetStats(
                        todayCount = 2,
                        dailyGoal = 3,
                        currentStreak = 5,
                        totalStoryCount = 24,
                    ),
                showStatsOnLargeWidget = true,
                refreshedAt = Instant.parse("2026-07-26T12:00:00Z"),
            )
    }
}
