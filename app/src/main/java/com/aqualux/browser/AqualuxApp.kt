package com.aqualux.browser

import android.app.Application
import android.content.pm.ApplicationInfo
import android.webkit.WebView

class AqualuxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
