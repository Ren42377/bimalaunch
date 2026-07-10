package com.ren42377.bimalaunch.ui.floating

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.ren42377.bimalaunch.R
import com.ren42377.bimalaunch.floating.FloatingState

@Composable
fun FloatingBubbleComposable(state: FloatingState) {
    Surface(
        modifier = Modifier
            .size(60.dp)
            .padding(2.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.bubble_icon_description),
                tint = Color.Unspecified,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
fun DiscardZoneComposable() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(bottom = 48.dp)
                .size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCircle(color = Color.White, radius = size.minDimension / 2f, center = center)
                }
            }
        }
    }
}
