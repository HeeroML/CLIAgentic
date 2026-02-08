package com.cliagentic.mobileterminal.data.repository

import com.cliagentic.mobileterminal.data.local.dao.KnownHostDao
import com.cliagentic.mobileterminal.data.local.entity.KnownHostEntity
import com.cliagentic.mobileterminal.data.model.KnownHost

interface KnownHostRepository {
    suspend fun get(host: String, port: Int): KnownHost?
    suspend fun trust(host: String, port: Int, algorithm: String, fingerprint: String)
    suspend fun clearAll()
}

class RoomKnownHostRepository(private val dao: KnownHostDao) : KnownHostRepository {
    override suspend fun get(host: String, port: Int): KnownHost? {
        return dao.get(host, port)?.toModel()
    }

    override suspend fun trust(host: String, port: Int, algorithm: String, fingerprint: String) {
        dao.upsert(
            KnownHostEntity(
                host = host,
                port = port,
                algorithm = algorithm,
                fingerprint = fingerprint,
                trustedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}

private fun KnownHostEntity.toModel(): KnownHost {
    return KnownHost(
        host = host,
        port = port,
        algorithm = algorithm,
        fingerprint = fingerprint,
        trustedAtMillis = trustedAtMillis
    )
}
