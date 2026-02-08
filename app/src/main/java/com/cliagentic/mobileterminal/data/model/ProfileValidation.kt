package com.cliagentic.mobileterminal.data.model

data class ProfileValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

object ProfileValidator {
    fun validate(
        profile: ConnectionProfile,
        newPassword: String?,
        newPrivateKey: String?,
        hasExistingSecret: Boolean
    ): ProfileValidationResult {
        val errors = mutableListOf<String>()

        if (profile.name.isBlank()) errors += "Profile name is required"
        if (profile.host.isBlank()) errors += "Host is required"
        if (profile.port !in 1..65535) errors += "Port must be between 1 and 65535"
        if (profile.username.isBlank()) errors += "Username is required"

        when (profile.authType) {
            AuthType.PASSWORD -> {
                val hasPassword = !newPassword.isNullOrBlank() || hasExistingSecret
                if (!hasPassword) errors += "Password auth requires a password"
            }

            AuthType.KEY -> {
                val hasKey = !newPrivateKey.isNullOrBlank() || hasExistingSecret
                if (!hasKey) errors += "Key auth requires a private key"
            }
        }

        return ProfileValidationResult(isValid = errors.isEmpty(), errors = errors)
    }
}
