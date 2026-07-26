package com.nutsnews.app.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.Test

class NutsNewsPaletteTest {
    @Test
    fun everyPaletteMatchesTheFrozenIosTokenTable() {
        assertEquals(
            expectedPaletteSnapshot.trimIndent(),
            NutsNewsAppTheme.entries.joinToString(separator = "\n") { theme ->
                NutsNewsPalettes.forTheme(theme).snapshot(theme)
            },
        )
    }

    @Test
    fun categoryDotsCycleDeterministicallyForPositiveAndNegativeIndices() {
        NutsNewsAppTheme.entries.forEach { theme ->
            val palette = NutsNewsPalettes.forTheme(theme)

            assertEquals(palette.categoryDots[0], NutsNewsPalettes.categoryDotColor(theme, 0, false))
            assertEquals(palette.categoryDots[1], NutsNewsPalettes.categoryDotColor(theme, 5, false))
            assertEquals(palette.categoryDots[1], NutsNewsPalettes.categoryDotColor(theme, -1, false))
            assertEquals(
                palette.buttonText.copy(alpha = 0.78f),
                NutsNewsPalettes.categoryDotColor(theme, -1, true),
            )
        }
    }

    private fun NutsNewsPalette.snapshot(theme: NutsNewsAppTheme): String =
        buildString {
            append(theme.rawValue)
            append("|accent=")
            append(accent.argb())
            append(",")
            append(accentRich.argb())
            append(",")
            append(accentDeep.argb())
            append(",")
            append(accentSoft.argb())
            append(",")
            append(accentHighlight.argb())
            append(",")
            append(accentGlow.argb())
            append("|surface=")
            append(cardBackground.argb())
            append(",")
            append(cardBackgroundStrong.argb())
            append(",")
            append(cardBorder.argb())
            append(",")
            append(likedCardAccent.argb())
            append(",")
            append(likedCardBorder.argb())
            append(",")
            append(likedCardGlow.argb())
            append(",")
            append(badgeBackground.argb())
            append("|text=")
            append(primaryText.argb())
            append(",")
            append(secondaryText.argb())
            append(",")
            append(mutedText.argb())
            append(",")
            append(buttonText.argb())
            append("|background=")
            append(backgroundGradient.joinToString(",") { it.argb() })
            append(",")
            append(backgroundOverlay.argb())
            append("|button=")
            append(buttonGradient.joinToString(",") { it.argb() })
            append("|dots=")
            append(categoryDots.joinToString(",") { it.argb() })
        }

    private fun Color.argb(): String =
        String.format(Locale.US, "%08X", toArgb())

    private val expectedPaletteSnapshot =
        """
        amber|accent=FFFACC15,FFF59E0B,FFF97316,FFFDE68A,FFFFFFFF,57FACC15|surface=E0121212,FF171717,3DFACC15,FFF59E0B,75FACC15,29F59E0B,4D451A03|text=FFF5F5F4,FFD6D3D1,FF78716C,FF111827|background=FF0A0A0A,FF17120A,FF0A0A0A,2EFACC15|button=FFFACC15,FFF59E0B|dots=FFFACC15,FFF59E0B,FFF97316,FFFDE68A
        sakura|accent=FF7AA95C,FF98C379,FF4F7F35,FFDCEBC9,FF3F2B34,47F472B6|surface=EBFFF7FB,FFFFF7FB,4DDB7093,FF98C379,807AA95C,2E7AA95C,297AA95C|text=FF49363D,FF6F5B62,FF9B7C86,FF17210F|background=FFFDEFF4,FFFFF7ED,FFF4EAD2,EBFDE2E7|button=FF7AA95C,FF98C379|dots=FF7AA95C,FF98C379,FF4F7F35,FFDB7093
        modernSaaS|accent=FF3B82F6,FF60A5FA,FF2563EB,FFBFDBFE,FFFFFFFF,5C3B82F6|surface=E61E1E1E,FF1E1E1E,4D3B82F6,FF60A5FA,8A60A5FA,293B82F6,213B82F6|text=FFE0E0E0,FFB7BEC8,FF7E8794,FFF8FAFC|background=FF121212,FF181818,FF101010,333B82F6|button=FF3B82F6,FF2563EB|dots=FF3B82F6,FF60A5FA,FF2563EB,FFBFDBFE
        sanJuan|accent=FF0077B6,FFE76F51,FF005F73,FFCCEFFF,FF3F2415,4D2A9DF4|surface=F0FFF8E5,FFFFF6DF,420077B6,FFE76F51,75E76F51,29E76F51,242A9DF4|text=FF4F3424,FF75513D,FF94684F,FFFFFAF0|background=FFFFF2D0,FFFFE4B0,FFD8F1E4,C2F6C453|button=FF0077B6,FFE76F51|dots=FF0077B6,FFE76F51,FF2A9DF4,FF2F9E44
        creativePremium|accent=FF7C3AED,FFA78BFA,FF5B21B6,FFDDD6FE,FFF8FAFC,6B7C3AED|surface=E61E293B,FF1E293B,577C3AED,FFA78BFA,8FA78BFA,2E7C3AED,247C3AED|text=FFCBD5E1,FF94A3B8,FF64748B,FFF8FAFC|background=FF0F172A,FF111827,FF0B1120,387C3AED|button=FFA78BFA,FF7C3AED,FF5B21B6|dots=FF7C3AED,FFA78BFA,FF5B21B6,FFDDD6FE
        moodyCyberpunk|accent=FFFACC15,FFFDE047,FFEAB308,FFFEF08A,FFF8FAFC,52FACC15|surface=E82C362F,FF2C362F,4DFACC15,FFFDE047,8AFDE047,24FACC15,1FFACC15|text=FFE5E7EB,FFCBD5C9,FF8B968B,FF111827|background=FF1A211B,FF20281F,FF151A16,2EFACC15|button=FFFACC15,FFFDE047|dots=FFFACC15,FFFDE047,FFEAB308,FF22C55E
        """
}
