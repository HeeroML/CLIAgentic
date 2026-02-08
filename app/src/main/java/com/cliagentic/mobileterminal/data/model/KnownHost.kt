package com.cliagentic.mobileterminal.data.model

data class KnownHost(
    val host: String,
    val port: Int,
    val algorithm: String,
    val fingerprint: String,
    val trustedAtMillis: Long
)
