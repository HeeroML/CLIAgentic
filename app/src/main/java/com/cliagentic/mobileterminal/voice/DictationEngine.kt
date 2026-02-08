package com.cliagentic.mobileterminal.voice

import com.cliagentic.mobileterminal.data.model.DictationEngineType

interface DictationEngine {
    val type: DictationEngineType

    fun start(listener: Listener)
    fun stop()
    fun release()

    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
    }
}
