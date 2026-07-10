package com.ren42377.bimalaunch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ren42377.bimalaunch.app.AppPreferences
import com.ren42377.bimalaunch.core.NativeBridge
import com.ren42377.bimalaunch.core.NativeLoader
import com.ren42377.bimalaunch.floating.FloatingMenuLoader
import com.ren42377.bimalaunch.floating.FloatingState
import com.ren42377.bimalaunch.ui.floating.InAppFloatingHost
import com.ren42377.bimalaunch.ui.floating.resolveToolItems
import com.ren42377.bimalaunch.ui.screens.LicenseScreen
import com.ren42377.bimalaunch.ui.screens.PermissionScreen
import com.ren42377.bimalaunch.ui.theme.BimalaunchTheme
import com.ren42377.bimalaunch.webview.KioskController
import com.ren42377.bimalaunch.webview.WebViewController
import com.ren42377.bimalaunch.webview.WebViewScreen
import org.json.JSONObject
import java.io.BufferedReader

class MainActivity : ComponentActivity(), KioskController {

    private var startupDestination by mutableStateOf<StartupDestination>(StartupDestination.LicenseLoading)
    private var licenseErrorText by mutableStateOf<String?>(null)
    private var isLicenseVerifying by mutableStateOf(false)
    private var dialogState by mutableStateOf<Pair<String, String>?>(null)

    private var licenseVerified = false

    @Volatile
    private var fakePinned = false

    private val floatingState = FloatingState()
    private val floatingGeometry = com.ren42377.bimalaunch.floating.FloatingGeometryConfig.DEFAULT
    private var floatingGeometryConfig = floatingGeometry

    private var webViewController: WebViewController? = null
    private var webViewInstance: android.webkit.WebView? by mutableStateOf(null)

    private val startupHandler = Handler(Looper.getMainLooper())
    private val resumeTransitionsRunnable = Runnable { handleResumeTransitions() }

    override val isFakePinned: Boolean get() = fakePinned

    override fun setFakePinned(pinned: Boolean) {
        fakePinned = pinned
        runOnUiThread { applySystemBars() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nativeReady = NativeLoader.ensureLoaded(this)
        if (nativeReady) {
            NativeBridge.initialize(buildConfigJson())
        }

        setupFloating()

        setContent {
            BimalaunchTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val destination = startupDestination
                    val webView = webViewInstance
                    if (destination is StartupDestination.WebView && webView != null) {
                        WebViewScreen(webView = webView, modifier = Modifier.fillMaxSize())
                        InAppFloatingHost(
                            state = floatingState,
                            geometryConfig = floatingGeometryConfig,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        when (destination) {
                            is StartupDestination.LicenseLoading -> LicenseScreen(
                                isLoading = true,
                                errorText = null,
                                isVerifying = false,
                                onVerifyClick = {}
                            )
                            is StartupDestination.LicenseInput -> LicenseScreen(
                                isLoading = false,
                                errorText = licenseErrorText,
                                isVerifying = isLicenseVerifying,
                                onVerifyClick = { verifyLicense(it) }
                            )
                            is StartupDestination.Permission -> PermissionScreen(
                                iconRes = destination.snapshot.iconRes,
                                titleText = destination.snapshot.title,
                                descText = destination.snapshot.description,
                                buttonText = destination.snapshot.buttonText,
                                onGrantClick = { requestMissingPermission() }
                            )
                            else -> LicenseScreen(
                                isLoading = true,
                                errorText = null,
                                isVerifying = false,
                                onVerifyClick = {}
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = startupDestination !is StartupDestination.WebView,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        LinearProgressIndicator(
                            progress = { startupDestination.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    dialogState?.let { (title, message) ->
                        AlertDialog(
                            onDismissRequest = { dialogState = null },
                            confirmButton = {
                                TextButton(onClick = { dialogState = null }) { Text("Tutup") }
                            },
                            title = { Text(title) },
                            text = { Text(message) }
                        )
                    }
                }
            }
        }

        if (AppPreferences.isLicenseVerified(this)) {
            licenseVerified = true
            NativeBridge.dispatch("verify_license", JSONObject().put("key", "developer").toString())
            proceedToPermissionFlow()
        } else {
            startupDestination = StartupDestination.LicenseLoading
            startupHandler.postDelayed({ startupDestination = StartupDestination.LicenseInput }, 800)
        }
    }

    private fun setupFloating() {
        val menu = FloatingMenuLoader.load()
        floatingState.menuTitle = menu.title
        floatingState.toolItems = resolveToolItems(menu.items)
        floatingGeometryConfig = menu.geometry
        floatingState.onBubbleClick = { floatingState.menuVisible = true }
        floatingState.onDismissMenu = { floatingState.menuVisible = false }
        floatingState.onBubbleDiscard = {
            floatingState.menuVisible = false
            floatingState.visible = false
        }
        floatingState.onToolClick = { item ->
            floatingState.menuVisible = false
            handleToolAction(item.action)
        }
    }

    private fun handleToolAction(action: String) {
        when (action) {
            "open_settings" -> startActivity(Intent(this, SettingsActivity::class.java))
            else -> {
                val response = runCatching {
                    JSONObject(NativeBridge.dispatch(action, "{}"))
                }.getOrNull()
                val effect = response?.optString("effect")
                if (effect == "dialog") {
                    dialogState = response.optString("title", "Info") to response.optString("message")
                } else {
                    Toast.makeText(this, response?.optString("message").orEmpty(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun verifyLicense(key: String) {
        isLicenseVerifying = true
        licenseErrorText = null
        val payload = JSONObject().put("key", key.trim()).toString()
        val response = runCatching {
            JSONObject(NativeBridge.dispatch("verify_license", payload))
        }.getOrNull()
        isLicenseVerifying = false
        if (response?.optBoolean("ok", false) == true) {
            AppPreferences.setLicenseVerified(this, true)
            licenseVerified = true
            proceedToPermissionFlow()
        } else {
            licenseErrorText = getString(R.string.license_error_invalid)
        }
    }

    private fun proceedToPermissionFlow() {
        if (!licenseVerified || webViewController != null) return
        if (hasStartupPermissions()) {
            loadExamWebView()
        } else {
            showPermissionScreen()
        }
    }

    private fun loadExamWebView() {
        val controller = WebViewController(this, this) {}
        webViewController = controller
        webViewInstance = controller.create()
        floatingState.visible = true
        startupDestination = StartupDestination.WebView
    }

    private fun showPermissionScreen() {
        startupDestination = StartupDestination.Permission(createPermissionSnapshot())
    }

    private fun refreshPermissionUi() {
        if (startupDestination is StartupDestination.Permission) {
            startupDestination = StartupDestination.Permission(createPermissionSnapshot())
        }
    }

    private fun createPermissionSnapshot(): PermissionUiSnapshot {
        val stage = if (!hasMediaImagesPermission()) PermissionStage.MEDIA else PermissionStage.INSTALLER
        return if (stage == PermissionStage.MEDIA) {
            PermissionUiSnapshot(
                stage = stage,
                iconRes = R.drawable.ic_akar_image,
                title = getString(R.string.permission_title),
                description = getString(R.string.media_permission_desc),
                buttonText = getString(R.string.grant_media_permission)
            )
        } else {
            PermissionUiSnapshot(
                stage = stage,
                iconRes = R.drawable.ic_update_download,
                title = getString(R.string.permission_title),
                description = getString(R.string.installer_permission_desc),
                buttonText = getString(R.string.grant_installer_permission)
            )
        }
    }

    private fun hasStartupPermissions(): Boolean =
        hasMediaImagesPermission() && canInstallPackages()

    private fun hasMediaImagesPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            else ->
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun canInstallPackages(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()
    }

    private fun requestMissingPermission() {
        if (!hasMediaImagesPermission()) {
            val permissions = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
                else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            requestPermissions(permissions, REQUEST_MEDIA_IMAGES)
            return
        }
        if (!canInstallPackages() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showSystemBarsTemporarily()
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_INSTALL_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermissionUi()
        if (requestCode == REQUEST_MEDIA_IMAGES && licenseVerified) {
            proceedToPermissionFlow()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refreshPermissionUi()
        if (requestCode == REQUEST_INSTALL_PERMISSION && licenseVerified) {
            proceedToPermissionFlow()
        }
    }

    override fun onResume() {
        super.onResume()
        startupHandler.removeCallbacks(resumeTransitionsRunnable)
        startupHandler.postDelayed(resumeTransitionsRunnable, 400)
    }

    private fun handleResumeTransitions() {
        refreshPermissionUi()
        if (licenseVerified && webViewController == null) {
            proceedToPermissionFlow()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                if (startupDestination is StartupDestination.WebView) {
                    floatingState.visible = true
                    floatingState.menuVisible = true
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && fakePinned) applySystemBars()
    }

    @Suppress("DEPRECATION")
    private fun applySystemBars() {
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(!fakePinned)
            val controller = window.insetsController ?: return
            if (fakePinned) {
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsets.Type.systemBars())
            } else {
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                controller.show(WindowInsets.Type.systemBars())
            }
            return
        }
        decorView.systemUiVisibility = if (fakePinned) {
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        } else {
            View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    @Suppress("DEPRECATION")
    private fun showSystemBarsTemporarily() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(WindowInsets.Type.systemBars())
            return
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onDestroy() {
        setFakePinned(false)
        startupHandler.removeCallbacksAndMessages(null)
        webViewController?.destroy()
        webViewController = null
        if (isFinishing) {
            NativeBridge.shutdown()
        }
        super.onDestroy()
    }

    private fun buildConfigJson(): String {
        val base = runCatching {
            assets.open("config/app-config.json").bufferedReader().use(BufferedReader::readText)
        }.getOrDefault("{}")
        return runCatching {
            val obj = JSONObject(base)
            val settings = obj.optJSONObject("settings") ?: JSONObject()
            val stored = AppPreferences.collectAsJson(this)
            val keys = stored.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                settings.put(key, stored.getString(key))
            }
            obj.put("settings", settings)
            obj.toString()
        }.getOrDefault(base)
    }

    companion object {
        private const val REQUEST_INSTALL_PERMISSION = 7002
        private const val REQUEST_MEDIA_IMAGES = 7004
    }
}
