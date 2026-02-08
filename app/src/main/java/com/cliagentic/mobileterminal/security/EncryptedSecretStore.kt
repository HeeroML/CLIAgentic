package com.cliagentic.mobileterminal.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedSecretStore(context: Context) : SecretStore {

    private val appContext = context.applicationContext

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun savePassword(profileId: Long, password: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(passwordKey(profileId), password).apply()
    }

    override suspend fun readPassword(profileId: Long): String? = withContext(Dispatchers.IO) {
        prefs.getString(passwordKey(profileId), null)
    }

    override suspend fun clearPassword(profileId: Long) = withContext(Dispatchers.IO) {
        prefs.edit().remove(passwordKey(profileId)).apply()
    }

    override suspend fun hasPassword(profileId: Long): Boolean = withContext(Dispatchers.IO) {
        prefs.contains(passwordKey(profileId))
    }

    override suspend fun savePrivateKey(profileId: Long, privateKey: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(privateKey(profileId), privateKey).apply()
    }

    override suspend fun readPrivateKey(profileId: Long): String? = withContext(Dispatchers.IO) {
        prefs.getString(privateKey(profileId), null)
    }

    override suspend fun clearPrivateKey(profileId: Long) = withContext(Dispatchers.IO) {
        prefs.edit().remove(privateKey(profileId)).apply()
    }

    override suspend fun hasPrivateKey(profileId: Long): Boolean = withContext(Dispatchers.IO) {
        prefs.contains(privateKey(profileId))
    }

    override suspend fun clearProfile(profileId: Long) {
        clearPassword(profileId)
        clearPrivateKey(profileId)
    }

    private fun passwordKey(profileId: Long): String = "profile_${profileId}_password"
    private fun privateKey(profileId: Long): String = "profile_${profileId}_private_key"

    companion object {
        private const val PREF_FILE = "encrypted_profile_secrets"
    }
}
