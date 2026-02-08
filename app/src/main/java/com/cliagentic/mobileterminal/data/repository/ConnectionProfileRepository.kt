package com.cliagentic.mobileterminal.data.repository

import com.cliagentic.mobileterminal.data.local.dao.ConnectionProfileDao
import com.cliagentic.mobileterminal.data.local.entity.ConnectionProfileEntity
import com.cliagentic.mobileterminal.data.model.AppSettings
import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.ConnectionProfile
import com.cliagentic.mobileterminal.data.model.DictationEngineType
import com.cliagentic.mobileterminal.data.model.ProfileSecrets
import com.cliagentic.mobileterminal.data.model.PtyType
import com.cliagentic.mobileterminal.data.model.TmuxPrefix
import com.cliagentic.mobileterminal.data.serialization.AppSettingsExportDto
import com.cliagentic.mobileterminal.data.serialization.ConnectionProfileExportDto
import com.cliagentic.mobileterminal.data.serialization.ProfileSettingsBundleDto
import com.cliagentic.mobileterminal.security.SecretStore
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ConnectionProfileRepository {
    val profiles: Flow<List<ConnectionProfile>>

    fun observeProfile(id: Long): Flow<ConnectionProfile?>
    suspend fun getProfile(id: Long): ConnectionProfile?

    suspend fun upsertProfile(
        profile: ConnectionProfile,
        password: String?,
        privateKey: String?
    ): Long

    suspend fun deleteProfile(id: Long)

    suspend fun hasSecretForAuth(profileId: Long, authType: AuthType): Boolean
    suspend fun loadSecrets(profileId: Long): ProfileSecrets

    suspend fun exportAsJson(settings: AppSettings): String
    suspend fun importFromJson(json: String): Result<ImportedBundle>
}

data class ImportedBundle(
    val importedProfiles: Int,
    val importedSettings: AppSettings?
)

class RoomConnectionProfileRepository(
    private val dao: ConnectionProfileDao,
    private val secretStore: SecretStore,
    moshi: Moshi
) : ConnectionProfileRepository {

    private val bundleAdapter: JsonAdapter<ProfileSettingsBundleDto> =
        moshi.adapter(ProfileSettingsBundleDto::class.java)

    override val profiles: Flow<List<ConnectionProfile>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override fun observeProfile(id: Long): Flow<ConnectionProfile?> {
        return dao.observeById(id).map { it?.toModel() }
    }

    override suspend fun getProfile(id: Long): ConnectionProfile? {
        return dao.getById(id)?.toModel()
    }

    override suspend fun upsertProfile(
        profile: ConnectionProfile,
        password: String?,
        privateKey: String?
    ): Long {
        val savedId = if (profile.id == 0L) {
            dao.insert(profile.toEntity())
        } else {
            dao.update(profile.toEntity())
            profile.id
        }

        when (profile.authType) {
            AuthType.PASSWORD -> {
                if (!password.isNullOrBlank()) {
                    secretStore.savePassword(savedId, password)
                }
                secretStore.clearPrivateKey(savedId)
            }

            AuthType.KEY -> {
                if (!privateKey.isNullOrBlank()) {
                    secretStore.savePrivateKey(savedId, privateKey)
                }
                secretStore.clearPassword(savedId)
            }
        }

        return savedId
    }

    override suspend fun deleteProfile(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.delete(entity)
        secretStore.clearProfile(id)
    }

    override suspend fun hasSecretForAuth(profileId: Long, authType: AuthType): Boolean {
        return when (authType) {
            AuthType.PASSWORD -> secretStore.hasPassword(profileId)
            AuthType.KEY -> secretStore.hasPrivateKey(profileId)
        }
    }

    override suspend fun loadSecrets(profileId: Long): ProfileSecrets {
        return ProfileSecrets(
            password = secretStore.readPassword(profileId),
            privateKey = secretStore.readPrivateKey(profileId)
        )
    }

    override suspend fun exportAsJson(settings: AppSettings): String {
        val profiles = dao.getAll().map {
            ConnectionProfileExportDto(
                name = it.name,
                host = it.host,
                port = it.port,
                username = it.username,
                authType = it.authType,
                biometricForKey = it.biometricForKey,
                tmuxPrefix = it.tmuxPrefix,
                ptyType = it.ptyType
            )
        }

        val bundle = ProfileSettingsBundleDto(
            generatedAtEpochMs = System.currentTimeMillis(),
            profiles = profiles,
            settings = AppSettingsExportDto(
                voiceAppendNewline = settings.voiceAppendNewline,
                preferredDictationEngine = settings.preferredDictationEngine.name,
                moshEnabledFlag = settings.moshEnabledFlag,
                terminalSkinId = settings.terminalSkinId
            )
        )

        return bundleAdapter.toJson(bundle)
    }

    override suspend fun importFromJson(json: String): Result<ImportedBundle> {
        return runCatching {
            val bundle = bundleAdapter.fromJson(json)
                ?: error("Invalid JSON bundle")

            var imported = 0
            bundle.profiles.forEach { dto ->
                val authType = AuthType.entries.firstOrNull { it.name == dto.authType } ?: AuthType.PASSWORD
                val profile = ConnectionProfile(
                    name = dto.name,
                    host = dto.host,
                    port = dto.port,
                    username = dto.username,
                    authType = authType,
                    biometricForKey = dto.biometricForKey,
                    tmuxPrefix = TmuxPrefix.fromNameOrDefault(dto.tmuxPrefix),
                    ptyType = PtyType.fromTermOrDefault(dto.ptyType)
                )
                dao.insert(profile.toEntity())
                imported += 1
            }

            val settings = AppSettings(
                voiceAppendNewline = bundle.settings.voiceAppendNewline,
                preferredDictationEngine = DictationEngineType.entries.firstOrNull {
                    it.name == bundle.settings.preferredDictationEngine
                } ?: DictationEngineType.ANDROID_SPEECH,
                moshEnabledFlag = bundle.settings.moshEnabledFlag,
                terminalSkinId = bundle.settings.terminalSkinId ?: "dracula"
            )

            ImportedBundle(importedProfiles = imported, importedSettings = settings)
        }
    }
}

private fun ConnectionProfileEntity.toModel(): ConnectionProfile {
    return ConnectionProfile(
        id = id,
        name = name,
        host = host,
        port = port,
        username = username,
        authType = AuthType.entries.firstOrNull { it.name == authType } ?: AuthType.PASSWORD,
        biometricForKey = biometricForKey,
        tmuxPrefix = TmuxPrefix.fromNameOrDefault(tmuxPrefix),
        ptyType = PtyType.fromTermOrDefault(ptyType)
    )
}

private fun ConnectionProfile.toEntity(): ConnectionProfileEntity {
    return ConnectionProfileEntity(
        id = id,
        name = name.trim(),
        host = host.trim(),
        port = port,
        username = username.trim(),
        authType = authType.name,
        biometricForKey = biometricForKey,
        tmuxPrefix = tmuxPrefix.name,
        ptyType = ptyType.term
    )
}
