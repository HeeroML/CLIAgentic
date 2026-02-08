package com.cliagentic.mobileterminal.ui.state

import com.cliagentic.mobileterminal.data.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val exportJson: String = "",
    val importJson: String = "",
    val statusMessage: String? = null
)
