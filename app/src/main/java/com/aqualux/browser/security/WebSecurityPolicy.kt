package com.aqualux.browser.security

/**
 * Hard limits for web content. [WebViewConfigurator] reads these flags
 * instead of inlining them, so the unit tests lock the policy the app ships.
 */
object WebSecurityPolicy {
    const val javascriptInterfaceAllowed = false
    const val allowFileAccess = false
    const val allowContentAccess = false
    const val allowFileAccessFromFileUrls = false
    const val allowUniversalAccessFromFileUrls = false
    const val safeBrowsing = true
    const val sslProceedAllowed = false
    const val mixedContent = "never"
}
