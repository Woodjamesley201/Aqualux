package com.aqualux.browser.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aqualux.browser.design.LiquidGlass
import com.aqualux.browser.model.CornerStyle
import com.aqualux.browser.model.Density
import com.aqualux.browser.model.UserSettings
import com.aqualux.browser.model.themeFromStore
import com.aqualux.browser.omnibox.SearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "aqualux_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<UserSettings> = context.settingsStore.data
        .map { it.toUserSettings() }
        .catch { emit(UserSettings()) }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.settingsStore.edit { prefs ->
            prefs.write(transform(prefs.toUserSettings()))
        }
    }

    private fun Preferences.toUserSettings(): UserSettings = UserSettings(
        themeMode = themeFromStore(this[Keys.THEME]),
        accentHue = LiquidGlass.wrapHue(this[Keys.HUE] ?: LiquidGlass.ACCENT_HUE_DEFAULT),
        glassIntensity = (this[Keys.GLASS] ?: 0.5f).coerceIn(0f, 1f),
        blurScale = (this[Keys.BLUR] ?: 1f).coerceIn(0.5f, 1.5f),
        cornerStyle = CornerStyle.fromId(this[Keys.CORNERS]),
        density = Density.fromId(this[Keys.DENSITY]),
        reducedEffects = this[Keys.REDUCED] ?: false,
        refraction = this[Keys.REFRACTION] ?: true,
        nativeMaterial = this[Keys.MATERIAL] ?: true,
        searchEngine = SearchEngine.fromId(this[Keys.ENGINE]),
        idleUnloadMinutes = this[Keys.IDLE] ?: 20,
        textZoom = this[Keys.TEXT_ZOOM] ?: 100,
        darkenPages = this[Keys.DARKEN] ?: false,
        blockThirdPartyCookies = this[Keys.BLOCK_THIRD_PARTY] ?: false,
    )

    private fun MutablePreferences.write(settings: UserSettings) {
        this[Keys.THEME] = settings.themeMode.id
        this[Keys.HUE] = LiquidGlass.wrapHue(settings.accentHue)
        this[Keys.GLASS] = settings.glassIntensity.coerceIn(0f, 1f)
        this[Keys.BLUR] = settings.blurScale.coerceIn(0.5f, 1.5f)
        this[Keys.CORNERS] = settings.cornerStyle.id
        this[Keys.DENSITY] = settings.density.id
        this[Keys.REDUCED] = settings.reducedEffects
        this[Keys.REFRACTION] = settings.refraction
        this[Keys.MATERIAL] = settings.nativeMaterial
        this[Keys.ENGINE] = settings.searchEngine.id
        this[Keys.IDLE] = settings.idleUnloadMinutes
        this[Keys.TEXT_ZOOM] = settings.textZoom
        this[Keys.DARKEN] = settings.darkenPages
        this[Keys.BLOCK_THIRD_PARTY] = settings.blockThirdPartyCookies
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val HUE = intPreferencesKey("accent_hue")
        val GLASS = floatPreferencesKey("glass")
        val BLUR = floatPreferencesKey("blur_scale")
        val CORNERS = stringPreferencesKey("corners")
        val DENSITY = stringPreferencesKey("density")
        val REDUCED = booleanPreferencesKey("reduced_effects")
        val REFRACTION = booleanPreferencesKey("refraction")
        val MATERIAL = booleanPreferencesKey("native_material")
        val ENGINE = stringPreferencesKey("engine")
        val IDLE = intPreferencesKey("idle_minutes")
        val TEXT_ZOOM = intPreferencesKey("text_zoom")
        val DARKEN = booleanPreferencesKey("darken_pages")
        val BLOCK_THIRD_PARTY = booleanPreferencesKey("block_third_party")
    }
}
