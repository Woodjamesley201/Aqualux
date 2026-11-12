package com.aqualux.browser.model

import com.aqualux.browser.errors.PageError
import com.aqualux.browser.omnibox.Suggestion
import com.aqualux.browser.session.Bookmark
import com.aqualux.browser.session.HistoryEntry

enum class Sheet { None, Tabs, Settings, Bookmarks, Menu }

data class OmniboxUi(
    val editing: Boolean = false,
    val draft: String = "",
)

data class TabSnapshot(
    val id: String,
    val url: String,
    val title: String,
    val faviconFile: String?,
    val progress: Float,
    val isLoading: Boolean,
    val pinned: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val desktop: Boolean,
    val unloaded: Boolean,
    val error: PageError?,
    val lastActiveAt: Long,
    val hasDocument: Boolean,
)

data class BrowserUiState(
    val ready: Boolean = false,
    val webViewAvailable: Boolean = true,
    val tabs: List<TabSnapshot> = emptyList(),
    val activeTabId: String? = null,
    val chromeCollapsed: Boolean = false,
    val sheet: Sheet = Sheet.None,
    val omnibox: OmniboxUi = OmniboxUi(),
    val suggestions: List<Suggestion> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val findOpen: Boolean = false,
    val findQuery: String = "",
    val findActive: Int = 0,
    val findCount: Int = 0,
    val fullscreen: Boolean = false,
    val message: String? = null,
) {
    val activeTab: TabSnapshot?
        get() = tabs.firstOrNull { it.id == activeTabId }

    fun isBookmarked(url: String): Boolean = url.isNotBlank() && bookmarks.any { it.url == url }
}
