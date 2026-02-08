package com.cliagentic.mobileterminal.ssh.v2

import com.cliagentic.mobileterminal.ssh.CommandBoundaryType
import com.cliagentic.mobileterminal.ssh.SshEvent

internal class Osc133Parser {
    private val buffer = StringBuilder()
    private var promptId: Int = 0

    fun consume(chunk: ByteArray): List<SshEvent.CommandBoundary> {
        if (chunk.isEmpty()) return emptyList()
        buffer.append(chunk.toString(Charsets.ISO_8859_1))

        val events = mutableListOf<SshEvent.CommandBoundary>()
        while (true) {
            val start = buffer.indexOf(OSC133_PREFIX)
            if (start < 0) {
                trimBufferIfNeeded()
                break
            }

            val bel = buffer.indexOf(BEL, start + OSC133_PREFIX.length)
            val st = buffer.indexOf(ST, start + OSC133_PREFIX.length)
            val end = firstTerminator(bel, st)
            if (end < 0) {
                if (start > 0) buffer.delete(0, start)
                break
            }

            val payload = buffer.substring(start + OSC133_PREFIX.length, end)
            parsePayload(payload)?.let(events::add)

            val terminatorLength = if (st >= 0 && st == end) ST.length else BEL.length
            buffer.delete(0, end + terminatorLength)
        }

        return events
    }

    private fun parsePayload(payload: String): SshEvent.CommandBoundary? {
        val code = payload.trim().firstOrNull() ?: return null
        return when (code) {
            'A' -> {
                promptId += 1
                SshEvent.CommandBoundary(promptId = promptId, type = CommandBoundaryType.PROMPT)
            }

            'B' -> SshEvent.CommandBoundary(
                promptId = promptId,
                type = CommandBoundaryType.COMMAND_INPUT_START
            )

            'C' -> SshEvent.CommandBoundary(
                promptId = promptId,
                type = CommandBoundaryType.COMMAND_OUTPUT_START
            )

            'D' -> {
                val exitCode = payload.substringAfter(';', "").substringBefore(';').toIntOrNull()
                SshEvent.CommandBoundary(
                    promptId = promptId,
                    type = CommandBoundaryType.COMMAND_FINISHED,
                    exitCode = exitCode
                )
            }

            else -> null
        }
    }

    private fun firstTerminator(bel: Int, st: Int): Int {
        if (bel < 0) return st
        if (st < 0) return bel
        return minOf(bel, st)
    }

    private fun trimBufferIfNeeded() {
        if (buffer.length > MAX_BUFFER_CHARS) {
            buffer.delete(0, buffer.length - MAX_BUFFER_CHARS)
        }
    }

    private companion object {
        private const val OSC133_PREFIX = "\u001B]133;"
        private const val BEL = "\u0007"
        private const val ST = "\u001B\\"
        private const val MAX_BUFFER_CHARS = 4096
    }
}
