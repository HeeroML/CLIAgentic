package com.cliagentic.mobileterminal.data.model

enum class PtyType(
    val term: String,
    val label: String
) {
    XTERM_256COLOR("xterm-256color", "xterm-256color"),
    TMUX_256COLOR("tmux-256color", "tmux-256color"),
    XTERM("xterm", "xterm"),
    VT100("vt100", "vt100");

    companion object {
        fun fromTermOrDefault(term: String?): PtyType {
            return entries.firstOrNull { it.term.equals(term, ignoreCase = true) } ?: XTERM_256COLOR
        }
    }
}
