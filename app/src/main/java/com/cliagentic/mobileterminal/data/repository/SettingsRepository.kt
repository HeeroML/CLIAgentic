package com.cliagentic.mobileterminal.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cliagentic.mobileterminal.data.model.AppSettings
import com.cliagentic.mobileterminal.data.model.DictationEngineType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun updateVoiceAppendNewline(enabled: Boolean)
    suspend fun updatePreferredDictationEngine(engineType: DictationEngineType)
    suspend fun updateMoshFeatureFlag(enabled: Boolean)
    suspend fun updateTerminalSkin(skinId: String)
    suspend fun replaceAll(settings: AppSettings)
}

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            voiceAppendNewline = prefs[VOICE_APPEND_NEWLINE] ?: true,
            preferredDictationEngine = DictationEngineType.entries.firstOrNull {
                it.name == prefs[PREFERRED_DICTATION_ENGINE]
            } ?: DictationEngineType.ANDROID_SPEECH,
            moshEnabledFlag = prefs[MOSH_ENABLED] ?: false,
            terminalSkinId = prefs[TERMINAL_SKIN] ?: "dracula"
        )
    }

    override suspend fun updateVoiceAppendNewline(enabled: Boolean) {
        context.settingsDataStore.edit { it[VOICE_APPEND_NEWLINE] = enabled }
    }

    override suspend fun updatePreferredDictationEngine(engineType: DictationEngineType) {
        context.settingsDataStore.edit { it[PREFERRED_DICTATION_ENGINE] = engineType.name }
    }

    override suspend fun updateMoshFeatureFlag(enabled: Boolean) {
        context.settingsDataStore.edit { it[MOSH_ENABLED] = enabled }
    }

    override suspend fun updateTerminalSkin(skinId: String) {
        context.settingsDataStore.edit { it[TERMINAL_SKIN] = skinId }
    }

    override suspend fun replaceAll(settings: AppSettings) {
        context.settingsDataStore.edit { prefs: MutablePreferences ->
            prefs[VOICE_APPEND_NEWLINE] = settings.voiceAppendNewline
            prefs[PREFERRED_DICTATION_ENGINE] = settings.preferredDictationEngine.name
            prefs[MOSH_ENABLED] = settings.moshEnabledFlag
            prefs[TERMINAL_SKIN] = settings.terminalSkinId
        }
    }

    companion object {
        private val VOICE_APPEND_NEWLINE = booleanPreferencesKey("voice_append_newline")
        private val PREFERRED_DICTATION_ENGINE = stringPreferencesKey("preferred_dictation_engine")
        private val MOSH_ENABLED = booleanPreferencesKey("mosh_enabled")
        private val TERMINAL_SKIN = stringPreferencesKey("terminal_skin")
    }
}
