package com.cliagentic.mobileterminal.ssh

data class HostKeyPrompt(
    val host: String,
    val port: Int,
    val algorithm: String,
    val sha256Fingerprint: String,
    val md5Fingerprint: String,
    val keyBlobBase64: String,
    val mismatchDetected: Boolean = false,
    val previousSha256Fingerprint: String? = null
) : SshPrompt
