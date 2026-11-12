package com.aqualux.browser.design

/**
 * Port of UI Isaiah's Liquid Glass tokens (aqualux-tokens.css / tokens.ts, v1.0.0).
 * Pure Kotlin so the numbers can be tested without Android. Compose reads this
 * through [com.aqualux.browser.ui.theme.GlassTokens].
 */
object LiquidGlass {
    const val VERSION = "1.0.0"
    const val ACCENT_HUE_DEFAULT = 199

    const val BLUR_XS = 12f
    const val BLUR_SM = 16f
    const val BLUR_MD = 20f
    const val BLUR_LG = 28f
    const val BLUR_XL = 40f

    const val SATURATE = 1.8f
    const val SATURATE_STRONG = 2.1f
    const val BRIGHTNESS_LIGHT = 1.08f
    const val BRIGHTNESS_DARK = 0.92f
    const val PRESS_BRIGHTNESS_LIGHT = 0.92f
    const val PRESS_BRIGHTNESS_DARK = 1.28f
    const val HOVER_BRIGHTNESS_LIGHT = 1.04f
    const val HOVER_BRIGHTNESS_DARK = 1.12f
    const val PRESS_SCALE = 0.97f

    const val PAD_SHELF = 6
    const val MARGIN_SHELF = 6
    const val PAD_CARD = 8
    const val PAD_SHEET = 12
    const val PAD_DROPDOWN = 6

    enum class CornerStyle { ROUND, SOFT, SQUARE }

    data class Radii(
        val control: Int,
        val shelf: Int,
        val window: Int,
        val card: Int,
        val thumb: Int,
        val sheet: Int,
        val sheetRow: Int,
        val dropdownItem: Int,
        val dropdown: Int,
    )

    fun radii(style: CornerStyle): Radii {
        val (control, card, sheet) = when (style) {
            CornerStyle.ROUND -> Triple(14, 18, 28)
            CornerStyle.SOFT -> Triple(10, 14, 22)
            CornerStyle.SQUARE -> Triple(6, 10, 14)
        }
        val shelf = control + PAD_SHELF
        val dropdownItem = when (style) {
            CornerStyle.ROUND -> 10
            CornerStyle.SOFT -> 8
            CornerStyle.SQUARE -> 4
        }
        return Radii(
            control = control,
            shelf = shelf,
            window = shelf + MARGIN_SHELF,
            card = card,
            thumb = (card - PAD_CARD).coerceAtLeast(2),
            sheet = sheet,
            sheetRow = (sheet - PAD_SHEET).coerceAtLeast(2),
            dropdownItem = dropdownItem,
            dropdown = dropdownItem + PAD_DROPDOWN,
        )
    }

    enum class Density { REGULAR, COMPACT }

    data class ChromeMetrics(
        val shelfMargin: Int,
        val shelfPad: Int,
        val rowGap: Int,
        val tabRow: Int,
        val tab: Int,
        val tabMinW: Int,
        val tabMaxW: Int,
        val tabActiveMinW: Int,
        val tabPinnedW: Int,
        val toolbar: Int,
        val omnibox: Int,
        val button: Int,
        val icon: Int,
        val suggestionRow: Int,
    )

    fun chrome(density: Density): ChromeMetrics = when (density) {
        Density.REGULAR -> ChromeMetrics(6, 6, 4, 32, 30, 72, 220, 120, 36, 40, 36, 32, 18, 40)
        Density.COMPACT -> ChromeMetrics(6, 6, 4, 28, 26, 64, 200, 104, 32, 34, 30, 28, 16, 34)
    }

    const val COLLAPSED_SHELF_PAD = 2
    const val COLLAPSED_TOOLBAR = 28
    const val COLLAPSED_OMNIBOX = 24
    const val COLLAPSED_PILL_MAX = 340

    fun chromeHeight(density: Density, collapsed: Boolean): Int {
        val metrics = chrome(density)
        return if (collapsed) {
            metrics.shelfMargin + COLLAPSED_SHELF_PAD * 2 + COLLAPSED_TOOLBAR
        } else {
            metrics.shelfMargin + metrics.shelfPad + metrics.tabRow + metrics.rowGap + metrics.toolbar + metrics.shelfPad
        }
    }

    /** fill-k = 1.35 − 0.7 × intensity. 0 is frosted, 0.5 is the default, 1 is clear. */
    fun fillK(glassIntensity: Float): Float {
        val intensity = glassIntensity.coerceIn(0f, 1f)
        return 1.35f - 0.7f * intensity
    }

    /**
     * Shelf blur. `min(40 × scale, 28 × scale × (1 + 0.4 × underContent))`.
     * Reduced effects force 0.
     */
    fun shelfBlur(blurScale: Float, underContent: Float = 0f, reduced: Boolean = false): Float {
        if (reduced) return 0f
        val scale = blurScale.coerceIn(0.5f, 1.5f)
        val under = underContent.coerceIn(0f, 1f)
        val boosted = BLUR_LG * scale * (1f + 0.4f * under)
        val cap = BLUR_XL * scale
        return minOf(cap, boosted)
    }

    fun stepBlur(basePx: Float, blurScale: Float, reduced: Boolean): Float {
        if (reduced) return 0f
        return basePx * blurScale.coerceIn(0.5f, 1.5f)
    }

    fun wrapHue(hue: Int): Int = ((hue % 360) + 360) % 360

    data class Rgb(val r: Float, val g: Float, val b: Float)

    /** CSS `hsl(h s% l%)` with s and l in 0..1. */
    fun hsl(hue: Float, saturation: Float, lightness: Float): Rgb {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val hp = h / 60f
        val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
        val (r1, g1, b1) = when {
            hp < 1f -> Triple(c, x, 0f)
            hp < 2f -> Triple(x, c, 0f)
            hp < 3f -> Triple(0f, c, x)
            hp < 4f -> Triple(0f, x, c)
            hp < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2f
        return Rgb(r1 + m, g1 + m, b1 + m)
    }
}
