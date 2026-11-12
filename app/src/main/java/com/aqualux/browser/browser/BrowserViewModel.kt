package com.aqualux.browser.browser

import android.app.Application
import android.app.DownloadManager
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Message
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aqualux.browser.BrowserHost
import com.aqualux.browser.data.BrowserRepository
import com.aqualux.browser.data.SettingsRepository
import com.aqualux.browser.errors.ErrorKind
import com.aqualux.browser.errors.ErrorTaxonomy
import com.aqualux.browser.errors.PageError
import com.aqualux.browser.model.BrowserUiState
import com.aqualux.browser.model.OmniboxUi
import com.aqualux.browser.model.Sheet
import com.aqualux.browser.model.TabSnapshot
import com.aqualux.browser.model.ThemeMode
import com.aqualux.browser.model.UserSettings
import com.aqualux.browser.omnibox.NavigationTarget
import com.aqualux.browser.omnibox.OmniboxResolver
import com.aqualux.browser.omnibox.Suggestion
import com.aqualux.browser.omnibox.SuggestionRanker
import com.aqualux.browser.omnibox.SuggestionSource
import com.aqualux.browser.session.Bookmark
import com.aqualux.browser.session.PersistedSession
import com.aqualux.browser.session.PersistedTab
import com.aqualux.browser.web.AqualuxChromeClient
import com.aqualux.browser.web.AqualuxWebViewClient
import com.aqualux.browser.web.DownloadNames
import com.aqualux.browser.web.WebEvents
import com.aqualux.browser.web.WebViewConfigurator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID

class BrowserViewModel(app: Application) : AndroidViewModel(app), WebEvents {
    private val repository = BrowserRepository(app)
    private val settingsRepository = SettingsRepository(app)

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _ui = MutableStateFlow(BrowserUiState())
    val ui: StateFlow<BrowserUiState> = _ui.asStateFlow()

    private val webViews = LinkedHashMap<String, WebView>()
    private val scrollPositions = HashMap<String, Pair<Int, Int>>()
    private val pendingScroll = HashMap<String, Pair<Int, Int>>()
    private var mobileUserAgent: String? = null
    private var pageBackground: Int = 0xFFF4FBFC.toInt()
    private var host: BrowserHost? = null
    private var hostResumed = false
    private var pendingIntent: Intent? = null
    private var persistJob: Job? = null
    private var messageJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { value ->
                _settings.value = value
                applyRuntimeSettings(value)
            }
        }
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) { repository.readSession() }
            val history = withContext(Dispatchers.IO) { repository.readHistory() }
            val bookmarks = withContext(Dispatchers.IO) { repository.readBookmarks() }
            restore(session, history, bookmarks)
        }
        viewModelScope.launch {
            while (isActive) {
                delay(20_000)
                unloadIdle()
            }
        }
    }

    fun attachHost(next: BrowserHost) {
        host = next
    }

    fun detachHost(current: BrowserHost) {
        if (host === current) host = null
    }

    fun onHostResume() {
        hostResumed = true
        resumeActive()
    }

    fun onHostPause() {
        hostResumed = false
        webViews.values.forEach { it.onPause() }
        persistNow()
    }

    fun setPageBackground(color: Int) {
        if (pageBackground == color) return
        pageBackground = color
        webViews.values.forEach { it.setBackgroundColor(color) }
    }

    fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_MAIN && intent.data == null && intent.type == null) return
        if (!_ui.value.ready) {
            pendingIntent = intent
            return
        }
        openIntent(intent)
    }

    fun peekWebView(tabId: String): WebView? = webViews[tabId]

    fun ensureWebView(tabId: String): WebView? {
        webViews[tabId]?.let { return it }
        val tab = find(tabId) ?: return null
        if (!tab.hasDocument || tab.url.isBlank()) return null
        return createWebView(tabId, loadUrl = tab.url)
    }

    fun handleBack(): Boolean {
        if (_ui.value.fullscreen) {
            host?.hideFullscreen()
            _ui.update { it.copy(fullscreen = false) }
            return true
        }
        if (_ui.value.findOpen) {
            closeFind()
            return true
        }
        if (_ui.value.sheet != Sheet.None) {
            closeSheet()
            return true
        }
        if (_ui.value.omnibox.editing) {
            stopEditing()
            return true
        }
        val id = _ui.value.activeTabId
        val webView = id?.let { webViews[it] }
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    fun beginEditing(initial: String? = null) {
        val draft = initial ?: active()?.url.orEmpty()
        _ui.update {
            it.copy(
                omnibox = OmniboxUi(editing = true, draft = draft),
                chromeCollapsed = false,
            )
        }
        refreshSuggestions()
    }

    fun onDraft(text: String) {
        _ui.update {
            it.copy(
                omnibox = it.omnibox.copy(editing = true, draft = text),
                chromeCollapsed = false,
            )
        }
        refreshSuggestions()
    }

    fun stopEditing() {
        _ui.update { it.copy(omnibox = OmniboxUi(), suggestions = emptyList()) }
    }

    fun submitOmnibox() {
        val draft = _ui.value.omnibox.draft
        val id = _ui.value.activeTabId ?: return
        when (val target = OmniboxResolver.resolve(draft, _settings.value.searchEngine)) {
            null -> Unit
            is NavigationTarget.Web -> navigate(id, target.url)
            is NavigationTarget.Search -> navigate(id, target.url)
            is NavigationTarget.Blocked -> showMessage(target.reason)
        }
        stopEditing()
    }

    fun acceptSuggestion(suggestion: Suggestion) {
        if (suggestion.source == SuggestionSource.TAB &&
            suggestion.tabId != null &&
            _ui.value.tabs.any { it.id == suggestion.tabId }
        ) {
            selectTab(suggestion.tabId)
        } else {
            val id = _ui.value.activeTabId ?: return
            navigate(id, suggestion.url)
        }
        stopEditing()
    }

    fun navigate(tabId: String, url: String) {
        mutate(tabId) {
            it.copy(
                url = url,
                error = null,
                isLoading = true,
                progress = 0.08f,
                unloaded = false,
                hasDocument = true,
            )
        }
        val existing = webViews[tabId]
        if (existing != null) {
            existing.loadUrl(url)
        }
        _ui.update { it.copy(chromeCollapsed = false) }
        schedulePersist()
    }

    fun retry() {
        val tab = active() ?: return
        if (tab.url.isBlank()) return
        pendingScroll.remove(tab.id)
        mutate(tab.id) { it.copy(error = null, isLoading = true, progress = 0.08f, hasDocument = true) }
        val existing = webViews[tab.id]
        if (existing != null) existing.loadUrl(tab.url) else ensureWebView(tab.id)
    }

    fun goBack() {
        val id = _ui.value.activeTabId ?: return
        webViews[id]?.takeIf { it.canGoBack() }?.goBack()
    }

    fun goForward() {
        val id = _ui.value.activeTabId ?: return
        webViews[id]?.takeIf { it.canGoForward() }?.goForward()
    }

    fun reloadOrStop() {
        val tab = active() ?: return
        val webView = webViews[tab.id]
        if (tab.isLoading && webView != null) {
            webView.stopLoading()
            mutate(tab.id) { it.copy(isLoading = false) }
            return
        }
        if (tab.url.isBlank()) return
        pendingScroll.remove(tab.id)
        mutate(tab.id) { it.copy(error = null, isLoading = true, hasDocument = true) }
        if (webView != null) webView.reload() else ensureWebView(tab.id)
    }

    fun newTab(url: String? = null, select: Boolean = true): String? {
        if (_ui.value.tabs.size >= MAX_TABS) {
            showMessage("Close a tab before opening another")
            return null
        }
        val id = UUID.randomUUID().toString()
        val tab = blankTab(id).copy(
            url = url.orEmpty(),
            hasDocument = !url.isNullOrBlank(),
        )
        _ui.update { state ->
            state.copy(
                tabs = state.tabs + tab,
                activeTabId = if (select) id else state.activeTabId,
                sheet = if (select) Sheet.None else state.sheet,
                chromeCollapsed = false,
                omnibox = if (select) OmniboxUi() else state.omnibox,
            )
        }
        schedulePersist()
        resumeActive()
        return id
    }

    fun closeTab(id: String) {
        val state = _ui.value
        val index = state.tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        destroyWebView(id)
        pendingScroll.remove(id)
        scrollPositions.remove(id)
        val remaining = state.tabs.filterNot { it.id == id }
        val nextTabs = remaining.ifEmpty { listOf(blankTab()) }
        val nextActive = when {
            state.activeTabId != id -> state.activeTabId?.takeIf { active -> nextTabs.any { it.id == active } }
            else -> null
        } ?: nextTabs.getOrNull(index.coerceAtMost(nextTabs.lastIndex))?.id ?: nextTabs.first().id
        _ui.update {
            it.copy(
                tabs = nextTabs,
                activeTabId = nextActive,
                sheet = if (remaining.isEmpty()) Sheet.None else it.sheet,
            )
        }
        schedulePersist()
        resumeActive()
    }

    fun duplicateTab(id: String) {
        val source = find(id) ?: return
        val created = newTab(select = true) ?: return
        if (source.desktop) mutate(created) { it.copy(desktop = true) }
        if (source.url.isNotBlank()) {
            currentScroll(id).takeIf { it.first != 0 || it.second != 0 }?.let { pendingScroll[created] = it }
            navigate(created, source.url)
        }
    }

    fun setPinned(id: String, pinned: Boolean) {
        val tabs = _ui.value.tabs
        val tab = tabs.find { it.id == id } ?: return
        val updated = tab.copy(pinned = pinned)
        val others = tabs.filterNot { it.id == id }
        val next = others.filter { it.pinned } + updated + others.filterNot { it.pinned }
        _ui.update { it.copy(tabs = next) }
        schedulePersist()
    }

    fun moveTab(id: String, targetId: String) {
        if (id == targetId) return
        val tabs = _ui.value.tabs.toMutableList()
        val from = tabs.indexOfFirst { it.id == id }
        val to = tabs.indexOfFirst { it.id == targetId }
        if (from < 0 || to < 0) return
        if (tabs[from].pinned != tabs[to].pinned) return
        tabs.add(to, tabs.removeAt(from))
        _ui.update { it.copy(tabs = tabs) }
        schedulePersist()
    }

    fun selectTab(id: String) {
        if (_ui.value.tabs.none { it.id == id }) return
        val now = System.currentTimeMillis()
        val previous = _ui.value.activeTabId
        _ui.update { state ->
            state.copy(
                activeTabId = id,
                chromeCollapsed = false,
                sheet = Sheet.None,
                omnibox = OmniboxUi(),
                suggestions = emptyList(),
                tabs = state.tabs.map { tab ->
                    if (tab.id == id || tab.id == previous) tab.copy(lastActiveAt = now) else tab
                },
            )
        }
        resumeActive()
        schedulePersist()
    }

    fun openSheet(sheet: Sheet) {
        _ui.update { it.copy(sheet = sheet, chromeCollapsed = false) }
    }

    fun closeSheet() {
        _ui.update { it.copy(sheet = Sheet.None) }
        resumeActive()
    }

    fun shareActive() {
        val tab = active() ?: return
        if (!tab.url.startsWith("http")) {
            showMessage("Nothing to share yet")
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, tab.url)
            putExtra(Intent.EXTRA_SUBJECT, tab.title)
        }
        if (host?.startActivitySafe(Intent.createChooser(send, "Share")) != true) {
            showMessage("Nothing can share this")
        }
    }

    fun toggleBookmark() {
        val tab = active() ?: return
        if (!tab.url.startsWith("http")) {
            showMessage("Nothing to bookmark yet")
            return
        }
        val existing = _ui.value.bookmarks.find { it.url == tab.url }
        val next = if (existing != null) {
            showMessage("Bookmark removed")
            _ui.value.bookmarks.filterNot { it.id == existing.id }
        } else {
            showMessage("Bookmarked")
            listOf(
                Bookmark(
                    id = UUID.randomUUID().toString(),
                    url = tab.url,
                    title = tab.title.ifBlank { tab.url },
                    createdAt = System.currentTimeMillis(),
                ),
            ) + _ui.value.bookmarks
        }
        _ui.update { it.copy(bookmarks = next) }
        schedulePersist()
    }

    fun deleteBookmark(id: String) {
        _ui.update { state -> state.copy(bookmarks = state.bookmarks.filterNot { it.id == id }) }
        schedulePersist()
    }

    fun openBookmark(url: String) {
        val id = _ui.value.activeTabId ?: return
        navigate(id, url)
        closeSheet()
    }

    fun clearHistory() {
        _ui.update { it.copy(history = emptyList()) }
        schedulePersist()
        showMessage("History cleared")
    }

    fun clearCookies() {
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
        }
        showMessage("Cookies cleared")
    }

    fun clearSiteData() {
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
        showMessage("Site data cleared")
    }

    fun openDownloads() {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        if (host?.startActivitySafe(intent) != true) showMessage("Downloads aren't available")
    }

    fun openFind() {
        _ui.update { it.copy(findOpen = true, sheet = Sheet.None, chromeCollapsed = false) }
    }

    fun setFindQuery(query: String) {
        _ui.update { it.copy(findQuery = query, findOpen = true, chromeCollapsed = false) }
        val webView = activeWebView()
        if (query.isBlank()) {
            webView?.clearMatches()
            _ui.update { it.copy(findActive = 0, findCount = 0) }
        } else {
            webView?.findAllAsync(query)
        }
    }

    fun findNext(forward: Boolean) {
        activeWebView()?.findNext(forward)
    }

    fun closeFind() {
        activeWebView()?.clearMatches()
        _ui.update { it.copy(findOpen = false, findQuery = "", findActive = 0, findCount = 0) }
    }

    fun setTextZoom(zoom: Int) {
        val value = zoom.coerceIn(50, 200)
        webViews.values.forEach { it.settings.textZoom = value }
        updateSettings { it.copy(textZoom = value) }
    }

    fun toggleDesktop() {
        val tab = active() ?: return
        val next = !tab.desktop
        mutate(tab.id) { it.copy(desktop = next) }
        val webView = webViews[tab.id]
        if (webView != null) {
            WebViewConfigurator.configure(
                webView,
                _settings.value,
                isDarkTheme(_settings.value),
                mobileUserAgent,
                next,
            )
            webView.setBackgroundColor(pageBackground)
            if (tab.url.isNotBlank()) webView.reload()
        }
        schedulePersist()
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    override fun onPageStarted(tabId: String, url: String) {
        if (url == "about:blank") return
        mutate(tabId) {
            it.copy(
                url = url,
                isLoading = true,
                progress = maxOf(it.progress, 0.08f),
                error = null,
                hasDocument = true,
                unloaded = false,
            )
        }
    }

    override fun onPageFinished(tabId: String, url: String) {
        val webView = webViews[tabId]
        val title = webView?.title?.takeIf { it.isNotBlank() }
        mutate(tabId) {
            it.copy(
                url = url.ifBlank { it.url },
                isLoading = false,
                progress = 1f,
                canGoBack = webView?.canGoBack() == true,
                canGoForward = webView?.canGoForward() == true,
                title = title ?: it.title,
                unloaded = false,
            )
        }
        applyPendingScroll(tabId)
        val tab = find(tabId)
        if (tab?.error == null && url.startsWith("http")) {
            recordHistory(url, title ?: tab?.title.orEmpty())
        }
        schedulePersist()
    }

    override fun onVisited(tabId: String, url: String, canGoBack: Boolean, canGoForward: Boolean) {
        mutate(tabId) {
            it.copy(
                url = if (url.isBlank() || url == "about:blank") it.url else url,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                hasDocument = true,
            )
        }
        if (url.startsWith("http") && find(tabId)?.error == null) {
            recordHistory(url, find(tabId)?.title.orEmpty())
        }
    }

    override fun onProgress(tabId: String, progress: Int) {
        val fraction = (progress / 100f).coerceIn(0f, 1f)
        mutate(tabId) { it.copy(progress = fraction, isLoading = progress < 100) }
    }

    override fun onTitle(tabId: String, title: String) {
        mutate(tabId) { it.copy(title = title) }
        val url = find(tabId)?.url ?: return
        val history = _ui.value.history
        val index = history.indexOfFirst { it.url == url }
        if (index >= 0 && history[index].title != title) {
            val updated = history.toMutableList()
            updated[index] = history[index].copy(title = title)
            _ui.update { it.copy(history = updated) }
        }
    }

    override fun onIcon(tabId: String, icon: Bitmap) {
        val hostName = OmniboxResolver.hostOf(find(tabId)?.url.orEmpty())
        val copy = try {
            icon.copy(icon.config ?: Bitmap.Config.ARGB_8888, false)
        } catch (_: Exception) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val path = try {
                repository.saveFavicon(hostName, copy)
            } finally {
                if (!copy.isRecycled) copy.recycle()
            }
            if (path != null) {
                withContext(Dispatchers.Main) {
                    mutate(tabId) { it.copy(faviconFile = path) }
                    schedulePersist()
                }
            }
        }
    }

    override fun onMainFrameError(tabId: String, error: PageError) {
        if (find(tabId)?.error?.kind == ErrorKind.SSL) return
        mutate(tabId) { it.copy(error = error, isLoading = false, progress = 0f) }
    }

    override fun onSsl(tabId: String, error: PageError) {
        mutate(tabId) { it.copy(error = error, isLoading = false, progress = 0f) }
    }

    override fun onUnsafe(tabId: String, host: String) {
        mutate(tabId) { it.copy(error = ErrorTaxonomy.unsafe(host), isLoading = false, progress = 0f) }
    }

    override fun onScroll(tabId: String, x: Int, y: Int, oldY: Int) {
        scrollPositions[tabId] = x to y
        if (tabId != _ui.value.activeTabId) return
        val state = _ui.value
        if (state.omnibox.editing || state.findOpen || state.sheet != Sheet.None) {
            if (state.chromeCollapsed) _ui.update { it.copy(chromeCollapsed = false) }
            return
        }
        val dy = y - oldY
        val collapsed = when {
            y <= 4 -> false
            dy > 24 -> true
            dy < -16 -> false
            else -> state.chromeCollapsed
        }
        if (collapsed != state.chromeCollapsed) {
            _ui.update { it.copy(chromeCollapsed = collapsed) }
        }
    }

    override fun onRenderGone(tabId: String) {
        destroyWebView(tabId)
        val hostName = OmniboxResolver.hostOf(find(tabId)?.url.orEmpty())
        mutate(tabId) {
            it.copy(
                isLoading = false,
                unloaded = true,
                error = ErrorTaxonomy.renderer(hostName),
            )
        }
    }

    override fun onCreateWindow(resultMsg: Message): Boolean {
        if (_ui.value.tabs.size >= MAX_TABS) {
            showMessage("Close a tab before opening another")
            return false
        }
        val id = UUID.randomUUID().toString()
        val tab = blankTab(id).copy(hasDocument = true, title = "New tab")
        _ui.update {
            it.copy(
                tabs = it.tabs + tab,
                activeTabId = id,
                sheet = Sheet.None,
                chromeCollapsed = false,
                omnibox = OmniboxUi(),
            )
        }
        val webView = try {
            buildWebView(id, desktop = false)
        } catch (_: Throwable) {
            _ui.update { state ->
                state.copy(
                    webViewAvailable = false,
                    tabs = state.tabs.filterNot { it.id == id },
                )
            }
            return false
        }
        webViews[id] = webView
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        transport.webView = webView
        resultMsg.sendToTarget()
        schedulePersist()
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val androidPerms = request.resources.mapNotNull { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> android.Manifest.permission.CAMERA
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> android.Manifest.permission.RECORD_AUDIO
                else -> null
            }
        }.distinct()
        val grantable = request.resources.filter { resource ->
            resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                resource == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
        }.toTypedArray()
        if (grantable.isEmpty()) {
            request.deny()
            return
        }
        if (androidPerms.isEmpty()) {
            request.grant(grantable)
            return
        }
        val current = host
        if (current == null) {
            request.deny()
            return
        }
        current.requestPermissions(androidPerms.toTypedArray()) { result ->
            if (androidPerms.all { result[it] == true }) request.grant(grantable) else request.deny()
        }
    }

    override fun onGeolocation(origin: String, callback: GeolocationPermissions.Callback) {
        val current = host
        if (current == null) {
            callback.invoke(origin, false, false)
            return
        }
        val perms = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        current.requestPermissions(perms) { result ->
            val granted = result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            callback.invoke(origin, granted, false)
            if (!granted) showMessage("Location permission denied")
        }
    }

    override fun onFileChooser(intent: Intent, callback: ValueCallback<Array<Uri>>): Boolean {
        val current = host ?: return false
        current.openFileChooser(intent) { uris -> callback.onReceiveValue(uris) }
        return true
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        val current = host
        if (current == null) {
            callback.onCustomViewHidden()
            return
        }
        current.showFullscreen(view) { callback.onCustomViewHidden() }
        _ui.update { it.copy(fullscreen = true) }
    }

    override fun onHideCustomView() {
        host?.hideFullscreen()
        _ui.update { it.copy(fullscreen = false) }
    }

    override fun onDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
    ) {
        if (!url.startsWith("http://") && !url.startsWith("https://") && !URLUtil.isValidUrl(url)) {
            showMessage("That download can't be started")
            return
        }
        val name = DownloadNames.fileName(contentDisposition, url, mimeType)
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT <= 28 &&
            ContextCompat.checkSelfPermission(app, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            host?.requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) { result ->
                if (result[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
                    enqueueDownload(url, userAgent, mimeType, name)
                } else {
                    showMessage("Storage permission is needed to download")
                }
            } ?: showMessage("Can't download right now")
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            host?.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)) {
                enqueueDownload(url, userAgent, mimeType, name)
            } ?: enqueueDownload(url, userAgent, mimeType, name)
            return
        }
        enqueueDownload(url, userAgent, mimeType, name)
    }

    override fun onExternalUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (host?.startActivitySafe(intent) != true) showMessage("No app can open that link")
    }

    override fun onIntentUri(raw: String) {
        try {
            val parsed = Intent.parseUri(raw, Intent.URI_INTENT_SCHEME)
            parsed.addCategory(Intent.CATEGORY_BROWSABLE)
            parsed.component = null
            parsed.selector = null
            val fallback = parsed.getStringExtra("browser_fallback_url")
            val launched = host?.startActivitySafe(parsed) == true
            if (!launched && !fallback.isNullOrBlank()) {
                val id = _ui.value.activeTabId ?: return
                when (val target = OmniboxResolver.resolve(fallback, _settings.value.searchEngine)) {
                    is NavigationTarget.Web -> navigate(id, target.url)
                    is NavigationTarget.Search -> navigate(id, target.url)
                    else -> showMessage("No app can open that link")
                }
            } else if (!launched) {
                showMessage("No app can open that link")
            }
        } catch (_: Exception) {
            showMessage("Couldn't open that link")
        }
    }

    override fun onCleared() {
        persistJob?.cancel()
        val snapshot = capturePersistence()
        runBlocking(Dispatchers.IO) {
            repository.writeAll(snapshot.session, snapshot.history, snapshot.bookmarks)
        }
        webViews.keys.toList().forEach { destroyWebView(it) }
        super.onCleared()
    }

    private fun restore(
        session: PersistedSession,
        history: List<com.aqualux.browser.session.HistoryEntry>,
        bookmarks: List<Bookmark>,
    ) {
        val restored = session.tabs.map { persisted ->
            if (persisted.scrollX != 0 || persisted.scrollY != 0) {
                pendingScroll[persisted.id] = persisted.scrollX to persisted.scrollY
                scrollPositions[persisted.id] = persisted.scrollX to persisted.scrollY
            }
            TabSnapshot(
                id = persisted.id,
                url = persisted.url,
                title = persisted.title.ifBlank { "New tab" },
                faviconFile = persisted.faviconFile,
                progress = 0f,
                isLoading = false,
                pinned = persisted.pinned,
                canGoBack = false,
                canGoForward = false,
                desktop = persisted.desktop,
                unloaded = persisted.url.isNotBlank(),
                error = null,
                lastActiveAt = System.currentTimeMillis(),
                hasDocument = persisted.url.isNotBlank(),
            )
        }
        val tabs = restored.ifEmpty { listOf(blankTab()) }
        val active = session.activeTabId?.takeIf { id -> tabs.any { it.id == id } } ?: tabs.first().id
        _ui.value = BrowserUiState(
            ready = true,
            tabs = tabs,
            activeTabId = active,
            history = history.take(500),
            bookmarks = bookmarks,
        )
        pendingIntent?.let {
            pendingIntent = null
            openIntent(it)
        }
    }

    private fun openIntent(intent: Intent) {
        val raw = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_WEB_SEARCH -> intent.getStringExtra(SearchManager.QUERY)
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.trim().orEmpty()
        if (raw.isEmpty()) return
        val target = OmniboxResolver.resolve(raw, _settings.value.searchEngine) ?: return
        val url = when (target) {
            is NavigationTarget.Web -> target.url
            is NavigationTarget.Search -> target.url
            is NavigationTarget.Blocked -> {
                showMessage(target.reason)
                return
            }
        }
        val current = active()
        if (current != null && current.url.isBlank() && !current.hasDocument) {
            navigate(current.id, url)
        } else {
            newTab(url, select = true)
        }
    }

    private fun createWebView(tabId: String, loadUrl: String): WebView? {
        val desktop = find(tabId)?.desktop == true
        val webView = try {
            buildWebView(tabId, desktop)
        } catch (_: Throwable) {
            _ui.update { it.copy(webViewAvailable = false) }
            return null
        }
        webViews[tabId] = webView
        webView.loadUrl(loadUrl)
        resumeActive()
        return webView
    }

    private fun buildWebView(tabId: String, desktop: Boolean): WebView {
        val context = getApplication<Application>()
        val webView = WebView(context)
        if (mobileUserAgent == null) {
            mobileUserAgent = webView.settings.userAgentString
        }
        WebViewConfigurator.configure(
            webView,
            _settings.value,
            isDarkTheme(_settings.value),
            mobileUserAgent,
            desktop,
        )
        webView.setBackgroundColor(pageBackground)
        webView.webViewClient = AqualuxWebViewClient(tabId, this)
        webView.webChromeClient = AqualuxChromeClient(tabId, this) { host }
        webView.setDownloadListener { url, userAgent, disposition, mime, length ->
            onDownload(url, userAgent, disposition, mime, length)
        }
        webView.setFindListener { activeMatch, count, _ ->
            if (tabId == _ui.value.activeTabId) {
                _ui.update { it.copy(findActive = activeMatch + 1, findCount = count) }
            }
        }
        webView.setOnScrollChangeListener { _, x, y, _, oldY ->
            onScroll(tabId, x, y, oldY)
        }
        webView.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                host?.dispatchBack()
                true
            } else {
                false
            }
        }
        return webView
    }

    private fun destroyWebView(id: String) {
        val webView = webViews.remove(id) ?: return
        scrollPositions[id] = webView.scrollX to webView.scrollY
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.setOnKeyListener(null)
        webView.setDownloadListener(null)
        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (_: Throwable) {
            // The view may already be detached from a dying process.
        }
    }

    private fun unloadIdle() {
        val minutes = _settings.value.idleUnloadMinutes
        if (minutes <= 0) return
        val now = System.currentTimeMillis()
        val cutoff = now - minutes * 60_000L
        val activeId = _ui.value.activeTabId
        val victims = _ui.value.tabs.filter { tab ->
            tab.id != activeId && webViews.containsKey(tab.id) && tab.lastActiveAt < cutoff
        }
        val overflow = webViews.keys.filter { it != activeId }.let { loaded ->
            if (loaded.size <= MAX_LOADED) emptyList()
            else {
                _ui.value.tabs
                    .filter { it.id in loaded }
                    .sortedBy { it.lastActiveAt }
                    .dropLast(MAX_LOADED - 1)
                    .map { it.id }
            }
        }
        (victims.map { it.id } + overflow).distinct().forEach { id ->
            val scroll = currentScroll(id)
            pendingScroll[id] = scroll
            destroyWebView(id)
            mutate(id) { it.copy(unloaded = true, isLoading = false) }
        }
    }

    private fun applyRuntimeSettings(settings: UserSettings) {
        val dark = isDarkTheme(settings)
        webViews.forEach { (id, webView) ->
            val desktop = find(id)?.desktop == true
            WebViewConfigurator.configure(webView, settings, dark, mobileUserAgent, desktop)
            webView.setBackgroundColor(pageBackground)
        }
    }

    private fun resumeActive() {
        val activeId = _ui.value.activeTabId
        val foreground = hostResumed && _ui.value.sheet == Sheet.None && !_ui.value.fullscreen
        webViews.forEach { (id, webView) ->
            if (foreground && id == activeId) webView.onResume() else webView.onPause()
        }
    }

    private fun applyPendingScroll(tabId: String) {
        val pending = pendingScroll[tabId] ?: return
        val webView = webViews[tabId] ?: return
        webView.post { webView.scrollTo(pending.first, pending.second) }
        webView.postDelayed({
            if (pendingScroll[tabId] == pending) {
                webView.scrollTo(pending.first, pending.second)
                pendingScroll.remove(tabId)
                scrollPositions[tabId] = pending
            }
        }, 200)
    }

    private fun currentScroll(id: String): Pair<Int, Int> {
        pendingScroll[id]?.let { return it }
        webViews[id]?.let { return it.scrollX to it.scrollY }
        return scrollPositions[id] ?: (0 to 0)
    }

    private fun recordHistory(url: String, title: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        val entry = com.aqualux.browser.session.HistoryEntry(
            url = url,
            title = title.ifBlank { url },
            visitedAt = System.currentTimeMillis(),
        )
        val next = listOf(entry) + _ui.value.history.filterNot { it.url == url }
        _ui.update { it.copy(history = next.take(500)) }
        schedulePersist()
    }

    private fun refreshSuggestions() {
        val state = _ui.value
        val draft = state.omnibox.draft.trim()
        val activeUrl = state.activeTab?.url.orEmpty()
        val suggestions = if (!state.omnibox.editing || draft.isEmpty() || draft == activeUrl) {
            emptyList()
        } else {
            val candidates = buildList {
                state.tabs.forEach { tab ->
                    if (tab.url.isNotBlank()) {
                        add(
                            Suggestion(
                                title = tab.title,
                                url = tab.url,
                                source = SuggestionSource.TAB,
                                tabId = tab.id,
                                recencyEpochMs = tab.lastActiveAt,
                            ),
                        )
                    }
                }
                state.bookmarks.forEach { bookmark ->
                    add(
                        Suggestion(
                            title = bookmark.title,
                            url = bookmark.url,
                            source = SuggestionSource.BOOKMARK,
                            recencyEpochMs = bookmark.createdAt,
                        ),
                    )
                }
                state.history.forEach { entry ->
                    add(
                        Suggestion(
                            title = entry.title,
                            url = entry.url,
                            source = SuggestionSource.HISTORY,
                            recencyEpochMs = entry.visitedAt,
                        ),
                    )
                }
            }
            SuggestionRanker.rank(draft, candidates)
        }
        _ui.update { it.copy(suggestions = suggestions) }
    }

    private fun showMessage(text: String) {
        _ui.update { it.copy(message = text) }
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            delay(3200)
            _ui.update { state -> if (state.message == text) state.copy(message = null) else state }
        }
    }

    private fun enqueueDownload(url: String, userAgent: String, mimeType: String, name: String) {
        try {
            val app = getApplication<Application>()
            val manager = app.getSystemService(DownloadManager::class.java)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(name)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                .addRequestHeader("User-Agent", userAgent)
            if (mimeType.isNotBlank()) request.setMimeType(mimeType)
            manager.enqueue(request)
            showMessage("Downloading $name")
        } catch (_: Exception) {
            showMessage("Couldn't start the download")
        }
    }

    private fun isDarkTheme(settings: UserSettings): Boolean = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            val night = getApplication<Application>().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
            night == Configuration.UI_MODE_NIGHT_YES
        }
    }

    private fun schedulePersist() {
        if (!_ui.value.ready) return
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(400)
            val snapshot = capturePersistence()
            withContext(Dispatchers.IO) {
                repository.writeAll(snapshot.session, snapshot.history, snapshot.bookmarks)
            }
        }
    }

    private fun persistNow() {
        if (!_ui.value.ready) return
        persistJob?.cancel()
        val snapshot = capturePersistence()
        viewModelScope.launch(Dispatchers.IO) {
            repository.writeAll(snapshot.session, snapshot.history, snapshot.bookmarks)
        }
    }

    private data class Persistence(
        val session: PersistedSession,
        val history: List<com.aqualux.browser.session.HistoryEntry>,
        val bookmarks: List<Bookmark>,
    )

    private fun capturePersistence(): Persistence {
        val state = _ui.value
        val tabs = state.tabs.map { tab ->
            val scroll = currentScroll(tab.id)
            PersistedTab(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                scrollX = scroll.first,
                scrollY = scroll.second,
                pinned = tab.pinned,
                desktop = tab.desktop,
                faviconFile = tab.faviconFile,
            )
        }
        return Persistence(
            session = PersistedSession(version = 1, activeTabId = state.activeTabId, tabs = tabs),
            history = state.history,
            bookmarks = state.bookmarks,
        )
    }

    private fun blankTab(id: String = UUID.randomUUID().toString()) = TabSnapshot(
        id = id,
        url = "",
        title = "New tab",
        faviconFile = null,
        progress = 0f,
        isLoading = false,
        pinned = false,
        canGoBack = false,
        canGoForward = false,
        desktop = false,
        unloaded = false,
        error = null,
        lastActiveAt = System.currentTimeMillis(),
        hasDocument = false,
    )

    private fun active(): TabSnapshot? = _ui.value.activeTab

    private fun activeWebView(): WebView? = _ui.value.activeTabId?.let { webViews[it] }

    private fun find(id: String): TabSnapshot? = _ui.value.tabs.find { it.id == id }

    private inline fun mutate(id: String, block: (TabSnapshot) -> TabSnapshot) {
        _ui.update { state ->
            state.copy(tabs = state.tabs.map { tab -> if (tab.id == id) block(tab) else tab })
        }
    }

    private companion object {
        const val MAX_TABS = 40
        const val MAX_LOADED = 4
    }
}
