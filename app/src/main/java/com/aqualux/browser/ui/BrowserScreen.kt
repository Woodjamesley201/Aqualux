package com.aqualux.browser.ui

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aqualux.browser.browser.BrowserViewModel
import com.aqualux.browser.model.Sheet
import com.aqualux.browser.model.TabSnapshot
import com.aqualux.browser.ui.bookmarks.BookmarksPage
import com.aqualux.browser.ui.chrome.BrowserChrome
import com.aqualux.browser.ui.chrome.BrowserMenu
import com.aqualux.browser.ui.error.ErrorOverlay
import com.aqualux.browser.ui.glass.AquaMark
import com.aqualux.browser.ui.glass.GlassToast
import com.aqualux.browser.ui.settings.SettingsHost
import com.aqualux.browser.ui.start.StartPage
import com.aqualux.browser.ui.tabs.TabSwitcher
import com.aqualux.browser.ui.theme.AqualuxTheme
import com.aqualux.browser.ui.theme.LocalGlassTokens

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AqualuxTheme(settings) {
        val tokens = LocalGlassTokens.current
        val view = LocalView.current
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !tokens.dark
            controller.isAppearanceLightNavigationBars = !tokens.dark
            viewModel.setPageBackground(tokens.pageBottom.toArgb())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(tokens.pageTop, tokens.pageBottom))),
        ) {
            if (!state.ready) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AquaMark(size = 96.dp)
                }
            } else if (!state.webViewAvailable) {
                StartPage(
                    bookmarks = state.bookmarks,
                    history = state.history,
                    onSearch = {},
                    onOpen = {},
                    message = "Android System WebView isn't available on this device.",
                )
            } else {
                val active = state.activeTab
                var chromeHeight by remember { mutableIntStateOf(0) }
                val showPage = active != null && active.hasDocument &&
                    state.sheet != Sheet.Tabs &&
                    state.sheet != Sheet.Settings &&
                    state.sheet != Sheet.Bookmarks
                if (showPage && active != null) {
                    val top = if (state.fullscreen) 0.dp else with(LocalDensity.current) { chromeHeight.toDp() }
                    TabSurface(active, viewModel, Modifier.padding(top = top).fillMaxSize())
                }
                if (active != null && !active.hasDocument && state.sheet == Sheet.None) {
                    StartPage(
                        bookmarks = state.bookmarks,
                        history = state.history,
                        onSearch = { viewModel.beginEditing("") },
                        onOpen = { url -> viewModel.navigate(active.id, url) },
                    )
                }
                active?.error?.takeIf { state.sheet == Sheet.None || state.sheet == Sheet.Menu }?.let { error ->
                    ErrorOverlay(error = error, url = active.url, onRetry = viewModel::retry)
                }
                if (state.sheet != Sheet.Tabs && state.sheet != Sheet.Settings && state.sheet != Sheet.Bookmarks && !state.fullscreen) {
                    BrowserChrome(
                        state = state,
                        onEdit = { viewModel.beginEditing() },
                        onDraft = viewModel::onDraft,
                        onSubmit = viewModel::submitOmnibox,
                        onStopEditing = viewModel::stopEditing,
                        onSuggestion = viewModel::acceptSuggestion,
                        onBack = viewModel::goBack,
                        onForward = viewModel::goForward,
                        onReload = viewModel::reloadOrStop,
                        onTabs = { viewModel.openSheet(Sheet.Tabs) },
                        onMenu = { viewModel.openSheet(Sheet.Menu) },
                        onNewTab = { viewModel.newTab() },
                        onFindQuery = viewModel::setFindQuery,
                        onFindNext = viewModel::findNext,
                        onCloseFind = viewModel::closeFind,
                        onSelectTab = viewModel::selectTab,
                        onCloseTab = viewModel::closeTab,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .onSizeChanged { chromeHeight = it.height },
                    )
                }
                if (state.sheet == Sheet.Menu && active != null) {
                    BrowserMenu(
                        bookmarked = state.isBookmarked(active.url),
                        desktop = active.desktop,
                        onDismiss = viewModel::closeSheet,
                        onNewTab = { viewModel.closeSheet(); viewModel.newTab() },
                        onDuplicate = { viewModel.closeSheet(); viewModel.duplicateTab(active.id) },
                        onFind = { viewModel.closeSheet(); viewModel.openFind() },
                        onShare = { viewModel.closeSheet(); viewModel.shareActive() },
                        onBookmark = viewModel::toggleBookmark,
                        onBookmarks = { viewModel.openSheet(Sheet.Bookmarks) },
                        onDownloads = { viewModel.closeSheet(); viewModel.openDownloads() },
                        onDesktop = viewModel::toggleDesktop,
                        onSettings = { viewModel.openSheet(Sheet.Settings) },
                    )
                }
                when (state.sheet) {
                    Sheet.Tabs -> TabSwitcher(
                        tabs = state.tabs,
                        activeId = state.activeTabId,
                        onSelect = viewModel::selectTab,
                        onClose = viewModel::closeTab,
                        onNewTab = { viewModel.newTab() },
                        onPin = viewModel::setPinned,
                        onDuplicate = viewModel::duplicateTab,
                        onMove = viewModel::moveTab,
                        onDone = viewModel::closeSheet,
                    )
                    Sheet.Settings -> SettingsHost(
                        settings = settings,
                        onChange = { updated -> viewModel.updateSettings { updated } },
                        onTextZoom = viewModel::setTextZoom,
                        onClearHistory = viewModel::clearHistory,
                        onClearCookies = viewModel::clearCookies,
                        onClearSiteData = viewModel::clearSiteData,
                        onBack = viewModel::closeSheet,
                    )
                    Sheet.Bookmarks -> BookmarksPage(
                        bookmarks = state.bookmarks,
                        onOpen = viewModel::openBookmark,
                        onDelete = viewModel::deleteBookmark,
                        onBack = viewModel::closeSheet,
                    )
                    else -> Unit
                }
            }
            state.message?.let { message ->
                Popup(
                    alignment = Alignment.BottomCenter,
                    properties = PopupProperties(focusable = false),
                ) {
                    GlassToast(message = message, modifier = Modifier.padding(bottom = 28.dp))
                }
            }
        }
    }
}

@Composable
private fun TabSurface(tab: TabSnapshot, viewModel: BrowserViewModel, modifier: Modifier) {
    var webView by remember(tab.id) { mutableStateOf(viewModel.peekWebView(tab.id)) }
    LaunchedEffect(tab.id, tab.url, tab.hasDocument) {
        webView = viewModel.ensureWebView(tab.id)
    }
    val current = webView ?: return
    AndroidView(
        modifier = modifier,
        factory = { context -> FrameLayout(context) },
        update = { frame ->
            if (frame.getChildAt(0) !== current) {
                (current.parent as? ViewGroup)?.removeView(current)
                frame.removeAllViews()
                frame.addView(
                    current,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        onRelease = { frame ->
            if (frame.getChildAt(0) === current) frame.removeAllViews()
        },
    )
}
