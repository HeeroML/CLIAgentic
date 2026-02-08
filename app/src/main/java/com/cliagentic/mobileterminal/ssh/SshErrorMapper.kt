package com.cliagentic.mobileterminal.ssh

import java.net.ConnectException
import java.net.UnknownHostException

object SshErrorMapper {
    fun toMessage(throwable: Throwable): String {
        val cause = throwable.cause ?: throwable
        val message = throwable.message.orEmpty()

        return when {
            cause is UnknownHostException -> "DNS lookup failed. Check host name and network."
            cause is ConnectException -> "Connection failed. Check host, port, and network reachability."
            message.contains("Auth fail", ignoreCase = true) ->
                "Authentication failed. Verify username and credentials."

            message.contains("Authentication failed", ignoreCase = true) ->
                "Authentication failed. Verify username and credentials."

            message.contains("reject HostKey", ignoreCase = true) ->
                "Host key was not trusted."

            message.contains("timeout", ignoreCase = true) ->
                "Connection timed out."

            message.contains("host key mismatch", ignoreCase = true) ->
                "Host key mismatch detected. Possible MITM or host re-provisioning."

            message.contains("host key", ignoreCase = true) &&
                message.contains("mismatch", ignoreCase = true) ->
                "Host key mismatch detected. Possible MITM or host re-provisioning."

            message.contains("private key", ignoreCase = true) ->
                "Private key is invalid or not accepted by the server."

            else -> "SSH error: ${throwable.message ?: throwable::class.java.simpleName}"
        }
    }
}
