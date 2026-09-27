package io.github.kellyson71.supaco.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Response from /api/rh/eu/
@Serializable
data class ProfileResponse(
    val identificacao: String,
    @SerialName("nome_usual") val nomeUsual: String,
    val nome: String = "",
    @SerialName("tipo_usuario") val tipoUsuario: String = "",
    val campus: String = "",
    val foto: String? = null,
    val email: String? = null,
)

// Internal app model — used by UI and Room
data class Profile(
    val id: Int,
    val matricula: String,
    val nomeUsual: String,
    val tipoVinculo: String,
    val vinculo: Vinculo,
    val fotoUrl: String? = null,
)

data class Vinculo(
    val matricula: String,
    val nome: String,
    val curso: String,
    val campus: String,
)

fun ProfileResponse.toProfile() = Profile(
    id = 1,
    matricula = identificacao,
    nomeUsual = nomeUsual.ifBlank { nome },
    tipoVinculo = tipoUsuario,
    vinculo = Vinculo(
        matricula = identificacao,
        nome = nome,
        curso = campus,
        campus = campus,
    ),
    fotoUrl = foto,
)
