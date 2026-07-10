package com.ren42377.bimalaunch.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ren42377.bimalaunch.clawd.ClawdLabelClasses
import com.ren42377.bimalaunch.clawd.ClawdLabelerState

@Composable
fun ClawdLabelOverlay(state: ClawdLabelerState, modifier: Modifier = Modifier) {
    if (!state.drawBoxEnabled || state.boxes.isEmpty()) return
    val boxes = state.boxes
    val viewportWidth = state.viewportWidth
    val viewportHeight = state.viewportHeight

    Canvas(modifier = modifier.fillMaxSize()) {
        if (viewportWidth <= 0 || viewportHeight <= 0) return@Canvas
        val scaleX = size.width / viewportWidth
        val scaleY = size.height / viewportHeight
        boxes.forEach { (labelKey, rect) ->
            val color = ClawdLabelClasses.colorFor(labelKey).copy(alpha = 0.85f)
            val left = rect.left * scaleX
            val top = rect.top * scaleY
            val width = rect.width * scaleX
            val height = rect.height * scaleY
            drawRect(
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(width, height)
            )
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(width, height),
                style = Stroke(width = 3f)
            )
        }
    }
}
