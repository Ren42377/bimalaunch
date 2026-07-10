package com.ren42377.bimalaunch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ren42377.bimalaunch.R
import com.ren42377.bimalaunch.settings.SettingsRow
import com.ren42377.bimalaunch.settings.SettingsSchema
import com.ren42377.bimalaunch.ui.theme.CloudMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    schema: SettingsSchema,
    onBackClick: () -> Unit,
    onTextChange: (String, String) -> Unit,
    onChoiceChange: (String, String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit
) {
    var activeSection by remember { mutableStateOf("main") }

    val goBack = {
        if (activeSection != "main") {
            activeSection = "main"
        } else {
            onBackClick()
        }
    }

    BackHandler { goBack() }

    val section = schema.section(activeSection) ?: schema.section("main")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = section?.title ?: "Pengaturan",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                section?.rows?.forEach { row ->
                    when (row) {
                        is SettingsRow.Menu -> MenuCard(row) { activeSection = row.id }
                        is SettingsRow.TextField -> TextRow(row, onTextChange)
                        is SettingsRow.Choice -> ChoiceGroup(row, onChoiceChange)
                        is SettingsRow.Switch -> SwitchRow(row, onSwitchChange)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(row: SettingsRow.Menu, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(row.title) },
        supportingContent = { Text(row.body) },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_majesticons_open_line),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CloudMedium, MaterialTheme.shapes.small)
            .clip(MaterialTheme.shapes.small)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    )
}

@Composable
private fun TextRow(row: SettingsRow.TextField, onTextChange: (String, String) -> Unit) {
    var value by remember(row.id) { mutableStateOf(row.value) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                onTextChange(row.id, it)
            },
            label = { Text(row.title) },
            visualTransformation = if (row.password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = CloudMedium
            )
        )
    }
}

@Composable
private fun ChoiceGroup(row: SettingsRow.Choice, onChoiceChange: (String, String) -> Unit) {
    var selected by remember(row.id) { mutableStateOf(row.selected) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CloudMedium, MaterialTheme.shapes.small)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(row.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.options.forEach { (id, label) ->
                ChoicePill(
                    text = label,
                    selected = selected == id,
                    onClick = {
                        selected = id
                        onChoiceChange(row.id, id)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) colorScheme.primary.copy(alpha = 0.16f) else colorScheme.surfaceContainerHighest)
            .border(
                1.dp,
                if (selected) colorScheme.primary else colorScheme.outlineVariant,
                MaterialTheme.shapes.small
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SwitchRow(row: SettingsRow.Switch, onSwitchChange: (String, Boolean) -> Unit) {
    var checked by remember(row.id) { mutableStateOf(row.value) }
    val colorScheme = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(row.title) },
        supportingContent = { Text(row.body) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onSwitchChange(row.id, it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorScheme.surfaceContainerLowest,
                    checkedTrackColor = colorScheme.primary,
                    checkedBorderColor = colorScheme.primary,
                    uncheckedTrackColor = colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = colorScheme.outlineVariant
                )
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CloudMedium, MaterialTheme.shapes.small)
            .clip(MaterialTheme.shapes.small)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                checked = !checked
                onSwitchChange(row.id, checked)
            },
        colors = ListItemDefaults.colors(containerColor = colorScheme.surfaceContainerLow)
    )
}
