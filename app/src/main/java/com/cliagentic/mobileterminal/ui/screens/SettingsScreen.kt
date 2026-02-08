package com.cliagentic.mobileterminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.data.model.DictationEngineType
import com.cliagentic.mobileterminal.data.model.TerminalSkins
import com.cliagentic.mobileterminal.ui.state.SettingsUiState
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onVoiceAppendNewlineChange: (Boolean) -> Unit,
    onDictationEngineChange: (DictationEngineType) -> Unit,
    onMoshFeatureFlagChange: (Boolean) -> Unit,
    onTerminalSkinChange: (String) -> Unit,
    onExport: () -> Unit,
    onImportJsonChange: (String) -> Unit,
    onImport: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onClearStatus: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Terminal Skin Picker
            SectionLabel("Terminal Skin")

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(TerminalSkins.all) { skin ->
                    val isSelected = state.settings.terminalSkinId == skin.id
                    Card(
                        onClick = { onTerminalSkinChange(skin.id) },
                        modifier = Modifier
                            .width(140.dp)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(16.dp)
                                ) else Modifier
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(skin.background)
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        "$ ssh user@host",
                                        style = TextStyle(
                                            fontFamily = JetBrainsMono,
                                            fontSize = 10.sp,
                                            color = skin.foreground
                                        )
                                    )
                                    Text(
                                        "Connected.",
                                        style = TextStyle(
                                            fontFamily = JetBrainsMono,
                                            fontSize = 10.sp,
                                            color = skin.accent
                                        )
                                    )
                                    Text(
                                        "~ $",
                                        style = TextStyle(
                                            fontFamily = JetBrainsMono,
                                            fontSize = 10.sp,
                                            color = skin.dimText
                                        )
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    skin.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Voice settings
            SectionLabel("Voice & Dictation")

            SettingsToggleCard(
                title = "Voice append newline",
                subtitle = "Automatically send Enter after dictated text",
                checked = state.settings.voiceAppendNewline,
                onCheckedChange = onVoiceAppendNewlineChange
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DictationEngineType.entries.forEach { engine ->
                    FilterChip(
                        selected = state.settings.preferredDictationEngine == engine,
                        onClick = { onDictationEngineChange(engine) },
                        label = { Text(engine.name.replace("_", " ")) },
                        leadingIcon = if (state.settings.preferredDictationEngine == engine) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Feature flags
            SectionLabel("Experimental")

            SettingsToggleCard(
                title = "Mosh protocol",
                subtitle = "UI-level flag only. Mosh is not bundled in MVP.",
                checked = state.settings.moshEnabledFlag,
                onCheckedChange = onMoshFeatureFlagChange
            )

            // Import / Export
            SectionLabel("Data")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onExport) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export")
                }
                FilledTonalButton(onClick = onImport) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import")
                }
            }

            if (state.exportJson.isNotBlank()) {
                OutlinedTextField(
                    value = state.exportJson,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Exported JSON") },
                    readOnly = true,
                    minLines = 3,
                    maxLines = 8,
                    textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    shape = MaterialTheme.shapes.medium
                )
            }

            OutlinedTextField(
                value = state.importJson,
                onValueChange = onImportJsonChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Paste JSON to import") },
                minLines = 3,
                maxLines = 8,
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                shape = MaterialTheme.shapes.medium
            )

            state.statusMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearStatus) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Privacy link
            Card(
                onClick = onOpenPrivacy,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Privacy", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "No analytics, all data on-device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
