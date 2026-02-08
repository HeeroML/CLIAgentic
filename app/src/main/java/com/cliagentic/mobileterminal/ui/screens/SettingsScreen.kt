package com.cliagentic.mobileterminal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cliagentic.mobileterminal.data.model.DictationEngineType
import com.cliagentic.mobileterminal.ui.state.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onVoiceAppendNewlineChange: (Boolean) -> Unit,
    onDictationEngineChange: (DictationEngineType) -> Unit,
    onMoshFeatureFlagChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImportJsonChange: (String) -> Unit,
    onImport: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onClearStatus: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Voice append newline")
                    Text(
                        "Automatically send Enter after dictated text",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = state.settings.voiceAppendNewline,
                    onCheckedChange = onVoiceAppendNewlineChange
                )
            }

            Text("Dictation engine", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DictationEngineType.entries.forEach { engine ->
                    FilterChip(
                        selected = state.settings.preferredDictationEngine == engine,
                        onClick = { onDictationEngineChange(engine) },
                        label = { Text(engine.name) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mosh feature flag (future)")
                    Text(
                        "UI-level flag only. Mosh protocol is not bundled in MVP due GPL/licensing review.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = state.settings.moshEnabledFlag,
                    onCheckedChange = onMoshFeatureFlagChange
                )
            }

            Text("Import / Export", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onExport) { Text("Export JSON") }
                TextButton(onClick = onImport) { Text("Import JSON") }
            }

            if (state.exportJson.isNotBlank()) {
                OutlinedTextField(
                    value = state.exportJson,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Exported JSON") },
                    readOnly = true,
                    minLines = 4,
                    maxLines = 12,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }

            OutlinedTextField(
                value = state.importJson,
                onValueChange = onImportJsonChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Paste JSON to import") },
                minLines = 4,
                maxLines = 12,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )

            state.statusMessage?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onClearStatus) { Text("Clear") }
                }
            }

            TextButton(onClick = onOpenPrivacy) {
                Text("Privacy")
            }
        }
    }
}
