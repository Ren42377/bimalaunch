package com.ren42377.bimalaunch.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ren42377.bimalaunch.core.NativeBridge
import org.json.JSONObject

class WebViewController(
    private val context: Context,
    private val kiosk: KioskController,
    private val onPageReady: () -> Unit
) {
    var webView: WebView? = null
        private set

    private var config: WebViewConfig = WebViewConfig.fromNative()

    private val actions = object : WebViewActionBridge.Actions {
        override fun captureBitmap(callback: (android.graphics.Bitmap?) -> Unit) {
            val view = webView
            if (view == null) {
                callback(null)
            } else {
                WebViewBitmapCapture.capture(view, callback)
            }
        }

        override fun clickAt(x: Float, y: Float): Boolean {
            val view = webView ?: return false
            val downTime = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
            val up = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0)
            val handled = view.dispatchTouchEvent(down) or view.dispatchTouchEvent(up)
            down.recycle()
            up.recycle()
            return handled
        }

        override fun scrollTo(position: WebViewActionBridge.ScrollPosition): Boolean {
            val view = webView ?: return false
            val contentHeight = (view.contentHeight * view.scale).toInt()
            val target = when (position) {
                WebViewActionBridge.ScrollPosition.TOP -> 0
                WebViewActionBridge.ScrollPosition.MIDDLE ->
                    ((contentHeight - view.height) / 2).coerceAtLeast(0)
                WebViewActionBridge.ScrollPosition.BOTTOM ->
                    (contentHeight - view.height).coerceAtLeast(0)
            }
            view.scrollTo(0, target)
            return true
        }

        override fun evaluateJavascript(script: String, callback: (String?) -> Unit) {
            val view = webView
            if (view == null) {
                callback(null)
            } else {
                view.evaluateJavascript(script) { callback(it) }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    fun create(): WebView {
        webView?.let { return it }
        config = WebViewConfig.fromNative()
        val view = WebView(context)
        view.setInitialScale(100)
        val settings = view.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        if (config.userAgent.isNotEmpty()) {
            settings.userAgentString = config.userAgent
        } else if (config.userAgentMarker.isNotEmpty()) {
            settings.userAgentString = settings.userAgentString + " " + config.userAgentMarker
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (config.autoLoginEnabled) {
                    injectAutoLogin(view)
                }
            }
        }
        view.webChromeClient = WebChromeClient()
        view.addJavascriptInterface(JSBridge(context, kiosk), "JSBridge")
        webView = view
        WebViewActionBridge.register(actions)
        loadConfiguredPage(view)
        onPageReady()
        return view
    }

    private fun loadConfiguredPage(view: WebView) {
        if (config.practiceMode) {
            val practiceUrl = runCatching {
                com.ren42377.bimalaunch.practice.PracticeLocalServer.start(context)
            }.getOrNull()
            if (practiceUrl != null) {
                view.loadUrl(practiceUrl)
                return
            }
        } else {
            runCatching { com.ren42377.bimalaunch.practice.PracticeLocalServer.stop() }
        }
        val url = config.loginUrl.ifEmpty { "about:blank" }
        if (config.headers.isEmpty()) {
            view.loadUrl(url)
        } else {
            view.loadUrl(url, config.headers)
        }
    }

    private fun injectAutoLogin(view: WebView) {
        val response = runCatching {
            JSONObject(NativeBridge.dispatch("auto_login_script", "{}"))
        }.getOrNull() ?: return
        val script = response.optJSONObject("data")?.optString("script").orEmpty()
        if (script.isNotBlank()) {
            view.evaluateJavascript(script, null)
        }
    }

    fun reloadIfConfigChanged() {
        val view = webView ?: return
        val next = WebViewConfig.fromNative()
        if (next.practiceMode != config.practiceMode) {
            config = next
            loadConfiguredPage(view)
        } else {
            config = next
        }
    }

    fun destroy() {
        kiosk.setFakePinned(false)
        WebViewActionBridge.unregister(actions)
        runCatching { com.ren42377.bimalaunch.practice.PracticeLocalServer.stop() }
        val view = webView ?: return
        webView = null
        view.stopLoading()
        view.removeJavascriptInterface("JSBridge")
        val parent = view.parent
        if (parent is ViewGroup) {
            parent.removeView(view)
        }
        view.destroy()
    }
}
