package com.aqualux.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.aqualux.browser.model.ThemeMode
import com.aqualux.browser.model.UserSettings

@Composable
fun AqualuxTheme(
    settings: UserSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val tokens = GlassPalette.resolve(settings, dark)
    val reduceMotion = rememberReduceMotion() || settings.reducedEffects
    val scheme = if (dark) {
        darkColorScheme(
            primary = tokens.accent,
            onPrimary = Color(0xFF042126),
            background = tokens.pageBottom,
            surface = tokens.solid,
            onBackground = tokens.ink,
            onSurface = tokens.ink,
            error = tokens.danger,
        )
    } else {
        lightColorScheme(
            primary = tokens.accent,
            onPrimary = Color.White,
            background = tokens.pageBottom,
            surface = tokens.solid,
            onBackground = tokens.ink,
            onSurface = tokens.ink,
            error = tokens.danger,
        )
    }
    CompositionLocalProvider(
        LocalGlassTokens provides tokens,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
