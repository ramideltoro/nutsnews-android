package com.nutsnews.app.designsystem

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class NutsNewsAppThemeTest {
    @Test
    fun sixThemesPreserveIosOrderIdentityAndPresentation() {
        assertEquals(
            listOf("amber", "sakura", "modernSaaS", "sanJuan", "creativePremium", "moodyCyberpunk"),
            NutsNewsAppTheme.entries.map(NutsNewsAppTheme::rawValue),
        )
        assertEquals(
            listOf("Amber", "Sakura", "SaaS", "Foxy", "Friday", "Bambi"),
            NutsNewsAppTheme.entries.map(NutsNewsAppTheme::title),
        )
        assertEquals(
            listOf(
                "Classic NutsNews amber glow.",
                "Cherry pink matcha calm.",
                "Sleek dark blue polish.",
                "Pastel streets tropical glow.",
                "Navy purple premium glow.",
                "Green cyber yellow glow.",
            ),
            NutsNewsAppTheme.entries.map(NutsNewsAppTheme::description),
        )
        assertEquals(
            listOf(
                "sun.max.fill",
                "camera.macro",
                "bolt.fill",
                "sparkles",
                "wand.and.stars",
                "leaf.fill",
            ),
            NutsNewsAppTheme.entries.map(NutsNewsAppTheme::iconName),
        )
    }

    @Test
    fun preferredSchemesMatchIosAndIgnorePlatformMode() {
        assertTrue(NutsNewsAppTheme.Amber.isDark)
        assertFalse(NutsNewsAppTheme.Sakura.isDark)
        assertTrue(NutsNewsAppTheme.SaaS.isDark)
        assertFalse(NutsNewsAppTheme.Foxy.isDark)
        assertTrue(NutsNewsAppTheme.Friday.isDark)
        assertTrue(NutsNewsAppTheme.Bambi.isDark)
    }

    @Test
    fun storedValuesKeepIosLegacyMigrationAndAmberFallback() {
        NutsNewsAppTheme.entries.forEach { theme ->
            assertEquals(theme, NutsNewsAppTheme.fromRawValue(theme.rawValue))
            assertEquals(theme, NutsNewsAppTheme.fromStoredValue(theme.rawValue))
        }

        assertEquals(NutsNewsAppTheme.Amber, NutsNewsAppTheme.fromStoredValue("plain"))
        assertEquals(NutsNewsAppTheme.Amber, NutsNewsAppTheme.fromStoredValue("dark"))
        assertEquals(NutsNewsAppTheme.Foxy, NutsNewsAppTheme.fromStoredValue("darkPink"))
        assertEquals(NutsNewsAppTheme.Sakura, NutsNewsAppTheme.fromStoredValue("lilac"))
        assertEquals(NutsNewsAppTheme.Amber, NutsNewsAppTheme.fromStoredValue("unknown"))
        assertEquals(NutsNewsAppTheme.Amber, NutsNewsAppTheme.fromStoredValue(null))
        assertNull(NutsNewsAppTheme.fromRawValue("unknown"))
    }
}
