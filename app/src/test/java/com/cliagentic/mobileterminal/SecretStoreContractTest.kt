package com.cliagentic.mobileterminal

import com.cliagentic.mobileterminal.security.SecretStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SecretStoreContractTest {

    private class InMemorySecretStore : SecretStore {
        private val passwords = mutableMapOf<Long, String>()
        private val keys = mutableMapOf<Long, String>()

        override suspend fun savePassword(profileId: Long, password: String) {
            passwords[profileId] = password
        }

        override suspend fun readPassword(profileId: Long): String? = passwords[profileId]

        override suspend fun clearPassword(profileId: Long) {
            passwords.remove(profileId)
        }

        override suspend fun hasPassword(profileId: Long): Boolean = passwords.containsKey(profileId)

        override suspend fun savePrivateKey(profileId: Long, privateKey: String) {
            keys[profileId] = privateKey
        }

        override suspend fun readPrivateKey(profileId: Long): String? = keys[profileId]

        override suspend fun clearPrivateKey(profileId: Long) {
            keys.remove(profileId)
        }

        override suspend fun hasPrivateKey(profileId: Long): Boolean = keys.containsKey(profileId)

        override suspend fun clearProfile(profileId: Long) {
            passwords.remove(profileId)
            keys.remove(profileId)
        }
    }

    @Test
    fun `clearProfile removes both password and key`() = runBlocking {
        val store = InMemorySecretStore()

        store.savePassword(42, "pw")
        store.savePrivateKey(42, "key")

        store.clearProfile(42)

        assertThat(store.hasPassword(42)).isFalse()
        assertThat(store.hasPrivateKey(42)).isFalse()
    }
}
