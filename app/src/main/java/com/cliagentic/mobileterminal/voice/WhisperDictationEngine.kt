package com.cliagentic.mobileterminal.voice

import com.cliagentic.mobileterminal.data.model.DictationEngineType

class WhisperDictationEngine : DictationEngine {
    override val type: DictationEngineType = DictationEngineType.WHISPER_STUB

    override fun start(listener: DictationEngine.Listener) {
        listener.onError(
            "Whisper.cpp engine is a stub. Add native binaries/model files and wire JNI per README."
        )
    }

    override fun stop() = Unit
    override fun release() = Unit
}
