package com.cliagentic.mobileterminal.data.model

data class KnownHost(
    val host: String,
    val port: Int,
    val algorithm: String,
    val hostKey: ByteArray,
    val sha256Fingerprint: String,
    val md5Fingerprint: String,
    val trustedAtMillis: Long
)
