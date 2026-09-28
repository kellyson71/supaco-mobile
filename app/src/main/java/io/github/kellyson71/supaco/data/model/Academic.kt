package io.github.kellyson71.supaco.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val results: List<T> = emptyList(),
    val count: Int = 0,
    val next: String? = null,
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
    val descricao: String = "",
    @SerialName("locais_de_aula") val locaisDeAula: List<String> = emptyList(),
    @SerialName("horarios_de_aula") val horariosDeAula: String? = null,
)

@Serializable
data class NotaEtapa(
    @Serializable(with = FlexibleDoubleSerializer::class) val nota: Double? = null,
    val faltas: Int = 0,
)

@Serializable
data class BoletimItem(
    @SerialName("codigo_diario") val codigoDiario: String,
    val disciplina: String,
    @SerialName("carga_horaria") val cargaHoraria: Int = 0,
    @SerialName("carga_horaria_cumprida") val cargaHorariaCumprida: Int = 0,
    @SerialName("numero_faltas") val numeroFaltas: Int = 0,
    @SerialName("percentual_carga_horaria_frequentada") val frequencia: Double = 100.0,
    val situacao: String = "",
    @SerialName("quantidade_avaliacoes") val quantidadeAvaliacoes: Int = 2,
    @Serializable(with = FlexibleDoubleSerializer::class)
    @SerialName("media_disciplina") val mediaDisciplina: Double? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    @SerialName("media_final_disciplina") val mediaFinal: Double? = null,
    @SerialName("nota_etapa_1") val notaEtapa1: NotaEtapa? = null,
    @SerialName("nota_etapa_2") val notaEtapa2: NotaEtapa? = null,
    @SerialName("nota_etapa_3") val notaEtapa3: NotaEtapa? = null,
    @SerialName("nota_etapa_4") val notaEtapa4: NotaEtapa? = null,
    @SerialName("nota_avaliacao_final") val notaAvaliacaoFinal: NotaEtapa? = null,
) {
    // Média consolidada: media_final (já considera a NAF) tem prioridade sobre media_disciplina
    val mediaDisplay: String get() = (mediaFinal ?: mediaDisciplina)?.toString() ?: "--"
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
