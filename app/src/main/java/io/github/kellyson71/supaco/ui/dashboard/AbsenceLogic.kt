package io.github.kellyson71.supaco.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.kellyson71.supaco.data.ScheduleData
import io.github.kellyson71.supaco.data.model.BoletimItem
import io.github.kellyson71.supaco.ui.components.OrgShape
import kotlin.math.floor

enum class AbsenceStatus { GO, WARN, LAST, NO, REPROVADO }

enum class MateriaCor { PRIMARY, SECONDARY, TERTIARY }

data class MateriaDisplay(
    val codigoDiario: String,
    val nome: String,
    val sigla: String,
    val professor: String?,
    val sala: String,
    val dia: String,
    val diaAbbr: String,
    val horaInicio: String,
    val horaFim: String,
    /** Todas as ocorrências semanais (grade real pode ter mais de um dia). */
    val horarios: List<io.github.kellyson71.supaco.data.ScheduleEntry>,
    val cor: MateriaCor,
    val shape: OrgShape,
    val icone: ImageVector,
    val total: Int,
    val faltas: Int,
    val frequencia: Double,
    val media: String?,
    val notaEtapa1: Double?,
    val notaEtapa2: Double?,
    val notaEtapa3: Double?,
    val notaEtapa4: Double?,
    val quantidadeAvaliacoes: Int,
    val situacao: String,
    val limite: Int,
    val restantes: Int,
    val status: AbsenceStatus,
    val ehHoje: Boolean,
    /** Aulas desta matéria hoje — faltar o dia custa esse tanto de faltas. */
    val aulasHoje: Int,
    /** Status considerando faltar todas as aulas de hoje (igual a [status] se não há aula hoje). */
    val statusHoje: AbsenceStatus,
) {
    /** Status que responde "posso faltar hoje?" para esta matéria. */
    val statusVeredito: AbsenceStatus get() = if (aulasHoje > 0) statusHoje else status
    /** Faltas livres que sobram depois de faltar hoje. */
    val restantesAposHoje: Int get() = restantes - aulasHoje
}

/** Máximo de faltas permitido: 25% da carga horária (frequência mínima de 75%). */
fun limiteFaltas(cargaHoraria: Int): Int = floor(cargaHoraria * 0.25).toInt()

/**
 * Status de risco ao faltar [aulas] aulas tendo [restantes] faltas livres.
 * Com aulas = 1 é o status "geral" da matéria.
 */
fun statusParaFaltar(restantes: Int, aulas: Int): AbsenceStatus {
    val depois = restantes - aulas
    return when {
        restantes < 0 -> AbsenceStatus.REPROVADO
        depois < 0 -> AbsenceStatus.NO
        depois == 0 -> AbsenceStatus.LAST
        depois <= 2 -> AbsenceStatus.WARN
        else -> AbsenceStatus.GO
    }
}

fun calcStatus(faltas: Int, cargaHoraria: Int): AbsenceStatus =
    statusParaFaltar(limiteFaltas(cargaHoraria) - faltas, aulas = 1)

/** Quão grave é o status — maior é pior. */
val AbsenceStatus.gravidade: Int get() = ordinal

/**
 * Resposta de "posso faltar hoje?": a matéria de hoje em pior situação depois de
 * contar todas as aulas do dia. Null se não há aula hoje.
 */
fun vereditoDoDia(materias: List<MateriaDisplay>): MateriaDisplay? =
    materias.filter { it.aulasHoje > 0 }
        .maxWithOrNull(compareBy<MateriaDisplay> { it.statusHoje.gravidade }.thenByDescending { it.restantesAposHoje })

/** Matéria com menos folga no semestre (independente do dia). */
fun materiaMaisCritica(materias: List<MateriaDisplay>): MateriaDisplay? =
    materias.minWithOrNull(compareBy<MateriaDisplay> { it.restantes }.thenBy { it.nome })

private fun iconeParaDisciplina(nome: String): ImageVector {
    val n = nome.lowercase()
    return when {
        "arquitetura" in n -> Icons.Rounded.Memory
        "sociedade" in n || "grupos" in n -> Icons.Rounded.Groups
        "metodologia" in n || "trabalho" in n || "científico" in n -> Icons.Rounded.Science
        "seminário" in n || "pesquisa" in n || "iniciação" in n -> Icons.Rounded.Lightbulb
        "cálculo" in n || "matem" in n -> Icons.Rounded.Calculate
        "algoritm" in n || "program" in n || "computação" in n -> Icons.Rounded.Code
        "banco" in n || "dados" in n -> Icons.Rounded.Storage
        "física" in n -> Icons.Rounded.Science
        "redes" in n || "network" in n || "comunicação" in n -> Icons.Rounded.Hub
        "sistemas" in n -> Icons.Rounded.Devices
        "web" in n || "internet" in n -> Icons.Rounded.Language
        "segurança" in n -> Icons.Rounded.Security
        "inteligência" in n || "ia" in n -> Icons.Rounded.Psychology
        "estatística" in n -> Icons.Rounded.BarChart
        else -> Icons.Rounded.AutoStories
    }
}

private val SHAPES_LIST = listOf(OrgShape.FLOWER, OrgShape.COOKIE, OrgShape.CLOVER)
private val CORES_LIST = listOf(MateriaCor.PRIMARY, MateriaCor.TERTIARY, MateriaCor.SECONDARY)

fun buildMaterias(
    items: List<BoletimItem>,
    horariosBySigla: Map<String, List<io.github.kellyson71.supaco.data.ScheduleEntry>> = emptyMap(),
    hoje: String = ScheduleData.currentDayName(),
): List<MateriaDisplay> {
    val diaOrdem = ScheduleData.DIAS_SEMANA.withIndex().associate { (i, d) -> d to i }
    return items.mapIndexed { i, item ->
        val nome = ScheduleData.limpaNome(item.disciplina)
        val sigla = ScheduleData.extraiSigla(item.disciplina)
        val horarios = horariosBySigla[sigla].orEmpty()
            .sortedWith(compareBy({ diaOrdem[it.dia] ?: 9 }, { it.horaInicio }))
        val schedule = horarios.firstOrNull()
        val totalFaltas = item.numeroFaltas
        val limite = limiteFaltas(item.cargaHoraria)
        val restantes = limite - totalFaltas
        val status = calcStatus(totalFaltas, item.cargaHoraria)
        val aulasHoje = horarios.filter { it.dia == hoje }.sumOf { it.aulas }
        // No card principal, prioriza a ocorrência de hoje se existir
        val principal = horarios.firstOrNull { it.dia == hoje } ?: schedule
        val dia = principal?.dia ?: "—"

        MateriaDisplay(
            codigoDiario = item.codigoDiario,
            nome = nome,
            sigla = sigla,
            professor = principal?.professor,
            sala = principal?.sala ?: "—",
            dia = dia,
            diaAbbr = ScheduleData.DIA_ABBR[dia] ?: dia.take(3),
            horaInicio = principal?.horaInicio ?: "—",
            horaFim = principal?.horaFim ?: "—",
            horarios = horarios,
            cor = CORES_LIST[i % CORES_LIST.size],
            shape = SHAPES_LIST[i % SHAPES_LIST.size],
            icone = iconeParaDisciplina(nome),
            total = item.cargaHoraria,
            faltas = totalFaltas,
            frequencia = item.frequencia,
            media = item.mediaDisplay,
            notaEtapa1 = item.notaEtapa1?.nota,
            notaEtapa2 = item.notaEtapa2?.nota,
            notaEtapa3 = item.notaEtapa3?.nota,
            notaEtapa4 = item.notaEtapa4?.nota,
            quantidadeAvaliacoes = item.quantidadeAvaliacoes,
            situacao = item.situacao,
            limite = limite,
            restantes = restantes,
            status = status,
            ehHoje = horarios.any { it.dia == hoje },
            aulasHoje = aulasHoje,
            statusHoje = if (aulasHoje > 0) statusParaFaltar(restantes, aulasHoje) else status,
        )
    }
}

// ── Verdict text ──
data class VerdictMeta(
    val word: String,
    val subtext: String,
    val icon: ImageVector,
)

private val LINES_ACIDO = mapOf(
    AbsenceStatus.GO to listOf(
        "Falta tranquilo, ninguém vai sentir sua ausência.",
        "Tá rico de falta. Gasta com responsabilidade (mentira).",
        "Fôlego de sobra. Vai lá tomar café da manhã às 14h."
    ),
    AbsenceStatus.WARN to listOf(
        "Dá pra faltar, mas tá vivendo perigosamente, hein.",
        "Maneira na empolgação, o limite tá te observando.",
        "Mais uma soneca dessas e o semestre cobra a conta."
    ),
    AbsenceStatus.LAST to listOf(
        "Última falta antes do sistema te dar um abraço (DP).",
        "Guarda essa bala pro dia da prova surpresa. Ou não.",
        "Respira: você tem exatamente UMA chance de vacilar."
    ),
    AbsenceStatus.NO to listOf(
        "Senta, respira e vai pra aula. Sério.",
        "Faltar agora é assinar o atestado de reprovação.",
        "A presença e você agora são namorados oficiais."
    ),
    AbsenceStatus.REPROVADO to listOf(
        "Reprovado por falta. Um clássico atemporal.",
        "Você bateu o recorde. Pena que é o errado.",
        "Próximo semestre a gente se vê de novo, campeão."
    ),
)

private val LINES_SERIO = mapOf(
    AbsenceStatus.GO to listOf(
        "Você tem uma boa margem de faltas nesta matéria.",
        "Situação tranquila: ainda sobram várias faltas livres.",
    ),
    AbsenceStatus.WARN to listOf(
        "Dá para faltar, mas a margem está ficando pequena.",
        "Atenção: restam poucas faltas livres nesta matéria.",
    ),
    AbsenceStatus.LAST to listOf(
        "Se faltar, você usa as últimas faltas permitidas.",
        "Esta seria sua última falta sem reprovar.",
    ),
    AbsenceStatus.NO to listOf(
        "Faltar agora ultrapassa o limite de 25% e reprova por falta.",
        "Não há faltas livres suficientes. Procure ir à aula.",
    ),
    AbsenceStatus.REPROVADO to listOf(
        "O limite de faltas desta matéria já foi ultrapassado.",
        "Converse com a coordenação sobre sua situação nesta matéria.",
    ),
)

/** Modo sério: textos neutros no lugar das mensagens irônicas (preferência do usuário). */
val LocalModoSerio = staticCompositionLocalOf { false }

fun verdictMetaFor(status: AbsenceStatus, salt: Int = 0, serio: Boolean = false): VerdictMeta {
    val lines = (if (serio) LINES_SERIO else LINES_ACIDO)[status].orEmpty()
    val sub = if (lines.isEmpty()) "" else lines[Math.floorMod(salt, lines.size)]
    return when (status) {
        AbsenceStatus.GO -> VerdictMeta("PODE FALTAR", sub, Icons.Rounded.SentimentVerySatisfied)
        AbsenceStatus.WARN -> VerdictMeta(if (serio) "COM CAUTELA" else "CALMA AÍ", sub, Icons.Rounded.SentimentNeutral)
        AbsenceStatus.LAST -> VerdictMeta(if (serio) "ÚLTIMA FALTA" else "ÚLTIMA BALA", sub, Icons.Rounded.SentimentDissatisfied)
        AbsenceStatus.NO -> VerdictMeta(if (serio) "NÃO FALTE" else "NEM PENSE", sub, Icons.Rounded.SentimentVeryDissatisfied)
        AbsenceStatus.REPROVADO -> VerdictMeta(if (serio) "LIMITE EXCEDIDO" else "JÁ ERA", sub, Icons.Rounded.SkullIcon)
    }
}

// Skull icon fallback (not in standard set)
val Icons.Rounded.SkullIcon: ImageVector get() = Icons.Rounded.SentimentVeryDissatisfied

fun rankDe(totalFaltas: Int, serio: Boolean = false): String = if (serio) when {
    totalFaltas > 45 -> "Muitas faltas"
    totalFaltas > 25 -> "Faltas acima da média"
    totalFaltas > 12 -> "Algumas faltas"
    else -> "Poucas faltas"
} else when {
    totalFaltas > 45 -> "Lenda da Vagabundagem"
    totalFaltas > 25 -> "Vagabundo Sênior"
    totalFaltas > 12 -> "Faltante Casual"
    else -> "CDF em Negação"
}

// ── Funções de Cálculo Ponderado IFRN (Semestral/Anual) ──

fun calculateMD(
    n1: Double,
    n2: Double,
    n3: Double = 0.0,
    n4: Double = 0.0,
    numStages: Int = 2
): Double {
    return if (numStages == 4) {
        (2.0 * n1 + 2.0 * n2 + 3.0 * n3 + 3.0 * n4) / 10.0
    } else {
        (2.0 * n1 + 3.0 * n2) / 5.0
    }
}

fun calculateMFD(
    md: Double,
    n1: Double,
    n2: Double,
    n3: Double = 0.0,
    n4: Double = 0.0,
    naf: Double,
    numStages: Int = 2
): Double {
    val mfdSimples = (md + naf) / 2.0
    return if (numStages == 4) {
        val opt1 = (2.0 * maxOf(n1, naf) + 2.0 * n2 + 3.0 * n3 + 3.0 * n4) / 10.0
        val opt2 = (2.0 * n1 + 2.0 * maxOf(n2, naf) + 3.0 * n3 + 3.0 * n4) / 10.0
        val opt3 = (2.0 * n1 + 2.0 * n2 + 3.0 * maxOf(n3, naf) + 3.0 * n4) / 10.0
        val opt4 = (2.0 * n1 + 2.0 * n2 + 3.0 * n3 + 3.0 * maxOf(n4, naf)) / 10.0
        maxOf(mfdSimples, opt1, opt2, opt3, opt4)
    } else {
        val opt1 = (2.0 * maxOf(n1, naf) + 3.0 * n2) / 5.0
        val opt2 = (2.0 * n1 + 3.0 * maxOf(n2, naf)) / 5.0
        maxOf(mfdSimples, opt1, opt2)
    }
}

fun calculateNafNecessaria(
    n1: Double,
    n2: Double,
    n3: Double = 0.0,
    n4: Double = 0.0,
    numStages: Int = 2
): Int {
    val md = calculateMD(n1, n2, n3, n4, numStages)
    if (md < 20.0 || md >= 60.0) return -1
    for (naf in 0..100) {
        val mfd = calculateMFD(md, n1, n2, n3, n4, naf.toDouble(), numStages)
        if (mfd >= 60.0) {
            return naf
        }
    }
    return -1
}
