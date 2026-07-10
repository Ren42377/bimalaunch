package com.ren42377.bimalaunch.webview

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.webkit.JavascriptInterface
import com.ren42377.bimalaunch.core.NativeBridge
import org.json.JSONObject
import java.io.File

interface KioskController {
    val isFakePinned: Boolean
    fun setFakePinned(pinned: Boolean)
}

class JSBridge(
    private val context: Context,
    private val kiosk: KioskController
) {
    @JavascriptInterface
    fun isAppPinned(): Boolean = kiosk.isFakePinned

    @JavascriptInterface
    fun isDevice(): Boolean {
        val facts = JSONObject().apply {
            put("fingerprint", Build.FINGERPRINT ?: "")
            put("model", Build.MODEL ?: "")
            put("manufacturer", Build.MANUFACTURER ?: "")
            put("host", Build.HOST ?: "")
            put("brand", Build.BRAND ?: "")
            put("device", Build.DEVICE ?: "")
            put("product", Build.PRODUCT ?: "")
            put("hardware", Build.HARDWARE ?: "")
            put("networkOperator", networkOperatorName())
            put("qemuFileExists", qemuFileExists())
            put("playStoreInstalled", isPackageInstalled(requiredPackage))
        }
        val response = JSONObject(NativeBridge.dispatch("anticheat_is_device", facts.toString()))
        val data = response.optJSONObject("data")
        return data?.optBoolean("value", false) ?: false
    }

    @JavascriptInterface
    fun isForbidden(): Boolean {
        for (packageName in forbiddenPackages) {
            if (isPackageInstalled(packageName)) {
                return true
            }
        }
        return false
    }

    @JavascriptInterface
    fun startPinningApp() {
        kiosk.setFakePinned(true)
    }

    @JavascriptInterface
    fun stopPinningApp() {
        kiosk.setFakePinned(false)
    }

    private fun networkOperatorName(): String {
        val service = context.getSystemService(Context.TELEPHONY_SERVICE)
        return if (service is TelephonyManager) service.networkOperatorName ?: "" else ""
    }

    private fun qemuFileExists(): Boolean {
        return QEMU_FILES.any { File(it).exists() }
    }

    @Suppress("DEPRECATION")
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private val antiCheatConfig: Pair<List<String>, String> by lazy { loadAntiCheatList() }
    private val forbiddenPackages: List<String> get() = antiCheatConfig.first
    private val requiredPackage: String get() = antiCheatConfig.second

    private fun loadAntiCheatList(): Pair<List<String>, String> {
        return runCatching {
            val response = JSONObject(NativeBridge.dispatch("anticheat_config", "{}"))
            val data = response.optJSONObject("data") ?: JSONObject()
            val array = data.optJSONArray("packages")
            val list = buildList {
                if (array != null) {
                    for (index in 0 until array.length()) {
                        add(array.optString(index))
                    }
                }
            }
            val required = data.optString("required", "com.android.vending")
            list to required
        }.getOrDefault(emptyList<String>() to "com.android.vending")
    }

    companion object {
        private val QEMU_FILES = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props"
        )
    }
}
