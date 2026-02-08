package com.cliagentic.mobileterminal.ssh

import com.cliagentic.mobileterminal.data.repository.KnownHostRepository
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

class JschSshClient(
    private val knownHostRepository: KnownHostRepository
) : SshClient {

    override suspend fun connect(
        request: SshConnectRequest,
        onUnknownHost: suspend (HostKeyPrompt) -> Boolean
    ): Result<SshSession> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val jsch = JSch()

                if (request.useKeyAuth) {
                    val key = request.privateKey?.trim().orEmpty()
                    require(key.isNotBlank()) { "Private key is required for key authentication" }
                    jsch.addIdentity(
                        "profile-key",
                        key.toByteArray(Charsets.UTF_8),
                        null,
                        null
                    )
                }

                val session = jsch.getSession(request.username, request.host, request.port)
                val config = Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put(
                        "PreferredAuthentications",
                        if (request.useKeyAuth) "publickey" else "password,keyboard-interactive"
                    )
                }
                session.setConfig(config)

                if (!request.useKeyAuth) {
                    session.setPassword(request.password)
                }

                session.connect(15_000)

                val hostKey = session.hostKey
                val fingerprint = hostKey.getFingerPrint(jsch)
                val algorithm = hostKey.type

                val knownHost = knownHostRepository.get(request.host, request.port)
                if (knownHost != null && knownHost.fingerprint != fingerprint) {
                    session.disconnect()
                    error("host key mismatch")
                }

                if (knownHost == null) {
                    val accepted = onUnknownHost(
                        HostKeyPrompt(
                            host = request.host,
                            port = request.port,
                            algorithm = algorithm,
                            fingerprint = fingerprint
                        )
                    )
                    if (!accepted) {
                        session.disconnect()
                        error("reject HostKey")
                    }

                    knownHostRepository.trust(
                        host = request.host,
                        port = request.port,
                        algorithm = algorithm,
                        fingerprint = fingerprint
                    )
                }

                val channel = session.openChannel("shell") as ChannelShell
                channel.setPtyType("xterm-256color")
                channel.connect(8_000)

                val outputStream = channel.outputStream
                val inputStream = channel.inputStream

                JschSshSession(
                    session = session,
                    channel = channel,
                    input = inputStream,
                    stdin = outputStream
                )
            }
        }
    }
}

private class JschSshSession(
    private val session: Session,
    private val channel: ChannelShell,
    private val input: InputStream,
    private val stdin: OutputStream
) : SshSession {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _output = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val output: Flow<String> = _output.asSharedFlow()

    init {
        scope.launch {
            val buffer = ByteArray(4096)
            while (channel.isConnected && isActive) {
                val read = runCatching { input.read(buffer) }.getOrElse { -1 }
                if (read <= 0) break
                val chunk = String(buffer, 0, read, Charsets.UTF_8)
                _output.emit(chunk)
            }
        }
    }

    override suspend fun send(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            stdin.write(bytes)
            stdin.flush()
        }
    }

    override suspend fun send(text: String) {
        send(text.toByteArray(Charsets.UTF_8))
    }

    override suspend fun execute(command: String, timeoutMs: Long): Result<SshCommandResult> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val stdout = ByteArrayOutputStream()
                val stderr = ByteArrayOutputStream()
                val exec = session.openChannel("exec") as ChannelExec
                exec.setCommand(command)
                exec.setInputStream(null)
                exec.setOutputStream(stdout)
                exec.setErrStream(stderr, true)
                exec.connect(5_000)

                val start = System.currentTimeMillis()
                while (!exec.isClosed && System.currentTimeMillis() - start < timeoutMs) {
                    delay(50)
                }

                if (!exec.isClosed) {
                    exec.disconnect()
                    error("Command timed out")
                }

                val result = SshCommandResult(
                    stdout = stdout.toString(Charsets.UTF_8.name()),
                    stderr = stderr.toString(Charsets.UTF_8.name()),
                    exitCode = exec.exitStatus
                )
                exec.disconnect()
                result
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching { channel.disconnect() }
            runCatching { session.disconnect() }
            scope.cancel()
        }
    }
}
