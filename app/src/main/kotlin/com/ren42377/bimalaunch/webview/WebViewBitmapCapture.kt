package com.ren42377.bimalaunch.webview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView

object WebViewBitmapCapture {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun capture(webView: WebView, callback: (Bitmap?) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { capture(webView, callback) }
            return
        }
        val visibleRect = webView.visibleWindowRect()
        if (visibleRect == null || visibleRect.width() <= 0 || visibleRect.height() <= 0) {
            callback(null)
            return
        }
        callback(webView.captureFallback(visibleRect))
    }

    private fun WebView.captureFallback(visibleWindowRect: Rect): Bitmap? {
        val ownWindowRect = ownWindowRect() ?: return null
        val width = visibleWindowRect.width()
        val height = visibleWindowRect.height()
        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.translate(
                (ownWindowRect.left - visibleWindowRect.left - scrollX).toFloat(),
                (ownWindowRect.top - visibleWindowRect.top - scrollY).toFloat()
            )
            draw(canvas)
        }
    }

    private fun WebView.visibleWindowRect(): Rect? {
        val globalVisible = Rect()
        if (!getGlobalVisibleRect(globalVisible)) return null
        val root = rootView ?: return null
        val rootScreen = IntArray(2)
        val rootWindow = IntArray(2)
        root.getLocationOnScreen(rootScreen)
        root.getLocationInWindow(rootWindow)
        globalVisible.offset(
            rootWindow[0] - rootScreen[0],
            rootWindow[1] - rootScreen[1]
        )
        return globalVisible
    }

    private fun View.ownWindowRect(): Rect? {
        if (width <= 0 || height <= 0) return null
        val location = IntArray(2)
        getLocationInWindow(location)
        return Rect(location[0], location[1], location[0] + width, location[1] + height)
    }
}
