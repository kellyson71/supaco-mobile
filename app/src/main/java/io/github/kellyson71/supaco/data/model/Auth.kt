package io.github.kellyson71.supaco.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

// POST /api/token/pair
@Serializable
data class TokenPairResponse(
    val access: String,
    val refresh: String,
)

@Serializable
data class RefreshRequest(
    val refresh: String,
)

// POST /api/token/refresh — o spec declara `access` como anulável.
@Serializable
data class RefreshResponse(
    val access: String? = null,
    val refresh: String? = null,
)
