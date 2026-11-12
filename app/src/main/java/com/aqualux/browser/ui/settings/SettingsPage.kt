package com.aqualux.browser.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqualux.browser.design.LiquidGlass
import com.aqualux.browser.model.AccentPresets
import com.aqualux.browser.model.CornerStyle
import com.aqualux.browser.model.Density
import com.aqualux.browser.model.ThemeMode
import com.aqualux.browser.model.UserSettings
import com.aqualux.browser.omnibox.SearchEngine
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aqualux.browser.ui.glass.AquaMark
import com.aqualux.browser.ui.glass.GlassIconButton
import com.aqualux.browser.ui.glass.GlassSurface
import com.aqualux.browser.ui.glass.GlassVariant
import com.aqualux.browser.ui.theme.LocalGlassTokens
import com.aqualux.browser.ui.theme.nestedRadius

@Composable
fun SettingsHost(
    settings: UserSettings,
    onChange: (UserSettings) -> Unit,
    onTextZoom: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onClearCookies: () -> Unit,
    onClearSiteData: () -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val configuration = LocalConfiguration.current
    val tablet = minOf(configuration.screenWidthDp, configuration.screenHeightDp) >= 600
    if (!tablet) {
        SettingsPage(settings, onChange, onTextZoom, onClearHistory, onClearCookies, onClearSiteData, onBack)
        return
    }
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(tokens.scrim)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .clickable(onClick = {}),
                shape = RoundedCornerShape(28.dp),
                variant = GlassVariant.Popover,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            ) {
                SettingsPage(
                    settings, onChange, onTextZoom, onClearHistory, onClearCookies, onClearSiteData, onBack,
                    embedded = true,
                )
            }
        }
    }
}

@Composable
fun SettingsPage(
    settings: UserSettings,
    onChange: (UserSettings) -> Unit,
    onTextZoom: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onClearCookies: () -> Unit,
    onClearSiteData: () -> Unit,
    onBack: () -> Unit,
    embedded: Boolean = false,
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    val version = remember {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: "1.0.0"
    }
    Column(
        Modifier
            .then(if (embedded) Modifier.fillMaxWidth() else Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (embedded) 4.dp else 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            Text("Settings", color = tokens.ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Section("Appearance") {
            ShelfPreview()
            Spacer(Modifier.height(12.dp))
            Text("Theme", color = tokens.muted, fontSize = 13.sp)
            ChoiceRow(
                options = ThemeMode.entries.map { it.label to it },
                selected = settings.themeMode,
                onSelect = { onChange(settings.copy(themeMode = it)) },
                compact = true,
            )
            Text("Accent", color = tokens.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                AccentPresets.all.forEach { preset ->
                    val color = accentSwatch(preset.hue, tokens.dark)
                    Spacer(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (LiquidGlass.wrapHue(settings.accentHue) == preset.hue) 2.dp else 0.dp,
                                color = tokens.ink,
                                shape = CircleShape,
                            )
                            .clickable { onChange(settings.copy(accentHue = preset.hue)) },
                    )
                }
            }
            Text(
                AccentPresets.all.firstOrNull { it.hue == LiquidGlass.wrapHue(settings.accentHue) }?.name
                    ?: "Hue ${LiquidGlass.wrapHue(settings.accentHue)}",
                color = tokens.faint,
                fontSize = 12.sp,
            )
            LabeledSlider(
                label = "Glass intensity",
                value = settings.glassIntensity,
                range = 0f..1f,
                readout = when {
                    settings.glassIntensity < 0.34f -> "Frosted"
                    settings.glassIntensity > 0.7f -> "Clear"
                    else -> "Balanced"
                },
                onChange = { onChange(settings.copy(glassIntensity = it)) },
            )
            LabeledSlider(
                label = "Blur amount",
                value = settings.blurScale,
                range = 0.5f..1.5f,
                readout = "${(settings.blurScale * 100).toInt()}%",
                onChange = { onChange(settings.copy(blurScale = it)) },
            )
            Text("Corners", color = tokens.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            ChoiceRow(
                options = CornerStyle.entries.map { it.label to it },
                selected = settings.cornerStyle,
                onSelect = { onChange(settings.copy(cornerStyle = it)) },
            )
            Text("Chrome", color = tokens.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            ChoiceRow(
                options = Density.entries.map { it.label to it },
                selected = settings.density,
                onSelect = { onChange(settings.copy(density = it)) },
            )
            ToggleRow(
                "Refraction",
                "Pointer-tracked highlight and a chromatic rim on the glass.",
                settings.refraction,
            ) { onChange(settings.copy(refraction = it)) }
            ToggleRow(
                "Reduce effects",
                "Solid fills, no blur, and short linear motion. Follows the system reduce-motion setting as well.",
                settings.reducedEffects,
            ) { onChange(settings.copy(reducedEffects = it)) }
            ToggleRow(
                "Blur the glass",
                "RenderEffect frost on Android 12 and later. Off uses the translucent fill only.",
                settings.nativeMaterial,
            ) { onChange(settings.copy(nativeMaterial = it)) }
            ToggleRow(
                "Darken websites",
                if (Build.VERSION.SDK_INT >= 33) {
                    "Ask WebView to darken pages while Aqualux is in a dark theme."
                } else {
                    "Available on Android 13 and later."
                },
                settings.darkenPages,
            ) { onChange(settings.copy(darkenPages = it)) }
        }
        Section("Search") {
            SearchEngine.entries.forEach { engine ->
                ChoiceLine(
                    label = engine.label,
                    selected = settings.searchEngine == engine,
                    onClick = { onChange(settings.copy(searchEngine = engine)) },
                )
            }
        }
        Section("Reading") {
            LabeledSlider(
                label = "Text size",
                value = settings.textZoom / 100f,
                range = 0.5f..2f,
                readout = "${settings.textZoom}%",
                onChange = { onTextZoom((it * 100).toInt()) },
            )
        }
        Section("Tabs") {
            LabeledSlider(
                label = "Unload idle background tabs",
                value = settings.idleUnloadMinutes.coerceIn(0, 120) / 120f,
                range = 0f..1f,
                readout = if (settings.idleUnloadMinutes <= 0) "Never" else "${settings.idleUnloadMinutes} min",
                onChange = { fraction ->
                    val minutes = (fraction * 120).toInt()
                    onChange(settings.copy(idleUnloadMinutes = if (minutes < 3) 0 else minutes))
                },
            )
            Text(
                "Open tabs keep their address, title, and scroll position. A sleeping tab reloads when you return to it.",
                color = tokens.muted,
                fontSize = 13.sp,
            )
        }
        Section("Privacy") {
            ToggleRow(
                "Block third-party cookies",
                "Some sites use them for embedded content. Blocking can sign you out of widgets.",
                settings.blockThirdPartyCookies,
            ) { onChange(settings.copy(blockThirdPartyCookies = it)) }
            ActionLine("Clear history", onClearHistory)
            ActionLine("Clear cookies", onClearCookies)
            ActionLine("Clear site data", onClearSiteData)
            Text(
                "Safe Browsing is on. Mixed content is blocked. Certificate errors are never bypassed, and pages cannot call into the app.",
                color = tokens.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Section("About") {
            Text("Aqualux $version", color = tokens.ink, fontWeight = FontWeight.Medium)
            Text(
                "A native Android browser. Pages render in Android System WebView, one view per tab. Glass tokens live in GlassTokens so a later design spec can retune them in one place.",
                color = tokens.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val tokens = LocalGlassTokens.current
    Text(
        title,
        color = tokens.accent,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 8.dp),
    )
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardRadius),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp), content = { content() })
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    compact: Boolean = false,
) {
    val tokens = LocalGlassTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        options.forEach { (label, value) ->
            val on = value == selected
            Text(
                label,
                color = if (on) tokens.onAccent else tokens.ink,
                modifier = Modifier
                    .height(if (compact) 28.dp else 36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) tokens.accent else tokens.hoverFill)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 12.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun ChoiceLine(label: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = LocalGlassTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = tokens.ink, modifier = Modifier.weight(1f))
        if (selected) Text("On", color = tokens.accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    readout: String,
    onChange: (Float) -> Unit,
) {
    val tokens = LocalGlassTokens.current
    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = tokens.ink, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Text(readout, color = tokens.muted, fontSize = 13.sp)
    }
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = tokens.accent,
            activeTrackColor = tokens.accent,
            inactiveTrackColor = tokens.hoverFill,
        ),
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val tokens = LocalGlassTokens.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = tokens.ink, fontSize = 15.sp)
            Text(subtitle, color = tokens.muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.height(26.dp).widthIn(max = 44.dp),
        )
    }
}

@Composable
private fun ShelfPreview() {
    val tokens = LocalGlassTokens.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(tokens.pageTop, tokens.pageBottom)))
            .padding(10.dp),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(tokens.shelfRadius),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        ) {
            Column {
                Text(
                    "Aqualux",
                    color = tokens.ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(tokens.controlRadius))
                        .background(Brush.verticalGradient(listOf(tokens.activeTabTop, tokens.activeTabBottom)))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AquaMark(size = 18.dp)
                    Text(
                        "Search or type a URL",
                        color = tokens.faint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

private fun accentSwatch(hue: Int, dark: Boolean): Color {
    val rgb = LiquidGlass.hsl(
        hue.toFloat(),
        if (dark) 0.92f else 0.88f,
        if (dark) 0.62f else 0.44f,
    )
    return Color(rgb.r, rgb.g, rgb.b)
}

@Composable
private fun ActionLine(label: String, onClick: () -> Unit) {
    val tokens = LocalGlassTokens.current
    Text(
        label,
        color = tokens.danger,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        fontSize = 15.sp,
    )
}
