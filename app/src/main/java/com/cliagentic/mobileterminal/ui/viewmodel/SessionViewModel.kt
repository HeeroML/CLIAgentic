package com.cliagentic.mobileterminal.ui.viewmodel

import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.TerminalInputMode
import com.cliagentic.mobileterminal.data.model.WatchRule
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.data.repository.ConnectionProfileRepository
import com.cliagentic.mobileterminal.data.repository.SettingsRepository
import com.cliagentic.mobileterminal.notifications.WatchNotificationManager
import com.cliagentic.mobileterminal.ssh.CommandBoundaryType
import com.cliagentic.mobileterminal.ssh.HostKeyPrompt
import com.cliagentic.mobileterminal.ssh.HostKeyPromptResponse
import com.cliagentic.mobileterminal.ssh.KeyboardInteractivePrompt
import com.cliagentic.mobileterminal.ssh.KeyboardInteractiveResponse
import com.cliagentic.mobileterminal.ssh.PassphrasePrompt
import com.cliagentic.mobileterminal.ssh.PassphrasePromptResponse
import com.cliagentic.mobileterminal.ssh.PasswordPrompt
import com.cliagentic.mobileterminal.ssh.PasswordPromptResponse
import com.cliagentic.mobileterminal.ssh.PromptCancelledResponse
import com.cliagentic.mobileterminal.ssh.SshClient
import com.cliagentic.mobileterminal.ssh.SshConnectRequest
import com.cliagentic.mobileterminal.ssh.SshErrorMapper
import com.cliagentic.mobileterminal.ssh.SshEvent
import com.cliagentic.mobileterminal.ssh.SshPrompt
import com.cliagentic.mobileterminal.ssh.SshPromptResponse
import com.cliagentic.mobileterminal.ssh.SshSession
import com.cliagentic.mobileterminal.terminal.TmuxSessionDiscovery
import com.cliagentic.mobileterminal.terminal.TmuxSessionDiscoveryParser
import com.cliagentic.mobileterminal.terminal.TerminalBuffer
import com.cliagentic.mobileterminal.terminal.WatchRuleMatcher
import com.cliagentic.mobileterminal.ui.state.SessionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.connectbot.terminal.TerminalEmulator
import org.connectbot.terminal.TerminalEmulatorFactory
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import kotlin.math.ceil

class SessionViewModel(
    private val profileId: Long,
    private val profileRepository: ConnectionProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val sshClient: SshClient,
    private val notificationManager: WatchNotificationManager
) : ViewModel() {

    private val terminalBuffer = TerminalBuffer()
    private val matcher = WatchRuleMatcher()

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var activeSession: SshSession? = null
    private var outputJob: Job? = null
    private var eventJob: Job? = null
    private var tmuxBootstrapJob: Job? = null
    private var promptDecision: CompletableDeferred<SshPromptResponse>? = null
    private val watchTextDecoder = Utf8StreamDecoder()

    init {
        viewModelScope.launch {
            profileRepository.observeProfile(profileId).collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }

        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        voiceAppendNewline = settings.voiceAppendNewline,
                        dictationEngineType = settings.preferredDictationEngine
                    )
                }
            }
        }
    }

    fun connect(biometricUnlocked: Boolean) {
        if (_uiState.value.isConnecting || _uiState.value.isConnected) return

        viewModelScope.launch {
            val profile = profileRepository.getProfile(profileId)
            if (profile == null) {
                _uiState.update { it.copy(errorMessage = "Profile not found") }
                return@launch
            }

            if (profile.authType == AuthType.KEY && profile.biometricForKey && !biometricUnlocked) {
                _uiState.update { it.copy(errorMessage = "Biometric unlock required to use this key") }
                return@launch
            }

            val secrets = profileRepository.loadSecrets(profile.id)

            val request = SshConnectRequest(
                host = profile.host,
                port = profile.port,
                username = profile.username,
                password = secrets.password,
                privateKey = secrets.privateKey,
                useKeyAuth = profile.authType == AuthType.KEY,
                ptyType = profile.ptyType.term
            )

            _uiState.update { it.copy(isConnecting = true, errorMessage = null, infoMessage = null) }

            val result = sshClient.connect(request) { prompt ->
                awaitPromptDecision(prompt)
            }

            result.onSuccess { session ->
                activeSession = session
                watchTextDecoder.reset()
                val terminalEmulator = createTerminalEmulator()
                notificationManager.notifySessionActive(profile.name)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        isPreparingTmux = true,
                        pendingSshPrompt = null,
                        terminalEmulator = terminalEmulator,
                        infoMessage = "Connected to ${profile.host}. Preparing tmux...",
                        errorMessage = null
                    )
                }
                observeSession(session, terminalEmulator)
                tmuxBootstrapJob?.cancel()
                tmuxBootstrapJob = viewModelScope.launch {
                    bootstrapTmuxSession()
                }
            }.onFailure { throwable ->
                watchTextDecoder.reset()
                notificationManager.clearSessionActive()
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        pendingSshPrompt = null,
                        errorMessage = SshErrorMapper.toMessage(throwable)
                    )
                }
            }
        }
    }

    private fun createTerminalEmulator(): TerminalEmulator {
        return TerminalEmulatorFactory.create(
            looper = Looper.getMainLooper(),
            initialRows = 24,
            initialCols = 80,
            onKeyboardInput = { bytes ->
                viewModelScope.launch {
                    activeSession?.send(bytes)
                }
            },
            onResize = { dimensions ->
                viewModelScope.launch {
                    activeSession?.resizePty(
                        cols = dimensions.columns,
                        rows = dimensions.rows,
                        widthPx = 0,
                        heightPx = 0
                    )
                }
            }
        )
    }

    private fun observeSession(session: SshSession, terminalEmulator: TerminalEmulator) {
        outputJob?.cancel()
        eventJob?.cancel()

        outputJob = viewModelScope.launch {
            session.output.collect { chunk ->
                terminalEmulator.writeInput(chunk, 0, chunk.size)
                val completedLines = terminalBuffer.append(watchTextDecoder.decode(chunk))
                _uiState.update { it.copy(terminalText = terminalBuffer.renderedText.value) }

                val currentRules = _uiState.value.watchRules
                if (currentRules.isNotEmpty()) {
                    val matches = matcher.match(completedLines, currentRules)
                    if (matches.isNotEmpty()) {
                        val updatedLog = (matches + _uiState.value.matchLog).take(20)
                        _uiState.update { it.copy(matchLog = updatedLog) }

                        val sessionLabel = _uiState.value.profile?.name ?: "Terminal Session"
                        matches.forEach { notificationManager.notifyWatchMatch(sessionLabel, it) }
                    }
                }
            }
        }

        eventJob = viewModelScope.launch {
            session.events.collect { event ->
                when (event) {
                    is SshEvent.CommandBoundary -> {
                        _uiState.update {
                            when (event.type) {
                                CommandBoundaryType.PROMPT -> it.copy(
                                    commandPromptId = event.promptId,
                                    commandRunning = false
                                )

                                CommandBoundaryType.COMMAND_INPUT_START,
                                CommandBoundaryType.COMMAND_OUTPUT_START -> it.copy(
                                    commandPromptId = event.promptId,
                                    commandRunning = true
                                )

                                CommandBoundaryType.COMMAND_FINISHED -> it.copy(
                                    commandPromptId = event.promptId,
                                    commandRunning = false,
                                    lastExitCode = event.exitCode
                                )
                            }
                        }
                    }

                    is SshEvent.Disconnected -> {
                        notificationManager.clearSessionActive()
                        _uiState.update {
                            it.copy(
                                isConnected = false,
                                isConnecting = false,
                                isPreparingTmux = false,
                                infoMessage = event.reason ?: "Disconnected"
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private suspend fun bootstrapTmuxSession() {
        val session = activeSession ?: return
        _uiState.update {
            it.copy(
                isPreparingTmux = true,
                showTmuxSessionSelector = false,
                tmuxSessionChoices = emptyList(),
                infoMessage = "Checking tmux sessions..."
            )
        }
        val discoveryCommand = """
            if ! command -v tmux >/dev/null 2>&1; then
              echo "${TmuxSessionDiscoveryParser.MISSING_MARKER}"
              exit 127
            fi
            tmux list-sessions -F '#S' 2>/dev/null || true
        """.trimIndent()

        try {
            val commandResult = session.execute(discoveryCommand).getOrElse { throwable ->
                _uiState.update { it.copy(infoMessage = "tmux probe failed: ${throwable.message}") }
                return
            }

            when (
                val discovery = TmuxSessionDiscoveryParser.parse(
                    stdout = commandResult.stdout,
                    exitCode = commandResult.exitCode
                )
            ) {
                TmuxSessionDiscovery.Missing -> {
                    _uiState.update {
                        it.copy(infoMessage = "tmux is not installed on the remote host")
                    }
                }

                is TmuxSessionDiscovery.Found -> {
                    when {
                        discovery.sessions.isEmpty() -> {
                            attachOrCreateTmuxSession(_uiState.value.tmuxDefaultSessionName)
                            _uiState.update {
                                it.copy(infoMessage = "Created tmux session: ${it.tmuxDefaultSessionName}")
                            }
                        }

                        discovery.sessions.size == 1 -> {
                            attachTmuxSession(discovery.sessions.first())
                            _uiState.update {
                                it.copy(infoMessage = "Attached tmux session: ${discovery.sessions.first()}")
                            }
                        }

                        else -> {
                            _uiState.update {
                                it.copy(
                                    showTmuxSessionSelector = true,
                                    tmuxSessionChoices = discovery.sessions,
                                    infoMessage = "Select tmux session"
                                )
                            }
                        }
                    }
                }
            }
        } finally {
            _uiState.update { it.copy(isPreparingTmux = false) }
        }
    }

    fun attachSelectedTmuxSession(sessionName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingTmux = true) }
            runCatching { attachTmuxSession(sessionName) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isPreparingTmux = false,
                            showTmuxSessionSelector = false,
                            tmuxSessionChoices = emptyList(),
                            infoMessage = "Attached tmux session: $sessionName"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isPreparingTmux = false,
                            errorMessage = "Failed to attach tmux session: ${throwable.message}"
                        )
                    }
                }
        }
    }

    fun createAndAttachTmuxSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingTmux = true) }
            val base = _uiState.value.tmuxDefaultSessionName
            val created = "$base-${System.currentTimeMillis().toString().takeLast(4)}"
            runCatching { attachOrCreateTmuxSession(created) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isPreparingTmux = false,
                            showTmuxSessionSelector = false,
                            tmuxSessionChoices = emptyList(),
                            infoMessage = "Created tmux session: $created"
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isPreparingTmux = false,
                            errorMessage = "Failed to create tmux session: ${throwable.message}"
                        )
                    }
                }
        }
    }

    fun dismissTmuxSessionSelector() {
        _uiState.update { it.copy(showTmuxSessionSelector = false) }
    }

    private suspend fun attachTmuxSession(sessionName: String) {
        val command = "tmux attach-session -t ${shellQuote(sessionName)}\n"
        activeSession?.send(command)
    }

    private suspend fun attachOrCreateTmuxSession(sessionName: String) {
        val command = "tmux new-session -A -s ${shellQuote(sessionName)}\n"
        activeSession?.send(command)
    }

    private fun shellQuote(value: String): String {
        return "'${value.replace("'", "'\"'\"'")}'"
    }

    fun disconnect() {
        viewModelScope.launch {
            outputJob?.cancel()
            outputJob = null
            eventJob?.cancel()
            eventJob = null
            tmuxBootstrapJob?.cancel()
            tmuxBootstrapJob = null
            activeSession?.disconnect()
            activeSession = null
            watchTextDecoder.reset()
            promptDecision?.let { deferred ->
                if (!deferred.isCompleted) deferred.complete(PromptCancelledResponse)
            }
            promptDecision = null
            notificationManager.clearSessionActive()
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    isConnected = false,
                    isPreparingTmux = false,
                    pendingSshPrompt = null,
                    terminalEmulator = null,
                    infoMessage = "Disconnected",
                    showTmuxSessionSelector = false,
                    tmuxSessionChoices = emptyList()
                )
            }
        }
    }

    fun onInputDraftChange(value: String) {
        _uiState.update { it.copy(inputDraft = value) }
    }

    fun onDictationPreviewChange(value: String) {
        _uiState.update { it.copy(dictationPreview = value) }
    }

    fun setDictating(active: Boolean) {
        _uiState.update { it.copy(isDictating = active) }
    }

    fun sendDraft() {
        if (_uiState.value.isPreparingTmux) return
        val draft = _uiState.value.inputDraft
        if (draft.isBlank()) return

        val prepared = if (_uiState.value.ctrlArmed) {
            val first = draft.first().code and 0x1F
            first.toChar().toString() + draft.drop(1)
        } else {
            draft
        }

        viewModelScope.launch {
            activeSession?.send("$prepared\n")
            _uiState.update { it.copy(inputDraft = "", ctrlArmed = false) }
        }
    }

    fun sendDictation() {
        if (_uiState.value.isPreparingTmux) return
        val text = _uiState.value.dictationPreview.trim()
        if (text.isBlank()) return

        val payload = if (_uiState.value.voiceAppendNewline) "$text\n" else text
        viewModelScope.launch {
            activeSession?.send(payload)
            _uiState.update { it.copy(dictationPreview = "") }
        }
    }

    fun sendBytes(bytes: ByteArray) {
        if (_uiState.value.isPreparingTmux) return
        viewModelScope.launch {
            activeSession?.send(bytes)
            if (_uiState.value.ctrlArmed) {
                _uiState.update { it.copy(ctrlArmed = false) }
            }
        }
    }

    fun sendLiteral(text: String) {
        if (_uiState.value.isPreparingTmux) return
        if (text.isEmpty()) return
        viewModelScope.launch {
            activeSession?.send(text)
            if (_uiState.value.ctrlArmed) {
                _uiState.update { it.copy(ctrlArmed = false) }
            }
        }
    }

    fun toggleCtrlArmed() {
        _uiState.update { it.copy(ctrlArmed = !it.ctrlArmed) }
    }

    fun setInputMode(mode: TerminalInputMode) {
        _uiState.update { it.copy(inputMode = mode, ctrlArmed = false, inputDraft = "") }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenOn = enabled) }
    }

    fun onWatchPatternInputChange(value: String) {
        _uiState.update { it.copy(watchPatternInput = value) }
    }

    fun onWatchTypeChange(value: WatchRuleType) {
        _uiState.update { it.copy(watchType = value) }
    }

    fun addWatchRule() {
        val pattern = _uiState.value.watchPatternInput.trim()
        if (pattern.isBlank()) return

        val rule = WatchRule(pattern = pattern, type = _uiState.value.watchType)
        _uiState.update {
            it.copy(
                watchRules = it.watchRules + rule,
                watchPatternInput = ""
            )
        }
    }

    fun removeWatchRule(ruleId: Long) {
        _uiState.update { it.copy(watchRules = it.watchRules.filterNot { rule -> rule.id == ruleId }) }
    }

    fun clearWatchRules() {
        _uiState.update { it.copy(watchRules = emptyList()) }
    }

    fun clearMatchLog() {
        _uiState.update { it.copy(matchLog = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearInfo() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun clearTerminal() {
        _uiState.value.terminalEmulator?.clearScreen()
        terminalBuffer.clear()
        watchTextDecoder.reset()
        _uiState.update { it.copy(terminalText = "") }
    }

    fun resolveSshPrompt(response: SshPromptResponse) {
        val deferred = promptDecision ?: return
        if (!deferred.isCompleted) {
            deferred.complete(response)
        }
    }

    private suspend fun awaitPromptDecision(prompt: SshPrompt): SshPromptResponse {
        val deferred = CompletableDeferred<SshPromptResponse>()
        promptDecision = deferred
        _uiState.update {
            it.copy(
                pendingSshPrompt = prompt,
                infoMessage = promptInfoMessage(prompt)
            )
        }

        return try {
            deferred.await()
        } finally {
            promptDecision = null
            _uiState.update { it.copy(pendingSshPrompt = null) }
        }
    }

    private fun promptInfoMessage(prompt: SshPrompt): String {
        return when (prompt) {
            is HostKeyPrompt -> "Verify host key fingerprint"
            is PasswordPrompt -> "Password required"
            is PassphrasePrompt -> "Private key passphrase required"
            is KeyboardInteractivePrompt -> "Additional authentication required"
        }
    }

    override fun onCleared() {
        super.onCleared()
        outputJob?.cancel()
        eventJob?.cancel()
        tmuxBootstrapJob?.cancel()
        promptDecision?.let { deferred ->
            if (!deferred.isCompleted) deferred.complete(PromptCancelledResponse)
        }
        notificationManager.clearSessionActive()
        viewModelScope.launch {
            activeSession?.disconnect()
            activeSession = null
        }
    }

    companion object {
        fun factory(
            profileId: Long,
            profileRepository: ConnectionProfileRepository,
            settingsRepository: SettingsRepository,
            sshClient: SshClient,
            notificationManager: WatchNotificationManager
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SessionViewModel(
                        profileId = profileId,
                        profileRepository = profileRepository,
                        settingsRepository = settingsRepository,
                        sshClient = sshClient,
                        notificationManager = notificationManager
                    ) as T
                }
            }
        }
    }
}

private class Utf8StreamDecoder {
    private val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    private var charBuffer = CharBuffer.allocate(8192)

    fun decode(chunk: ByteArray): String {
        if (chunk.isEmpty()) return ""
        val input = ByteBuffer.wrap(chunk)
        var out = ensureCapacity(maxOf(32, ceil(chunk.size * decoder.maxCharsPerByte()).toInt() + 4))
        out.clear()

        while (true) {
            val result = decoder.decode(input, out, false)
            if (result.isOverflow) {
                out = grow(out)
                continue
            }
            break
        }

        out.flip()
        return out.toString()
    }

    fun reset() {
        decoder.reset()
    }

    private fun ensureCapacity(requiredChars: Int): CharBuffer {
        if (charBuffer.capacity() >= requiredChars) {
            return charBuffer
        }
        charBuffer = CharBuffer.allocate(requiredChars)
        return charBuffer
    }

    private fun grow(current: CharBuffer): CharBuffer {
        val expanded = CharBuffer.allocate(current.capacity() * 2)
        current.flip()
        expanded.put(current)
        charBuffer = expanded
        return expanded
    }
}
