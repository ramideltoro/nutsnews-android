package com.nutsnews.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsAdaptiveWindow
import com.nutsnews.app.designsystem.NutsNewsAppTheme
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsMinimumTouchTarget
import com.nutsnews.app.designsystem.nutsNewsPane

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val firstPage = remember(initialPage) {
        initialPage.coerceIn(OnboardingPages.indices)
    }
    var currentPageIndex by rememberSaveable(firstPage) {
        mutableIntStateOf(firstPage)
    }
    val currentPage = OnboardingPages[currentPageIndex]

    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(OnboardingScreenTag)
                .nutsNewsPane("Welcome to NutsNews"),
    ) {
        NutsNewsAdaptiveWindow {
            NutsNewsAdaptivePane {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                ) {
                    OnboardingTopBar(onSkip = onComplete)
                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag(OnboardingContentTag),
                        contentPadding =
                            PaddingValues(
                                start = NutsNewsTheme.spacing.medium,
                                top = NutsNewsTheme.spacing.small,
                                end = NutsNewsTheme.spacing.medium,
                                bottom = NutsNewsTheme.spacing.large,
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item(key = "progress-$currentPageIndex") {
                            OnboardingProgress(currentPageIndex)
                        }
                        item(key = "icon-${currentPage.id}") {
                            OnboardingHeroIcon(currentPage)
                        }
                        item(key = "copy-${currentPage.id}") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                            ) {
                                Text(
                                    text = currentPage.title,
                                    color = NutsNewsTheme.colors.primaryText,
                                    style = NutsNewsTheme.typography.title,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier
                                            .testTag("onboarding_heading_${currentPage.id}")
                                            .nutsNewsHeading(),
                                )
                                Text(
                                    text = currentPage.body,
                                    color = NutsNewsTheme.colors.secondaryText,
                                    style = NutsNewsTheme.typography.body,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        item(key = "highlights-${currentPage.id}") {
                            OnboardingHighlights(currentPage)
                        }
                    }
                    OnboardingNavigation(
                        currentPageIndex = currentPageIndex,
                        onBack = { currentPageIndex -= 1 },
                        onNext = { currentPageIndex += 1 },
                        onComplete = onComplete,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(onSkip: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = NutsNewsTheme.spacing.medium,
                    top = NutsNewsTheme.spacing.small,
                    end = NutsNewsTheme.spacing.small,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "NutsNews",
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = onSkip,
            modifier =
                Modifier
                    .nutsNewsMinimumTouchTarget()
                    .testTag(OnboardingSkipTag),
        ) {
            Text("Skip")
        }
    }
}

@Composable
private fun OnboardingProgress(currentPageIndex: Int) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Onboarding progress"
                    stateDescription =
                        "Step ${currentPageIndex + 1} of ${OnboardingPages.size}"
                }.testTag(OnboardingProgressTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
    ) {
        Text(
            text = "STEP ${currentPageIndex + 1} OF ${OnboardingPages.size}",
            color = NutsNewsTheme.colors.accentText,
            style = NutsNewsTheme.typography.label,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OnboardingPages.indices.forEach { index ->
                val isCurrent = index == currentPageIndex
                Box(
                    modifier =
                        Modifier
                            .size(if (isCurrent) 12.dp else 8.dp)
                            .background(
                                color =
                                    if (index <= currentPageIndex) {
                                        NutsNewsTheme.colors.accent
                                    } else {
                                        NutsNewsTheme.colors.cardBorder
                                    },
                                shape = CircleShape,
                            ),
                )
            }
        }
    }
}

@Composable
private fun OnboardingHeroIcon(page: OnboardingPage) {
    Surface(
        modifier = Modifier.size(112.dp),
        shape = CircleShape,
        color = NutsNewsTheme.colors.accentSoft,
        border = BorderStroke(NutsNewsTheme.borders.emphasized, NutsNewsTheme.colors.cardBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = NutsNewsTheme.colors.accentText,
                modifier = Modifier.size(54.dp),
            )
        }
    }
}

@Composable
private fun OnboardingHighlights(page: OnboardingPage) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NutsNewsTheme.radii.large),
        color = NutsNewsTheme.colors.cardBackground,
        border = BorderStroke(NutsNewsTheme.borders.hairline, NutsNewsTheme.colors.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(NutsNewsTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
        ) {
            page.highlights.forEach { highlight ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .size(7.dp)
                                .background(NutsNewsTheme.colors.accent, CircleShape),
                    )
                    Text(
                        text = highlight,
                        color = NutsNewsTheme.colors.primaryText,
                        style = NutsNewsTheme.typography.callout,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingNavigation(
    currentPageIndex: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
) {
    val isFinalPage = currentPageIndex == OnboardingPages.lastIndex
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = NutsNewsTheme.spacing.medium,
                    end = NutsNewsTheme.spacing.medium,
                    bottom = NutsNewsTheme.spacing.medium,
                ),
        horizontalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentPageIndex > 0) {
            OutlinedButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag(OnboardingBackTag),
            ) {
                Text("Back")
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Button(
            onClick = if (isFinalPage) onComplete else onNext,
            modifier =
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag(
                        if (isFinalPage) {
                            OnboardingGetStartedTag
                        } else {
                            OnboardingNextTag
                        },
                    ),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = NutsNewsTheme.colors.accent,
                    contentColor = NutsNewsTheme.colors.buttonText,
                ),
        ) {
            Text(if (isFinalPage) "Get Started" else "Next")
        }
    }
}

@Immutable
private data class OnboardingPage(
    val id: String,
    val title: String,
    val body: String,
    val highlights: List<String>,
    val icon: ImageVector,
)

private val OnboardingPages =
    listOf(
        OnboardingPage(
            id = "categories",
            title = "Browse what lifts you up",
            body =
                "Use the category row to move between the kinds of positive news " +
                    "you want to read today.",
            highlights =
                listOf(
                    "Explore Animals, Science, Community, Nature, and more.",
                    "Switch categories anytime without losing your place.",
                ),
            icon = Icons.Filled.Category,
        ),
        OnboardingPage(
            id = "favorites",
            title = "Keep good news close",
            body =
                "Tap the heart on a story to add it to Favorites, then find it " +
                    "again from the app menu.",
            highlights =
                listOf(
                    "A filled heart means the story is in your Favorites.",
                    "Remove a Favorite only after a clear confirmation.",
                ),
            icon = Icons.Filled.Favorite,
        ),
        OnboardingPage(
            id = "reading",
            title = "Read the full story",
            body =
                "Open any card for the NutsNews summary, positive takeaway, and " +
                    "a link to the original reporting.",
            highlights =
                listOf(
                    "Use reading tools to listen, reflect, or save a note.",
                    "Choose Read Original when you want the source article.",
                ),
            icon = Icons.AutoMirrored.Filled.Article,
        ),
    )

private const val OnboardingScreenTag = "onboarding_screen"
private const val OnboardingContentTag = "onboarding_content"
private const val OnboardingProgressTag = "onboarding_progress"
private const val OnboardingSkipTag = "onboarding_skip"
private const val OnboardingBackTag = "onboarding_back"
private const val OnboardingNextTag = "onboarding_next"
private const val OnboardingGetStartedTag = "onboarding_get_started"

@Preview(showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    NutsNewsTheme(
        theme = NutsNewsAppTheme.Amber,
        updateSystemBars = false,
    ) {
        OnboardingScreen(onComplete = {})
    }
}
