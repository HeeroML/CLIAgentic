package com.cliagentic.mobileterminal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.PtyType
import com.cliagentic.mobileterminal.data.model.TmuxPrefix
import com.cliagentic.mobileterminal.ui.state.ProfileEditorUiState
import com.cliagentic.mobileterminal.ui.theme.JetBrainsMono

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
    onPtyTypeChange: (PtyType) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.id == 0L) "New Profile" else "Edit Profile",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onSave,
                        enabled = !state.isSaving,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection section
            SectionHeader("Connection")

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Profile name") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = state.host,
                onValueChange = onHostChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Host") },
                placeholder = { Text("devbox.example.com") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.port,
                    onValueChange = onPortChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Port") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier.weight(2f),
                    label = { Text("Username") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Authentication section
            SectionHeader("Authentication")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthType.entries.forEach { type ->
                    FilterChip(
                        selected = state.authType == type,
                        onClick = { onAuthTypeChange(type) },
                        label = { Text(type.name) },
                        leadingIcon = if (state.authType == type) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
                            Text(
                                "Password stored \u2022 leave blank to keep",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                )
            } else {
                OutlinedTextField(
                    value = state.privateKey,
                    onValueChange = onPrivateKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Private key") },
                    placeholder = {
                        Text(
                            "-----BEGIN OPENSSH PRIVATE KEY-----",
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp)
                        )
                    },
                    supportingText = {
                        Column {
                            Text("PEM or OpenSSH format")
                            if (state.hasStoredPrivateKey) {
                                Text(
                                    "Key stored \u2022 leave blank to keep",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    minLines = 4,
                    maxLines = 8,
                    textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    shape = MaterialTheme.shapes.medium
                )

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    "Biometric unlock",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Require fingerprint to use key",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.biometricForKey,
                            onCheckedChange = onBiometricToggle
                        )
                    }
                }
            }

            // Tmux section
            SectionHeader("tmux Prefix")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TmuxPrefix.entries.forEach { prefix ->
                    FilterChip(
                        selected = state.tmuxPrefix == prefix,
                        onClick = { onTmuxPrefixChange(prefix) },
                        label = { Text(prefix.label) },
                        leadingIcon = if (state.tmuxPrefix == prefix) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            SectionHeader("Terminal Type")

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PtyType.entries.forEach { ptyType ->
                    FilterChip(
                        selected = state.ptyType == ptyType,
                        onClick = { onPtyTypeChange(ptyType) },
                        label = { Text(ptyType.label) },
                        leadingIcon = if (state.ptyType == ptyType) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            // Errors
            if (state.errors.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.errors.forEach { error ->
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (state.isSaving) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}
