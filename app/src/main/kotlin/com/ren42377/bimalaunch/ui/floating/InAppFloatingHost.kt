package com.ren42377.bimalaunch.ui.floating

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ren42377.bimalaunch.floating.FloatingGeometry
import com.ren42377.bimalaunch.floating.FloatingGeometryConfig
import com.ren42377.bimalaunch.floating.FloatingState
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun InAppFloatingHost(
    state: FloatingState,
    geometryConfig: FloatingGeometryConfig,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        val bubbleSize = with(density) { geometryConfig.bubbleSize.dp.toPx() }
        val edgeMargin = with(density) { geometryConfig.edgeMargin.dp.toPx() }
        val verticalMargin = with(density) { geometryConfig.verticalMargin.dp.toPx() }
        val panelCorner = with(density) { geometryConfig.panelCornerRadius.dp.toPx() }
        val baseMenuWidth = with(density) { geometryConfig.baseMenuWidth.dp.toPx() }
        val baseMenuHeight = with(density) { geometryConfig.baseMenuHeight.dp.toPx() }
        val discardStart = with(density) { geometryConfig.discardStartOffset.dp.toPx() }
        val discardRange = with(density) { geometryConfig.discardRange.dp.toPx() }

        var bubbleX by remember(screenWidth) { mutableFloatStateOf(0f) }
        var bubbleY by remember(screenHeight) {
            mutableFloatStateOf(
                ((screenHeight - bubbleSize) * geometryConfig.defaultBubbleYFraction).coerceAtLeast(0f)
            )
        }
        var contentSize by remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        var snapSerial by remember { mutableIntStateOf(0) }
        var snapTarget by remember { mutableFloatStateOf(0f) }
        val snap = remember { Animatable(0f) }
        val transition = remember { Animatable(0f) }
        var animating by remember { androidx.compose.runtime.mutableStateOf(false) }

        fun maxPanelWidth() = (screenWidth - edgeMargin * 2).coerceAtLeast(baseMenuWidth)
        fun maxPanelHeight() = (screenHeight - verticalMargin * 2).coerceAtLeast(baseMenuHeight)
        fun clampPanelX(v: Float, width: Float) =
            v.coerceIn(edgeMargin, (screenWidth - width - edgeMargin).coerceAtLeast(edgeMargin))
        fun clampPanelY(v: Float, height: Float) =
            v.coerceIn(verticalMargin, (screenHeight - height - verticalMargin).coerceAtLeast(verticalMargin))

        fun bubbleGeometry(): FloatingGeometry {
            val size = bubbleSize - with(density) { geometryConfig.bubbleInset.dp.toPx() } * 2
            return FloatingGeometry(
                x = bubbleX + with(density) { geometryConfig.bubbleInset.dp.toPx() },
                y = bubbleY + with(density) { geometryConfig.bubbleInset.dp.toPx() },
                width = size,
                height = size,
                cornerRadius = size / 2f
            )
        }

        fun targetGeometry(): FloatingGeometry {
            val width = baseMenuWidth.coerceIn(bubbleSize, maxPanelWidth())
            val height = baseMenuHeight.coerceIn(bubbleSize, maxPanelHeight())
            val dockedRight = bubbleX + bubbleSize / 2 >= screenWidth / 2
            state.bubbleDockedRight = dockedRight
            val x = if (dockedRight) screenWidth - width - edgeMargin else edgeMargin
            val y = bubbleY
            return FloatingGeometry(clampPanelX(x, width), clampPanelY(y, height), width, height, panelCorner)
        }

        LaunchedEffect(snapSerial) {
            if (snapSerial == 0 || screenWidth <= 0f) return@LaunchedEffect
            snap.snapTo(bubbleX)
            snap.animateTo(snapTarget, tween(geometryConfig.snapDurationMillis, easing = FastOutSlowInEasing)) {
                bubbleX = value.coerceIn(0f, (screenWidth - bubbleSize).coerceAtLeast(0f))
            }
            bubbleX = snapTarget
            state.bubbleDockedRight = bubbleX > 0f
        }

        LaunchedEffect(state.menuVisible) {
            if (state.menuVisible) {
                animating = true
                val start = bubbleGeometry()
                val target = targetGeometry()
                state.displayedPanel = state.activePanel
                state.menuGeometry = start
                state.menuContentAlpha = 0f
                transition.snapTo(0f)
                transition.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) {
                    state.menuGeometry = start.interpolate(target, value)
                    state.menuContentAlpha = value
                    state.bubbleAlpha = if (value < 0.5f) 1f - value * 2f else 0f
                }
                state.menuGeometry = target
                state.menuContentAlpha = 1f
                state.bubbleAlpha = 0f
                animating = false
            } else {
                if (state.menuContentAlpha == 0f && state.bubbleAlpha == 1f) return@LaunchedEffect
                animating = true
                val start = state.menuGeometry.copy(
                    width = max(state.menuGeometry.width, bubbleSize),
                    height = max(state.menuGeometry.height, bubbleSize)
                )
                val target = bubbleGeometry()
                state.menuGeometry = start
                state.bubbleAlpha = 0f
                transition.snapTo(0f)
                transition.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) {
                    state.menuGeometry = start.interpolate(target, value)
                    state.menuContentAlpha = (1f - value).coerceIn(0f, 1f)
                    if (value >= 0.5f) state.bubbleAlpha = (value - 0.5f) * 2f
                }
                state.menuContentAlpha = 0f
                state.bubbleAlpha = 1f
                animating = false
            }
        }

        val showBubble = !state.menuVisible || animating

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.discardVisible) {
                Box(modifier = Modifier.fillMaxSize().alpha(state.discardAlpha)) {
                    DiscardZoneComposable()
                }
            }

            if (state.menuVisible && !animating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { state.onDismissMenu() }
                )
            }

            if (state.menuVisible || animating) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(state.menuGeometry.x.roundToInt(), state.menuGeometry.y.roundToInt()) }
                        .size(
                            with(density) { state.menuGeometry.width.toDp() },
                            with(density) { state.menuGeometry.height.toDp() }
                        )
                        .alpha(state.menuContentAlpha)
                        .onSizeChanged { contentSize = it }
                ) {
                    FloatingMenuComposable(state)
                }
            }

            if (showBubble) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) }
                        .size(geometryConfig.bubbleSize.dp)
                        .alpha(state.bubbleAlpha)
                        .pointerInput(screenWidth, screenHeight, state.menuVisible, animating) {
                            if (state.menuVisible || animating) return@pointerInput
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    bubbleX = (bubbleX + dragAmount.x).coerceIn(0f, (screenWidth - bubbleSize).coerceAtLeast(0f))
                                    bubbleY = (bubbleY + dragAmount.y).coerceIn(0f, (screenHeight - bubbleSize).coerceAtLeast(0f))
                                    state.bubbleDockedRight = bubbleX + bubbleSize / 2 >= screenWidth / 2
                                    val centerY = bubbleY + bubbleSize / 2
                                    val startAt = screenHeight - discardStart
                                    if (centerY >= startAt) {
                                        state.discardVisible = true
                                        state.discardAlpha = max(0.45f, ((centerY - startAt) / discardRange).coerceIn(0f, 1f))
                                    } else {
                                        state.discardVisible = false
                                        state.discardAlpha = 0f
                                    }
                                },
                                onDragEnd = {
                                    if (state.discardVisible) {
                                        state.discardVisible = false
                                        state.discardAlpha = 0f
                                        state.onBubbleDiscard()
                                    } else {
                                        state.discardVisible = false
                                        state.discardAlpha = 0f
                                        snapTarget = if (bubbleX + bubbleSize / 2 >= screenWidth / 2) {
                                            (screenWidth - bubbleSize).coerceAtLeast(0f)
                                        } else {
                                            0f
                                        }
                                        snapSerial += 1
                                    }
                                },
                                onDragCancel = {
                                    state.discardVisible = false
                                    state.discardAlpha = 0f
                                }
                            )
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!state.menuVisible && !animating) state.onBubbleClick()
                        }
                ) {
                    FloatingBubbleComposable(state)
                }
            }
        }
    }
}
