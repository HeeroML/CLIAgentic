package com.cliagentic.mobileterminal.ui.state

import com.cliagentic.mobileterminal.data.model.ConnectionProfile

data class ProfilesUiState(
    val profiles: List<ConnectionProfile> = emptyList(),
    val isLoading: Boolean = true
)
