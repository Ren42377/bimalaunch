package com.ren42377.bimalaunch.core

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

object NativeLoader {
    private const val TAG = "NativeLoader"
    private const val LIBRARY_NAME = "nativecore"
    private const val LIBRARY_FILE = "libnativecore.so"
    private const val ASSET_DIR = "native"

    @Volatile
    private var loaded = false

    @Synchronized
    fun ensureLoaded(context: Context): Boolean {
        if (loaded) {
            return true
        }
        if (loadFromAssets(context) || loadFromPackagedLibrary()) {
            loaded = true
        }
        return loaded
    }

    private fun loadFromAssets(context: Context): Boolean {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return false
        val assetPath = "$ASSET_DIR/$abi/$LIBRARY_FILE"
        return try {
            val available = context.assets.list("$ASSET_DIR/$abi")?.contains(LIBRARY_FILE) == true
            if (!available) {
                return false
            }
            val target = File(context.filesDir, LIBRARY_FILE)
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            target.setReadOnly()
            System.load(target.absolutePath)
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Asset library load skipped: ${e.message}")
            false
        }
    }

    private fun loadFromPackagedLibrary(): Boolean {
        return try {
            System.loadLibrary(LIBRARY_NAME)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Packaged library load failed", e)
            false
        }
    }
}
