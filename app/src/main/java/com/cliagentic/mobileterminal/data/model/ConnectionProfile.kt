package com.cliagentic.mobileterminal.data.model

data class ConnectionProfile(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType = AuthType.PASSWORD,
    val biometricForKey: Boolean = false,
    val tmuxPrefix: TmuxPrefix = TmuxPrefix.CTRL_B
)
