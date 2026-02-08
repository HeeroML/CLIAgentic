package com.cliagentic.mobileterminal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cliagentic.mobileterminal.data.model.DictationEngineType
import com.cliagentic.mobileterminal.data.repository.ConnectionProfileRepository
import com.cliagentic.mobileterminal.data.repository.SettingsRepository
import com.cliagentic.mobileterminal.ui.state.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val profileRepository: ConnectionProfileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onImportJsonChange(value: String) {
        _uiState.update { it.copy(importJson = value) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun setVoiceAppendNewline(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateVoiceAppendNewline(enabled)
        }
    }

    fun setDictationEngine(type: DictationEngineType) {
        viewModelScope.launch {
            settingsRepository.updatePreferredDictationEngine(type)
        }
    }

    fun setMoshFeatureFlag(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMoshFeatureFlag(enabled)
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.settings
            val json = profileRepository.exportAsJson(currentSettings)
            _uiState.update {
                it.copy(
                    exportJson = json,
                    statusMessage = "Exported ${System.currentTimeMillis()}"
                )
            }
        }
    }

    fun importJson() {
        val json = _uiState.value.importJson
        if (json.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Paste JSON to import") }
            return
        }

        viewModelScope.launch {
            profileRepository.importFromJson(json)
                .onSuccess { imported ->
                    imported.importedSettings?.let { settingsRepository.replaceAll(it) }
                    _uiState.update {
                        it.copy(
                            statusMessage = "Imported ${imported.importedProfiles} profiles",
                            importJson = ""
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(statusMessage = "Import failed: ${throwable.message}") }
                }
        }
    }

    companion object {
        fun factory(
            profileRepository: ConnectionProfileRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(profileRepository, settingsRepository) as T
                }
            }
        }
    }
}
