package com.ren42377.bimalaunch.ui.floating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ren42377.bimalaunch.R
import com.ren42377.bimalaunch.clawd.ClawdLabelerBridge
import com.ren42377.bimalaunch.floating.FloatingPanel
import com.ren42377.bimalaunch.floating.FloatingState
import com.ren42377.bimalaunch.floating.FloatingToolItem

private fun iconResFor(name: String): Int {
    return when (name) {
        "ic_clawd_icon" -> R.drawable.ic_clawd_icon
        "ic_ri_claude_fill" -> R.drawable.ic_ri_claude_fill
        "ic_lsicon_settings_outline" -> R.drawable.ic_lsicon_settings_outline
        else -> R.drawable.ic_launcher_foreground
    }
}

@Composable
fun FloatingMenuComposable(state: FloatingState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        when (state.displayedPanel) {
            FloatingPanel.TOOLS -> ToolsPanel(state)
            FloatingPanel.CLAWD -> ClawdPanel(state)
        }
    }
}

@Composable
private fun ToolsPanel(state: FloatingState) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = state.menuTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
        )
        state.toolItems.forEach { item ->
            ToolRow(item = item, onClick = { state.onToolClick(item) })
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ClawdPanel(state: FloatingState) {
    val labelerState = ClawdLabelerBridge.state
    Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = state.onClawdBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Clawd",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            ClawdPetBackground(labelerState = labelerState)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = labelerState.status.ifEmpty { "Siap mendeteksi" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !labelerState.detecting, onClick = state.onClawdDetectClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cil_magnifying_glass),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (labelerState.detecting) "Mendeteksi..." else "Detect",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        SwitchRow(
            title = "Always On",
            checked = labelerState.alwaysOnEnabled,
            onCheckedChange = state.onClawdAlwaysOnChange
        )
        SwitchRow(
            title = "Draw Box",
            checked = labelerState.drawBoxEnabled,
            onCheckedChange = state.onClawdDrawBoxChange
        )
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ToolRow(item: FloatingToolItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun resolveToolItems(raw: List<Pair<Triple<String, String, String>, String>>): List<FloatingToolItem> {
    return raw.map { (triple, action) ->
        FloatingToolItem(
            id = triple.first,
            title = triple.second,
            iconRes = iconResFor(triple.third),
            action = action
        )
    }
}

