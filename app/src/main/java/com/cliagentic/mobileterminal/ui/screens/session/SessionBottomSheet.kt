package com.cliagentic.mobileterminal.ui.screens.session

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.data.model.TerminalInputMode
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.ui.state.SessionUiState
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionBottomSheet(
    state: SessionUiState,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendBytes: (ByteArray) -> Unit,
    onSendLiteral: (String) -> Unit,
    onClearTerminal: () -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onInputModeChange: (TerminalInputMode) -> Unit,
    onDictationPreviewChange: (String) -> Unit,
    onStartDictation: () -> Unit,
    onStopDictation: () -> Unit,
    onSendDictation: () -> Unit,
    onWatchPatternInputChange: (String) -> Unit,
    onWatchTypeChange: (WatchRuleType) -> Unit,
    onAddWatchRule: () -> Unit,
    onRemoveWatchRule: (Long) -> Unit,
    onClearWatchRules: () -> Unit,
    onClearMatchLog: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── SESSION ──
            SectionLabel("SESSION")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(onClick = {
                    onDismiss()
                    onBack()
                }, label = { Text("\u2190 Back") })
                Spacer(Modifier.weight(1f))
                if (state.isConnected) {
                    AssistChip(
                        onClick = onDisconnect,
                        label = { Text("Disconnect") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                } else {
                    AssistChip(
                        onClick = onConnect,
                        enabled = !state.isConnecting,
                        label = { Text("Connect") }
                    )
                }
            }
            state.profile?.let { p ->
                Text(
                    "${p.name}  \u2022  ${p.username}@${p.host}:${p.port}",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionDivider()

            // ── KEYS ──
            SectionLabel("KEYS")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tmuxPrefixLabel = state.profile?.tmuxPrefix?.label ?: "Prefix"
                val tmuxPrefixByte = state.profile?.tmuxPrefix?.controlByte ?: 0x02
                AssistChip(
                    enabled = !state.isPreparingTmux,
                    onClick = { onSendBytes(byteArrayOf(tmuxPrefixByte)) },
                    label = { Text(tmuxPrefixLabel, fontWeight = FontWeight.SemiBold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                listOf(
                    "PgUp" to "\u001B[5~".toByteArray(),
                    "PgDn" to "\u001B[6~".toByteArray(),
                    "Home" to "\u001B[H".toByteArray(),
                    "End" to "\u001B[F".toByteArray()
                ).forEach { (label, bytes) ->
                    AssistChip(
                        enabled = !state.isPreparingTmux,
                        onClick = { onSendBytes(bytes) },
                        label = { Text(label) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
                AssistChip(
                    enabled = !state.isPreparingTmux,
                    onClick = { onSendLiteral("\u0003") },
                    label = { Text("SIGINT") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val entry = clipboard.getClipEntry()
                            val text = entry?.clipData
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.coerceToText(view.context)
                                ?.toString()
                                .orEmpty()
                            if (text.isNotBlank()) onSendLiteral(text)
                        }
                    },
                    enabled = !state.isPreparingTmux
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            val clipEntry = ClipData.newPlainText("terminal-output", state.terminalText).toClipEntry()
                            clipboard.setClipEntry(clipEntry)
                        }
                    },
                    enabled = !state.isPreparingTmux
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onClearTerminal, enabled = !state.isPreparingTmux) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                }
            }

            SectionDivider()

            // ── VOICE ──
            SectionLabel("VOICE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Voice Input",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = if (state.isDictating) onStopDictation else onStartDictation
                ) {
                    Icon(
                        imageVector = if (state.isDictating) Icons.Default.Pause else Icons.Default.KeyboardVoice,
                        contentDescription = "Dictate",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isDictating) "Stop" else "Record")
                }
            }
            OutlinedTextField(
                value = state.dictationPreview,
                onValueChange = onDictationPreviewChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Dictation preview...") },
                minLines = 2,
                shape = MaterialTheme.shapes.small,
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(onClick = onSendDictation) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send")
                }
            }

            SectionDivider()

            // ── WATCH ──
            SectionLabel("WATCH")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.watchPatternInput,
                    onValueChange = onWatchPatternInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Pattern...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp)
                )
                FilledTonalButton(onClick = onAddWatchRule) { Text("Add") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = state.watchType == WatchRuleType.PREFIX,
                    onClick = { onWatchTypeChange(WatchRuleType.PREFIX) },
                    label = { Text("Prefix") }
                )
                FilterChip(
                    selected = state.watchType == WatchRuleType.REGEX,
                    onClick = { onWatchTypeChange(WatchRuleType.REGEX) },
                    label = { Text("Regex") }
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClearWatchRules) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }
            state.watchRules.forEach { rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${rule.type.name}: ${rule.pattern}",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onRemoveWatchRule(rule.id) }) {
                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (state.matchLog.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent matches",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onClearMatchLog) {
                        Text("Clear log", style = MaterialTheme.typography.labelSmall)
                    }
                }
                state.matchLog.take(20).forEach { match ->
                    Text(
                        "\u2022 ${match.rulePattern} \u2192 ${match.snippet}",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionDivider()

            // ── DISPLAY ──
            SectionLabel("DISPLAY")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keep screen on", style = MaterialTheme.typography.bodySmall)
                Switch(checked = state.keepScreenOn, onCheckedChange = onKeepScreenOnChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRawMode = state.inputMode == TerminalInputMode.CONTROL
                Text(
                    text = if (isRawMode) "RAW mode" else "PROMPT mode",
                    style = MaterialTheme.typography.bodySmall
                )
                Switch(
                    checked = isRawMode,
                    onCheckedChange = { checked ->
                        onInputModeChange(
                            if (checked) TerminalInputMode.CONTROL else TerminalInputMode.PROMPT
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(4.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
