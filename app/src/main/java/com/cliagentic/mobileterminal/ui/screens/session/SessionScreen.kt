package com.cliagentic.mobileterminal.ui.screens.session

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.cliagentic.mobileterminal.data.model.TerminalInputMode
import com.cliagentic.mobileterminal.data.model.TerminalSkin
import com.cliagentic.mobileterminal.data.model.TerminalSkins
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.ssh.HostKeyPrompt
import com.cliagentic.mobileterminal.ssh.HostKeyPromptResponse
import com.cliagentic.mobileterminal.ssh.KeyboardInteractivePrompt
import com.cliagentic.mobileterminal.ssh.KeyboardInteractiveResponse
import com.cliagentic.mobileterminal.ssh.PassphrasePrompt
import com.cliagentic.mobileterminal.ssh.PassphrasePromptResponse
import com.cliagentic.mobileterminal.ssh.PasswordPrompt
import com.cliagentic.mobileterminal.ssh.PasswordPromptResponse
import com.cliagentic.mobileterminal.ssh.PromptCancelledResponse
import com.cliagentic.mobileterminal.ssh.SshPromptResponse
import com.cliagentic.mobileterminal.ui.state.SessionUiState

@Composable
fun SessionScreen(
    state: SessionUiState,
    terminalSkin: TerminalSkin = TerminalSkins.Dracula,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onInputDraftChange: (String) -> Unit,
    onSendDraft: () -> Unit,
    onSendLiteral: (String) -> Unit,
    onSendBytes: (ByteArray) -> Unit,
    onInputModeChange: (TerminalInputMode) -> Unit,
    onToggleCtrl: () -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onWatchPatternInputChange: (String) -> Unit,
    onWatchTypeChange: (WatchRuleType) -> Unit,
    onAddWatchRule: () -> Unit,
    onRemoveWatchRule: (Long) -> Unit,
    onClearWatchRules: () -> Unit,
    onClearMatchLog: () -> Unit,
    onSshPromptResponse: (SshPromptResponse) -> Unit,
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val promptFocusRequester = remember { FocusRequester() }
    val terminalFocusRequester = remember { FocusRequester() }
    val view = LocalView.current
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var hasSeenInitialInputMode by rememberSaveable { mutableStateOf(false) }
    var previousInputMode by rememberSaveable { mutableStateOf(state.inputMode) }
    var controlFocusArmed by rememberSaveable { mutableStateOf(false) }

    // Keep screen on
    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Snackbar for errors and info
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

    // Connected toast
    LaunchedEffect(state.isConnected) {
        if (state.isConnected) {
            Toast.makeText(view.context, "Connected. Keepalive active.", Toast.LENGTH_SHORT).show()
        }
    }

    // Route IME focus only on explicit mode switches to avoid startup layout jumps.
    LaunchedEffect(state.inputMode, state.isPreparingTmux) {
        if (state.isPreparingTmux) return@LaunchedEffect
        if (!hasSeenInitialInputMode) {
            hasSeenInitialInputMode = true
            previousInputMode = state.inputMode
            return@LaunchedEffect
        }
        if (previousInputMode == state.inputMode) return@LaunchedEffect
        previousInputMode = state.inputMode

        when (state.inputMode) {
            TerminalInputMode.CONTROL -> {
                terminalFocusRequester.requestFocus()
            }

            TerminalInputMode.PROMPT -> {
                promptFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    LaunchedEffect(state.isConnected, state.isPreparingTmux, state.inputMode) {
        if (!state.isConnected) {
            controlFocusArmed = false
            return@LaunchedEffect
        }
        if (state.inputMode != TerminalInputMode.CONTROL) {
            controlFocusArmed = false
            return@LaunchedEffect
        }
        if (state.isPreparingTmux || controlFocusArmed) return@LaunchedEffect

        terminalFocusRequester.requestFocus()
        controlFocusArmed = true
    }

    // ── Dialogs ──
    when (val prompt = state.pendingSshPrompt) {
        is HostKeyPrompt -> {
            HostKeyDialog(prompt = prompt) { trust ->
                onSshPromptResponse(HostKeyPromptResponse(trust))
            }
        }

        is PasswordPrompt -> {
            PasswordPromptDialog(
                title = "Password Required",
                message = prompt.message
            ) { value ->
                onSshPromptResponse(
                    if (value != null) PasswordPromptResponse(value) else PromptCancelledResponse
                )
            }
        }

        is PassphrasePrompt -> {
            PasswordPromptDialog(
                title = "Passphrase Required",
                message = prompt.message
            ) { value ->
                onSshPromptResponse(
                    if (value != null) PassphrasePromptResponse(value) else PromptCancelledResponse
                )
            }
        }

        is KeyboardInteractivePrompt -> {
            KeyboardInteractivePromptDialog(prompt = prompt) { values ->
                onSshPromptResponse(
                    if (values != null) KeyboardInteractiveResponse(values) else PromptCancelledResponse
                )
            }
        }

        null -> Unit
    }
    if (state.showTmuxSessionSelector) {
        TmuxSessionSelectorDialog(
            sessions = state.tmuxSessionChoices,
            onSessionSelected = onTmuxSessionSelected,
            onCreateSession = onCreateTmuxSession,
            onDismiss = onDismissTmuxSelector
        )
    }

    // ── Main layout ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(terminalSkin.background)
    ) {
        // Layer 1: Terminal output (full screen)
        TerminalOutputPane(
            terminalEmulator = state.terminalEmulator,
            terminalText = state.terminalText,
            isConnected = state.isConnected,
            terminalSkin = terminalSkin,
            keyboardEnabled = state.isConnected && state.inputMode == TerminalInputMode.CONTROL,
            showSoftKeyboard = state.isConnected &&
                state.inputMode == TerminalInputMode.CONTROL &&
                !state.isPreparingTmux,
            focusRequester = terminalFocusRequester,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        )

        ConnectionStatusBanner(
            isConnecting = state.isConnecting,
            isPreparingTmux = state.isPreparingTmux,
            commandPromptId = state.commandPromptId,
            commandRunning = state.commandRunning,
            lastExitCode = state.lastExitCode,
            terminalSkin = terminalSkin,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )

        // Bottom-aligned column with ime padding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Layer 2: Accessory keyboard row (always visible while connected)
            AccessoryKeyboardRow(
                visible = state.isConnected,
                enabled = !state.isPreparingTmux,
                inputMode = state.inputMode,
                ctrlArmed = state.ctrlArmed,
                terminalSkin = terminalSkin,
                onSendBytes = onSendBytes,
                onToggleCtrl = onToggleCtrl,
                onInputModeChange = onInputModeChange
            )

            // Layer 5: Prompt input bar (PROMPT mode only), anchored directly above IME
            PromptInputBar(
                visible = state.isConnected && state.inputMode == TerminalInputMode.PROMPT,
                inputDraft = state.inputDraft,
                enabled = !state.isPreparingTmux,
                terminalSkin = terminalSkin,
                focusRequester = promptFocusRequester,
                onInputDraftChange = onInputDraftChange,
                onSendDraft = onSendDraft
            )
        }

        // Layer 3: Control pill (top-right)
        ControlPill(
            isConnected = state.isConnected,
            isConnecting = state.isConnecting,
            terminalSkin = terminalSkin,
            onTap = { showBottomSheet = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 12.dp)
        )

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Layer 4: Bottom sheet
    if (showBottomSheet) {
        SessionBottomSheet(
            state = state,
            onDismiss = { showBottomSheet = false },
            onBack = onBack,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            onSendBytes = onSendBytes,
            onSendLiteral = onSendLiteral,
            onClearTerminal = onClearTerminal,
            onKeepScreenOnChange = onKeepScreenOnChange,
            onInputModeChange = onInputModeChange,
            onDictationPreviewChange = onDictationPreviewChange,
            onStartDictation = onStartDictation,
            onStopDictation = onStopDictation,
            onSendDictation = onSendDictation,
            onWatchPatternInputChange = onWatchPatternInputChange,
            onWatchTypeChange = onWatchTypeChange,
            onAddWatchRule = onAddWatchRule,
            onRemoveWatchRule = onRemoveWatchRule,
            onClearWatchRules = onClearWatchRules,
            onClearMatchLog = onClearMatchLog
        )
    }
}
