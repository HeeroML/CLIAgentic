package com.cliagentic.mobileterminal.ssh

import kotlinx.coroutines.flow.Flow

interface SshSession {
    val output: Flow<ByteArray>
    val events: Flow<SshEvent>
    suspend fun send(bytes: ByteArray)
    suspend fun send(text: String) = send(text.toByteArray(Charsets.UTF_8))
    suspend fun resizePty(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0)
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
        onPrompt: suspend (SshPrompt) -> SshPromptResponse
    ): Result<SshSession>
}

data class SshConnectRequest(
    val host: String,
    val port: Int,
    val username: String,
    val password: String?,
    val privateKey: String?,
    val useKeyAuth: Boolean,
    val ptyType: String = "xterm-256color",
    val connectTimeoutMs: Int = 15_000,
    val keepaliveIntervalMs: Int = 30_000
)

sealed interface SshPrompt

data class PasswordPrompt(
    val username: String,
    val message: String = "Password required"
) : SshPrompt

data class PassphrasePrompt(
    val keyLabel: String = "private key",
    val message: String = "Passphrase required for private key"
) : SshPrompt

data class KeyboardInteractivePrompt(
    val name: String,
    val instruction: String,
    val prompts: List<ChallengePrompt>
) : SshPrompt

data class ChallengePrompt(
    val prompt: String,
    val echo: Boolean
)

sealed interface SshPromptResponse

data class HostKeyPromptResponse(val trust: Boolean) : SshPromptResponse

data class PasswordPromptResponse(val password: String?) : SshPromptResponse

data class PassphrasePromptResponse(val passphrase: String?) : SshPromptResponse

data class KeyboardInteractiveResponse(val responses: List<String>) : SshPromptResponse

data object PromptCancelledResponse : SshPromptResponse

sealed interface SshEvent {
    data object Connected : SshEvent

    data class Disconnected(val reason: String? = null) : SshEvent

    data class KexInfo(
        val keyExchangeAlgorithm: String,
        val clientCipher: String,
        val serverCipher: String,
        val hostKeyAlgorithm: String
    ) : SshEvent

    data class CommandBoundary(
        val promptId: Int,
        val type: CommandBoundaryType,
        val exitCode: Int? = null
    ) : SshEvent

    data class Progress(
        val state: String,
        val percent: Int
    ) : SshEvent
}

enum class CommandBoundaryType {
    PROMPT,
    COMMAND_INPUT_START,
    COMMAND_OUTPUT_START,
    COMMAND_FINISHED
}
