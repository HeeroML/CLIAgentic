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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.TmuxPrefix
import com.cliagentic.mobileterminal.ui.state.ProfileEditorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    state: ProfileEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onAuthTypeChange: (AuthType) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onTmuxPrefixChange: (TmuxPrefix) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == 0L) "New Profile" else "Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !state.isSaving) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Profile name") }
            )

            OutlinedTextField(
                value = state.host,
                onValueChange = onHostChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Host") },
                singleLine = true,
                supportingText = { Text("Example: devbox.example.com") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.port,
                    onValueChange = onPortChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Port") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier.weight(2f),
                    label = { Text("Username") },
                    singleLine = true
                )
            }

            Text("Authentication", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthType.entries.forEach { type ->
                    AssistChip(
                        onClick = { onAuthTypeChange(type) },
                        label = { Text(type.name) }
                    )
                }
            }

            if (state.authType == AuthType.PASSWORD) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    supportingText = {
                        if (state.hasStoredPassword) {
                            Text("A password is already stored; leave blank to keep it")
                        }
                    }
                )
            } else {
                OutlinedTextField(
                    value = state.privateKey,
                    onValueChange = onPrivateKeyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    label = { Text("Private key") },
                    placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----") },
                    supportingText = {
                        Column {
                            Text("PEM or OpenSSH format")
                            if (state.hasStoredPrivateKey) {
                                Text("A key is already stored; leave blank to keep it")
                            }
                        }
                    },
                    minLines = 5,
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Require biometric unlock for key")
                    Switch(checked = state.biometricForKey, onCheckedChange = onBiometricToggle)
                }
            }

            Text("tmux prefix", style = MaterialTheme.typography.titleSmall)
            var tmuxExpanded by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { tmuxExpanded = !tmuxExpanded }, label = { Text(state.tmuxPrefix.label) })
                TmuxPrefix.entries.forEach { prefix ->
                    AssistChip(onClick = { onTmuxPrefixChange(prefix) }, label = { Text(prefix.label) })
                }
            }

            if (state.errors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.errors.forEach { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (state.isSaving) {
                CircularProgressIndicator()
            }
        }
    }
}
