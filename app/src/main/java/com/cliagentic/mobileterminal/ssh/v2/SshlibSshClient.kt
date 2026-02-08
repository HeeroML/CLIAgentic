package com.cliagentic.mobileterminal.ssh.v2

import android.util.Base64
import com.cliagentic.mobileterminal.data.repository.KnownHostRepository
import com.cliagentic.mobileterminal.ssh.ChallengePrompt
import com.cliagentic.mobileterminal.ssh.CommandBoundaryType
import com.cliagentic.mobileterminal.ssh.HostKeyPrompt
import com.cliagentic.mobileterminal.ssh.HostKeyPromptResponse
import com.cliagentic.mobileterminal.ssh.KeyboardInteractivePrompt
import com.cliagentic.mobileterminal.ssh.KeyboardInteractiveResponse
import com.cliagentic.mobileterminal.ssh.PassphrasePrompt
import com.cliagentic.mobileterminal.ssh.PassphrasePromptResponse
import com.cliagentic.mobileterminal.ssh.PasswordPrompt
import com.cliagentic.mobileterminal.ssh.PasswordPromptResponse
import com.cliagentic.mobileterminal.ssh.PromptCancelledResponse
import com.cliagentic.mobileterminal.ssh.SshClient
import com.cliagentic.mobileterminal.ssh.SshCommandResult
import com.cliagentic.mobileterminal.ssh.SshConnectRequest
import com.cliagentic.mobileterminal.ssh.SshEvent
import com.cliagentic.mobileterminal.ssh.SshPrompt
import com.cliagentic.mobileterminal.ssh.SshPromptResponse
import com.cliagentic.mobileterminal.ssh.SshSession
import com.trilead.ssh2.ChannelCondition
import com.trilead.ssh2.Connection
import com.trilead.ssh2.ConnectionInfo
import com.trilead.ssh2.ConnectionMonitor
import com.trilead.ssh2.ExtendedServerHostKeyVerifier
import com.trilead.ssh2.InteractiveCallback
import com.trilead.ssh2.Session
import com.trilead.ssh2.crypto.fingerprint.KeyFingerprint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class SshlibSshClient(
    private val knownHostRepository: KnownHostRepository
) : SshClient {

    override suspend fun connect(
        request: SshConnectRequest,
        onPrompt: suspend (SshPrompt) -> SshPromptResponse
    ): Result<SshSession> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val verifier = buildHostKeyVerifier(request, onPrompt)
                val connection = Connection(request.host, request.port)
                val info = connection.connect(verifier, request.connectTimeoutMs, request.connectTimeoutMs)
                authenticate(connection, request, onPrompt)
                if (!connection.isAuthenticationComplete) {
                    error("Authentication failed")
                }

                val shellSession = connection.openSession()
                shellSession.requestPTY(request.ptyType.trim().ifBlank { "xterm-256color" })
                shellSession.startShell()

                SshlibSshSession(
                    connection = connection,
                    shellSession = shellSession,
                    keepaliveIntervalMs = request.keepaliveIntervalMs,
                    connectionInfo = info
                )
            }
        }
    }

    private fun buildHostKeyVerifier(
        request: SshConnectRequest,
        onPrompt: suspend (SshPrompt) -> SshPromptResponse
    ): ExtendedServerHostKeyVerifier {
        return object : ExtendedServerHostKeyVerifier() {
            override fun getKnownKeyAlgorithmsForHost(host: String, port: Int): MutableList<String> {
                val known = runBlocking {
                    knownHostRepository.get(host, port)
                        ?: knownHostRepository.get(request.host, request.port)
                }
                return known?.let { mutableListOf(it.algorithm) } ?: mutableListOf()
            }

            override fun removeServerHostKey(host: String, port: Int, algorithm: String, hostKey: ByteArray) {
                runBlocking {
                    val known = knownHostRepository.get(host, port)
                        ?: knownHostRepository.get(request.host, request.port)
                    if (known != null &&
                        known.algorithm == algorithm &&
                        known.hostKey.contentEquals(hostKey)
                    ) {
                        knownHostRepository.remove(known.host, known.port)
                    }
                }
            }

            override fun addServerHostKey(host: String, port: Int, algorithm: String, hostKey: ByteArray) {
                val (sha256, md5) = fingerprints(hostKey)
                runBlocking {
                    knownHostRepository.trust(
                        host = host,
                        port = port,
                        algorithm = algorithm,
                        hostKey = hostKey,
                        sha256Fingerprint = sha256,
                        md5Fingerprint = md5
                    )
                }
            }

            override fun verifyServerHostKey(
                hostname: String,
                port: Int,
                serverHostKeyAlgorithm: String,
                serverHostKey: ByteArray
            ): Boolean {
                val known = runBlocking {
                    knownHostRepository.get(hostname, port)
                        ?: knownHostRepository.get(request.host, request.port)
                }
                val (sha256, md5) = fingerprints(serverHostKey)

                val prompt = HostKeyPrompt(
                    host = hostname,
                    port = port,
                    algorithm = serverHostKeyAlgorithm,
                    sha256Fingerprint = sha256,
                    md5Fingerprint = md5,
                    keyBlobBase64 = Base64.encodeToString(serverHostKey, Base64.NO_WRAP),
                    mismatchDetected = known != null &&
                        (known.algorithm != serverHostKeyAlgorithm || !known.hostKey.contentEquals(serverHostKey)),
                    previousSha256Fingerprint = known?.sha256Fingerprint
                )

                val shouldTrust = when {
                    known == null -> {
                        (runBlocking { onPrompt(prompt) } as? HostKeyPromptResponse)?.trust == true
                    }

                    known.algorithm == serverHostKeyAlgorithm && known.hostKey.contentEquals(serverHostKey) -> true
                    else -> {
                        (runBlocking { onPrompt(prompt) } as? HostKeyPromptResponse)?.trust == true
                    }
                }

                if (shouldTrust) {
                    runBlocking {
                        knownHostRepository.trust(
                            host = hostname,
                            port = port,
                            algorithm = serverHostKeyAlgorithm,
                            hostKey = serverHostKey,
                            sha256Fingerprint = sha256,
                            md5Fingerprint = md5
                        )
                    }
                }

                return shouldTrust
            }
        }
    }

    private fun fingerprints(hostKey: ByteArray): Pair<String, String> {
        return KeyFingerprint.createSHA256Fingerprint(hostKey) to
            KeyFingerprint.createMD5Fingerprint(hostKey)
    }

    private suspend fun authenticate(
        connection: Connection,
        request: SshConnectRequest,
        onPrompt: suspend (SshPrompt) -> SshPromptResponse
    ) {
        if (connection.authenticateWithNone(request.username)) return

        if (request.useKeyAuth) {
            val privateKey = request.privateKey?.trim().orEmpty()
            if (privateKey.isBlank()) error("Private key is required for key authentication")

            var passphrase: String? = null
            var promptedPassphrase = false

            while (true) {
                val success = runCatching {
                    connection.authenticateWithPublicKey(request.username, privateKey.toCharArray(), passphrase)
                }.getOrElse { throwable ->
                    val shouldPrompt = !promptedPassphrase &&
                        (throwable.message?.contains("encrypted", ignoreCase = true) == true ||
                            throwable.message?.contains("passphrase", ignoreCase = true) == true)

                    if (!shouldPrompt) throw throwable

                    promptedPassphrase = true
                    val response = onPrompt(PassphrasePrompt())
                    passphrase = (response as? PassphrasePromptResponse)?.passphrase
                    if (passphrase.isNullOrBlank()) {
                        false
                    } else {
                        connection.authenticateWithPublicKey(request.username, privateKey.toCharArray(), passphrase)
                    }
                }
                if (success) return
                break
            }
        }

        if (connection.isAuthMethodAvailable(request.username, AUTH_KEYBOARD_INTERACTIVE)) {
            val interactiveOk = connection.authenticateWithKeyboardInteractive(
                request.username,
                InteractiveCallback { name, instruction, numPrompts, prompts, echo ->
                    val challengePrompts = (0 until numPrompts).map { index ->
                        ChallengePrompt(
                            prompt = prompts.getOrNull(index).orEmpty(),
                            echo = echo.getOrNull(index) ?: false
                        )
                    }

                    val allHidden = challengePrompts.isNotEmpty() && challengePrompts.all { !it.echo }
                    val prefilledPassword = request.password

                    if (allHidden && !prefilledPassword.isNullOrBlank()) {
                        return@InteractiveCallback Array(numPrompts) { prefilledPassword }
                    }

                    val response = runBlocking {
                        onPrompt(
                            KeyboardInteractivePrompt(
                                name = name.orEmpty(),
                                instruction = instruction.orEmpty(),
                                prompts = challengePrompts
                            )
                        )
                    }

                    when (response) {
                        is KeyboardInteractiveResponse -> {
                            Array(numPrompts) { index ->
                                response.responses.getOrNull(index).orEmpty()
                            }
                        }

                        is PasswordPromptResponse -> {
                            Array(numPrompts) { response.password.orEmpty() }
                        }

                        is PromptCancelledResponse -> Array(numPrompts) { "" }
                        else -> Array(numPrompts) { "" }
                    }
                }
            )
            if (interactiveOk) return
        }

        if (connection.isAuthMethodAvailable(request.username, AUTH_PASSWORD)) {
            var password = request.password
            if (password.isNullOrBlank()) {
                val response = onPrompt(PasswordPrompt(username = request.username))
                password = (response as? PasswordPromptResponse)?.password
            }
            if (!password.isNullOrBlank()) {
                if (connection.authenticateWithPassword(request.username, password)) return
            }
        }
    }

    private companion object {
        private const val AUTH_PASSWORD = "password"
        private const val AUTH_KEYBOARD_INTERACTIVE = "keyboard-interactive"
    }
}

private class SshlibSshSession(
    private val connection: Connection,
    private val shellSession: Session,
    private val keepaliveIntervalMs: Int,
    connectionInfo: ConnectionInfo
) : SshSession, ConnectionMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val osc133Parser = Osc133Parser()

    private val _output = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _events = MutableSharedFlow<SshEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val output: Flow<ByteArray> = _output.asSharedFlow()
    override val events: Flow<SshEvent> = _events.asSharedFlow()

    private val stdin = shellSession.stdin
    private val stdout = shellSession.stdout

    init {
        connection.addConnectionMonitor(this)
        scope.launch {
            _events.emit(SshEvent.Connected)
            _events.emit(
                SshEvent.KexInfo(
                    keyExchangeAlgorithm = connectionInfo.keyExchangeAlgorithm.orEmpty(),
                    clientCipher = connectionInfo.clientToServerCryptoAlgorithm.orEmpty(),
                    serverCipher = connectionInfo.serverToClientCryptoAlgorithm.orEmpty(),
                    hostKeyAlgorithm = connectionInfo.serverHostKeyAlgorithm.orEmpty()
                )
            )

            val readBuffer = ByteArray(4096)
            while (isActive) {
                val read = runCatching { stdout.read(readBuffer) }.getOrElse { -1 }
                if (read <= 0) break
                val chunk = readBuffer.copyOf(read)
                _output.emit(chunk)
                osc133Parser.consume(chunk).forEach { boundary ->
                    _events.emit(boundary)
                }
            }
            _events.emit(SshEvent.Disconnected("SSH stream closed"))
        }

        scope.launch {
            while (isActive) {
                delay(keepaliveIntervalMs.toLong())
                runCatching { connection.ping() }
                    .onFailure { throwable ->
                        _events.emit(SshEvent.Disconnected(throwable.message))
                        disconnect()
                    }
            }
        }
    }

    override suspend fun send(bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            stdin.write(bytes)
            stdin.flush()
        }
    }

    override suspend fun resizePty(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        withContext(Dispatchers.IO) {
            shellSession.resizePTY(cols, rows, widthPx, heightPx)
        }
    }

    override suspend fun execute(command: String, timeoutMs: Long): Result<SshCommandResult> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val execSession = connection.openSession()
                val stdoutBytes = ByteArrayOutputStream()
                val stderrBytes = ByteArrayOutputStream()

                execSession.execCommand(command)
                val execStdout = execSession.stdout
                val execStderr = execSession.stderr

                val deadline = System.currentTimeMillis() + timeoutMs
                val conditions = ChannelCondition.STDOUT_DATA or
                    ChannelCondition.STDERR_DATA or
                    ChannelCondition.EOF or
                    ChannelCondition.CLOSED or
                    ChannelCondition.EXIT_STATUS

                while (System.currentTimeMillis() < deadline) {
                    val remaining = (deadline - System.currentTimeMillis()).coerceAtMost(200)
                    val met = execSession.waitForCondition(conditions, remaining)

                    if ((met and ChannelCondition.STDOUT_DATA) != 0) {
                        drain(execStdout, stdoutBytes)
                    }
                    if ((met and ChannelCondition.STDERR_DATA) != 0) {
                        drain(execStderr, stderrBytes)
                    }
                    if ((met and (ChannelCondition.EOF or ChannelCondition.CLOSED)) != 0) {
                        break
                    }
                }

                if (System.currentTimeMillis() >= deadline && execSession.exitStatus == null) {
                    execSession.close()
                    error("Command timed out")
                }

                drain(execStdout, stdoutBytes)
                drain(execStderr, stderrBytes)

                val result = SshCommandResult(
                    stdout = stdoutBytes.toString(Charsets.UTF_8.name()),
                    stderr = stderrBytes.toString(Charsets.UTF_8.name()),
                    exitCode = execSession.exitStatus ?: -1
                )
                execSession.close()
                result
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching { shellSession.close() }
            runCatching { connection.close() }
            scope.cancel()
            _events.tryEmit(SshEvent.Disconnected("Disconnected by user"))
        }
    }

    override fun connectionLost(reason: Throwable) {
        _events.tryEmit(SshEvent.Disconnected(reason.message))
    }

    private fun drain(stream: InputStream, sink: ByteArrayOutputStream) {
        val buffer = ByteArray(4096)
        while (true) {
            val available = stream.available()
            val read = if (available > 0) {
                stream.read(buffer, 0, minOf(buffer.size, available))
            } else {
                stream.read(buffer, 0, buffer.size)
            }
            if (read <= 0) break
            sink.write(buffer, 0, read)
            if (available <= buffer.size) break
        }
    }
}
