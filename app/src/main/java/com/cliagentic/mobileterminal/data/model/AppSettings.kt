package com.cliagentic.mobileterminal.data.model

data class AppSettings(
    val voiceAppendNewline: Boolean = true,
    val preferredDictationEngine: DictationEngineType = DictationEngineType.ANDROID_SPEECH,
    val moshEnabledFlag: Boolean = false,
    val terminalSkinId: String = "dracula"
)

enum class DictationEngineType {
    ANDROID_SPEECH,
    WHISPER_STUB
}
