package com.cliagentic.mobileterminal.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TerminalBuffer(private val maxLines: Int = 2_000) {

    private val lines = ArrayDeque<String>()
    private val currentLine = StringBuilder()

    private val _renderedText = MutableStateFlow("")
    val renderedText: StateFlow<String> = _renderedText.asStateFlow()

    fun append(rawChunk: String): List<String> {
        val chunk = AnsiSanitizer.sanitize(rawChunk)
        val completedLines = mutableListOf<String>()

        chunk.forEach { char ->
            when (char) {
                '\r' -> currentLine.clear()
                '\n' -> {
                    val finalized = currentLine.toString()
                    lines.addLast(finalized)
                    completedLines += finalized
                    currentLine.clear()
                    trimIfNeeded()
                }

                else -> currentLine.append(char)
            }
        }

        _renderedText.value = buildString {
            lines.forEach { appendLine(it) }
            append(currentLine.toString())
        }

        return completedLines
    }

    fun clear() {
        lines.clear()
        currentLine.clear()
        _renderedText.value = ""
    }

    private fun trimIfNeeded() {
        while (lines.size > maxLines) {
            lines.removeFirst()
        }
    }
}
