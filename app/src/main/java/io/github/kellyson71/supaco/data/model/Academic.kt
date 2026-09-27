package io.github.kellyson71.supaco.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val results: List<T>,
    val count: Int = 0,
)

@Serializable
data class PeriodoLetivo(
    @SerialName("ano_letivo") val anoLetivo: Int,
    @SerialName("periodo_letivo") val periodoLetivo: Int,
) {
    val label: String get() = "$anoLetivo.$periodoLetivo"
}

// /api/ensino/minhas-turmas-virtuais/{ano}/{periodo}/ — fonte da grade real de horários
@Serializable
data class TurmaVirtual(
    val id: String,
    val sigla: String,
    val descricao: String,
    @SerialName("locais_de_aula") val locaisDeAula: List<String> = emptyList(),
    @SerialName("horarios_de_aula") val horariosDeAula: String? = null,
)

@Serializable
data class NotaEtapa(
    val nota: Double? = null,
    val faltas: Int = 0,
)

@Serializable
data class BoletimItem(
    @SerialName("codigo_diario") val codigoDiario: String,
    val disciplina: String,
    @SerialName("carga_horaria") val cargaHoraria: Int,
    @SerialName("numero_faltas") val numeroFaltas: Int,
    @SerialName("percentual_carga_horaria_frequentada") val frequencia: Double,
    val situacao: String,
    @SerialName("quantidade_avaliacoes") val quantidadeAvaliacoes: Int = 2,
    @SerialName("media_disciplina") val mediaDisciplina: Double? = null,
    @SerialName("media_final_disciplina") val mediaFinal: Double? = null,
    @SerialName("nota_etapa_1") val notaEtapa1: NotaEtapa? = null,
    @SerialName("nota_etapa_2") val notaEtapa2: NotaEtapa? = null,
    @SerialName("nota_etapa_3") val notaEtapa3: NotaEtapa? = null,
    @SerialName("nota_etapa_4") val notaEtapa4: NotaEtapa? = null,
) {
    // Média consolidada: media_disciplina ou media_final (a que vier primeiro)
    val mediaDisplay: String get() = when {
        mediaDisciplina != null -> mediaDisciplina.toString()
        mediaFinal != null -> mediaFinal.toString()
        else -> "--"
    }

    // Nota da etapa 1 como string exibível
    val notaEtapa1Display: String get() = notaEtapa1?.nota?.toString() ?: "--"
    val notaEtapa2Display: String get() = notaEtapa2?.nota?.toString() ?: "--"
}

@Serializable
data class Servidor(
    val matricula: String? = null,
    val nome: String,
    @SerialName("setor_suap") val setorSuap: String? = null,
    val campus: String? = null,
    val cargo: String? = null,
    val categoria: String? = null,
    @SerialName("url_foto_75x100") val urlFoto: String? = null,
    @SerialName("telefones_institucionais") val telefones: List<String> = emptyList(),
    @SerialName("curriculo_lattes") val curriculoLattes: String? = null,
)

