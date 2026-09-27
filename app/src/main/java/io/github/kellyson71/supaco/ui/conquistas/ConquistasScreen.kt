package io.github.kellyson71.supaco.ui.conquistas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay

private data class Achievement(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val unlocked: Boolean,
    val isSecret: Boolean,
    val hint: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConquistasScreen(
    materias: List<MateriaDisplay>,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val totalFaltas = materias.sumOf { it.faltas }
    val folga = materias.sumOf { maxOf(0, it.restantes) }
    val naCorda = materias.count { it.status in listOf(AbsenceStatus.NO, AbsenceStatus.LAST, AbsenceStatus.REPROVADO) }

    // Lógicas de conquistas
    val cdfSupremo = materias.isNotEmpty() && materias.all { it.frequencia >= 100.0 }
    val vivendoNoLimite = materias.any { it.restantes == 0 }
    val ricoDeFaltas = folga > 10
    val mestreDoSuap = materias.isNotEmpty()
    val sobrevivente = materias.any { it.restantes == 1 }
    
    // Novas conquistas
    val faltistaProfissional = totalFaltas > 30
    val frequenciaImperial = materias.any { it.frequencia >= 100.0 }
    val noLimiteDaMorte = naCorda >= 3
    val equilibrado = materias.isNotEmpty() && materias.all { it.frequencia >= 80.0 }
    val madrugador = materias.flatMap { it.horarios }.any { it.horaInicio == "07:00" }
    
    val materiasComNota = materias.filter { it.notaEtapa1 != null || it.notaEtapa2 != null || it.media?.toDoubleOrNull() != null }
    val inabalavel = materiasComNota.isNotEmpty() && materiasComNota.all { m ->
        val md = m.media?.toDoubleOrNull() ?: 0.0
        val n1 = m.notaEtapa1 ?: 0.0
        val n2 = m.notaEtapa2 ?: 0.0
        md >= 90.0 || (n1 >= 90.0 && n2 >= 90.0)
    }

    val achievements = listOf(
        Achievement("CDF Supremo", "100% de frequência em tudo", Icons.Rounded.Star, cdfSupremo, false, "", Color(0xFFFFD700)),
        Achievement("Vivendo no Limite", "Matéria com 0 faltas livres", Icons.Rounded.Bolt, vivendoNoLimite, false, "", Color(0xFFFF5252)),
        Achievement("Rico de Faltas", "Mais de 10 faltas no cofre", Icons.Rounded.Savings, ricoDeFaltas, false, "", Color(0xFF66BB6A)),
        Achievement("Mestre do SUAP", "Sincronização efetuada", Icons.Rounded.Cloud, mestreDoSuap, false, "", Color(0xFF42A5F5)),
        Achievement("Sobrevivente", "Apenas 1 falta restante", Icons.Rounded.Favorite, sobrevivente, false, "", Color(0xFFAB47BC)),
        
        // Segredos
        Achievement("Faltista Profissional", "Faltou mais que 30 vezes no total", Icons.Rounded.LocalFireDepartment, faltistaProfissional, true, "Dica: Faltas demais acumuladas...", Color(0xFFFF7043)),
        Achievement("Frequência Imperial", "100% de frequência em alguma matéria", Icons.Rounded.WorkspacePremium, frequenciaImperial, true, "Dica: Presença exemplar em alguma aula...", Color(0xFFFFB300)),
        Achievement("No Limite da Morte", "3 ou mais matérias no fio da navalha", Icons.Rounded.Dangerous, noLimiteDaMorte, true, "Dica: Muitos pratos se equilibrando...", Color(0xFFE53935)),
        Achievement("Equilibrado", "Todas as matérias com mais de 80% de presença", Icons.Rounded.LinearScale, equilibrado, true, "Dica: Frequência saudável em tudo...", Color(0xFF26A69A)),
        Achievement("Madrugador", "Aula cadastrada às 07:00 da manhã", Icons.Rounded.Alarm, madrugador, true, "Dica: Assistir aula antes do sol nascer...", Color(0xFF26C6DA)),
        Achievement("Inabalável", "Média maior que 90 em todas as disciplinas", Icons.Rounded.EmojiEvents, inabalavel, true, "Dica: Notas excepcionais em tudo...", Color(0xFFEC407A))
    )

    val unlockedCount = achievements.count { it.unlocked }
    val progress = if (achievements.isNotEmpty()) unlockedCount.toFloat() / achievements.size else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conquistas", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card showing progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Seu Progresso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$unlockedCount de ${achievements.size} Conquistas Desbloqueadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // Grid of achievements
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(achievements) { ach ->
                    AchievementItemRow(ach)
                }
            }
        }
    }
}

@Composable
private fun AchievementItemRow(ach: Achievement) {
    val showContent = ach.unlocked || !ach.isSecret
    val containerColor = if (ach.unlocked) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val alpha = if (ach.unlocked) 1f else 0.38f

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder(enabled = ach.unlocked)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShapeContainer(
                shape = OrgShape.PEBBLE,
                size = 48.dp,
                containerColor = if (ach.unlocked) ach.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (ach.unlocked) ach.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ) {
                Icon(
                    if (ach.unlocked) ach.icon else Icons.Rounded.Lock,
                    null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (showContent) ach.title else "Conquista Oculta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (showContent) ach.description else ach.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
            if (ach.unlocked) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Desbloqueada",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
