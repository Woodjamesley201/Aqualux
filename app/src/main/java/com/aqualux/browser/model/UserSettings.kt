package com.aqualux.browser.model

import com.aqualux.browser.design.LiquidGlass
import com.aqualux.browser.omnibox.SearchEngine

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    ;

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * v1.0.0 shipped with no stored theme, which rendered as System.
 * A missing value migrates to Light. An explicit Dark or System choice is kept.
 */
fun themeFromStore(stored: String?): ThemeMode {
    if (stored.isNullOrBlank()) return ThemeMode.LIGHT
    return ThemeMode.entries.firstOrNull { it.id == stored } ?: ThemeMode.LIGHT
}

enum class Density(val id: String, val label: String) {
    REGULAR("regular", "Regular"),
    COMPACT("compact", "Compact"),
    ;

    companion object {
        fun fromId(id: String?): Density = entries.firstOrNull { it.id == id } ?: REGULAR
    }

    fun toToken(): LiquidGlass.Density = when (this) {
        REGULAR -> LiquidGlass.Density.REGULAR
        COMPACT -> LiquidGlass.Density.COMPACT
    }
}

enum class CornerStyle(val id: String, val label: String) {
    ROUND("round", "Round"),
    SOFT("soft", "Soft"),
    SQUARE("square", "Square"),
    ;

    companion object {
        fun fromId(id: String?): CornerStyle = entries.firstOrNull { it.id == id } ?: ROUND
    }

    fun toToken(): LiquidGlass.CornerStyle = when (this) {
        ROUND -> LiquidGlass.CornerStyle.ROUND
        SOFT -> LiquidGlass.CornerStyle.SOFT
        SQUARE -> LiquidGlass.CornerStyle.SQUARE
    }
}

data class AccentPreset(val name: String, val hue: Int)

object AccentPresets {
    val all = listOf(
        AccentPreset("Lagoon", 199),
        AccentPreset("Glacier", 214),
        AccentPreset("Iris", 252),
        AccentPreset("Orchid", 300),
        AccentPreset("Coral", 12),
        AccentPreset("Amber", 38),
        AccentPreset("Lime", 96),
        AccentPreset("Mint", 160),
    )
}

/**
 * Isaiah's customization schema. Glass intensity runs frosted (0) to clear (1).
 * [reducedEffects] is the manual override that turns off blur, refraction, and spring motion.
 */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentHue: Int = LiquidGlass.ACCENT_HUE_DEFAULT,
    val glassIntensity: Float = 0.5f,
    val blurScale: Float = 1f,
    val cornerStyle: CornerStyle = CornerStyle.ROUND,
    val density: Density = Density.REGULAR,
    val reducedEffects: Boolean = false,
    val refraction: Boolean = true,
    val nativeMaterial: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val idleUnloadMinutes: Int = 20,
    val textZoom: Int = 100,
    val darkenPages: Boolean = false,
    val blockThirdPartyCookies: Boolean = false,
)
