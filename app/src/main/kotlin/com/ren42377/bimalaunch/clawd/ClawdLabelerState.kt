package com.ren42377.bimalaunch.clawd

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

@Stable
class ClawdLabelerState {
    var boxes by mutableStateOf<Map<String, Rect>>(emptyMap())
    var status by mutableStateOf("")
    var detecting by mutableStateOf(false)
    var alwaysOnEnabled by mutableStateOf(false)
    var drawBoxEnabled by mutableStateOf(false)
    var lastLatencyMs by mutableStateOf(0L)
    var lastRuntime by mutableStateOf(ClawdRuntime.CPU)
    var viewportWidth by mutableStateOf(0)
    var viewportHeight by mutableStateOf(0)

    fun applyResult(result: ClawdModelDetector.Result) {
        boxes = result.boxes
        lastLatencyMs = result.latencyMs
        lastRuntime = result.runtime
        status = if (result.boxes.isEmpty()) "Tidak ada elemen terdeteksi" else "${result.boxes.size} elemen terdeteksi"
    }

    fun clear() {
        boxes = emptyMap()
        status = ""
    }
}

object ClawdLabelerBridge {
    val state = ClawdLabelerState()
}
