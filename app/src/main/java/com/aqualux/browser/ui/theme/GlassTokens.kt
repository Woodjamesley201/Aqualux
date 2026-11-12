package com.aqualux.browser.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aqualux.browser.design.LiquidGlass
import com.aqualux.browser.model.UserSettings
import kotlin.math.min

data class GlassTokens(
    val dark: Boolean,
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val danger: Color,
    val warning: Color,
    val success: Color,
    val pageTop: Color,
    val pageBottom: Color,
    val solid: Color,
    val solidAlt: Color,
    val fillTop: Color,
    val fillMid: Color,
    val fillBottom: Color,
    val controlTop: Color,
    val controlBottom: Color,
    val popoverTop: Color,
    val popoverMid: Color,
    val popoverBottom: Color,
    val hoverFill: Color,
    val activeTabTop: Color,
    val activeTabBottom: Color,
    val scrim: Color,
    val hairline: Color,
    val hairlineOuter: Color,
    val specular: Color,
    val innerShadow: Color,
    val innerGlow: Color,
    val ambientShadow: Color,
    val refractHighlight: Color,
    val chromaA: Color,
    val chromaB: Color,
    val focusCore: Color,
    val focusHalo: Color,
    val blurRadius: Dp,
    val cardBlur: Dp,
    val pressScale: Float,
    val refraction: Boolean,
    val shelfRadius: Dp,
    val controlRadius: Dp,
    val cardRadius: Dp,
    val thumbRadius: Dp,
    val sheetRadius: Dp,
    val sheetRowRadius: Dp,
    val dropdownRadius: Dp,
    val omniboxHeight: Dp,
    val toolbarHeight: Dp,
    val tabRowHeight: Dp,
    val tabHeight: Dp,
    val tabMinWidth: Dp,
    val tabMaxWidth: Dp,
    val tabActiveMinWidth: Dp,
    val tabPinnedWidth: Dp,
    val buttonSize: Dp,
    val iconSize: Dp,
    val shelfMargin: Dp,
    val shelfPad: Dp,
    val rowGap: Dp,
    val collapsedPillMax: Dp,
    val collapsedOmnibox: Dp,
    val suggestionRow: Dp,
    val ambientElevation: Dp,
)

val LocalGlassTokens = compositionLocalOf { GlassPalette.resolve(UserSettings(), dark = false) }
val LocalReduceMotion = compositionLocalOf { false }

fun GlassTokens.nestedRadius(outer: Dp, inset: Dp): Dp = (outer - inset).coerceAtLeast(2.dp)

object GlassPalette {
    fun resolve(settings: UserSettings, dark: Boolean, underContent: Float = 0f): GlassTokens {
        val reduced = settings.reducedEffects
        val hue = LiquidGlass.wrapHue(settings.accentHue).toFloat()
        val k = if (reduced) 1f else LiquidGlass.fillK(settings.glassIntensity)
        val radii = LiquidGlass.radii(settings.cornerStyle.toToken())
        val metrics = LiquidGlass.chrome(settings.density.toToken())
        val blurScale = settings.blurScale
        val shelfBlur = if (!settings.nativeMaterial) {
            0f
        } else {
            LiquidGlass.shelfBlur(blurScale, underContent, reduced)
        }
        val accent = if (dark) hsl(hue, 0.92f, 0.62f) else hsl(hue, 0.88f, 0.44f)
        val accentStrong = if (dark) hsl(hue, 0.95f, 0.70f) else hsl(hue, 0.92f, 0.36f)
        val accentSoft = if (dark) hsl(hue, 0.92f, 0.62f, 0.20f) else hsl(hue, 0.90f, 0.50f, 0.16f)
        val ink = if (dark) rgb(240, 246, 252, 0.94f) else rgb(12, 22, 32, 0.92f)
        val muted = if (dark) rgb(240, 246, 252, 0.66f) else rgb(12, 22, 32, 0.64f)
        val faint = if (dark) rgb(240, 246, 252, 0.44f) else rgb(12, 22, 32, 0.44f)
        val solid = if (dark) Color(0xFF1C1E22) else Color(0xFFF7F8FA)
        val solidAlt = if (dark) Color(0xFF24262A) else Color.White
        val pageTop = if (dark) Color(0xFF16181C) else Color(0xFFF5F6F8)
        val pageBottom = if (dark) Color(0xFF1C1E22) else Color(0xFFEEF0F3)
        val fill = if (reduced) {
            Triple(solid, solid, solid)
        } else if (dark) {
            Triple(
                rgb(46, 48, 52, cap(0.96f, 0.80f * k)),
                rgb(38, 40, 44, cap(0.96f, 0.76f * k)),
                rgb(30, 32, 36, cap(0.96f, 0.74f * k)),
            )
        } else {
            Triple(
                rgb(255, 255, 255, cap(0.96f, 0.86f * k)),
                rgb(250, 251, 252, cap(0.96f, 0.78f * k)),
                rgb(244, 245, 247, cap(0.96f, 0.72f * k)),
            )
        }
        val control = if (reduced) {
            fill.first to fill.third
        } else if (dark) {
            fill.first to fill.third
        } else {
            fill.first to fill.third
        }
        val popover = if (reduced) {
            Triple(solidAlt, solidAlt, solidAlt)
        } else if (dark) {
            Triple(
                rgb(46, 48, 52, cap(0.98f, 0.88f * k)),
                rgb(38, 40, 44, cap(0.98f, 0.84f * k)),
                rgb(30, 32, 36, cap(0.98f, 0.82f * k)),
            )
        } else {
            Triple(
                rgb(255, 255, 255, cap(0.98f, 0.94f * k)),
                rgb(247, 248, 250, cap(0.98f, 0.90f * k)),
                rgb(243, 244, 246, cap(0.98f, 0.88f * k)),
            )
        }
        return GlassTokens(
            dark = dark,
            accent = accent,
            accentStrong = accentStrong,
            accentSoft = accentSoft,
            onAccent = if (dark) Color(0xFF04121C) else Color.White,
            ink = ink,
            muted = muted,
            faint = faint,
            danger = if (dark) Color(0xFFFF5A64) else Color(0xFFD92D3A),
            warning = if (dark) Color(0xFFFFB340) else Color(0xFFC77700),
            success = if (dark) Color(0xFF3DDC84) else Color(0xFF18A058),
            pageTop = pageTop,
            pageBottom = pageBottom,
            solid = solid,
            solidAlt = solidAlt,
            fillTop = fill.first,
            fillMid = fill.second,
            fillBottom = fill.third,
            controlTop = control.first,
            controlBottom = control.second,
            popoverTop = popover.first,
            popoverMid = popover.second,
            popoverBottom = popover.third,
            hoverFill = if (dark) rgb(255, 255, 255, 0.08f) else rgb(12, 22, 32, 0.06f),
            activeTabTop = if (dark) rgb(255, 255, 255, 0.18f) else rgb(255, 255, 255, 0.92f),
            activeTabBottom = if (dark) rgb(255, 255, 255, 0.10f) else rgb(255, 255, 255, 0.74f),
            scrim = if (dark) rgb(0, 0, 0, 0.45f) else rgb(14, 24, 36, 0.28f),
            hairline = rgb(255, 255, 255, if (dark) 0.14f else 0.70f),
            hairlineOuter = if (dark) rgb(0, 0, 0, 0.45f) else rgb(10, 30, 50, 0.10f),
            specular = rgb(255, 255, 255, if (dark) 0.28f else 0.95f),
            innerShadow = if (dark) rgb(0, 0, 0, 0.30f) else rgb(10, 30, 50, 0.06f),
            innerGlow = rgb(255, 255, 255, if (dark) 0.04f else 0.35f),
            ambientShadow = if (dark) rgb(0, 0, 0, 0.45f) else rgb(10, 30, 50, 0.16f),
            refractHighlight = rgb(255, 255, 255, if (dark) 0.22f else 0.55f),
            chromaA = hsl(hue - 40f, 1f, if (dark) 0.65f else 0.70f, if (dark) 0.40f else 0.45f),
            chromaB = hsl(hue + 150f, 1f, if (dark) 0.68f else 0.72f, if (dark) 0.34f else 0.40f),
            focusCore = if (dark) hsl(hue, 1f, 0.72f) else hsl(hue, 0.95f, 0.38f),
            focusHalo = if (dark) hsl(hue, 1f, 0.62f, 0.42f) else hsl(hue, 0.95f, 0.55f, 0.38f),
            blurRadius = shelfBlur.dp,
            cardBlur = LiquidGlass.stepBlur(LiquidGlass.BLUR_MD, blurScale, reduced || !settings.nativeMaterial).dp,
            pressScale = if (reduced) 1f else LiquidGlass.PRESS_SCALE,
            refraction = settings.refraction && !reduced,
            shelfRadius = radii.shelf.dp,
            controlRadius = radii.control.dp,
            cardRadius = radii.card.dp,
            thumbRadius = radii.thumb.dp,
            sheetRadius = radii.sheet.dp,
            sheetRowRadius = radii.sheetRow.dp,
            dropdownRadius = radii.dropdown.dp,
            omniboxHeight = metrics.omnibox.dp,
            toolbarHeight = metrics.toolbar.dp,
            tabRowHeight = metrics.tabRow.dp,
            tabHeight = metrics.tab.dp,
            tabMinWidth = metrics.tabMinW.dp,
            tabMaxWidth = metrics.tabMaxW.dp,
            tabActiveMinWidth = metrics.tabActiveMinW.dp,
            tabPinnedWidth = metrics.tabPinnedW.dp,
            buttonSize = metrics.button.dp,
            iconSize = metrics.icon.dp,
            shelfMargin = metrics.shelfMargin.dp,
            shelfPad = metrics.shelfPad.dp,
            rowGap = metrics.rowGap.dp,
            collapsedPillMax = LiquidGlass.COLLAPSED_PILL_MAX.dp,
            collapsedOmnibox = LiquidGlass.COLLAPSED_OMNIBOX.dp,
            suggestionRow = metrics.suggestionRow.dp,
            ambientElevation = if (reduced) 1.dp else 10.dp,
        )
    }

    private fun cap(limit: Float, value: Float) = min(limit, value)

    private fun rgb(r: Int, g: Int, b: Int, alpha: Float) = Color(r / 255f, g / 255f, b / 255f, alpha)

    private fun hsl(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
        val rgb = LiquidGlass.hsl(h, s, l)
        return Color(rgb.r, rgb.g, rgb.b, alpha)
    }
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    var scale by remember { mutableFloatStateOf(readAnimatorScale(context)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scale = readAnimatorScale(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale == 0f
}

private fun readAnimatorScale(context: Context): Float = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
} catch (_: Exception) {
    1f
}
