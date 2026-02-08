package com.cliagentic.mobileterminal.ssh

import kotlinx.coroutines.flow.Flow

interface SshSession {
    val output: Flow<String>
    suspend fun send(bytes: ByteArray)
    suspend fun send(text: String)
    suspend fun execute(command: String, timeoutMs: Long = 8_000): Result<SshCommandResult>
    suspend fun disconnect()
}

data class SshCommandResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)

interface SshClient {
    suspend fun connect(
        request: SshConnectRequest,
        onUnknownHost: suspend (HostKeyPrompt) -> Boolean
    ): Result<SshSession>
}

data class SshConnectRequest(
    val host: String,
    val port: Int,
    val username: String,
    val password: String?,
    val privateKey: String?,
    val useKeyAuth: Boolean
)
