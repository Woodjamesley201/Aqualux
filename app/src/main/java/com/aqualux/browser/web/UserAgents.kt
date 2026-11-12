package com.aqualux.browser.web

object UserAgents {
    private val fallback =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    fun desktop(mobileUa: String): String {
        if (mobileUa.isBlank()) return fallback
        val withoutMobile = mobileUa.replace(" Mobile", "")
        return withoutMobile.replace(Regex("""\([^)]*Android[^)]*\)"""), "(X11; Linux x86_64)")
    }
}
