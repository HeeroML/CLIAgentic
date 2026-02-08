package com.cliagentic.mobileterminal.ui.state

import com.cliagentic.mobileterminal.data.model.ConnectionProfile
import com.cliagentic.mobileterminal.data.model.DictationEngineType
import com.cliagentic.mobileterminal.data.model.TerminalInputMode
import com.cliagentic.mobileterminal.data.model.WatchMatch
import com.cliagentic.mobileterminal.data.model.WatchRule
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.ssh.SshPrompt
import org.connectbot.terminal.TerminalEmulator

data class SessionUiState(
    val profile: ConnectionProfile? = null,
    val terminalText: String = "",
    val terminalEmulator: TerminalEmulator? = null,
    val inputDraft: String = "",
    val inputMode: TerminalInputMode = TerminalInputMode.CONTROL,
    val dictationPreview: String = "",
    val isDictating: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val ctrlArmed: Boolean = false,
    val keepScreenOn: Boolean = false,
    val watchPatternInput: String = "",
    val watchType: WatchRuleType = WatchRuleType.PREFIX,
    val watchRules: List<WatchRule> = emptyList(),
    val matchLog: List<WatchMatch> = emptyList(),
    val pendingSshPrompt: SshPrompt? = null,
    val isPreparingTmux: Boolean = false,
    val showTmuxSessionSelector: Boolean = false,
    val tmuxSessionChoices: List<String> = emptyList(),
    val tmuxDefaultSessionName: String = "terminal-pilot",
    val commandPromptId: Int = -1,
    val commandRunning: Boolean = false,
    val lastExitCode: Int? = null,
    val voiceAppendNewline: Boolean = true,
    val dictationEngineType: DictationEngineType = DictationEngineType.ANDROID_SPEECH,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
