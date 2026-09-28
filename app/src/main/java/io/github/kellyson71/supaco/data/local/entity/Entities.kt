package io.github.kellyson71.supaco.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.kellyson71.supaco.data.model.BoletimItem
import io.github.kellyson71.supaco.data.model.NotaEtapa
import io.github.kellyson71.supaco.data.model.PeriodoLetivo
import io.github.kellyson71.supaco.data.model.Profile
import io.github.kellyson71.supaco.data.model.Vinculo

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int,
    val matricula: String,
    val nomeUsual: String,
    val tipoVinculo: String,
    val fotoUrl: String?,
    @Embedded(prefix = "vinculo_") val vinculo: VinculoEntity
) {
    fun toDomain() = Profile(
        id = id,
        matricula = matricula,
        nomeUsual = nomeUsual,
        tipoVinculo = tipoVinculo,
        fotoUrl = fotoUrl,
        vinculo = Vinculo(
            matricula = vinculo.matricula,
            nome = vinculo.nome,
            curso = vinculo.curso,
            campus = vinculo.campus
        )
    )
    
    companion object {
        fun fromDomain(profile: Profile) = ProfileEntity(
            id = profile.id,
            matricula = profile.matricula,
            nomeUsual = profile.nomeUsual,
            tipoVinculo = profile.tipoVinculo,
            fotoUrl = profile.fotoUrl,
            vinculo = VinculoEntity(
                matricula = profile.vinculo.matricula,
                nome = profile.vinculo.nome,
                curso = profile.vinculo.curso,
                campus = profile.vinculo.campus
            )
        )
    }
}

data class VinculoEntity(
    val matricula: String,
    val nome: String,
    val curso: String,
    val campus: String
)

/** Cache do boletim do período corrente — alimenta widgets e notificações. */
@Entity(tableName = "boletim")
data class BoletimEntity(
    @PrimaryKey val codigoDiario: String,
    val anoLetivo: Int,
    val periodoLetivo: Int,
    val disciplina: String,
    val cargaHoraria: Int,
    val cargaHorariaCumprida: Int,
    val numeroFaltas: Int,
    val frequencia: Double,
    val situacao: String,
    val quantidadeAvaliacoes: Int,
    val mediaDisciplina: Double?,
    val mediaFinal: Double?,
    val notaEtapa1: Double?,
    val notaEtapa2: Double?,
    val notaEtapa3: Double?,
    val notaEtapa4: Double?,
    val notaAvaliacaoFinal: Double?,
) {
    fun toDomain() = BoletimItem(
        codigoDiario = codigoDiario,
        disciplina = disciplina,
        cargaHoraria = cargaHoraria,
        cargaHorariaCumprida = cargaHorariaCumprida,
        numeroFaltas = numeroFaltas,
        frequencia = frequencia,
        situacao = situacao,
        quantidadeAvaliacoes = quantidadeAvaliacoes,
        mediaDisciplina = mediaDisciplina,
        mediaFinal = mediaFinal,
        notaEtapa1 = notaEtapa1?.let { NotaEtapa(nota = it) },
        notaEtapa2 = notaEtapa2?.let { NotaEtapa(nota = it) },
        notaEtapa3 = notaEtapa3?.let { NotaEtapa(nota = it) },
        notaEtapa4 = notaEtapa4?.let { NotaEtapa(nota = it) },
        notaAvaliacaoFinal = notaAvaliacaoFinal?.let { NotaEtapa(nota = it) },
    )

    companion object {
        fun fromDomain(item: BoletimItem, periodo: PeriodoLetivo) = BoletimEntity(
            codigoDiario = item.codigoDiario,
            anoLetivo = periodo.anoLetivo,
            periodoLetivo = periodo.periodoLetivo,
            disciplina = item.disciplina,
            cargaHoraria = item.cargaHoraria,
            cargaHorariaCumprida = item.cargaHorariaCumprida,
            numeroFaltas = item.numeroFaltas,
            frequencia = item.frequencia,
            situacao = item.situacao,
            quantidadeAvaliacoes = item.quantidadeAvaliacoes,
            mediaDisciplina = item.mediaDisciplina,
            mediaFinal = item.mediaFinal,
            notaEtapa1 = item.notaEtapa1?.nota,
            notaEtapa2 = item.notaEtapa2?.nota,
            notaEtapa3 = item.notaEtapa3?.nota,
            notaEtapa4 = item.notaEtapa4?.nota,
            notaAvaliacaoFinal = item.notaAvaliacaoFinal?.nota,
        )
    }
}

@Entity(tableName = "horarios")
data class HorarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sigla: String,
    val dia: String,        // "Terça"
    val horaInicio: String, // "13:00"
    val horaFim: String,    // "16:20"
    val sala: String,
    /** Quantidade de aulas (blocos de 45 min) — cada uma conta como uma falta. */
    val aulas: Int,
)
