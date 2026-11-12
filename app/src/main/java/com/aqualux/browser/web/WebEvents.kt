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
import com.aqualux.browser.errors.PageError

interface WebEvents {
    fun onPageStarted(tabId: String, url: String)
    fun onPageFinished(tabId: String, url: String)
    fun onVisited(tabId: String, url: String, canGoBack: Boolean, canGoForward: Boolean)
    fun onProgress(tabId: String, progress: Int)
    fun onTitle(tabId: String, title: String)
    fun onIcon(tabId: String, icon: Bitmap)
    fun onMainFrameError(tabId: String, error: PageError)
    fun onSsl(tabId: String, error: PageError)
    fun onUnsafe(tabId: String, host: String)
    fun onScroll(tabId: String, x: Int, y: Int, oldY: Int)
    fun onRenderGone(tabId: String)
    fun onCreateWindow(resultMsg: Message): Boolean
    fun onPermissionRequest(request: PermissionRequest)
    fun onGeolocation(origin: String, callback: GeolocationPermissions.Callback)
    fun onFileChooser(intent: Intent, callback: ValueCallback<Array<Uri>>): Boolean
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
    fun onDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long)
    fun onExternalUri(uri: Uri)
    fun onIntentUri(raw: String)
}
