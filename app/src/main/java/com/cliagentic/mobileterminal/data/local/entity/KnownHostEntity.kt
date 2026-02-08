package com.cliagentic.mobileterminal.data.local.entity

import androidx.room.Entity

@Entity(tableName = "known_hosts", primaryKeys = ["host", "port"])
data class KnownHostEntity(
    val host: String,
    val port: Int,
    val algorithm: String,
    val hostKey: ByteArray,
    val sha256Fingerprint: String,
    val md5Fingerprint: String,
    val trustedAtMillis: Long
)
