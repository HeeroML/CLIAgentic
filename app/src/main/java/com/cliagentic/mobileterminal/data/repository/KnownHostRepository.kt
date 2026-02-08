package com.cliagentic.mobileterminal.data.repository

import com.cliagentic.mobileterminal.data.local.dao.KnownHostDao
import com.cliagentic.mobileterminal.data.local.entity.KnownHostEntity
import com.cliagentic.mobileterminal.data.model.KnownHost

interface KnownHostRepository {
    suspend fun get(host: String, port: Int): KnownHost?
    suspend fun trust(
        host: String,
        port: Int,
        algorithm: String,
        hostKey: ByteArray,
        sha256Fingerprint: String,
        md5Fingerprint: String
    )

    suspend fun remove(host: String, port: Int)
    suspend fun clearAll()
}

class RoomKnownHostRepository(private val dao: KnownHostDao) : KnownHostRepository {
    override suspend fun get(host: String, port: Int): KnownHost? {
        return dao.get(host, port)?.toModel()
    }

    override suspend fun trust(
        host: String,
        port: Int,
        algorithm: String,
        hostKey: ByteArray,
        sha256Fingerprint: String,
        md5Fingerprint: String
    ) {
        dao.upsert(
            KnownHostEntity(
                host = host,
                port = port,
                algorithm = algorithm,
                hostKey = hostKey,
                sha256Fingerprint = sha256Fingerprint,
                md5Fingerprint = md5Fingerprint,
                trustedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun remove(host: String, port: Int) {
        dao.delete(host, port)
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
        hostKey = hostKey,
        sha256Fingerprint = sha256Fingerprint,
        md5Fingerprint = md5Fingerprint,
        trustedAtMillis = trustedAtMillis
    )
}
