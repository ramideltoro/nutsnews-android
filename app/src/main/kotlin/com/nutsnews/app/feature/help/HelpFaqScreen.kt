package com.nutsnews.app.feature.help

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsButtonGradient

@Composable
fun HelpFaqScreen(
    onClose: () -> Unit,
    onOpenTodayPicks: () -> Unit,
    onOpenGoodMood: () -> Unit,
    onOpenReadingStats: () -> Unit,
    onOpenSavedStories: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onOpenStoryFeatures: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(HelpScreenTag),
    ) {
        NutsNewsAdaptivePane {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                HelpTopBar(onClose = onClose)
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag(HelpListTag),
                    contentPadding =
                        PaddingValues(
                            start = NutsNewsTheme.spacing.medium,
                            top = NutsNewsTheme.spacing.medium,
                            end = NutsNewsTheme.spacing.medium,
                            bottom = NutsNewsTheme.spacing.xl,
                        ),
                    verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                ) {
                item(key = "hero") {
                    HelpHeroCard()
                }
                item(key = "start-here") {
                    HelpFeatureSection(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Start here",
                        subtitle =
                            "Use these features first to shape your daily feed. " +
                                "You can find these options later in the menu.",
                    ) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                        ) {
                            HelpActionButton(
                                title = "Personalize NutsNews",
                                icon = Icons.Filled.Tune,
                                testTag = "help_action_personalize",
                                onClick = onOpenPersonalization,
                            )
                            HelpActionButton(
                                title = "Today’s Picks",
                                icon = Icons.Filled.Newspaper,
                                testTag = "help_action_today_picks",
                                onClick = onOpenTodayPicks,
                            )
                            HelpActionButton(
                                title = "Good Mood",
                                icon = Icons.Filled.AutoAwesome,
                                testTag = "help_action_good_mood",
                                onClick = onOpenGoodMood,
                            )
                        }
                    }
                }
                item(key = "story-tools") {
                    HelpFeatureSection(
                        icon = Icons.AutoMirrored.Filled.LibraryBooks,
                        title = "Story tools",
                        subtitle = "Open a story to use the native reading tools.",
                    ) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                        ) {
                            StoryToolChecklist.forEach { item ->
                                HelpChecklistRow(item)
                            }
                            HelpActionButton(
                                title = "Open a story",
                                icon = Icons.Filled.Description,
                                testTag = "help_action_story",
                                accessibilityLabel = "Open a story",
                                onClick = onOpenStoryFeatures,
                            )
                        }
                    }
                }
                item(key = "voice-quality") {
                    HelpFeatureSection(
                        icon = Icons.Filled.GraphicEq,
                        title = "Better Listen Mode voice",
                        subtitle =
                            "For the smoothest story listening, install a " +
                                "high-quality English voice on your Android device.",
                    ) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                        ) {
                            VoiceChecklist.forEach { item ->
                                HelpChecklistRow(item)
                            }
                        }
                    }
                }
                item(key = "daily-habit") {
                    HelpFeatureSection(
                        icon = Icons.Filled.Favorite,
                        title = "Build a small habit",
                        subtitle = "Use NutsNews like a daily positive reset.",
                    ) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                        ) {
                            HelpActionButton(
                                title = "Reading Stats",
                                icon = Icons.Filled.BarChart,
                                testTag = "help_action_reading_stats",
                                onClick = onOpenReadingStats,
                            )
                            HelpActionButton(
                                title = "Saved Stories",
                                icon = Icons.Filled.Bookmark,
                                testTag = "help_action_saved",
                                onClick = onOpenSavedStories,
                            )
                            HelpActionButton(
                                title = "Archive Search",
                                icon = Icons.Filled.Search,
                                testTag = "help_action_search",
                                onClick = onOpenSearch,
                            )
                        }
                    }
                }
                item(key = "android-features") {
                    HelpFeatureSection(
                        icon = Icons.Filled.Android,
                        title = "Android features",
                        subtitle = "NutsNews also works outside the main feed.",
                    ) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                        ) {
                            AndroidFeatureChecklist.forEach { item ->
                                HelpChecklistRow(item)
                            }
                        }
                    }
                }
                item(key = "faq") {
                    HelpFeatureSection(
                        icon = Icons.Filled.QuestionAnswer,
                        title = "FAQ",
                        subtitle = "Common questions about NutsNews.",
                    ) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                        ) {
                            FaqItems.forEach { item ->
                                HelpFaqRow(item)
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun HelpTopBar(onClose: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = NutsNewsTheme.spacing.small),
    ) {
        Text(
            text = "Help & F.A.Q.",
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .nutsNewsHeading(),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp)
                    .testTag("help_close"),
            onClick = onClose,
            shape = CircleShape,
            color = NutsNewsTheme.colors.badgeBackground,
            border =
                BorderStroke(
                    NutsNewsTheme.borders.hairline,
                    NutsNewsTheme.colors.cardBorder,
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close help",
                    modifier = Modifier.size(18.dp),
                    tint = NutsNewsTheme.colors.accentHighlight,
                )
            }
        }
    }
}

@Composable
private fun HelpHeroCard() {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape =
                        RoundedCornerShape(
                            NutsNewsTheme.dimensions.cardCornerRadius,
                        ),
                    ambientColor = NutsNewsTheme.colors.accentGlow,
                    spotColor = NutsNewsTheme.colors.accentGlow,
                ).testTag("help_hero"),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = NutsNewsTheme.colors.cardBackgroundStrong,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Row(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = NutsNewsTheme.colors.badgeBackground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.QuestionMark,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = NutsNewsTheme.colors.accent,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
            ) {
                Text(
                    text = "How to use NutsNews",
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.title,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        "A simple guide to the native tools that make NutsNews " +
                            "feel calm, personal, and easy to return to.",
                    color = NutsNewsTheme.colors.secondaryText,
                    style = NutsNewsTheme.typography.subheadline,
                )
            }
        }
    }
}

@Composable
private fun HelpFeatureSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("help_section_${title.toTestTag()}"),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = NutsNewsTheme.colors.cardBackground,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = NutsNewsTheme.colors.badgeBackground,
                    border =
                        BorderStroke(
                            NutsNewsTheme.borders.hairline,
                            NutsNewsTheme.colors.cardBorder,
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = NutsNewsTheme.colors.accentHighlight,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.nutsNewsHeading(),
                        color = NutsNewsTheme.colors.primaryText,
                        style = NutsNewsTheme.typography.headline,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        color = NutsNewsTheme.colors.secondaryText,
                        style = NutsNewsTheme.typography.subheadline,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun HelpChecklistRow(item: HelpChecklistItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier =
                Modifier
                    .padding(top = NutsNewsTheme.spacing.xxs)
                    .size(16.dp),
            tint = NutsNewsTheme.colors.accent,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = item.title,
                color = NutsNewsTheme.colors.primaryText,
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.subtitle,
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun HelpFaqRow(item: HelpFaqItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NutsNewsTheme.radii.medium),
        color = NutsNewsTheme.colors.badgeBackground,
        border =
            BorderStroke(
                NutsNewsTheme.borders.hairline,
                NutsNewsTheme.colors.cardBorder,
            ),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = item.question,
                color = NutsNewsTheme.colors.primaryText,
                style = NutsNewsTheme.typography.subheadline,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.answer,
                color = NutsNewsTheme.colors.secondaryText,
                style = NutsNewsTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun HelpActionButton(
    title: String,
    icon: ImageVector,
    testTag: String,
    accessibilityLabel: String = "Open $title",
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(NutsNewsTheme.radii.medium)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(testTag)
                .semantics {
                    contentDescription = accessibilityLabel
                }.clip(shape)
                .background(nutsNewsButtonGradient())
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).padding(
                    horizontal = NutsNewsTheme.spacing.medium,
                    vertical = NutsNewsTheme.spacing.small,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = NutsNewsTheme.colors.buttonText,
        )
        Spacer(Modifier.width(NutsNewsTheme.spacing.small))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = NutsNewsTheme.colors.buttonText,
            style = NutsNewsTheme.typography.subheadline,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = NutsNewsTheme.colors.buttonText,
        )
    }
}

private data class HelpChecklistItem(
    val title: String,
    val subtitle: String,
)

private data class HelpFaqItem(
    val question: String,
    val answer: String,
)

private val StoryToolChecklist =
    listOf(
        HelpChecklistItem(
            title = "NutsNews Brief",
            subtitle = "A quick feel-good summary and takeaway.",
        ),
        HelpChecklistItem(
            title = "Listen Mode",
            subtitle =
                "Have Android read the brief aloud. For the best sound, install a " +
                    "high-quality English Text-to-Speech voice in Android Settings.",
        ),
        HelpChecklistItem(
            title = "Daily Reflection",
            subtitle =
                "Save a private reaction like “Made me smile” or “Gave me hope.”",
        ),
        HelpChecklistItem(
            title = "Good News Share Card",
            subtitle =
                "Create a branded image card to share through the Android Sharesheet.",
        ),
    )

private val VoiceChecklist =
    listOf(
        HelpChecklistItem(
            title = "Install a high-quality voice",
            subtitle =
                "Open Android Settings and search for “Text-to-speech output.” " +
                    "Choose your preferred engine, then install an English voice.",
        ),
        HelpChecklistItem(
            title = "Use it in NutsNews",
            subtitle =
                "After the voice finishes downloading, reopen NutsNews, open any " +
                    "story, and tap Play. Listen Mode will automatically use the " +
                    "best installed English voice available.",
        ),
    )

private val AndroidFeatureChecklist =
    listOf(
        HelpChecklistItem(
            title = "Home Screen Widget",
            subtitle =
                "Add NutsNews Daily from the Android widget picker for a quick " +
                    "positive headline.",
        ),
        HelpChecklistItem(
            title = "Local reminders",
            subtitle =
                "Use onboarding or personalization to set a gentle good-news reminder.",
        ),
        HelpChecklistItem(
            title = "Native sharing",
            subtitle =
                "Share positive story cards through the built-in Android Sharesheet.",
        ),
        HelpChecklistItem(
            title = "Private on-device choices",
            subtitle =
                "Your saved stories, reflections, stats, theme, and preferences " +
                    "stay on your device.",
        ),
    )

private val FaqItems =
    listOf(
        HelpFaqItem(
            question = "What is NutsNews for?",
            answer =
                "NutsNews is for quick, calm breaks with positive stories and " +
                    "simple tools that help you save, reflect, and return to good news.",
        ),
        HelpFaqItem(
            question = "How do I change what I see?",
            answer =
                "Open Personalize to adjust topics, mood, reading goal, and " +
                    "reminder preferences.",
        ),
        HelpFaqItem(
            question = "How do I save something for later?",
            answer =
                "Open any story and use Save, or use Daily Reflection to mark " +
                    "why a story mattered to you.",
        ),
        HelpFaqItem(
            question = "How do I add the widget?",
            answer =
                "Long press the Android Home Screen, tap Widgets, find NutsNews, " +
                    "then add NutsNews Daily.",
        ),
        HelpFaqItem(
            question = "Can I listen instead of read?",
            answer =
                "Yes. Open a story and tap Play to hear the NutsNews Brief aloud. " +
                    "For the best listening experience, open Android Settings, " +
                    "search for “Text-to-speech output,” and install a high-quality " +
                    "English voice.",
        ),
    )

private const val HelpScreenTag = "help_screen"
private const val HelpListTag = "help_list"

private fun String.toTestTag(): String =
    lowercase().map { character ->
        if (character.isLetterOrDigit()) character else '_'
    }.joinToString("")

@Preview(showBackground = true)
@Composable
private fun HelpFaqPreview() {
    NutsNewsTheme(updateSystemBars = false) {
        HelpFaqScreen(
            onClose = {},
            onOpenTodayPicks = {},
            onOpenGoodMood = {},
            onOpenReadingStats = {},
            onOpenSavedStories = {},
            onOpenSearch = {},
            onOpenPersonalization = {},
            onOpenStoryFeatures = {},
        )
    }
}
