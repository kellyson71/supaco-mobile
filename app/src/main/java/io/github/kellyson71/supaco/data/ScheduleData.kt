package io.github.kellyson71.supaco.data

import java.util.Calendar

data class ScheduleEntry(
    val dia: String,        // "Terça"
    val horaInicio: String, // "13:00"
    val horaFim: String,    // "16:20"
    val sala: String,
    val professor: String?,
    /** Quantidade de aulas de 45 min no bloco — faltar o bloco inteiro custa esse tanto de faltas. */
    val aulas: Int = 1,
)

object ScheduleData {

    // ── Parser do formato de horário do SUAP ("3V1234 / 5M12") ──
    // Dia: 2=Segunda … 7=Sábado. Turno: M/V/N. Dígitos: blocos de 45min.

    private val DIA_SUAP = mapOf(
        '2' to "Segunda", '3' to "Terça", '4' to "Quarta",
        '5' to "Quinta", '6' to "Sexta", '7' to "Sábado",
    )

    // Início e fim de cada bloco por turno (grade padrão IFRN)
    private val BLOCOS = mapOf(
        'M' to listOf("07:00" to "07:45", "07:45" to "08:30", "08:50" to "09:35", "09:35" to "10:20", "10:30" to "11:15", "11:15" to "12:00"),
        'V' to listOf("13:00" to "13:45", "13:45" to "14:30", "14:50" to "15:35", "15:35" to "16:20", "16:30" to "17:15", "17:15" to "18:00"),
        'N' to listOf("19:00" to "19:45", "19:45" to "20:30", "20:45" to "21:30", "21:30" to "22:15"),
    )

    /**
     * "3V1234" → [ScheduleEntry(Terça, 13:00, 16:20, …)].
     * Blocos do mesmo token viram um único intervalo (primeiro início → último fim).
     */
    fun parseSuapHorario(codigo: String?, sala: String = "—", professor: String? = null): List<ScheduleEntry> {
        if (codigo.isNullOrBlank()) return emptyList()
        return Regex("([2-7])([MVN])(\\d+)").findAll(codigo).mapNotNull { match ->
            val dia = DIA_SUAP[match.groupValues[1][0]] ?: return@mapNotNull null
            val blocos = BLOCOS[match.groupValues[2][0]] ?: return@mapNotNull null
            val nums = match.groupValues[3].mapNotNull { it.digitToIntOrNull() }
                .filter { it in 1..blocos.size }
                .distinct()
                .sorted()
            if (nums.isEmpty()) return@mapNotNull null
            ScheduleEntry(
                dia = dia,
                horaInicio = blocos[nums.first() - 1].first,
                horaFim = blocos[nums.last() - 1].second,
                sala = sala,
                professor = professor,
                aulas = nums.size,
            )
        }.toList()
    }

    /**
     * "Chave 117 - Sala de Aula 16 (Piso 02) - Bloco 10 - Salas de Aula (PF)"
     * → "Bloco 10 · Sala 16"
     */
    fun limpaSala(local: String?): String {
        if (local.isNullOrBlank()) return "—"
        val sala = Regex("Sala(?: de Aula)?\\s*(\\d+\\w*)").find(local)?.groupValues?.get(1)
        val bloco = Regex("Bloco\\s*(\\d+\\w*)").find(local)?.groupValues?.get(1)
        return when {
            bloco != null && sala != null -> "Bloco $bloco · Sala $sala"
            sala != null -> "Sala $sala"
            else -> local.take(40)
        }
    }

    val DIAS_SEMANA = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta")

    val DIA_ABBR = mapOf(
        "Segunda" to "Seg", "Terça" to "Ter", "Quarta" to "Qua",
        "Quinta" to "Qui", "Sexta" to "Sex", "Sábado" to "Sáb",
    )

    fun extraiSigla(disciplina: String): String =
        Regex("^([A-Z]+\\.\\d+)").find(disciplina)?.value ?: ""

    fun limpaNome(disciplina: String): String =
        disciplina.replace(Regex("^[A-Z]+\\.\\d+\\s*-\\s*"), "")
            .replace(Regex("\\s*\\((Curso\\s*\\d+|NCT|.*?)\\)\\s*$"), "")
            .trim()

    fun currentDayName(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Segunda"
        Calendar.TUESDAY -> "Terça"
        Calendar.WEDNESDAY -> "Quarta"
        Calendar.THURSDAY -> "Quinta"
        Calendar.FRIDAY -> "Sexta"
        Calendar.SATURDAY -> "Sábado"
        else -> "Domingo"
    }

    /** Índice do dia atual em DIAS_SEMANA, ou 0 (Segunda) no fim de semana. */
    fun todayIndex(): Int = DIAS_SEMANA.indexOf(currentDayName()).coerceAtLeast(0)

    /** Minutos desde 00:00 para "HH:mm"; -1 se inválido. */
    fun parseMinutes(hora: String): Int {
        val parts = hora.split(":")
        return if (parts.size == 2) {
            (parts[0].toIntOrNull() ?: return -1) * 60 + (parts[1].toIntOrNull() ?: return -1)
        } else -1
    }

    fun nowMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
