package com.ren42377.bimalaunch.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ren42377.bimalaunch.ui.theme.EditorialTitle

@Composable
fun PermissionScreen(
    @DrawableRes iconRes: Int,
    titleText: String,
    descText: String,
    buttonText: String,
    onGrantClick: () -> Unit
) {
    val iconTransition = rememberInfiniteTransition(label = "permissionIcon")
    val iconScale = iconTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "permissionIconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .height(40.dp)
                            .graphicsLayer {
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                            },
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = titleText,
                        style = EditorialTitle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val btnScale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "btnScale")
            Button(
                onClick = onGrantClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(btnScale),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                interactionSource = interactionSource
            ) {
                Text(
                    text = buttonText.uppercase(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
