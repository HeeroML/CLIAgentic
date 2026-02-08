package com.cliagentic.mobileterminal.terminal

sealed interface TmuxSessionDiscovery {
    data object Missing : TmuxSessionDiscovery
    data class Found(val sessions: List<String>) : TmuxSessionDiscovery
}

object TmuxSessionDiscoveryParser {
    const val MISSING_MARKER = "__TERMINAL_PILOT_TMUX_MISSING__"

    fun parse(stdout: String, exitCode: Int): TmuxSessionDiscovery {
        val lines = stdout
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val missing = exitCode == 127 || lines.contains(MISSING_MARKER)
        if (missing) return TmuxSessionDiscovery.Missing

        val sessions = lines
            .filterNot { it == MISSING_MARKER }
            .distinct()

        return TmuxSessionDiscovery.Found(sessions)
    }
}
