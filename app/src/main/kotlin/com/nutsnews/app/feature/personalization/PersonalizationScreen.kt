package com.nutsnews.app.feature.personalization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.nutsnews.app.data.preferences.MoodPreference
import com.nutsnews.app.data.preferences.NutsNewsPersonalization
import com.nutsnews.app.data.preferences.TopicPreference
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsButtonGradient
import com.nutsnews.app.designsystem.nutsNewsPoliteAnnouncement

@Composable
fun PersonalizationScreen(
    uiState: PersonalizationUiState,
    mode: PersonalizationMode,
    onTopicToggled: (String) -> Unit,
    onMoodSelected: (String) -> Unit,
    onDailyGoalChanged: (Int) -> Unit,
    onReminderEnabledChanged: (Boolean) -> Unit,
    onReminderHourSelected: (Int) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("personalization_screen"),
    ) {
        NutsNewsAdaptivePane {
            Column(modifier = Modifier.fillMaxSize()) {
                PersonalizationTopBar(
                    mode = mode,
                    onClose = onClose,
                )

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = NutsNewsTheme.colors.accent)
                    }
                } else {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .padding(NutsNewsTheme.spacing.medium),
                        verticalArrangement =
                            Arrangement.spacedBy(NutsNewsTheme.spacing.large),
                    ) {
                        PersonalizationHero()
                        PersonalizationSection(
                            number = "1",
                            title = "Pick your favorite good news",
                        ) {
                            TopicChoices(
                                selectedTopicIds = uiState.selectedTopicIds,
                                onTopicToggled = onTopicToggled,
                            )
                        }
                        PersonalizationSection(
                            number = "2",
                            title = "Choose your default mood",
                        ) {
                            MoodChoices(
                                selectedMoodId = uiState.selectedMoodId,
                                onMoodSelected = onMoodSelected,
                            )
                        }
                        PersonalizationSection(
                            number = "3",
                            title = "Set a daily good-news goal",
                        ) {
                            DailyGoalControl(
                                dailyGoal = uiState.dailyGoal,
                                onDailyGoalChanged = onDailyGoalChanged,
                            )
                        }
                        PersonalizationSection(
                            number = "4",
                            title = "Optional daily reminder",
                        ) {
                            ReminderControls(
                                isEnabled = uiState.reminderEnabled,
                                selectedHour = uiState.reminderHour,
                                statusText = uiState.statusText,
                                onEnabledChanged = onReminderEnabledChanged,
                                onHourSelected = onReminderHourSelected,
                            )
                        }
                        SaveButton(
                            mode = mode,
                            isEnabled = uiState.canSave,
                            isSaving = uiState.isSaving,
                            onSave = onSave,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalizationTopBar(
    mode: PersonalizationMode,
    onClose: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = NutsNewsTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(64.dp))
        Text(
            text = if (mode == PersonalizationMode.Editor) "Personalize" else "Welcome",
            modifier =
                Modifier
                    .weight(1f)
                    .nutsNewsHeading(),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            textAlign = TextAlign.Center,
        )
        if (mode == PersonalizationMode.Editor) {
            Row(
                modifier =
                    Modifier
                        .width(64.dp)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(NutsNewsTheme.radii.small))
                        .clickable(
                            role = Role.Button,
                            onClick = onClose,
                        ).testTag("personalization_close"),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Close",
                    color = NutsNewsTheme.colors.accentText,
                    style = NutsNewsTheme.typography.callout,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(64.dp))
        }
    }
}

@Composable
private fun PersonalizationHero() {
    val colors = NutsNewsTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius),
        color = colors.cardBackgroundStrong,
        border =
            BorderStroke(
                NutsNewsTheme.borders.emphasized,
                colors.cardBorder,
            ),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.badgeBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = colors.accentHighlight,
                    )
                }
                Text(
                    text = "NutsNews",
                    color = colors.accentHighlight,
                    style =
                        NutsNewsTheme.typography.brandTitle.copy(
                            letterSpacing = 1.6.sp,
                        ),
                )
            }
            Text(
                text = "Build your good-news habit",
                color = colors.primaryText,
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 29.sp,
                        lineHeight = 35.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Text(
                text = "Choose what feels uplifting to you. NutsNews will use this to shape your For You picks, daily goal, and good-news reset.",
                color = colors.secondaryText,
                style =
                    NutsNewsTheme.typography.body.copy(
                        lineHeight = 25.sp,
                    ),
            )
        }
    }
}

@Composable
private fun PersonalizationSection(
    number: String,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(nutsNewsButtonGradient()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number,
                    color = NutsNewsTheme.colors.buttonText,
                    style = NutsNewsTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                color = NutsNewsTheme.colors.primaryText,
                style = NutsNewsTheme.typography.title3,
                fontWeight = FontWeight.Bold,
            )
        }
        content()
    }
}

@Composable
private fun TopicChoices(
    selectedTopicIds: Set<String>,
    onTopicToggled: (String) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        NutsNewsPersonalization.topics.chunked(2).forEach { rowTopics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            ) {
                rowTopics.forEach { topic ->
                    TopicChoice(
                        topic = topic,
                        isSelected = topic.id in selectedTopicIds,
                        onClick = { onTopicToggled(topic.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowTopics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TopicChoice(
    topic: TopicPreference,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NutsNewsTheme.colors
    val shape = RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius)
    Row(
        modifier =
            modifier
                .clip(shape)
                .background(
                    if (isSelected) {
                        nutsNewsButtonGradient()
                    } else {
                        Brush.linearGradient(
                            listOf(colors.badgeBackground, colors.badgeBackground),
                        )
                    },
                ).border(
                    width = NutsNewsTheme.borders.hairline,
                    color = if (isSelected) Color.Transparent else colors.cardBorder,
                    shape = shape,
                ).selectable(
                    selected = isSelected,
                    role = Role.Checkbox,
                    onClick = onClick,
                ).testTag("topic_${topic.id}")
                .padding(
                    horizontal = NutsNewsTheme.spacing.medium,
                    vertical = 13.dp,
                ),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = topicIcon(topic.id),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isSelected) colors.buttonText else colors.secondaryText,
        )
        Text(
            text = topic.title,
            color = if (isSelected) colors.buttonText else colors.secondaryText,
            style = NutsNewsTheme.typography.subheadline,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MoodChoices(
    selectedMoodId: String,
    onMoodSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        NutsNewsPersonalization.moods.forEach { mood ->
            MoodChoice(
                mood = mood,
                isSelected = mood.id == selectedMoodId,
                onClick = { onMoodSelected(mood.id) },
            )
        }
    }
}

@Composable
private fun MoodChoice(
    mood: MoodPreference,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = NutsNewsTheme.colors
    val shape = RoundedCornerShape(NutsNewsTheme.dimensions.cardCornerRadius)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (isSelected) {
                        colors.badgeBackground
                    } else {
                        colors.cardBackgroundStrong
                    },
                ).border(
                    width = 1.1.dp,
                    color =
                        if (isSelected) {
                            colors.accentHighlight.copy(alpha = 0.72f)
                        } else {
                            colors.cardBorder
                        },
                    shape = shape,
                ).selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).testTag("mood_${mood.id}")
                .padding(NutsNewsTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            nutsNewsButtonGradient()
                        } else {
                            Brush.linearGradient(
                                listOf(colors.badgeBackground, colors.badgeBackground),
                            )
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = moodIcon(mood.id),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) colors.buttonText else colors.accentHighlight,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
        ) {
            Text(
                text = mood.title,
                color = colors.primaryText,
                style = NutsNewsTheme.typography.headline,
            )
            Text(
                text = mood.subtitle,
                color = colors.secondaryText,
                style = NutsNewsTheme.typography.subheadline,
            )
        }
        Icon(
            imageVector =
                if (isSelected) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = if (isSelected) colors.accentHighlight else colors.mutedText,
        )
    }
}

@Composable
private fun DailyGoalControl(
    dailyGoal: Int,
    onDailyGoalChanged: (Int) -> Unit,
) {
    val colors = NutsNewsTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small)) {
        Text(
            text = "$dailyGoal stories per day",
            modifier = Modifier.testTag("goal_value"),
            color = colors.primaryText,
            style = NutsNewsTheme.typography.title3,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Small enough to feel easy. Consistent enough to become a habit.",
                modifier = Modifier.weight(1f),
                color = colors.secondaryText,
                style = NutsNewsTheme.typography.subheadline,
            )
            Spacer(modifier = Modifier.width(NutsNewsTheme.spacing.small))
            StepperButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Decrease daily goal",
                enabled = dailyGoal > 1,
                testTag = "goal_decrease",
                onClick = { onDailyGoalChanged(dailyGoal - 1) },
            )
            Spacer(modifier = Modifier.width(NutsNewsTheme.spacing.xs))
            StepperButton(
                icon = Icons.Filled.Add,
                contentDescription = "Increase daily goal",
                enabled = dailyGoal < 5,
                testTag = "goal_increase",
                onClick = { onDailyGoalChanged(dailyGoal + 1) },
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = NutsNewsTheme.colors
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .size(48.dp)
                .alpha(if (enabled) 1f else 0.45f)
                .testTag(testTag),
        enabled = enabled,
        shape = CircleShape,
        color = colors.badgeBackground,
        contentColor = colors.accent,
        border = BorderStroke(NutsNewsTheme.borders.hairline, colors.cardBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ReminderControls(
    isEnabled: Boolean,
    selectedHour: Int,
    statusText: String,
    onEnabledChanged: (Boolean) -> Unit,
    onHourSelected: (Int) -> Unit,
) {
    val colors = NutsNewsTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isEnabled,
                        role = Role.Switch,
                        onValueChange = onEnabledChanged,
                    )
                    .testTag("reminder_toggle"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            ) {
                Text(
                    text = "Daily good-news reset",
                    color = colors.primaryText,
                    style = NutsNewsTheme.typography.headline,
                )
                Text(
                    text = "A local Android notification brings you back to Today’s Picks.",
                    color = colors.secondaryText,
                    style = NutsNewsTheme.typography.subheadline,
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = null,
            )
        }

        if (isEnabled) {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xs),
            ) {
                Text(
                    text = "Reminder time",
                    color = colors.secondaryText,
                    style = NutsNewsTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                )
                ReminderTimeOptions.forEach { option ->
                    val selected = option.hour == selectedHour
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(NutsNewsTheme.radii.medium))
                                .background(
                                    if (selected) {
                                        colors.badgeBackground
                                    } else {
                                        colors.cardBackgroundStrong
                                    },
                                ).border(
                                    NutsNewsTheme.borders.hairline,
                                    if (selected) colors.accent else colors.cardBorder,
                                    RoundedCornerShape(NutsNewsTheme.radii.medium),
                                ).selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onHourSelected(option.hour) },
                                ).testTag("reminder_time_${option.hour}")
                                .padding(
                                    horizontal = NutsNewsTheme.spacing.medium,
                                    vertical = NutsNewsTheme.spacing.small,
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.title,
                            modifier = Modifier.weight(1f),
                            color = colors.primaryText,
                            style = NutsNewsTheme.typography.subheadline,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = option.displayTime,
                            color = if (selected) colors.accent else colors.secondaryText,
                            style = NutsNewsTheme.typography.subheadline,
                        )
                    }
                }
            }
        }

        if (statusText.isNotEmpty()) {
            Text(
                text = statusText,
                modifier =
                    Modifier
                        .nutsNewsPoliteAnnouncement()
                        .testTag("reminder_status"),
                color = colors.accentText,
                style = NutsNewsTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SaveButton(
    mode: PersonalizationMode,
    isEnabled: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    val colors = NutsNewsTheme.colors
    val shape = RoundedCornerShape(NutsNewsTheme.dimensions.controlCornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = NutsNewsTheme.shadows.buttonBlurRadius,
                    shape = shape,
                    ambientColor = colors.accentGlow,
                    spotColor = colors.accentGlow,
                ).clip(shape)
                .background(nutsNewsButtonGradient())
                .clickable(
                    enabled = isEnabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onSave,
                ).alpha(if (isEnabled) 1f else 0.55f)
                .testTag("personalization_save")
                .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.buttonText,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colors.buttonText,
            )
        }
        Spacer(modifier = Modifier.width(NutsNewsTheme.spacing.small))
        Text(
            text =
                if (mode == PersonalizationMode.Editor) {
                    "Save personalization"
                } else {
                    "Start my good-news reset"
                },
            color = colors.buttonText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun topicIcon(topicId: String): ImageVector =
    when (topicId) {
        "animals" -> Icons.Filled.Pets
        "science" -> Icons.Filled.Science
        "community" -> Icons.Filled.Groups
        "wellness" -> Icons.Filled.LocalFlorist
        "achievements" -> Icons.Filled.EmojiEvents
        "travel" -> Icons.Filled.AirplanemodeActive
        "culture" -> Icons.Filled.TheaterComedy
        "nature" -> Icons.Filled.Park
        else -> Icons.Filled.AutoAwesome
    }

private fun moodIcon(moodId: String): ImageVector =
    when (moodId) {
        "calm" -> Icons.Filled.SelfImprovement
        "hopeful" -> Icons.Filled.AutoAwesome
        "inspired" -> Icons.Filled.VolunteerActivism
        "curious" -> Icons.Filled.Lightbulb
        else -> Icons.Filled.AutoAwesome
    }

@Preview(name = "First run", widthDp = 393, heightDp = 852)
@Composable
private fun FirstRunPersonalizationPreview() {
    NutsNewsTheme(theme = NutsNewsAppTheme.Amber, updateSystemBars = false) {
        PersonalizationScreen(
            uiState =
                PersonalizationUiState(
                    isLoading = false,
                ),
            mode = PersonalizationMode.FirstRun,
            onTopicToggled = {},
            onMoodSelected = {},
            onDailyGoalChanged = {},
            onReminderEnabledChanged = {},
            onReminderHourSelected = {},
            onSave = {},
            onClose = {},
        )
    }
}
