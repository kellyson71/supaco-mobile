package io.github.kellyson71.supaco.ui.conquistas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val unlocked: Boolean,
    val isSecret: Boolean,
    val hint: String,
    val color: Color,
)

/** Regras das conquistas — única fonte para Perfil, galeria e anúncio de desbloqueio. */
fun computeAchievements(materias: List<MateriaDisplay>): List<Achievement> {
    val totalFaltas = materias.sumOf { it.faltas }
    val folga = materias.sumOf { maxOf(0, it.restantes) }
    val naCorda = materias.count { it.status in listOf(AbsenceStatus.NO, AbsenceStatus.LAST, AbsenceStatus.REPROVADO) }
    val materiasComNota = materias.filter { it.notaEtapa1 != null || it.notaEtapa2 != null || it.media?.toDoubleOrNull() != null }

    fun a(id: String, title: String, desc: String, icon: ImageVector, unlocked: Boolean, secret: Boolean, hint: String, color: Long) =
        Achievement(id, title, desc, icon, unlocked, secret, hint, Color(color))

    return listOf(
        a("cdf", "CDF Supremo", "100% de frequência em tudo", Icons.Rounded.Star,
            materias.isNotEmpty() && materias.all { it.frequencia >= 100.0 }, false, "", 0xFFFFD700),
        a("limite", "Vivendo no Limite", "Matéria com 0 faltas livres", Icons.Rounded.Bolt,
            materias.any { it.restantes == 0 }, false, "", 0xFFFF5252),
        a("rico", "Rico de Faltas", "Mais de 10 faltas no cofre", Icons.Rounded.Savings,
            folga > 10, false, "", 0xFF66BB6A),
        a("suap", "Mestre do SUAP", "Sincronização efetuada", Icons.Rounded.Cloud,
            materias.isNotEmpty(), false, "", 0xFF42A5F5),
        a("sobrevivente", "Sobrevivente", "Apenas 1 falta restante", Icons.Rounded.Favorite,
            materias.any { it.restantes == 1 }, false, "", 0xFFAB47BC),
        a("faltista", "Faltista Profissional", "Faltou mais que 30 vezes no total", Icons.Rounded.LocalFireDepartment,
            totalFaltas > 30, true, "Dica: Faltas demais acumuladas...", 0xFFFF7043),
        a("imperial", "Frequência Imperial", "100% de frequência em alguma matéria", Icons.Rounded.WorkspacePremium,
            materias.any { it.frequencia >= 100.0 }, true, "Dica: Presença exemplar em alguma aula...", 0xFFFFB300),
        a("morte", "No Limite da Morte", "3 ou mais matérias no fio da navalha", Icons.Rounded.Dangerous,
            naCorda >= 3, true, "Dica: Muitos pratos se equilibrando...", 0xFFE53935),
        a("equilibrado", "Equilibrado", "Todas as matérias com mais de 80% de presença", Icons.Rounded.LinearScale,
            materias.isNotEmpty() && materias.all { it.frequencia >= 80.0 }, true, "Dica: Frequência saudável em tudo...", 0xFF26A69A),
        a("madrugador", "Madrugador", "Aula cadastrada às 07:00 da manhã", Icons.Rounded.Alarm,
            materias.flatMap { it.horarios }.any { it.horaInicio == "07:00" }, true, "Dica: Assistir aula antes do sol nascer...", 0xFF26C6DA),
        a("inabalavel", "Inabalável", "Média maior que 90 em todas as disciplinas", Icons.Rounded.EmojiEvents,
            materiasComNota.isNotEmpty() && materiasComNota.all { m ->
                val md = m.media?.toDoubleOrNull() ?: 0.0
                md >= 90.0 || ((m.notaEtapa1 ?: 0.0) >= 90.0 && (m.notaEtapa2 ?: 0.0) >= 90.0)
            }, true, "Dica: Notas excepcionais em tudo...", 0xFFEC407A),
    )
}
