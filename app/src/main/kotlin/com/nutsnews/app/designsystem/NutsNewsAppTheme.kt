package com.nutsnews.app.designsystem

import androidx.compose.runtime.Immutable

@Immutable
enum class NutsNewsAppTheme(
    val rawValue: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isDark: Boolean,
) {
    Amber(
        rawValue = "amber",
        title = "Amber",
        description = "Classic NutsNews amber glow.",
        iconName = "sun.max.fill",
        isDark = true,
    ),
    Sakura(
        rawValue = "sakura",
        title = "Sakura",
        description = "Cherry pink matcha calm.",
        iconName = "camera.macro",
        isDark = false,
    ),
    SaaS(
        rawValue = "modernSaaS",
        title = "SaaS",
        description = "Sleek dark blue polish.",
        iconName = "bolt.fill",
        isDark = true,
    ),
    Foxy(
        rawValue = "sanJuan",
        title = "Foxy",
        description = "Pastel streets tropical glow.",
        iconName = "sparkles",
        isDark = false,
    ),
    Friday(
        rawValue = "creativePremium",
        title = "Friday",
        description = "Navy purple premium glow.",
        iconName = "wand.and.stars",
        isDark = true,
    ),
    Bambi(
        rawValue = "moodyCyberpunk",
        title = "Bambi",
        description = "Green cyber yellow glow.",
        iconName = "leaf.fill",
        isDark = true,
    ),
    ;

    companion object {
        const val StorageKey = "nutsnews.selectedTheme"
        val Default = Amber

        fun fromRawValue(rawValue: String?): NutsNewsAppTheme? =
            entries.firstOrNull { it.rawValue == rawValue }

        fun fromStoredValue(rawValue: String?): NutsNewsAppTheme =
            fromRawValue(rawValue)
                ?: when (rawValue) {
                    "plain", "dark" -> Amber
                    "darkPink" -> Foxy
                    "lilac" -> Sakura
                    else -> Default
                }
    }
}
