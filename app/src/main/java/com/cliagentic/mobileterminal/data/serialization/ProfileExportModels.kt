package com.cliagentic.mobileterminal.data.serialization

data class ConnectionProfileExportDto(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val authType: String,
    val biometricForKey: Boolean,
    val tmuxPrefix: String
)

data class AppSettingsExportDto(
    val voiceAppendNewline: Boolean,
    val preferredDictationEngine: String,
    val moshEnabledFlag: Boolean
)

data class ProfileSettingsBundleDto(
    val schemaVersion: Int = 1,
    val generatedAtEpochMs: Long,
    val note: String = "Secrets are intentionally excluded from export.",
    val profiles: List<ConnectionProfileExportDto>,
    val settings: AppSettingsExportDto
)
