package com.cliagentic.mobileterminal.ui.state

import com.cliagentic.mobileterminal.data.model.ConnectionProfile
import com.cliagentic.mobileterminal.data.model.DictationEngineType
import com.cliagentic.mobileterminal.data.model.WatchMatch
import com.cliagentic.mobileterminal.data.model.WatchRule
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.ssh.HostKeyPrompt

data class SessionUiState(
    val profile: ConnectionProfile? = null,
    val terminalText: String = "",
    val inputDraft: String = "",
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
    val hostKeyPrompt: HostKeyPrompt? = null,
    val showTmuxSessionSelector: Boolean = false,
    val tmuxSessionChoices: List<String> = emptyList(),
    val tmuxDefaultSessionName: String = "terminal-pilot",
    val voiceAppendNewline: Boolean = true,
    val dictationEngineType: DictationEngineType = DictationEngineType.ANDROID_SPEECH,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
