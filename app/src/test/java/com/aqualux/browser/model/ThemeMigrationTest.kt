package com.aqualux.browser.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeMigrationTest {
    @Test
    fun factorySystemDefaultMigratesToLight() {
        assertEquals(ThemeMode.LIGHT, themeFromStore(null))
        assertEquals(ThemeMode.LIGHT, themeFromStore(""))
        assertEquals(ThemeMode.LIGHT, UserSettings().themeMode)
    }

    @Test
    fun explicitDarkAndSystemStay() {
        assertEquals(ThemeMode.DARK, themeFromStore("dark"))
        assertEquals(ThemeMode.SYSTEM, themeFromStore("system"))
        assertEquals(ThemeMode.LIGHT, themeFromStore("light"))
    }
}
