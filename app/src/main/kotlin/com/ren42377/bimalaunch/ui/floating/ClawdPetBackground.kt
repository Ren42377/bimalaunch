package com.ren42377.bimalaunch.ui.floating

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ren42377.bimalaunch.clawd.ClawdLabelerState
import com.ren42377.bimalaunch.clawd.ClawdPetAssets
import com.ren42377.bimalaunch.clawd.ClawdPetState
import kotlinx.coroutines.delay

@Composable
fun ClawdPetBackground(labelerState: ClawdLabelerState, modifier: Modifier = Modifier) {
    var petState by remember { mutableStateOf(ClawdPetState.INTRO) }
    var frameIndex by remember { mutableIntStateOf(0) }
    var currentFrame by remember { mutableStateOf(ClawdPetAssets.FALLBACK_FRAME) }

    LaunchedEffect(Unit) {
        petState = ClawdPetState.INTRO
        for (frame in ClawdPetAssets.INTRO_FRAMES) {
            currentFrame = frame
            delay(ClawdPetAssets.FRAME_INTERVAL_MS)
        }
        petState = ClawdPetState.IDLE
    }

    LaunchedEffect(labelerState.detecting) {
        if (labelerState.detecting) {
            petState = ClawdPetState.THINKING
            frameIndex = 0
            while (labelerState.detecting) {
                currentFrame = ClawdPetAssets.THINKING_FRAMES[frameIndex % ClawdPetAssets.THINKING_FRAMES.size]
                frameIndex++
                delay(ClawdPetAssets.FRAME_INTERVAL_MS)
            }
        } else if (petState != ClawdPetState.INTRO) {
            petState = ClawdPetState.IDLE
        }
    }

    LaunchedEffect(petState) {
        if (petState != ClawdPetState.IDLE) return@LaunchedEffect
        frameIndex = 0
        while (petState == ClawdPetState.IDLE) {
            currentFrame = ClawdPetAssets.IDLE_FRAMES[frameIndex % ClawdPetAssets.IDLE_FRAMES.size]
            frameIndex++
            delay(ClawdPetAssets.FRAME_INTERVAL_MS * 4)
        }
    }

    ClawdPetWebView(frameFile = currentFrame, modifier = modifier.size(48.dp))
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ClawdPetWebView(frameFile: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.allowFileAccess = true
            }
        },
        update = { view ->
            view.loadUrl("file:///android_asset/${ClawdPetAssets.ASSET_DIR}/$frameFile")
        },
        modifier = modifier.fillMaxSize()
    )
}
