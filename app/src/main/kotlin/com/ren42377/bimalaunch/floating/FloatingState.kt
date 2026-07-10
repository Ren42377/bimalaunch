package com.ren42377.bimalaunch.floating

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class FloatingPanel {
    TOOLS
}

data class FloatingToolItem(
    val id: String,
    val title: String,
    val iconRes: Int,
    val action: String
)

data class FloatingGeometry(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val cornerRadius: Float
) {
    fun interpolate(target: FloatingGeometry, fraction: Float): FloatingGeometry {
        val p = fraction.coerceIn(0f, 1f)
        if (p == 0f) return this
        if (p == 1f) return target
        return FloatingGeometry(
            x = x + (target.x - x) * p,
            y = y + (target.y - y) * p,
            width = width + (target.width - width) * p,
            height = height + (target.height - height) * p,
            cornerRadius = cornerRadius + (target.cornerRadius - cornerRadius) * p
        )
    }
}

@Stable
class FloatingState {
    var visible by mutableStateOf(false)
    var menuVisible by mutableStateOf(false)
    var activePanel by mutableStateOf(FloatingPanel.TOOLS)
    var displayedPanel by mutableStateOf(FloatingPanel.TOOLS)

    var menuGeometry by mutableStateOf(FloatingGeometry(0f, 0f, 56f, 56f, 28f))
    var menuContentAlpha by mutableStateOf(0f)
    var bubbleAlpha by mutableStateOf(1f)
    var bubbleDockedRight by mutableStateOf(false)

    var discardVisible by mutableStateOf(false)
    var discardAlpha by mutableStateOf(0f)

    var toolItems by mutableStateOf<List<FloatingToolItem>>(emptyList())
    var menuTitle by mutableStateOf("Menu")

    var onBubbleClick: () -> Unit = {}
    var onBubbleDiscard: () -> Unit = {}
    var onToolClick: (FloatingToolItem) -> Unit = {}
    var onDismissMenu: () -> Unit = {}
}
