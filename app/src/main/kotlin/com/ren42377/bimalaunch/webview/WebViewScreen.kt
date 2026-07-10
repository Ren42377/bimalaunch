package com.ren42377.bimalaunch.webview

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("ClickableViewAccessibility")
@Composable
fun WebViewScreen(
    webView: WebView,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
