package com.ren42377.bimalaunch.app

import android.content.Context
import com.ren42377.bimalaunch.core.NativeBridge
import org.json.JSONObject

object AppPreferences {
    private const val PREF_FILE = "bimalaunch_settings"
    private const val KEY_LICENSE_OK = "license_ok"

    private val syncedKeys = listOf(
        "gemini_api_key",
        "ai_prompt",
        "clawd_auto_mode",
        "clawd_runtime",
        "clawd_always_on",
        "clawd_draw_box",
        "practice_mode_enabled",
        "auto_username",
        "auto_password"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun isLicenseVerified(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LICENSE_OK, false)

    fun setLicenseVerified(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LICENSE_OK, value).apply()
    }

    fun getString(context: Context, key: String, fallback: String = ""): String =
        prefs(context).getString(key, fallback) ?: fallback

    fun setString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
        pushToNative(key, value)
    }

    fun collectAsJson(context: Context): JSONObject {
        val store = prefs(context)
        val json = JSONObject()
        for (key in syncedKeys) {
            val value = store.getString(key, null)
            if (value != null) {
                json.put(key, value)
            }
        }
        return json
    }

    private fun pushToNative(key: String, value: String) {
        val payload = JSONObject().apply {
            put("key", key)
            put("value", value)
        }
        runCatching { NativeBridge.dispatch("set_setting", payload.toString()) }
    }
}
