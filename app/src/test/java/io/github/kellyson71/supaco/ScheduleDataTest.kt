package io.github.kellyson71.supaco

import io.github.kellyson71.supaco.data.ScheduleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleDataTest {

    @Test
    fun `parse de horario com varios dias e turnos`() {
        val entries = ScheduleData.parseSuapHorario("2M34 / 4V1234 / 6N12", sala = "Sala 16")
        assertEquals(3, entries.size)

        val seg = entries[0]
        assertEquals("Segunda", seg.dia)
        assertEquals("08:50", seg.horaInicio)
        assertEquals("10:20", seg.horaFim)
        assertEquals(2, seg.aulas)

        val qua = entries[1]
        assertEquals("Quarta", qua.dia)
        assertEquals("13:00", qua.horaInicio)
        assertEquals("16:20", qua.horaFim)
        assertEquals(4, qua.aulas)

        val sex = entries[2]
        assertEquals("Sexta", sex.dia)
        assertEquals("19:00", sex.horaInicio)
        assertEquals(2, sex.aulas)
        assertEquals("Sala 16", sex.sala)
    }

    @Test
    fun `blocos repetidos ou fora da grade sao ignorados`() {
        val entry = ScheduleData.parseSuapHorario("3V1129").single()
        assertEquals(2, entry.aulas) // 1 e 2; o 9 não existe no turno
        assertTrue(ScheduleData.parseSuapHorario("").isEmpty())
        assertTrue(ScheduleData.parseSuapHorario(null).isEmpty())
        assertTrue(ScheduleData.parseSuapHorario("9X12").isEmpty())
    }

    @Test
    fun `limpa nome da sala e da disciplina`() {
        assertEquals(
            "Bloco 10 · Sala 16",
            ScheduleData.limpaSala("Chave 117 - Sala de Aula 16 (Piso 02) - Bloco 10 - Salas de Aula (PF)"),
        )
        assertEquals("—", ScheduleData.limpaSala(null))
        assertEquals("Banco de Dados", ScheduleData.limpaNome("TEC.0012 - Banco de Dados (NCT)"))
        assertEquals("TEC.0012", ScheduleData.extraiSigla("TEC.0012 - Banco de Dados (NCT)"))
    }
}
