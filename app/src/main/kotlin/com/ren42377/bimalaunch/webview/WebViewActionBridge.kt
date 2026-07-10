package com.ren42377.bimalaunch.webview

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper

object WebViewActionBridge {
    interface Actions {
        fun captureBitmap(callback: (Bitmap?) -> Unit)
        fun clickAt(x: Float, y: Float): Boolean
        fun scrollTo(position: ScrollPosition): Boolean
        fun evaluateJavascript(script: String, callback: (String?) -> Unit)
    }

    enum class ScrollPosition {
        TOP,
        MIDDLE,
        BOTTOM
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var actions: Actions? = null

    fun register(value: Actions) {
        actions = value
    }

    fun unregister(value: Actions) {
        if (actions === value) actions = null
    }

    fun captureBitmap(callback: (Bitmap?) -> Unit) {
        mainHandler.post {
            val currentActions = actions
            if (currentActions == null) {
                callback(null)
            } else {
                runCatching {
                    currentActions.captureBitmap(callback)
                }.onFailure {
                    callback(null)
                }
            }
        }
    }

    fun clickAt(x: Float, y: Float, callback: (Boolean) -> Unit) {
        mainHandler.post {
            callback(actions?.clickAt(x, y) == true)
        }
    }

    fun scrollTo(position: ScrollPosition, callback: (Boolean) -> Unit) {
        mainHandler.post {
            callback(actions?.scrollTo(position) == true)
        }
    }

    fun evaluateJavascript(script: String, callback: (String?) -> Unit) {
        mainHandler.post {
            val currentActions = actions
            if (currentActions == null) {
                callback(null)
            } else {
                runCatching {
                    currentActions.evaluateJavascript(script, callback)
                }.onFailure {
                    callback(null)
                }
            }
        }
    }
}
