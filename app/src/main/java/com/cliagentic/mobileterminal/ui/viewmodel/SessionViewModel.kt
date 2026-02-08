package com.cliagentic.mobileterminal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.WatchRule
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.data.repository.ConnectionProfileRepository
import com.cliagentic.mobileterminal.data.repository.SettingsRepository
import com.cliagentic.mobileterminal.notifications.WatchNotificationManager
import com.cliagentic.mobileterminal.ssh.HostKeyPrompt
import com.cliagentic.mobileterminal.ssh.SshClient
import com.cliagentic.mobileterminal.ssh.SshConnectRequest
import com.cliagentic.mobileterminal.ssh.SshErrorMapper
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
    private var tmuxBootstrapJob: Job? = null
    private var hostDecision: CompletableDeferred<Boolean>? = null

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
                useKeyAuth = profile.authType == AuthType.KEY
            )

            _uiState.update { it.copy(isConnecting = true, errorMessage = null, infoMessage = null) }

            val result = sshClient.connect(request) { prompt ->
                awaitHostDecision(prompt)
            }

            result.onSuccess { session ->
                activeSession = session
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        isPreparingTmux = true,
                        infoMessage = "Connected to ${profile.host}. Preparing tmux...",
                        errorMessage = null
                    )
                }
                observeOutput(session)
                tmuxBootstrapJob?.cancel()
                tmuxBootstrapJob = viewModelScope.launch {
                    bootstrapTmuxSession()
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = SshErrorMapper.toMessage(throwable)
                    )
                }
            }
        }
    }

    private fun observeOutput(session: SshSession) {
        outputJob?.cancel()
        outputJob = viewModelScope.launch {
            session.output.collect { chunk ->
                val completedLines = terminalBuffer.append(chunk)
                val rendered = terminalBuffer.renderedText.value
                _uiState.update { it.copy(terminalText = rendered) }

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
            tmuxBootstrapJob?.cancel()
            tmuxBootstrapJob = null
            activeSession?.disconnect()
            activeSession = null
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    isConnected = false,
                    isPreparingTmux = false,
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
            activeSession?.send(prepared)
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
        }
    }

    fun toggleCtrlArmed() {
        _uiState.update { it.copy(ctrlArmed = !it.ctrlArmed) }
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
        terminalBuffer.clear()
        _uiState.update { it.copy(terminalText = "") }
    }

    fun resolveHostKeyPrompt(accept: Boolean) {
        val deferred = hostDecision ?: return
        if (!deferred.isCompleted) {
            deferred.complete(accept)
        }
    }

    private suspend fun awaitHostDecision(prompt: HostKeyPrompt): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        hostDecision = deferred
        _uiState.update { it.copy(hostKeyPrompt = prompt, infoMessage = "Verify host key fingerprint") }

        return try {
            deferred.await()
        } finally {
            hostDecision = null
            _uiState.update { it.copy(hostKeyPrompt = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        outputJob?.cancel()
        tmuxBootstrapJob?.cancel()
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
