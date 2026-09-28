package io.github.kellyson71.supaco

import io.github.kellyson71.supaco.data.model.BoletimItem
import io.github.kellyson71.supaco.data.model.PaginatedResponse
import io.github.kellyson71.supaco.data.model.ProfileResponse
import io.github.kellyson71.supaco.data.model.RefreshResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Formatos baseados no openapi.json do SUAP (PagedBoletimSchema, TokenRefreshOutputSchema, EuSchema). */
class SuapJsonTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `boletim tolera medias como string, virgula, traco e null`() {
        val body = """
            {"count": 3, "next": "https://suap.ifrn.edu.br/api/ensino/meu-boletim/2026/2/?page=2", "previous": null,
             "results": [
              {"codigo_diario": "1", "disciplina": "TEC.0001 - A", "segundo_semestre": false,
               "carga_horaria": 80, "carga_horaria_cumprida": 40, "numero_faltas": 6,
               "percentual_carga_horaria_frequentada": 92.5, "situacao": "Cursando",
               "quantidade_avaliacoes": 2, "nota_etapa_1": {"nota": 75, "faltas": 2},
               "nota_etapa_2": {"nota": null, "faltas": 0}, "nota_etapa_3": null, "nota_etapa_4": null,
               "media_disciplina": 70, "nota_avaliacao_final": {"nota": null, "faltas": 0},
               "media_final_disciplina": "7,5"},
              {"codigo_diario": "2", "disciplina": "TEC.0002 - B", "carga_horaria": 60,
               "numero_faltas": 0, "percentual_carga_horaria_frequentada": 100, "situacao": "Cursando",
               "media_disciplina": null, "media_final_disciplina": "-"},
              {"codigo_diario": "3", "disciplina": "TEC.0003 - C", "media_final_disciplina": null,
               "nota_etapa_1": {"nota": "80", "faltas": 1}}
             ]}
        """.trimIndent()

        val page = json.decodeFromString<PaginatedResponse<BoletimItem>>(body)
        assertEquals(3, page.results.size)
        assertTrue(page.next != null)

        val (a, b, c) = page.results
        assertEquals(7.5, a.mediaFinal!!, 0.0)
        assertEquals(70.0, a.mediaDisciplina!!, 0.0)
        assertEquals(40, a.cargaHorariaCumprida)
        assertEquals(75.0, a.notaEtapa1?.nota!!, 0.0)
        assertNull(a.notaEtapa2?.nota)
        assertEquals("7.5", a.mediaDisplay)

        assertNull(b.mediaFinal)
        assertEquals("--", b.mediaDisplay)

        assertEquals(0, c.cargaHoraria)
        assertEquals(80.0, c.notaEtapa1?.nota!!, 0.0)
    }

    @Test
    fun `refresh aceita access nulo`() {
        val r = json.decodeFromString<RefreshResponse>("""{"refresh": "r2", "access": null}""")
        assertNull(r.access)
        assertEquals("r2", r.refresh)
    }

    @Test
    fun `perfil identifica contas que nao sao de aluno`() {
        val aluno = json.decodeFromString<ProfileResponse>("""{"identificacao": "2021", "nome_usual": "Ana", "tipo_usuario": "Aluno"}""")
        val servidor = json.decodeFromString<ProfileResponse>("""{"identificacao": "123", "nome_usual": "Beto", "tipo_usuario": "Servidor (Docente)"}""")
        assertTrue(aluno.isAluno)
        assertFalse(servidor.isAluno)
    }
}
