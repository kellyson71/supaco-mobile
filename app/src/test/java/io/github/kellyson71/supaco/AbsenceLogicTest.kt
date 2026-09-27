package io.github.kellyson71.supaco

import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.calcStatus
import io.github.kellyson71.supaco.ui.dashboard.rankDe
import io.github.kellyson71.supaco.ui.dashboard.calculateMD
import io.github.kellyson71.supaco.ui.dashboard.calculateMFD
import io.github.kellyson71.supaco.ui.dashboard.calculateNafNecessaria
import org.junit.Assert.assertEquals
import org.junit.Test

class AbsenceLogicTest {

    @Test
    fun testCalcStatus() {
        // Carga horária = 80 horas. Limite de faltas = floor(80 * 0.25) = 20 faltas.
        
        // 0 faltas -> Sobram 20 -> GO
        assertEquals(AbsenceStatus.GO, calcStatus(0, 80))
        
        // 16 faltas -> Sobram 4 -> GO
        assertEquals(AbsenceStatus.GO, calcStatus(16, 80))
        
        // 17 faltas -> Sobram 3 -> WARN
        assertEquals(AbsenceStatus.WARN, calcStatus(17, 80))
        
        // 18 faltas -> Sobram 2 -> WARN
        assertEquals(AbsenceStatus.WARN, calcStatus(18, 80))
        
        // 19 faltas -> Sobram 1 -> LAST
        assertEquals(AbsenceStatus.LAST, calcStatus(19, 80))
        
        // 20 faltas -> Sobram 0 -> NO
        assertEquals(AbsenceStatus.NO, calcStatus(20, 80))
        
        // 21 faltas -> Sobram -1 -> REPROVADO
        assertEquals(AbsenceStatus.REPROVADO, calcStatus(21, 80))
    }

    @Test
    fun testRankDe() {
        assertEquals("Lenda da Vagabundagem", rankDe(46))
        assertEquals("Vagabundo Sênior", rankDe(26))
        assertEquals("Faltante Casual", rankDe(13))
        assertEquals("CDF em Negação", rankDe(5))
    }

    @Test
    fun testCalculateMD() {
        // Semestral (2 etapas)
        assertEquals(60.0, calculateMD(60.0, 60.0, numStages = 2), 0.001)
        assertEquals(40.0, calculateMD(100.0, 0.0, numStages = 2), 0.001)
        assertEquals(60.0, calculateMD(0.0, 100.0, numStages = 2), 0.001)

        // Anual (4 etapas)
        assertEquals(60.0, calculateMD(60.0, 60.0, 60.0, 60.0, numStages = 4), 0.001)
        assertEquals(40.0, calculateMD(100.0, 100.0, 0.0, 0.0, numStages = 4), 0.001)
        assertEquals(60.0, calculateMD(0.0, 0.0, 100.0, 100.0, numStages = 4), 0.001)
    }

    @Test
    fun testCalculateMFD() {
        // Semestral (2 etapas): MD = 36.0 (N1 = 30.0, N2 = 40.0)
        // NAF = 80.0
        // mfdSimples = (36 + 80)/2 = 58.0
        // opt1 (substitui N1) = (2 * 80 + 3 * 40)/5 = 56.0
        // opt2 (substitui N2) = (2 * 30 + 3 * 80)/5 = 60.0
        // MFD final = maxOf(58.0, 56.0, 60.0) = 60.0
        assertEquals(60.0, calculateMFD(36.0, 30.0, 40.0, naf = 80.0, numStages = 2), 0.001)

        // NAF = 90.0
        // mfdSimples = (36 + 90)/2 = 63.0
        // opt1 = (2 * 90 + 3 * 40)/5 = 60.0
        // opt2 = (2 * 30 + 3 * 90)/5 = 66.0
        // MFD final = max(63, 60, 66) = 66.0
        assertEquals(66.0, calculateMFD(36.0, 30.0, 40.0, naf = 90.0, numStages = 2), 0.001)

        // Anual (4 etapas): MD = 42.0 (N1 = 30, N2 = 30, N3 = 50, N4 = 50)
        // MD = (2*30 + 2*30 + 3*50 + 3*50)/10 = 42.0
        // NAF = 80.0
        // mfdSimples = (42 + 80)/2 = 61.0
        // opt1 (substitui N1) = (2*80 + 2*30 + 3*50 + 3*50)/10 = 52.0
        // opt2 (substitui N2) = 52.0
        // opt3 (substitui N3) = (2*30 + 2*30 + 3*80 + 3*50)/10 = 51.0
        // MFD final = max(61.0, 52.0, 52.0, 51.0, 51.0) = 61.0
        assertEquals(61.0, calculateMFD(42.0, 30.0, 30.0, 50.0, 50.0, naf = 80.0, numStages = 4), 0.001)
    }

    @Test
    fun testCalculateNafNecessaria() {
        // N1 = 30.0, N2 = 40.0, MD = 36.0
        // NAF = 80.0 -> MFD opt2 = (2*30 + 3*80)/5 = 60.0
        assertEquals(80, calculateNafNecessaria(30.0, 40.0, numStages = 2))

        // N1 = 60.0, N2 = 60.0 -> MD = 60.0 -> Já aprovado, retorna -1
        assertEquals(-1, calculateNafNecessaria(60.0, 60.0, numStages = 2))

        // N1 = 10.0, N2 = 10.0 -> MD = 10.0 -> Abaixo de 20.0, reprovado sem direito a final, retorna -1
        assertEquals(-1, calculateNafNecessaria(10.0, 10.0, numStages = 2))
    }
}

