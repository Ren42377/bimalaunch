package com.ren42377.bimalaunch.ui.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ren42377.bimalaunch.core.RenderNode
import com.ren42377.bimalaunch.ui.theme.EditorialTitle
import com.ren42377.bimalaunch.ui.theme.MetadataStyle

@Composable
fun DynamicRenderer(
    node: RenderNode?,
    dialog: DialogState?,
    onAction: (RenderNode) -> Unit,
    onDismissDialog: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            node == null -> ErrorScreen("Manifest tidak tersedia")
            node.component == "menu" -> MenuScreen(node, onAction)
            else -> ErrorScreen("Komponen tidak dikenali: ${node.component}")
        }
    }

    if (dialog != null) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            confirmButton = {
                TextButton(onClick = onDismissDialog) { Text("Tutup") }
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) }
        )
    }
}

@Composable
private fun MenuScreen(node: RenderNode, onAction: (RenderNode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(text = node.title, style = EditorialTitle, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            text = node.subtitle.uppercase(),
            style = MetadataStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        node.children.forEach { child ->
            RenderComponent(child, onAction)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RenderComponent(node: RenderNode, onAction: (RenderNode) -> Unit) {
    when (node.component) {
        "card", "button" -> FeatureCard(node, onAction)
        else -> FeatureCard(node, onAction)
    }
}

@Composable
private fun FeatureCard(node: RenderNode, onAction: (RenderNode) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(node) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (node.subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = node.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!node.available) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "SEGERA HADIR",
                    style = MetadataStyle,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Memuat Native Core", style = MetadataStyle)
        }
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

data class DialogState(
    val title: String,
    val message: String
)
