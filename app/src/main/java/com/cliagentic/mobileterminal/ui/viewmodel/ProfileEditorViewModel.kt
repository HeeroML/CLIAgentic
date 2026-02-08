package com.cliagentic.mobileterminal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.ConnectionProfile
import com.cliagentic.mobileterminal.data.model.ProfileValidator
import com.cliagentic.mobileterminal.data.model.PtyType
import com.cliagentic.mobileterminal.data.model.TmuxPrefix
import com.cliagentic.mobileterminal.data.repository.ConnectionProfileRepository
import com.cliagentic.mobileterminal.ui.state.ProfileEditorUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileEditorViewModel(
    private val profileRepository: ConnectionProfileRepository,
    private val profileId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditorUiState())
    val uiState: StateFlow<ProfileEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (profileId != null && profileId > 0) {
                val profile = profileRepository.getProfile(profileId)
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            id = profile.id,
                            name = profile.name,
                            host = profile.host,
                            port = profile.port.toString(),
                            username = profile.username,
                            authType = profile.authType,
                            biometricForKey = profile.biometricForKey,
                            tmuxPrefix = profile.tmuxPrefix,
                            ptyType = profile.ptyType,
                            isLoading = false,
                            hasStoredPassword = profileRepository.hasSecretForAuth(profile.id, AuthType.PASSWORD),
                            hasStoredPrivateKey = profileRepository.hasSecretForAuth(profile.id, AuthType.KEY)
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errors = listOf("Profile not found")) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errors = emptyList()) }
    fun onHostChange(value: String) = _uiState.update { it.copy(host = value, errors = emptyList()) }
    fun onPortChange(value: String) = _uiState.update { it.copy(port = value, errors = emptyList()) }
    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, errors = emptyList()) }
    fun onAuthTypeChange(value: AuthType) = _uiState.update { it.copy(authType = value, errors = emptyList()) }
    fun onBiometricToggle(value: Boolean) = _uiState.update { it.copy(biometricForKey = value) }
    fun onTmuxPrefixChange(value: TmuxPrefix) = _uiState.update { it.copy(tmuxPrefix = value) }
    fun onPtyTypeChange(value: PtyType) = _uiState.update { it.copy(ptyType = value, errors = emptyList()) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errors = emptyList()) }
    fun onPrivateKeyChange(value: String) = _uiState.update { it.copy(privateKey = value, errors = emptyList()) }

    fun consumeSavedProfile() {
        _uiState.update { it.copy(savedProfileId = null) }
    }

    fun saveProfile() {
        val current = _uiState.value
        if (current.isSaving) return

        viewModelScope.launch {
            val port = current.port.toIntOrNull()
            if (port == null) {
                _uiState.update { it.copy(errors = listOf("Port must be numeric")) }
                return@launch
            }

            val profile = ConnectionProfile(
                id = current.id,
                name = current.name,
                host = current.host,
                port = port,
                username = current.username,
                authType = current.authType,
                biometricForKey = current.biometricForKey,
                tmuxPrefix = current.tmuxPrefix,
                ptyType = current.ptyType
            )

            val hasExistingSecret = if (current.authType == AuthType.PASSWORD) {
                current.hasStoredPassword
            } else {
                current.hasStoredPrivateKey
            }

            val validation = ProfileValidator.validate(
                profile = profile,
                newPassword = current.password,
                newPrivateKey = current.privateKey,
                hasExistingSecret = hasExistingSecret
            )

            if (!validation.isValid) {
                _uiState.update { it.copy(errors = validation.errors) }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, errors = emptyList()) }

            val savedId = profileRepository.upsertProfile(
                profile = profile,
                password = current.password.takeIf { it.isNotBlank() },
                privateKey = current.privateKey.takeIf { it.isNotBlank() }
            )

            _uiState.update {
                it.copy(
                    isSaving = false,
                    savedProfileId = savedId,
                    password = "",
                    privateKey = "",
                    hasStoredPassword = if (profile.authType == AuthType.PASSWORD) true else false,
                    hasStoredPrivateKey = if (profile.authType == AuthType.KEY) true else false
                )
            }
        }
    }

    companion object {
        fun factory(
            profileRepository: ConnectionProfileRepository,
            profileId: Long?
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileEditorViewModel(profileRepository, profileId) as T
                }
            }
        }
    }
}
