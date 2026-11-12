package com.aqualux.browser.ui.chrome

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqualux.browser.model.BrowserUiState
import com.aqualux.browser.model.Sheet
import com.aqualux.browser.model.TabSnapshot
import com.aqualux.browser.omnibox.OmniboxResolver
import com.aqualux.browser.omnibox.Suggestion
import com.aqualux.browser.omnibox.SuggestionSource
import com.aqualux.browser.ui.glass.GlassIconButton
import com.aqualux.browser.ui.glass.GlassSurface
import com.aqualux.browser.ui.icons.BrowserIcons
import com.aqualux.browser.ui.theme.LocalGlassTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BrowserChrome(
    state: BrowserUiState,
    onEdit: () -> Unit,
    onDraft: (String) -> Unit,
    onSubmit: () -> Unit,
    onStopEditing: () -> Unit,
    onSuggestion: (Suggestion) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    onNewTab: () -> Unit,
    onFindQuery: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onCloseFind: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGlassTokens.current
    val density = LocalDensity.current
    val tab = state.activeTab
    val editing = state.omnibox.editing
    val imeOpen = WindowInsets.ime.getBottom(density) > 0
    val expanded = editing || state.findOpen || imeOpen || !state.chromeCollapsed || state.sheet == Sheet.Menu
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = tokens.shelfMargin)
            .padding(top = tokens.shelfMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!expanded && tab != null) {
            CollapsedPill(url = tab.url, loading = tab.isLoading, onClick = onEdit)
        } else {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(tokens.shelfRadius),
                contentPadding = PaddingValues(tokens.shelfPad),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.rowGap)) {
                    TabStrip(
                        tabs = state.tabs,
                        activeId = state.activeTabId,
                        onSelect = onSelectTab,
                        onClose = onCloseTab,
                        onNew = onNewTab,
                    )
                    Toolbar(
                        state = state,
                        onEdit = onEdit,
                        onDraft = onDraft,
                        onSubmit = onSubmit,
                        onStopEditing = onStopEditing,
                        onBack = onBack,
                        onForward = onForward,
                        onReload = onReload,
                        onTabs = onTabs,
                        onMenu = onMenu,
                    )
                }
            }
        }
        if (state.suggestions.isNotEmpty() && expanded) {
            SuggestionList(state.suggestions, onSuggestion, Modifier.padding(top = 4.dp))
        }
        if (state.findOpen) {
            Spacer(Modifier.height(8.dp))
            FindRow(state.findQuery, state.findActive, state.findCount, onFindQuery, onFindNext, onCloseFind)
        }
    }
}

@Composable
private fun CollapsedPill(url: String, loading: Boolean, onClick: () -> Unit) {
    val tokens = LocalGlassTokens.current
    GlassSurface(
        modifier = Modifier
            .widthIn(max = tokens.collapsedPillMax)
            .height(LiquidCollapsedHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (url.startsWith("https://")) {
                Icon(Icons.Filled.Lock, "Secure", tint = tokens.muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                OmniboxResolver.displayLabel(url).ifBlank { "Aqualux" },
                color = tokens.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (loading) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .width(36.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(tokens.accent),
                )
            }
        }
    }
}

private val LiquidCollapsedHeight = 28.dp

@Composable
private fun TabStrip(
    tabs: List<TabSnapshot>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNew: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val pinned = tabs.filter { it.pinned }
    val rest = tabs.filterNot { it.pinned }
    Row(
        Modifier.fillMaxWidth().height(tokens.tabRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pinned.forEach { tab ->
                TabChip(tab, tab.id == activeId, pinned = true, onSelect = { onSelect(tab.id) }, onClose = { onClose(tab.id) })
                Spacer(Modifier.width(2.dp))
            }
            if (pinned.isNotEmpty() && rest.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(16.dp)
                        .background(tokens.hairlineOuter),
                )
            }
            rest.forEach { tab ->
                TabChip(tab, tab.id == activeId, pinned = false, onSelect = { onSelect(tab.id) }, onClose = { onClose(tab.id) })
                Spacer(Modifier.width(2.dp))
            }
        }
        GlassIconButton(Icons.Filled.Add, "New tab", onNew, size = tokens.buttonSize)
    }
}

@Composable
private fun TabChip(
    tab: TabSnapshot,
    active: Boolean,
    pinned: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val favicon by produceState<ImageBitmap?>(null, tab.faviconFile) {
        value = withContext(Dispatchers.IO) {
            val path = tab.faviconFile ?: return@withContext null
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    val shape = RoundedCornerShape(tokens.controlRadius)
    Row(
        Modifier
            .height(tokens.tabHeight)
            .then(
                if (pinned) {
                    Modifier.width(tokens.tabPinnedWidth)
                } else {
                    Modifier.widthIn(
                        min = if (active) tokens.tabActiveMinWidth else tokens.tabMinWidth,
                        max = tokens.tabMaxWidth,
                    )
                },
            )
            .clip(shape)
            .background(
                if (active) {
                    Brush.verticalGradient(listOf(tokens.activeTabTop, tokens.activeTabBottom))
                } else {
                    Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Transparent))
                },
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = if (pinned) 0.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (pinned) Arrangement.Center else Arrangement.Start,
    ) {
        if (favicon != null) {
            Image(favicon!!, null, Modifier.size(16.dp))
        } else {
            Text(
                tab.title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•",
                color = tokens.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!pinned) {
            Text(
                tab.title.ifBlank { "New tab" },
                color = if (active) tokens.ink else tokens.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f, fill = false)
                    .graphicsLayer { alpha = if (tab.unloaded) 0.6f else 1f },
            )
            if (active) {
                Box(
                    Modifier
                        .padding(start = 2.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Close tab", tint = tokens.muted, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun Toolbar(
    state: BrowserUiState,
    onEdit: () -> Unit,
    onDraft: (String) -> Unit,
    onSubmit: () -> Unit,
    onStopEditing: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    val tab = state.activeTab
    Row(
        Modifier.fillMaxWidth().height(tokens.toolbarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, enabled = tab?.canGoBack == true, size = tokens.buttonSize)
        GlassIconButton(Icons.AutoMirrored.Filled.ArrowForward, "Forward", onForward, enabled = tab?.canGoForward == true, size = tokens.buttonSize)
        GlassIconButton(
            if (tab?.isLoading == true) Icons.Filled.Close else Icons.Filled.Refresh,
            if (tab?.isLoading == true) "Stop" else "Reload",
            onReload,
            enabled = tab?.isLoading == true || !tab?.url.isNullOrBlank(),
            size = tokens.buttonSize,
        )
        Omnibox(
            editing = state.omnibox.editing,
            draft = state.omnibox.draft,
            url = tab?.url.orEmpty(),
            loading = tab?.isLoading == true,
            progress = tab?.progress ?: 0f,
            onEdit = onEdit,
            onDraft = onDraft,
            onSubmit = onSubmit,
            onStopEditing = onStopEditing,
            onReload = onReload,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )
        TabCountButton(state.tabs.size, onTabs)
        GlassIconButton(Icons.Filled.MoreVert, "Menu", onMenu, size = tokens.buttonSize)
    }
}

@Composable
private fun TabCountButton(count: Int, onClick: () -> Unit) {
    val tokens = LocalGlassTokens.current
    Box(
        Modifier
            .size(tokens.buttonSize)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            count.coerceAtMost(99).toString(),
            color = tokens.ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .border(1.5.dp, tokens.ink, RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun Omnibox(
    editing: Boolean,
    draft: String,
    url: String,
    loading: Boolean,
    progress: Float,
    onEdit: () -> Unit,
    onDraft: (String) -> Unit,
    onSubmit: () -> Unit,
    onStopEditing: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGlassTokens.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    LaunchedEffect(editing) {
        if (editing) {
            field = TextFieldValue(draft, TextRange(0, draft.length))
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }
    val shape = RoundedCornerShape(999.dp)
    val fill = if (editing) {
        Brush.verticalGradient(listOf(tokens.popoverTop, tokens.popoverMid, tokens.popoverBottom))
    } else {
        Brush.verticalGradient(listOf(tokens.controlTop, tokens.controlBottom))
    }
    Box(
        modifier
            .height(tokens.omniboxHeight)
            .clip(shape)
            .background(fill)
            .then(
                if (editing) {
                    Modifier.background(tokens.focusHalo.copy(alpha = 0.0f))
                } else {
                    Modifier
                },
            )
            .clickable(enabled = !editing, onClick = onEdit)
            .padding(horizontal = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val secure = when {
                url.startsWith("https://") -> true
                url.startsWith("http://") -> false
                else -> null
            }
            if (secure != null && !editing) {
                Icon(
                    if (secure) Icons.Filled.Lock else BrowserIcons.LockOpen,
                    if (secure) "Secure" else "Not secure",
                    tint = if (secure) tokens.muted else tokens.warning,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Box(Modifier.weight(1f)) {
                if (editing) {
                    BasicTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            onDraft(it.text)
                        },
                        textStyle = TextStyle(color = tokens.ink, fontSize = 13.sp),
                        cursorBrush = SolidColor(tokens.accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                            autoCorrectEnabled = false,
                        ),
                        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                } else {
                    val host = OmniboxResolver.displayLabel(url)
                    Text(
                        text = host.ifBlank { "Search or type a URL" },
                        color = if (host.isBlank()) tokens.faint else tokens.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                    )
                }
            }
            if (editing) {
                GlassIconButton(Icons.Filled.Close, "Clear", onStopEditing, size = 28.dp)
            }
        }
        if (loading && progress in 0.02f..0.99f) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(listOf(tokens.accent, tokens.accentStrong)),
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
private fun SuggestionList(suggestions: List<Suggestion>, onSuggestion: (Suggestion) -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalGlassTokens.current
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.dropdownRadius),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            suggestions.forEach { suggestion ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(tokens.suggestionRow)
                        .clip(RoundedCornerShape(tokens.dropdownRadius - 6.dp))
                        .clickable { onSuggestion(suggestion) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (suggestion.source) {
                            SuggestionSource.TAB -> BrowserIcons.Tabs
                            SuggestionSource.BOOKMARK -> Icons.Filled.Star
                            SuggestionSource.HISTORY -> Icons.Filled.Search
                        },
                        null,
                        tint = tokens.muted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(suggestion.title.ifBlank { suggestion.url }, color = tokens.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(OmniboxResolver.displayLabel(suggestion.url), color = tokens.faint, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    }
                    Text(
                        when (suggestion.source) {
                            SuggestionSource.TAB -> "Tab"
                            SuggestionSource.BOOKMARK -> "Bookmark"
                            SuggestionSource.HISTORY -> "History"
                        },
                        color = tokens.muted,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(tokens.hoverFill)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FindRow(
    query: String,
    active: Int,
    count: Int,
    onQuery: (String) -> Unit,
    onNext: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val tokens = LocalGlassTokens.current
    GlassSurface(
        modifier = Modifier.fillMaxWidth().height(if (tokens.omniboxHeight < 34.dp) 34.dp else 40.dp),
        shape = RoundedCornerShape(999.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = tokens.ink, fontSize = 14.sp),
                cursorBrush = SolidColor(tokens.accent),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("Find in page", color = tokens.faint, fontSize = 14.sp)
                        inner()
                    }
                },
            )
            Text(
                if (query.isEmpty()) "" else "$active/$count",
                color = if (query.isNotEmpty() && count == 0) tokens.danger else tokens.muted,
                fontSize = 12.sp,
            )
            GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Previous match", { onNext(false) }, size = 32.dp)
            GlassIconButton(Icons.AutoMirrored.Filled.ArrowForward, "Next match", { onNext(true) }, size = 32.dp)
            GlassIconButton(Icons.Filled.Close, "Close find", onClose, size = 32.dp)
        }
    }
}
