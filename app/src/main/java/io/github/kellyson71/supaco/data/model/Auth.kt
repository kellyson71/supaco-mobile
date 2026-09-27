package io.github.kellyson71.supaco.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class TokenResponse(
    val access: String,
    val refresh: String? = null
)

@Serializable
data class RefreshRequest(
    val refresh: String
)
