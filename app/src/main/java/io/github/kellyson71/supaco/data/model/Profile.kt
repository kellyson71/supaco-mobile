package io.github.kellyson71.supaco.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Response from /api/rh/eu/
@Serializable
data class ProfileResponse(
    val identificacao: String,
    @SerialName("nome_usual") val nomeUsual: String = "",
    @SerialName("nome_social") val nomeSocial: String? = null,
    val nome: String = "",
    @SerialName("tipo_usuario") val tipoUsuario: String = "",
    val campus: String = "",
    val foto: String? = null,
    val email: String? = null,
) {
    /** Contas de servidor/prestador não têm boletim — o app é só para alunos. */
    val isAluno: Boolean
        get() = !tipoUsuario.contains("servidor", ignoreCase = true) &&
            !tipoUsuario.contains("prestador", ignoreCase = true)
}

// Response from /api/ensino/meus-dados-aluno/
@Serializable
data class DadosAlunoResponse(
    val curso: String = "",
    val situacao: String = "",
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

fun ProfileResponse.toProfile(curso: String = "") = Profile(
    id = 1,
    matricula = identificacao,
    nomeUsual = nomeUsual.ifBlank { nomeSocial?.takeIf { it.isNotBlank() } ?: nome },
    tipoVinculo = tipoUsuario,
    vinculo = Vinculo(
        matricula = identificacao,
        // Nome social tem prioridade sobre o de registro
        nome = nomeSocial?.takeIf { it.isNotBlank() } ?: nome,
        curso = curso,
        campus = campus,
    ),
    fotoUrl = foto,
)
