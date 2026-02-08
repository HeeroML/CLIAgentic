package com.cliagentic.mobileterminal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.ui.state.SessionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    state: SessionUiState,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onInputDraftChange: (String) -> Unit,
    onSendDraft: () -> Unit,
    onSendLiteral: (String) -> Unit,
    onSendBytes: (ByteArray) -> Unit,
    onToggleCtrl: () -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onWatchPatternInputChange: (String) -> Unit,
    onWatchTypeChange: (WatchRuleType) -> Unit,
    onAddWatchRule: () -> Unit,
    onRemoveWatchRule: (Long) -> Unit,
    onClearWatchRules: () -> Unit,
    onClearMatchLog: () -> Unit,
    onHostKeyDecision: (Boolean) -> Unit,
    onTmuxSessionSelected: (String) -> Unit,
    onCreateTmuxSession: () -> Unit,
    onDismissTmuxSelector: () -> Unit,
    onClearError: () -> Unit,
    onClearInfo: () -> Unit,
    onClearTerminal: () -> Unit,
    onDictationPreviewChange: (String) -> Unit,
    onStartDictation: () -> Unit,
    onStopDictation: () -> Unit,
    onSendDictation: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val view = LocalView.current

    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearInfo()
        }
    }

    if (state.hostKeyPrompt != null) {
        AlertDialog(
            onDismissRequest = { onHostKeyDecision(false) },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Trust new host key?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${state.hostKeyPrompt.host}:${state.hostKeyPrompt.port}")
                    Text("Algorithm: ${state.hostKeyPrompt.algorithm}")
                    Text("Fingerprint: ${state.hostKeyPrompt.fingerprint}", fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = { onHostKeyDecision(true) }) { Text("Trust") }
            },
            dismissButton = {
                TextButton(onClick = { onHostKeyDecision(false) }) { Text("Reject") }
            }
        )
    }

    if (state.showTmuxSessionSelector) {
        AlertDialog(
            onDismissRequest = onDismissTmuxSelector,
            title = { Text("Select tmux session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Found sessions on remote host:")
                    state.tmuxSessionChoices.forEach { sessionName ->
                        TextButton(
                            onClick = { onTmuxSessionSelected(sessionName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(sessionName, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onCreateTmuxSession) {
                    Text("Create new")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissTmuxSelector) {
                    Text("Later")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.profile?.name ?: "Session")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isConnected) {
                        TextButton(onClick = onDisconnect) { Text("Disconnect") }
                    } else {
                        TextButton(onClick = onConnect, enabled = !state.isConnecting) { Text("Connect") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (state.isConnected) "Connected" else "Disconnected",
                    color = if (state.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Keep screen on")
                    Switch(checked = state.keepScreenOn, onCheckedChange = onKeepScreenOnChange)
                }
            }

            ElevatedCard(modifier = Modifier.weight(1f)) {
                val terminalScroll = rememberScrollState()
                LaunchedEffect(state.terminalText.length) {
                    terminalScroll.animateScrollTo(terminalScroll.maxValue)
                }

                SelectionContainer {
                    Text(
                        text = if (state.terminalText.isBlank()) "Waiting for terminal output..." else state.terminalText,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(terminalScroll),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.inputDraft,
                    onValueChange = onInputDraftChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Terminal input") },
                    singleLine = true
                )
                IconButton(onClick = onSendDraft) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }

            SpecialKeysToolbar(
                ctrlArmed = state.ctrlArmed,
                tmuxPrefixLabel = state.profile?.tmuxPrefix?.label ?: "Prefix",
                onToggleCtrl = onToggleCtrl,
                onSendBytes = onSendBytes,
                onSendLiteral = onSendLiteral,
                onPaste = {
                    val text = clipboard.getText()?.text.orEmpty()
                    if (text.isNotBlank()) onSendLiteral(text)
                },
                onCopyVisible = {
                    clipboard.setText(AnnotatedString(state.terminalText))
                },
                onClearTerminal = onClearTerminal,
                tmuxPrefixByte = state.profile?.tmuxPrefix?.controlByte ?: 0x02
            )

            VoicePanel(
                state = state,
                onPreviewChange = onDictationPreviewChange,
                onStart = onStartDictation,
                onStop = onStopDictation,
                onSend = onSendDictation
            )

            WatchRulesPanel(
                state = state,
                onPatternChange = onWatchPatternInputChange,
                onTypeChange = onWatchTypeChange,
                onAddRule = onAddWatchRule,
                onRemoveRule = onRemoveWatchRule,
                onClearRules = onClearWatchRules,
                onClearMatchLog = onClearMatchLog
            )
        }
    }
}

@Composable
private fun SpecialKeysToolbar(
    ctrlArmed: Boolean,
    tmuxPrefixLabel: String,
    tmuxPrefixByte: Byte,
    onToggleCtrl: () -> Unit,
    onSendBytes: (ByteArray) -> Unit,
    onSendLiteral: (String) -> Unit,
    onPaste: () -> Unit,
    onCopyVisible: () -> Unit,
    onClearTerminal: () -> Unit
) {
    val items = listOf(
        "Esc" to byteArrayOf(0x1B),
        "Tab" to byteArrayOf('\t'.code.toByte()),
        "Alt" to byteArrayOf(0x1B),
        "↑" to "\u001B[A".toByteArray(),
        "↓" to "\u001B[B".toByteArray(),
        "←" to "\u001B[D".toByteArray(),
        "→" to "\u001B[C".toByteArray(),
        "PgUp" to "\u001B[5~".toByteArray(),
        "PgDn" to "\u001B[6~".toByteArray(),
        "Home" to "\u001B[H".toByteArray(),
        "End" to "\u001B[F".toByteArray()
    )

    val scroll = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = ctrlArmed,
            onClick = onToggleCtrl,
            label = { Text("Ctrl") }
        )
        AssistChip(
            onClick = { onSendBytes(byteArrayOf(tmuxPrefixByte)) },
            label = { Text(tmuxPrefixLabel) }
        )
        items.forEach { (label, bytes) ->
            AssistChip(onClick = { onSendBytes(bytes) }, label = { Text(label) })
        }
        AssistChip(onClick = { onSendLiteral("\u0003") }, label = { Text("SIGINT") })
        IconButton(onClick = onPaste) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Paste")
        }
        IconButton(onClick = onCopyVisible) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
        }
        IconButton(onClick = onClearTerminal) {
            Icon(Icons.Default.Clear, contentDescription = "Clear terminal")
        }
    }
}

@Composable
private fun VoicePanel(
    state: SessionUiState,
    onPreviewChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Voice to terminal", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = if (state.isDictating) onStop else onStart) {
                    Icon(
                        imageVector = if (state.isDictating) Icons.Default.Pause else Icons.Default.KeyboardVoice,
                        contentDescription = "Dictate"
                    )
                }
                Text(if (state.isDictating) "Listening..." else "Tap mic to dictate")
            }
            OutlinedTextField(
                value = state.dictationPreview,
                onValueChange = onPreviewChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Preview before send") },
                minLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSend) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun WatchRulesPanel(
    state: SessionUiState,
    onPatternChange: (String) -> Unit,
    onTypeChange: (WatchRuleType) -> Unit,
    onAddRule: () -> Unit,
    onRemoveRule: (Long) -> Unit,
    onClearRules: () -> Unit,
    onClearMatchLog: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Get notified", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.watchPatternInput,
                    onValueChange = onPatternChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Pattern") },
                    singleLine = true
                )
                TextButton(onClick = onAddRule) { Text("Add") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = state.watchType == WatchRuleType.PREFIX,
                    onClick = { onTypeChange(WatchRuleType.PREFIX) },
                    label = { Text("Prefix") }
                )
                FilterChip(
                    selected = state.watchType == WatchRuleType.REGEX,
                    onClick = { onTypeChange(WatchRuleType.REGEX) },
                    label = { Text("Regex") }
                )
                TextButton(onClick = onClearRules) { Text("Clear rules") }
            }

            state.watchRules.forEach { rule ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${rule.type.name}: ${rule.pattern}")
                    TextButton(onClick = { onRemoveRule(rule.id) }) { Text("Remove") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Last matches (max 20)")
                TextButton(onClick = onClearMatchLog) { Text("Clear log") }
            }

            state.matchLog.take(20).forEach { match ->
                Text("• ${match.rulePattern} -> ${match.snippet}")
            }
        }
    }
}
