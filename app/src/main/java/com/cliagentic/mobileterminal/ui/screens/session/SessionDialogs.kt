package com.cliagentic.mobileterminal.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.ssh.HostKeyPrompt
import com.cliagentic.mobileterminal.ssh.KeyboardInteractivePrompt
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono

@Composable
internal fun HostKeyDialog(
    prompt: HostKeyPrompt,
    onDecision: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDecision(false) },
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = if (prompt.mismatchDetected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(if (prompt.mismatchDetected) "Host Key Changed" else "Verify Host Key")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${prompt.host}:${prompt.port}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Algorithm: ${prompt.algorithm}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (prompt.previousSha256Fingerprint != null) {
                    Text(
                        "Previously trusted: ${prompt.previousSha256Fingerprint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    prompt.sha256Fingerprint,
                    style = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    "MD5: ${prompt.md5Fingerprint}",
                    style = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = { onDecision(true) }) { Text("Trust") }
        },
        dismissButton = {
            TextButton(onClick = { onDecision(false) }) { Text("Reject") }
        }
    )
}

@Composable
internal fun PasswordPromptDialog(
    title: String,
    message: String,
    onSubmit: (String?) -> Unit
) {
    val value = remember { androidx.compose.runtime.mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onSubmit(null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = value.value,
                    onValueChange = { value.value = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Credential") }
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = { onSubmit(value.value) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubmit(null) }) { Text("Cancel") }
        }
    )
}

@Composable
internal fun KeyboardInteractivePromptDialog(
    prompt: KeyboardInteractivePrompt,
    onSubmit: (List<String>?) -> Unit
) {
    val values = remember(prompt.prompts) { mutableStateListOf<String>().apply { repeat(prompt.prompts.size) { add("") } } }
    AlertDialog(
        onDismissRequest = { onSubmit(null) },
        title = { Text(prompt.name.ifBlank { "Keyboard Interactive" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (prompt.instruction.isNotBlank()) {
                    Text(prompt.instruction, style = MaterialTheme.typography.bodySmall)
                }
                prompt.prompts.forEachIndexed { index, challenge ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { values[index] = it },
                        label = { Text(challenge.prompt.ifBlank { "Response ${index + 1}" }) },
                        singleLine = true,
                        visualTransformation = if (!challenge.echo) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = { onSubmit(values.toList()) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubmit(null) }) { Text("Cancel") }
        }
    )
}

@Composable
internal fun TmuxSessionSelectorDialog(
    sessions: List<String>,
    onSessionSelected: (String) -> Unit,
    onCreateSession: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select tmux session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Found sessions on remote host:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                sessions.forEach { sessionName ->
                    Card(
                        onClick = { onSessionSelected(sessionName) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Text(
                            sessionName,
                            modifier = Modifier.padding(12.dp),
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onCreateSession) { Text("New session") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}
