package com.cliagentic.mobileterminal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cliagentic.mobileterminal.data.repository.ConnectionProfileRepository
import com.cliagentic.mobileterminal.ui.state.ProfilesUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesViewModel(
    private val profileRepository: ConnectionProfileRepository
) : ViewModel() {

    val uiState = profileRepository.profiles
        .map { ProfilesUiState(profiles = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            profileRepository.deleteProfile(id)
        }
    }

    companion object {
        fun factory(profileRepository: ConnectionProfileRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfilesViewModel(profileRepository) as T
                }
            }
        }
    }
}
