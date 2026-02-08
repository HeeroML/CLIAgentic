package com.cliagentic.mobileterminal.data.local.entity

import androidx.room.Entity

@Entity(tableName = "known_hosts", primaryKeys = ["host", "port"])
data class KnownHostEntity(
    val host: String,
    val port: Int,
    val algorithm: String,
    val fingerprint: String,
    val trustedAtMillis: Long
)
