package com.ren42377.bimalaunch

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ren42377.bimalaunch.core.DispatchResult
import com.ren42377.bimalaunch.core.ManifestParser
import com.ren42377.bimalaunch.core.NativeBridge
import com.ren42377.bimalaunch.core.NativeLoader
import com.ren42377.bimalaunch.ui.render.DialogState
import com.ren42377.bimalaunch.ui.render.DynamicRenderer
import com.ren42377.bimalaunch.ui.render.LoadingScreen
import com.ren42377.bimalaunch.ui.theme.BimalaunchTheme
import java.io.BufferedReader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nativeReady = NativeLoader.ensureLoaded(this)
        val manifest = if (nativeReady) {
            NativeBridge.initialize(loadConfigJson())
            ManifestParser.parse(NativeBridge.getManifest())
        } else {
            null
        }

        setContent {
            BimalaunchTheme {
                var dialog by remember { mutableStateOf<DialogState?>(null) }
                val context = LocalContext.current

                if (manifest == null) {
                    LoadingScreen()
                    return@BimalaunchTheme
                }

                DynamicRenderer(
                    node = manifest.root,
                    dialog = dialog,
                    onAction = { node ->
                        val result = DispatchResult.parse(
                            NativeBridge.dispatch(node.action, "{}")
                        )
                        dialog = applyEffect(result) { text ->
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismissDialog = { dialog = null }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            NativeBridge.shutdown()
        }
    }

    private fun applyEffect(result: DispatchResult, toast: (String) -> Unit): DialogState? {
        return when (result.effect) {
            "dialog" -> DialogState(
                title = result.title.ifEmpty { "Info" },
                message = result.message
            )
            "navigate" -> {
                toast("Navigasi native: ${result.target}")
                null
            }
            "error" -> DialogState(
                title = "Kesalahan",
                message = result.message.ifEmpty { result.errorType }
            )
            else -> {
                if (result.message.isNotEmpty()) {
                    toast(result.message)
                }
                null
            }
        }
    }

    private fun loadConfigJson(): String {
        return runCatching {
            assets.open("config/app-config.json").bufferedReader().use(BufferedReader::readText)
        }.getOrDefault("{}")
    }
}
