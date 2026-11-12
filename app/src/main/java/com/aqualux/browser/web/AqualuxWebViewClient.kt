package com.aqualux.browser.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.aqualux.browser.errors.ErrorTaxonomy
import com.aqualux.browser.omnibox.OmniboxResolver
import com.aqualux.browser.security.WebSecurityPolicy

class AqualuxWebViewClient(
    private val tabId: String,
    private val events: WebEvents,
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        if (url != null) events.onPageStarted(tabId, url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        events.onPageFinished(tabId, url.orEmpty())
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        val webView = view ?: return
        events.onVisited(tabId, url.orEmpty(), webView.canGoBack(), webView.canGoForward())
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return true
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme == "http" || scheme == "https") return false
        if (!request.isForMainFrame) return true
        return when (scheme) {
            "about" -> uri.toString() != "about:blank"
            "intent" -> {
                events.onIntentUri(uri.toString())
                true
            }
            "mailto", "tel", "sms", "smsto", "geo", "market" -> {
                events.onExternalUri(uri)
                true
            }
            else -> true
        }
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        if (request?.isForMainFrame != true || error == null) return
        val host = OmniboxResolver.hostOf(request.url?.toString().orEmpty())
        events.onMainFrameError(
            tabId,
            ErrorTaxonomy.fromWebView(error.errorCode, error.description?.toString(), host),
        )
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
        // Certificate failures are never continued. There is no proceed path.
        handler?.cancel()
        check(!WebSecurityPolicy.sslProceedAllowed)
        val host = error?.url?.let(OmniboxResolver::hostOf).orEmpty()
        events.onSsl(tabId, ErrorTaxonomy.fromSsl(error?.primaryError ?: ErrorTaxonomy.SSL_INVALID, host))
    }

    @SuppressLint("NewApi")
    override fun onSafeBrowsingHit(
        view: WebView?,
        request: WebResourceRequest?,
        threatType: Int,
        callback: SafeBrowsingResponse?,
    ) {
        callback?.backToSafety(true)
        val host = OmniboxResolver.hostOf(request?.url?.toString().orEmpty())
        events.onUnsafe(tabId, host)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        events.onRenderGone(tabId)
        return true
    }
}
