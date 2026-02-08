package com.cliagentic.mobileterminal.security

interface SecretStore {
    suspend fun savePassword(profileId: Long, password: String)
    suspend fun readPassword(profileId: Long): String?
    suspend fun clearPassword(profileId: Long)
    suspend fun hasPassword(profileId: Long): Boolean

    suspend fun savePrivateKey(profileId: Long, privateKey: String)
    suspend fun readPrivateKey(profileId: Long): String?
    suspend fun clearPrivateKey(profileId: Long)
    suspend fun hasPrivateKey(profileId: Long): Boolean

    suspend fun clearProfile(profileId: Long)
}
