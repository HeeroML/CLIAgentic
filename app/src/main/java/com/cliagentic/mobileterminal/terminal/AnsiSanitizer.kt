package com.cliagentic.mobileterminal.terminal

object AnsiSanitizer {
    private val ansiRegex = Regex("\\u001B\\[[0-?]*[ -/]*[@-~]")
    private val oscRegex = Regex("\\u001B\\].*?(\\u0007|\\u001B\\\\)")

    fun sanitize(input: String): String {
        return input
            .replace(oscRegex, "")
            .replace(ansiRegex, "")
    }
}
