package com.cliagentic.mobileterminal

import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.ConnectionProfile
import com.cliagentic.mobileterminal.data.model.ProfileValidator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProfileValidatorTest {

    @Test
    fun `password profile requires password when no existing secret`() {
        val profile = ConnectionProfile(
            name = "Dev",
            host = "example.com",
            port = 22,
            username = "alice",
            authType = AuthType.PASSWORD
        )

        val result = ProfileValidator.validate(
            profile = profile,
            newPassword = null,
            newPrivateKey = null,
            hasExistingSecret = false
        )

        assertThat(result.isValid).isFalse()
        assertThat(result.errors).contains("Password auth requires a password")
    }

    @Test
    fun `key profile is valid with existing key`() {
        val profile = ConnectionProfile(
            name = "Prod",
            host = "prod.internal",
            port = 22,
            username = "ops",
            authType = AuthType.KEY
        )

        val result = ProfileValidator.validate(
            profile = profile,
            newPassword = null,
            newPrivateKey = null,
            hasExistingSecret = true
        )

        assertThat(result.isValid).isTrue()
        assertThat(result.errors).isEmpty()
    }
}
