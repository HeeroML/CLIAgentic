package com.cliagentic.mobileterminal.ui.state

import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.TmuxPrefix

data class ProfileEditorUiState(
    val id: Long = 0,
    val name: String = "",
    val host: String = "",
    val port: String = "22",
    val username: String = "",
    val authType: AuthType = AuthType.PASSWORD,
    val biometricForKey: Boolean = false,
    val tmuxPrefix: TmuxPrefix = TmuxPrefix.CTRL_B,
    val password: String = "",
    val privateKey: String = "",
    val hasStoredPassword: Boolean = false,
    val hasStoredPrivateKey: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errors: List<String> = emptyList(),
    val savedProfileId: Long? = null
)
