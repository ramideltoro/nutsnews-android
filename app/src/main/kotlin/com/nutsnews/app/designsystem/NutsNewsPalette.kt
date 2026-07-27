package com.nutsnews.app.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class NutsNewsPalette(
    val accent: Color,
    val accentRich: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val accentText: Color,
    val accentHighlight: Color,
    val accentGlow: Color,
    val cardBackground: Color,
    val cardBackgroundStrong: Color,
    val cardBorder: Color,
    val likedCardAccent: Color,
    val likedCardBorder: Color,
    val likedCardGlow: Color,
    val badgeBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val buttonText: Color,
    val backgroundGradient: List<Color>,
    val backgroundOverlay: Color,
    val buttonGradient: List<Color>,
    val categoryDots: List<Color>,
)

object NutsNewsPalettes {
    fun forTheme(theme: NutsNewsAppTheme): NutsNewsPalette =
        when (theme) {
            NutsNewsAppTheme.Amber -> amber
            NutsNewsAppTheme.Sakura -> sakura
            NutsNewsAppTheme.SaaS -> saas
            NutsNewsAppTheme.Foxy -> foxy
            NutsNewsAppTheme.Friday -> friday
            NutsNewsAppTheme.Bambi -> bambi
        }

    fun categoryDotColor(
        theme: NutsNewsAppTheme,
        index: Int,
        isSelected: Boolean,
    ): Color {
        val palette = forTheme(theme)
        val colors =
            if (isSelected) {
                listOf(
                    palette.buttonText,
                    palette.buttonText.copy(alpha = 0.78f),
                    palette.buttonText.copy(alpha = 0.58f),
                )
            } else {
                palette.categoryDots
            }
        val normalizedIndex = kotlin.math.abs(index.toLong())
        return colors[(normalizedIndex % colors.size).toInt()]
    }

    private fun color(hex: Long, opacity: Float = 1f): Color =
        Color(0xFF000000L or hex).copy(alpha = opacity)

    private val amber =
        NutsNewsPalette(
            accent = color(0xFACC15),
            accentRich = color(0xF59E0B),
            accentDeep = color(0xF97316),
            accentSoft = color(0xFDE68A),
            accentText = color(0xFDE68A),
            accentHighlight = color(0xFFFFFF),
            accentGlow = color(0xFACC15, 0.34f),
            cardBackground = color(0x121212, 0.88f),
            cardBackgroundStrong = color(0x171717),
            cardBorder = color(0xFACC15, 0.24f),
            likedCardAccent = color(0xF59E0B),
            likedCardBorder = color(0xFACC15, 0.46f),
            likedCardGlow = color(0xF59E0B, 0.16f),
            badgeBackground = color(0x451A03, 0.30f),
            primaryText = color(0xF5F5F4),
            secondaryText = color(0xD6D3D1),
            mutedText = color(0x8A817B),
            buttonText = color(0x111827),
            backgroundGradient =
                listOf(
                    color(0x0A0A0A),
                    color(0x17120A),
                    color(0x0A0A0A),
                ),
            backgroundOverlay = color(0xFACC15, 0.18f),
            buttonGradient = listOf(color(0xFACC15), color(0xF59E0B)),
            categoryDots =
                listOf(
                    color(0xFACC15),
                    color(0xF59E0B),
                    color(0xF97316),
                    color(0xFDE68A),
                ),
        )

    private val sakura =
        NutsNewsPalette(
            accent = color(0x7AA95C),
            accentRich = color(0x98C379),
            accentDeep = color(0x4F7F35),
            accentSoft = color(0xDCEBC9),
            accentText = color(0x4B762F),
            accentHighlight = color(0x3F2B34),
            accentGlow = color(0xF472B6, 0.28f),
            cardBackground = color(0xFFF7FB, 0.92f),
            cardBackgroundStrong = color(0xFFF7FB),
            cardBorder = color(0xDB7093, 0.30f),
            likedCardAccent = color(0x98C379),
            likedCardBorder = color(0x7AA95C, 0.50f),
            likedCardGlow = color(0x7AA95C, 0.18f),
            badgeBackground = color(0x7AA95C, 0.16f),
            primaryText = color(0x49363D),
            secondaryText = color(0x6F5B62),
            mutedText = color(0x856570),
            buttonText = color(0x17210F),
            backgroundGradient =
                listOf(
                    color(0xFDEFF4),
                    color(0xFFF7ED),
                    color(0xF4EAD2),
                ),
            backgroundOverlay = color(0xFDE2E7, 0.92f),
            buttonGradient = listOf(color(0x7AA95C), color(0x98C379)),
            categoryDots =
                listOf(
                    color(0x7AA95C),
                    color(0x98C379),
                    color(0x4F7F35),
                    color(0xDB7093),
                ),
        )

    private val saas =
        NutsNewsPalette(
            accent = color(0x3B82F6),
            accentRich = color(0x60A5FA),
            accentDeep = color(0x2563EB),
            accentSoft = color(0xBFDBFE),
            accentText = color(0x60A5FA),
            accentHighlight = color(0xFFFFFF),
            accentGlow = color(0x3B82F6, 0.36f),
            cardBackground = color(0x1E1E1E, 0.90f),
            cardBackgroundStrong = color(0x1E1E1E),
            cardBorder = color(0x3B82F6, 0.30f),
            likedCardAccent = color(0x60A5FA),
            likedCardBorder = color(0x60A5FA, 0.54f),
            likedCardGlow = color(0x3B82F6, 0.16f),
            badgeBackground = color(0x3B82F6, 0.13f),
            primaryText = color(0xE0E0E0),
            secondaryText = color(0xB7BEC8),
            mutedText = color(0x7E8794),
            buttonText = color(0xF8FAFC),
            backgroundGradient =
                listOf(
                    color(0x121212),
                    color(0x181818),
                    color(0x101010),
                ),
            backgroundOverlay = color(0x3B82F6, 0.20f),
            buttonGradient = listOf(color(0x2563EB), color(0x1D4ED8)),
            categoryDots =
                listOf(
                    color(0x3B82F6),
                    color(0x60A5FA),
                    color(0x2563EB),
                    color(0xBFDBFE),
                ),
        )

    private val foxy =
        NutsNewsPalette(
            accent = color(0x0077B6),
            accentRich = color(0xE76F51),
            accentDeep = color(0x005F73),
            accentSoft = color(0xCCEFFF),
            accentText = color(0x005F73),
            accentHighlight = color(0x3F2415),
            accentGlow = color(0x2A9DF4, 0.30f),
            cardBackground = color(0xFFF8E5, 0.94f),
            cardBackgroundStrong = color(0xFFF6DF),
            cardBorder = color(0x0077B6, 0.26f),
            likedCardAccent = color(0xE76F51),
            likedCardBorder = color(0xE76F51, 0.46f),
            likedCardGlow = color(0xE76F51, 0.16f),
            badgeBackground = color(0x2A9DF4, 0.14f),
            primaryText = color(0x4F3424),
            secondaryText = color(0x75513D),
            mutedText = color(0x91654D),
            buttonText = color(0xFFFAF0),
            backgroundGradient =
                listOf(
                    color(0xFFF2D0),
                    color(0xFFE4B0),
                    color(0xD8F1E4),
                ),
            backgroundOverlay = color(0xF6C453, 0.76f),
            buttonGradient = listOf(color(0x0077B6), color(0xB64129)),
            categoryDots =
                listOf(
                    color(0x0077B6),
                    color(0xE76F51),
                    color(0x2A9DF4),
                    color(0x2F9E44),
                ),
        )

    private val friday =
        NutsNewsPalette(
            accent = color(0x7C3AED),
            accentRich = color(0xA78BFA),
            accentDeep = color(0x5B21B6),
            accentSoft = color(0xDDD6FE),
            accentText = color(0xDDD6FE),
            accentHighlight = color(0xF8FAFC),
            accentGlow = color(0x7C3AED, 0.42f),
            cardBackground = color(0x1E293B, 0.90f),
            cardBackgroundStrong = color(0x1E293B),
            cardBorder = color(0x7C3AED, 0.34f),
            likedCardAccent = color(0xA78BFA),
            likedCardBorder = color(0xA78BFA, 0.56f),
            likedCardGlow = color(0x7C3AED, 0.18f),
            badgeBackground = color(0x7C3AED, 0.14f),
            primaryText = color(0xCBD5E1),
            secondaryText = color(0x94A3B8),
            mutedText = color(0x8493A7),
            buttonText = color(0xF8FAFC),
            backgroundGradient =
                listOf(
                    color(0x0F172A),
                    color(0x111827),
                    color(0x0B1120),
                ),
            backgroundOverlay = color(0x7C3AED, 0.22f),
            buttonGradient =
                listOf(
                    color(0x6D28D9),
                    color(0x5B21B6),
                ),
            categoryDots =
                listOf(
                    color(0x7C3AED),
                    color(0xA78BFA),
                    color(0x5B21B6),
                    color(0xDDD6FE),
                ),
        )

    private val bambi =
        NutsNewsPalette(
            accent = color(0xFACC15),
            accentRich = color(0xFDE047),
            accentDeep = color(0xEAB308),
            accentSoft = color(0xFEF08A),
            accentText = color(0xFEF08A),
            accentHighlight = color(0xF8FAFC),
            accentGlow = color(0xFACC15, 0.32f),
            cardBackground = color(0x2C362F, 0.91f),
            cardBackgroundStrong = color(0x2C362F),
            cardBorder = color(0xFACC15, 0.30f),
            likedCardAccent = color(0xFDE047),
            likedCardBorder = color(0xFDE047, 0.54f),
            likedCardGlow = color(0xFACC15, 0.14f),
            badgeBackground = color(0xFACC15, 0.12f),
            primaryText = color(0xE5E7EB),
            secondaryText = color(0xCBD5C9),
            mutedText = color(0x98A298),
            buttonText = color(0x111827),
            backgroundGradient =
                listOf(
                    color(0x1A211B),
                    color(0x20281F),
                    color(0x151A16),
                ),
            backgroundOverlay = color(0xFACC15, 0.18f),
            buttonGradient = listOf(color(0xFACC15), color(0xFDE047)),
            categoryDots =
                listOf(
                    color(0xFACC15),
                    color(0xFDE047),
                    color(0xEAB308),
                    color(0x22C55E),
                ),
        )
}
