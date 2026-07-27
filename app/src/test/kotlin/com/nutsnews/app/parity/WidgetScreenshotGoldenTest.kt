package com.nutsnews.app.parity

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.widget.NutsNewsWidgetSizeClass
import com.nutsnews.app.widget.NutsNewsWidgetSizes
import com.nutsnews.app.widget.WidgetArticle
import com.nutsnews.app.widget.WidgetArticleStatus
import com.nutsnews.app.widget.WidgetData
import com.nutsnews.app.widget.WidgetStats
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-rUS-w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetScreenshotGoldenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun smallLauncherWidgetMatchesApprovedCompactLayout() {
        captureWidget("widget_s24_small", NutsNewsWidgetSizes.Small)
    }

    @Test
    fun mediumLauncherWidgetMatchesApprovedSummaryLayout() {
        captureWidget("widget_s25_medium", NutsNewsWidgetSizes.Medium)
    }

    @Test
    fun largeLauncherWidgetMatchesApprovedStatsLayout() {
        captureWidget("widget_s26_large", NutsNewsWidgetSizes.Large)
    }

    private fun captureWidget(
        name: String,
        size: DpSize,
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NutsNewsTheme(
                theme = WidgetFixture.theme,
                updateSystemBars = false,
                reducedMotionOverride = true,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    WidgetParityPreview(
                        data = WidgetFixture,
                        size = size,
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        val bitmap = composeRule.runOnIdle { captureLargestWindow() }
        ScreenshotGolden.assertMatches(name, bitmap)
    }
}

@Composable
private fun WidgetParityPreview(
    data: WidgetData,
    size: DpSize,
) {
    val sizeClass = NutsNewsWidgetSizes.classify(size)
    val palette = NutsNewsTheme.colors
    val padding =
        when (sizeClass) {
            NutsNewsWidgetSizeClass.Small -> 14.dp
            NutsNewsWidgetSizeClass.Medium -> 16.dp
            NutsNewsWidgetSizeClass.Large -> 18.dp
        }
    val spacing =
        when (sizeClass) {
            NutsNewsWidgetSizeClass.Small -> 8.dp
            NutsNewsWidgetSizeClass.Medium -> 10.dp
            NutsNewsWidgetSizeClass.Large -> 12.dp
        }

    Column(
        modifier =
            Modifier
                .size(size)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.backgroundGradient.first())
                .border(1.dp, palette.cardBorder, RoundedCornerShape(26.dp))
                .padding(padding)
                .semantics { testTag = WidgetGoldenTag },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "◒",
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "↻",
                color = palette.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(spacing))
        Text(
            text = data.article.title,
            color = palette.primaryText,
            fontSize =
                when (sizeClass) {
                    NutsNewsWidgetSizeClass.Small -> 17.sp
                    NutsNewsWidgetSizeClass.Medium -> 18.sp
                    NutsNewsWidgetSizeClass.Large -> 22.sp
                },
            fontWeight = FontWeight.Bold,
            maxLines =
                when (sizeClass) {
                    NutsNewsWidgetSizeClass.Small -> 4
                    NutsNewsWidgetSizeClass.Medium -> 3
                    NutsNewsWidgetSizeClass.Large -> 4
                },
            overflow = TextOverflow.Ellipsis,
        )

        if (sizeClass != NutsNewsWidgetSizeClass.Small) {
            Spacer(modifier = Modifier.height(spacing))
            Text(
                text = data.article.summary,
                color = palette.secondaryText,
                fontSize =
                    if (sizeClass == NutsNewsWidgetSizeClass.Large) {
                        14.sp
                    } else {
                        12.sp
                    },
                fontWeight = FontWeight.Medium,
                maxLines = if (sizeClass == NutsNewsWidgetSizeClass.Large) 2 else 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (sizeClass == NutsNewsWidgetSizeClass.Large) {
            Spacer(modifier = Modifier.height(spacing))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.cardBackgroundStrong)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Today’s calm reset",
                        color = palette.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = data.stats.progressText,
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.cardBorder),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(data.stats.progressFraction)
                                .height(7.dp)
                                .background(palette.accent),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Streak", data.stats.currentStreak.toString())
                    StatPill("Stories", data.stats.totalStoryCount.toString())
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.article.mood,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(palette.badgeBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                color = palette.accentHighlight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = data.article.source,
                color = palette.secondaryText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(NutsNewsTheme.colors.accent)
                .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = NutsNewsTheme.colors.buttonText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            color = NutsNewsTheme.colors.buttonText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val WidgetGoldenTag = "widget_golden"

private val WidgetFixture =
    WidgetData(
        article =
            WidgetArticle(
                storyId = "community-garden",
                title = "Neighborhood garden blooms",
                summary = "Volunteers created a welcoming green space.",
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
