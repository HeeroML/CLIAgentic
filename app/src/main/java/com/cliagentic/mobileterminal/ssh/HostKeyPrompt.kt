package com.cliagentic.mobileterminal.ssh

data class HostKeyPrompt(
    val host: String,
    val port: Int,
    val algorithm: String,
    val fingerprint: String
)
