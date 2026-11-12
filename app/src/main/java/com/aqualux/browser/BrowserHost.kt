package com.aqualux.browser

import android.content.Intent
import android.net.Uri
import android.view.View

interface BrowserHost {
    fun requestPermissions(permissions: Array<String>, onResult: (Map<String, Boolean>) -> Unit)
    fun openFileChooser(intent: Intent, onResult: (Array<Uri>?) -> Unit)
    fun showFullscreen(view: View, onHidden: () -> Unit)
    fun hideFullscreen()
    fun startActivitySafe(intent: Intent): Boolean
    fun dispatchBack()
}
