package com.aqualux.browser.web

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.aqualux.browser.model.UserSettings
import com.aqualux.browser.security.WebSecurityPolicy

object WebViewConfigurator {
    /**
     * Applies browser settings. This function must not call
     * [WebView.addJavascriptInterface]. Web content never receives an app bridge.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configure(
        webView: WebView,
        settings: UserSettings,
        darkTheme: Boolean,
        mobileUserAgent: String?,
        desktop: Boolean,
    ) {
        check(!WebSecurityPolicy.javascriptInterfaceAllowed) {
            "Aqualux does not expose Javascript interfaces to web content."
        }
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.loadsImagesAutomatically = true
        webSettings.mediaPlaybackRequiresUserGesture = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = !desktop
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportZoom(true)
        webSettings.textZoom = settings.textZoom.coerceIn(50, 200)
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.mixedContentMode = when (WebSecurityPolicy.mixedContent) {
            "never" -> WebSettings.MIXED_CONTENT_NEVER_ALLOW
            else -> error("Refusing mixed content mode ${WebSecurityPolicy.mixedContent}")
        }
        webSettings.allowFileAccess = WebSecurityPolicy.allowFileAccess
        webSettings.allowContentAccess = WebSecurityPolicy.allowContentAccess
        @Suppress("DEPRECATION")
        webSettings.allowFileAccessFromFileURLs = WebSecurityPolicy.allowFileAccessFromFileUrls
        @Suppress("DEPRECATION")
        webSettings.allowUniversalAccessFromFileURLs = WebSecurityPolicy.allowUniversalAccessFromFileUrls
        webSettings.safeBrowsingEnabled = WebSecurityPolicy.safeBrowsing
        webSettings.setGeolocationEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            webSettings.isAlgorithmicDarkeningAllowed = settings.darkenPages && darkTheme
        }
        val baseUa = mobileUserAgent ?: webSettings.userAgentString
        webSettings.userAgentString = if (desktop) UserAgents.desktop(baseUa) else baseUa

        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(webView, !settings.blockThirdPartyCookies)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.isScrollbarFadingEnabled = true
    }
}
