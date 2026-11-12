package com.aqualux.browser.design

import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidGlassTest {
    @Test
    fun fillKMatchesTheSpecCurve() {
        assertEquals(1.35f, LiquidGlass.fillK(0f), 0.001f)
        assertEquals(1.0f, LiquidGlass.fillK(0.5f), 0.001f)
        assertEquals(0.65f, LiquidGlass.fillK(1f), 0.001f)
    }

    @Test
    fun concentricRadiiFollowOuterEqualsInnerPlusPadding() {
        val round = LiquidGlass.radii(LiquidGlass.CornerStyle.ROUND)
        assertEquals(14, round.control)
        assertEquals(20, round.shelf)
        assertEquals(26, round.window)
        assertEquals(18, round.card)
        assertEquals(10, round.thumb)
        assertEquals(28, round.sheet)
        assertEquals(16, round.sheetRow)
        assertEquals(16, round.dropdown)
        val soft = LiquidGlass.radii(LiquidGlass.CornerStyle.SOFT)
        assertEquals(10, soft.control)
        assertEquals(16, soft.shelf)
        assertEquals(22, soft.sheet)
        val square = LiquidGlass.radii(LiquidGlass.CornerStyle.SQUARE)
        assertEquals(6, square.control)
        assertEquals(14, square.sheet)
    }

    @Test
    fun chromeHeightsMatchTheSpec() {
        assertEquals(94, LiquidGlass.chromeHeight(LiquidGlass.Density.REGULAR, collapsed = false))
        assertEquals(84, LiquidGlass.chromeHeight(LiquidGlass.Density.COMPACT, collapsed = false))
        assertEquals(38, LiquidGlass.chromeHeight(LiquidGlass.Density.REGULAR, collapsed = true))
        assertEquals(38, LiquidGlass.chromeHeight(LiquidGlass.Density.COMPACT, collapsed = true))
    }

    @Test
    fun shelfBlurScalesAndCapsAndShutsOff() {
        assertEquals(28f, LiquidGlass.shelfBlur(1f, 0f, false), 0.01f)
        assertEquals(39.2f, LiquidGlass.shelfBlur(1f, 1f, false), 0.01f)
        assertEquals(0f, LiquidGlass.shelfBlur(1.5f, 1f, true), 0.01f)
        assertEquals(14f, LiquidGlass.shelfBlur(0.5f, 0f, false), 0.01f)
    }

    @Test
    fun defaultAccentHueIsLagoon() {
        assertEquals(199, LiquidGlass.ACCENT_HUE_DEFAULT)
        assertEquals(10, LiquidGlass.wrapHue(370))
        assertEquals(340, LiquidGlass.wrapHue(-20))
        val lagoon = LiquidGlass.hsl(199f, 0.88f, 0.44f)
        assertEquals(0.053f, lagoon.r, 0.02f)
        assertEquals(0.58f, lagoon.g, 0.03f)
        assertEquals(0.83f, lagoon.b, 0.03f)
    }
}
