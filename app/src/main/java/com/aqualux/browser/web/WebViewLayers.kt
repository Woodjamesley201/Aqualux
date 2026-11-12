package com.aqualux.browser.web

import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView

/**
 * YouTube captions and video layers are SurfaceViews. Left at the default
 * media overlay z-order they paint above every Compose sibling in this window.
 * Keep them inside the WebView's own bounds, under chrome that sits outside
 * those bounds and under any dialog window.
 */
object WebViewLayers {
    fun contain(webView: WebView) {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.viewTreeObserver.addOnGlobalLayoutListener { clamp(webView) }
        clamp(webView)
    }

    private fun clamp(view: View) {
        if (view is SurfaceView) {
            try {
                view.setZOrderOnTop(false)
                view.setZOrderMediaOverlay(false)
            } catch (_: RuntimeException) {
                // Some surfaces reject a z-order change after they are attached.
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                clamp(view.getChildAt(index))
            }
        }
    }
}
