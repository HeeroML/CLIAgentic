package com.cliagentic.mobileterminal.di

import android.content.Context
import androidx.room.Room
import com.cliagentic.mobileterminal.data.local.AppDatabase
import com.cliagentic.mobileterminal.data.model.DictationEngineType
import com.cliagentic.mobileterminal.data.repository.ConnectionProfileRepository
import com.cliagentic.mobileterminal.data.repository.DataStoreSettingsRepository
import com.cliagentic.mobileterminal.data.repository.KnownHostRepository
import com.cliagentic.mobileterminal.data.repository.RoomConnectionProfileRepository
import com.cliagentic.mobileterminal.data.repository.RoomKnownHostRepository
import com.cliagentic.mobileterminal.data.repository.SettingsRepository
import com.cliagentic.mobileterminal.notifications.WatchNotificationManager
import com.cliagentic.mobileterminal.security.BiometricAuthenticator
import com.cliagentic.mobileterminal.security.EncryptedSecretStore
import com.cliagentic.mobileterminal.security.SecretStore
import com.cliagentic.mobileterminal.ssh.SshClient
import com.cliagentic.mobileterminal.ssh.v2.SshlibSshClient
import com.cliagentic.mobileterminal.voice.AndroidSpeechDictationEngine
import com.cliagentic.mobileterminal.voice.DictationEngine
import com.cliagentic.mobileterminal.voice.WhisperDictationEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class AppContainer(private val context: Context) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "terminal_pilot.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val secretStore: SecretStore by lazy { EncryptedSecretStore(context) }

    val profileRepository: ConnectionProfileRepository by lazy {
        RoomConnectionProfileRepository(
            dao = database.connectionProfileDao(),
            secretStore = secretStore,
            moshi = moshi
        )
    }

    val knownHostRepository: KnownHostRepository by lazy {
        RoomKnownHostRepository(database.knownHostDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(context)
    }

    val sshClient: SshClient by lazy {
        SshlibSshClient(knownHostRepository)
    }

    val biometricAuthenticator: BiometricAuthenticator by lazy {
        BiometricAuthenticator(context)
    }

    val watchNotificationManager: WatchNotificationManager by lazy {
        WatchNotificationManager(context).also { it.ensureChannel() }
    }

    fun createDictationEngine(type: DictationEngineType): DictationEngine {
        return when (type) {
            DictationEngineType.ANDROID_SPEECH -> AndroidSpeechDictationEngine(context)
            DictationEngineType.WHISPER_STUB -> WhisperDictationEngine()
        }
    }
}
