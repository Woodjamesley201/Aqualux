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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aqualux.browser.browser.BrowserViewModel
import com.aqualux.browser.model.Sheet
import com.aqualux.browser.model.TabSnapshot
import com.aqualux.browser.ui.bookmarks.BookmarksPage
import com.aqualux.browser.ui.chrome.BrowserChrome
import com.aqualux.browser.ui.error.ErrorOverlay
import com.aqualux.browser.ui.glass.AquaMark
import com.aqualux.browser.ui.glass.GlassToast
import com.aqualux.browser.ui.settings.SettingsPage
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
                if (active != null && active.hasDocument && state.sheet != Sheet.Tabs && state.sheet != Sheet.Settings) {
                    TabSurface(active, viewModel)
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
                        textZoom = settings.textZoom,
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
                        onCloseMenu = viewModel::closeSheet,
                        onNewTab = { viewModel.newTab() },
                        onDuplicate = { state.activeTabId?.let(viewModel::duplicateTab) },
                        onFind = viewModel::openFind,
                        onFindQuery = viewModel::setFindQuery,
                        onFindNext = viewModel::findNext,
                        onCloseFind = viewModel::closeFind,
                        onShare = viewModel::shareActive,
                        onBookmark = viewModel::toggleBookmark,
                        onBookmarks = { viewModel.openSheet(Sheet.Bookmarks) },
                        onDownloads = viewModel::openDownloads,
                        onDesktop = viewModel::toggleDesktop,
                        onTextZoom = viewModel::setTextZoom,
                        onSettings = { viewModel.openSheet(Sheet.Settings) },
                        onSelectTab = viewModel::selectTab,
                        onCloseTab = viewModel::closeTab,
                        modifier = Modifier.align(Alignment.TopCenter),
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
                    Sheet.Settings -> SettingsPage(
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
                GlassToast(
                    message = message,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun TabSurface(tab: TabSnapshot, viewModel: BrowserViewModel) {
    var webView by remember(tab.id) { mutableStateOf(viewModel.peekWebView(tab.id)) }
    LaunchedEffect(tab.id, tab.url, tab.hasDocument) {
        webView = viewModel.ensureWebView(tab.id)
    }
    val current = webView ?: return
    AndroidView(
        modifier = Modifier.fillMaxSize(),
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
