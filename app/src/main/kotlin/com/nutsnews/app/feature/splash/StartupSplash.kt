package com.nutsnews.app.feature.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutsnews.app.R
import com.nutsnews.app.designsystem.nutsNewsHeading
import com.nutsnews.app.designsystem.nutsNewsPane

@Composable
internal fun StartupSplash(
    uiState: StartupSplashUiState,
    modifier: Modifier = Modifier,
) {
    val iconAlpha by splashElementAlpha(uiState.isIconVisible)
    val titleAlpha by splashElementAlpha(uiState.isTitleVisible)
    val subtitleAlpha by splashElementAlpha(uiState.isSubtitleVisible)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .nutsNewsPane("Starting NutsNews")
                .background(StartupSplashColors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.brand_splash),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(220.dp)
                        .alpha(iconAlpha)
                        .semantics {
                            contentDescription = "NutsNews chestnuts"
                        },
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "NutsNews",
                color = StartupSplashColors.title,
                style =
                    TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                    ),
                modifier =
                    Modifier
                        .alpha(titleAlpha)
                        .nutsNewsHeading(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Positive News, Simplified",
                color = StartupSplashColors.subtitle,
                style =
                    TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                    ),
                modifier = Modifier.alpha(subtitleAlpha),
            )
        }
    }
}

@Composable
private fun splashElementAlpha(isVisible: Boolean) =
    animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = StartupSplashTiming.ElementAnimationMillis,
                easing = FastOutSlowInEasing,
            ),
        label = "Startup splash element alpha",
    )

private object StartupSplashColors {
    val background =
        Brush.linearGradient(
            colors =
                listOf(
                    Color(0xFFFCC233),
                    Color(0xFFF28A0F),
                    Color(0xFFBF4A00),
                ),
        )
    val title = Color(0xFFFFEBAD)
    val subtitle = Color(0xFFFFD675)
}

@Preview(showSystemUi = true)
@Composable
private fun StartupSplashPreview() {
    StartupSplash(
        uiState = StartupSplashUiState(StartupSplashStage.SubtitleVisible),
    )
}
