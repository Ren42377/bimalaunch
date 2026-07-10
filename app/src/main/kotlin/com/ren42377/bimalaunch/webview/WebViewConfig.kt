package com.ren42377.bimalaunch.webview

import com.ren42377.bimalaunch.core.NativeBridge
import org.json.JSONObject

data class WebViewConfig(
    val id: String,
    val loginUrl: String,
    val userAgent: String,
    val userAgentMarker: String,
    val headers: Map<String, String>,
    val autoLoginEnabled: Boolean,
    val practiceMode: Boolean,
    val configured: Boolean
) {
    companion object {
        fun fromNative(): WebViewConfig {
            val response = runCatching {
                JSONObject(NativeBridge.dispatch("webview_config", "{}"))
            }.getOrNull()
            val data = response?.optJSONObject("data") ?: JSONObject()
            val headersObj = data.optJSONObject("headers") ?: JSONObject()
            val headers = buildMap {
                val keys = headersObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, headersObj.optString(key))
                }
            }
            return WebViewConfig(
                id = data.optString("id"),
                loginUrl = data.optString("loginUrl"),
                userAgent = data.optString("userAgent"),
                userAgentMarker = data.optString("userAgentMarker"),
                headers = headers,
                autoLoginEnabled = data.optBoolean("autoLoginEnabled", false),
                practiceMode = data.optBoolean("practiceMode", false),
                configured = data.optBoolean("configured", false)
            )
        }
    }
}
