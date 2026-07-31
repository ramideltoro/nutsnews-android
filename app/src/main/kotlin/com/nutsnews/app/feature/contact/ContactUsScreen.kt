package com.nutsnews.app.feature.contact

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutsnews.app.designsystem.NutsNewsAdaptivePane
import com.nutsnews.app.designsystem.NutsNewsBackground
import com.nutsnews.app.designsystem.NutsNewsTheme
import com.nutsnews.app.designsystem.nutsNewsHeading

object NutsNewsContactDetails {
    const val EMAIL = "rami.deltoro@nutsnews.com"
    const val CONTACT_URL = "https://www.nutsnews.com/contact"
}

@Composable
fun ContactUsScreen(
    onEmail: () -> Unit,
    onOpenContactPage: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutsNewsBackground(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("contact_screen"),
    ) {
        NutsNewsAdaptivePane {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                ContactTopBar(
                    onBack = onBack,
                    onGoHome = onGoHome,
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(NutsNewsTheme.spacing.medium)
                            .testTag("contact_content"),
                    verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.medium),
                ) {
                    Text(
                        text = "Contact NutsNews",
                        modifier = Modifier.nutsNewsHeading(),
                        color = NutsNewsTheme.colors.primaryText,
                        style = NutsNewsTheme.typography.title,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text =
                            "Questions, corrections, feedback, or support requests are welcome. " +
                                "Contact the NutsNews developer directly using the details below.",
                        color = NutsNewsTheme.colors.secondaryText,
                        style = NutsNewsTheme.typography.body,
                    )
                    ContactCard(
                        icon = Icons.Filled.Email,
                        label = "Email",
                        value = NutsNewsContactDetails.EMAIL,
                        valueTag = "contact_email_address",
                    )
                    OutlinedButton(
                        onClick = onEmail,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("contact_email_action"),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Email NutsNews")
                    }
                    ContactCard(
                        icon = Icons.Filled.Language,
                        label = "Contact page",
                        value = NutsNewsContactDetails.CONTACT_URL,
                        valueTag = "contact_website_address",
                    )
                    OutlinedButton(
                        onClick = onOpenContactPage,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("contact_website_action"),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Open contact page")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactTopBar(
    onBack: () -> Unit,
    onGoHome: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = NutsNewsTheme.spacing.small),
    ) {
        HeaderButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to settings",
            testTag = "contact_back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = "Contact us",
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .nutsNewsHeading(),
            color = NutsNewsTheme.colors.primaryText,
            style = NutsNewsTheme.typography.headline,
            fontWeight = FontWeight.Bold,
        )
        HeaderButton(
            icon = Icons.Filled.Home,
            contentDescription = "Go home",
            testTag = "contact_home",
            onClick = onGoHome,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun HeaderButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .size(48.dp)
                .testTag(testTag),
        onClick = onClick,
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
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
                tint = NutsNewsTheme.colors.accentHighlight,
            )
        }
    }
}

@Composable
private fun ContactCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueTag: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = NutsNewsTheme.colors.accentHighlight,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(NutsNewsTheme.spacing.xxs),
            ) {
                Text(
                    text = label,
                    color = NutsNewsTheme.colors.primaryText,
                    style = NutsNewsTheme.typography.headline,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = value,
                    modifier = Modifier.testTag(valueTag),
                    color = NutsNewsTheme.colors.secondaryText,
                    style = NutsNewsTheme.typography.body,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactUsPreview() {
    NutsNewsTheme(updateSystemBars = false) {
        ContactUsScreen(
            onEmail = {},
            onOpenContactPage = {},
            onBack = {},
            onGoHome = {},
        )
    }
}
