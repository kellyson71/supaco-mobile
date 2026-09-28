package io.github.kellyson71.supaco

import io.github.kellyson71.supaco.data.ScheduleEntry
import io.github.kellyson71.supaco.data.model.BoletimItem
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.buildMaterias
import io.github.kellyson71.supaco.ui.dashboard.materiaMaisCritica
import io.github.kellyson71.supaco.ui.dashboard.statusParaFaltar
import io.github.kellyson71.supaco.ui.dashboard.vereditoDoDia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VereditoDoDiaTest {

    private fun item(sigla: String, faltas: Int, carga: Int = 80) = BoletimItem(
        codigoDiario = sigla,
        disciplina = "$sigla - Matéria $sigla",
        cargaHoraria = carga,
        numeroFaltas = faltas,
    )

    private fun aula(dia: String, aulas: Int) = ScheduleEntry(dia, "13:00", "16:20", "Sala 1", null, aulas)

    @Test
    fun `statusParaFaltar com uma aula mantém a regra antiga`() {
        assertEquals(AbsenceStatus.GO, statusParaFaltar(restantes = 4, aulas = 1))
        assertEquals(AbsenceStatus.WARN, statusParaFaltar(restantes = 3, aulas = 1))
        assertEquals(AbsenceStatus.WARN, statusParaFaltar(restantes = 2, aulas = 1))
        assertEquals(AbsenceStatus.LAST, statusParaFaltar(restantes = 1, aulas = 1))
        assertEquals(AbsenceStatus.NO, statusParaFaltar(restantes = 0, aulas = 1))
        assertEquals(AbsenceStatus.REPROVADO, statusParaFaltar(restantes = -1, aulas = 1))
    }

    @Test
    fun `faltar um bloco de 4 aulas com 2 faltas livres reprova`() {
        assertEquals(AbsenceStatus.NO, statusParaFaltar(restantes = 2, aulas = 4))
        assertEquals(AbsenceStatus.LAST, statusParaFaltar(restantes = 4, aulas = 4))
        assertEquals(AbsenceStatus.WARN, statusParaFaltar(restantes = 6, aulas = 4))
        assertEquals(AbsenceStatus.GO, statusParaFaltar(restantes = 7, aulas = 4))
    }

    @Test
    fun `veredito considera todas as aulas do dia e escolhe a pior materia`() {
        // Limite de 80h = 20 faltas
        val materias = buildMaterias(
            items = listOf(item("TEC.0001", faltas = 5), item("TEC.0002", faltas = 18)),
            horariosBySigla = mapOf(
                "TEC.0001" to listOf(aula("Terça", 2)),
                "TEC.0002" to listOf(aula("Terça", 2), aula("Terça", 2)),
            ),
            hoje = "Terça",
        )
        val veredito = vereditoDoDia(materias)!!
        assertEquals("TEC.0002", veredito.sigla)
        assertEquals(4, veredito.aulasHoje)
        // 2 livres - 4 aulas: faltar hoje estoura o limite
        assertEquals(AbsenceStatus.NO, veredito.statusHoje)
        // O status geral continua sendo o de "uma falta"
        assertEquals(AbsenceStatus.WARN, veredito.status)
    }

    @Test
    fun `sem aula hoje nao ha veredito e a mais critica vem pelo saldo`() {
        val materias = buildMaterias(
            items = listOf(item("TEC.0001", faltas = 2), item("TEC.0002", faltas = 15)),
            horariosBySigla = mapOf("TEC.0001" to listOf(aula("Segunda", 2))),
            hoje = "Sábado",
        )
        assertNull(vereditoDoDia(materias))
        assertEquals("TEC.0002", materiaMaisCritica(materias)?.sigla)
        materias.forEach { assertEquals(it.status, it.statusVeredito) }
    }

    @Test
    fun `materia sem grade nao inventa horario`() {
        val materia = buildMaterias(listOf(item("TEC.0017", faltas = 0)), emptyMap(), hoje = "Terça").single()
        assertEquals(emptyList<ScheduleEntry>(), materia.horarios)
        assertEquals("—", materia.sala)
        assertEquals(0, materia.aulasHoje)
    }
}
