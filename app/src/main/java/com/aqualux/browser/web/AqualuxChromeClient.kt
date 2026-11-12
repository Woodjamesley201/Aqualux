package com.aqualux.browser.web

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.aqualux.browser.BrowserHost

class AqualuxChromeClient(
    private val tabId: String,
    private val events: WebEvents,
    private val host: () -> BrowserHost?,
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        events.onProgress(tabId, newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        if (!title.isNullOrBlank()) events.onTitle(tabId, title)
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        if (icon != null) events.onIcon(tabId, icon)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
    ): Boolean {
        if (!isUserGesture || resultMsg == null) return false
        return events.onCreateWindow(resultMsg)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        events.onPermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) = Unit

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        if (origin == null || callback == null) return
        events.onGeolocation(origin, callback)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        val callback = filePathCallback ?: return false
        val intent = try {
            fileChooserParams?.createIntent()
        } catch (_: Exception) {
            null
        } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        val accepted = events.onFileChooser(intent, callback)
        if (!accepted) callback.onReceiveValue(null)
        return true
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view == null || callback == null) {
            callback?.onCustomViewHidden()
            return
        }
        if (host() == null) {
            callback.onCustomViewHidden()
            return
        }
        events.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        events.onHideCustomView()
    }
}
