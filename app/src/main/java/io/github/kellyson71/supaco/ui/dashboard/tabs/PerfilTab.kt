package io.github.kellyson71.supaco.ui.dashboard.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kellyson71.supaco.theme.verdictColors
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay
import io.github.kellyson71.supaco.ui.dashboard.rankDe
import io.github.kellyson71.supaco.ui.dashboard.verdictMetaFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilTab(
    matricula: String,
    nomeUsual: String,
    materias: List<MateriaDisplay>,
    streakDays: Int,
    onSearchServidores: () -> Unit,
    onDownloadDeclaration: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onViewAchievements: () -> Unit = {},
) {

    val vc = MaterialTheme.verdictColors
    val totalFaltas = materias.sumOf { it.faltas }
    val folga = materias.sumOf { maxOf(0, it.restantes) }
    val naCorda = materias.count { it.status in listOf(AbsenceStatus.NO, AbsenceStatus.LAST, AbsenceStatus.REPROVADO) }
    val piorMateria = materias.minByOrNull { it.restantes }
    val rank = rankDe(totalFaltas)

    // Lógicas de conquistas para o resumo
    val cdfSupremo = materias.isNotEmpty() && materias.all { it.frequencia >= 100.0 }
    val vivendoNoLimite = materias.any { it.restantes == 0 }
    val ricoDeFaltas = folga > 10
    val mestreDoSuap = materias.isNotEmpty()
    val sobrevivente = materias.any { it.restantes == 1 }

    // Novas conquistas (segredos)
    val totalFaltasVal = totalFaltas
    val naCordaVal = naCorda
    val faltistaProfissional = totalFaltasVal > 30
    val frequenciaImperial = materias.any { it.frequencia >= 100.0 }
    val noLimiteDaMorte = naCordaVal >= 3
    val equilibrado = materias.isNotEmpty() && materias.all { it.frequencia >= 80.0 }
    val madrugador = materias.flatMap { it.horarios }.any { it.horaInicio == "07:00" }
    
    val materiasComNota = materias.filter { it.notaEtapa1 != null || it.notaEtapa2 != null || it.media?.toDoubleOrNull() != null }
    val inabalavel = materiasComNota.isNotEmpty() && materiasComNota.all { m ->
        val md = m.media?.toDoubleOrNull() ?: 0.0
        val n1 = m.notaEtapa1 ?: 0.0
        val n2 = m.notaEtapa2 ?: 0.0
        md >= 90.0 || (n1 >= 90.0 && n2 >= 90.0)
    }

    val totalAchievementsCount = 11
    val unlockedCount = listOf(
        cdfSupremo, vivendoNoLimite, ricoDeFaltas, mestreDoSuap, sobrevivente,
        faltistaProfissional, frequenciaImperial, noLimiteDaMorte, equilibrado, madrugador, inabalavel
    ).count { it }
    val achievementsProgress = unlockedCount.toFloat() / totalAchievementsCount.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Eu, o réu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Configurações")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp).plus(PaddingValues(bottom = 120.dp)),
        ) {
            // Identity card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(1.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ShapeContainer(
                            shape = OrgShape.FLOWER,
                            size = 68.dp,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Icon(Icons.Rounded.Person, null, modifier = Modifier.size(36.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                nomeUsual.split(" ").take(2).joinToString(" "),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.extraLarge,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.MilitaryTech, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                    Text(rank, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Matrícula $matricula · IFRN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Streak Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ShapeContainer(
                            shape = OrgShape.COOKIE,
                            size = 48.dp,
                            containerColor = Color(0xFFFFAB40), // Orange tint
                            contentColor = Color.White,
                        ) {
                            Icon(Icons.Rounded.Whatshot, null, modifier = Modifier.size(26.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (streakDays > 0) "$streakDays dias de streak!" else "Comece o seu streak!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (streakDays > 0) "Sem novas faltas no SUAP." else "Fique um dia sem faltas para iniciar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // Stats grid 2x2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        icon = Icons.Rounded.Weekend,
                        value = "$totalFaltas",
                        label = "faltas no total",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        icon = Icons.Rounded.Savings,
                        value = "$folga",
                        label = "faltas no cofre",
                        color = vc.goSolid,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        icon = Icons.Rounded.Warning,
                        value = "$naCorda",
                        label = "matérias no fio",
                        color = vc.lastSolid,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        icon = Icons.Rounded.LocalFireDepartment,
                        value = "${materias.size}",
                        label = "matérias sofrendo",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            // Pior matéria
            piorMateria?.let { pior ->
                item {
                    val (container, onContainer, solid) = when (pior.status) {
                        AbsenceStatus.GO -> Triple(vc.goContainer, vc.onGoContainer, vc.goSolid)
                        AbsenceStatus.WARN -> Triple(vc.warnContainer, vc.onWarnContainer, vc.warnSolid)
                        AbsenceStatus.LAST -> Triple(vc.lastContainer, vc.onLastContainer, vc.lastSolid)
                        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Triple(vc.noContainer, vc.onNoContainer, vc.noSolid)
                    }

                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "MATÉRIA MAIS NO PERIGO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.6.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ShapeContainer(
                                    shape = OrgShape.CLOVER,
                                    size = 44.dp,
                                    containerColor = container,
                                    contentColor = solid,
                                ) {
                                    Icon(pior.icone, null, modifier = Modifier.size(22.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pior.nome, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (pior.restantes <= 0) "sem faltas sobrando — corre!" else "só ${pior.restantes} falta${if (pior.restantes == 1) "" else "s"} restando",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }

            // Achievements Card
            item {
                Text(
                    "CONQUISTAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ShapeContainer(
                                shape = OrgShape.PEBBLE,
                                size = 48.dp,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                Icon(Icons.Rounded.EmojiEvents, null, modifier = Modifier.size(26.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Galeria de Conquistas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "$unlockedCount de $totalAchievementsCount desbloqueadas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onViewAchievements) {
                                Text("Ver Tudo")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { achievementsProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Buscar Servidores Card
            item {
                Card(
                    onClick = onSearchServidores,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ShapeContainer(
                            shape = OrgShape.PEBBLE,
                            size = 46.dp,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(Icons.Rounded.Search, null, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Buscar Servidores", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("encontrar salas, telefones e cargos dos professores", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Declaração de Matrícula Card
            item {
                Card(
                    onClick = onDownloadDeclaration,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ShapeContainer(
                            shape = OrgShape.PEBBLE,
                            size = 46.dp,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Icon(Icons.Rounded.Description, null, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Declaração de Matrícula", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("gerar declaração de vínculo oficial no SUAP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Logout
            item {
                Card(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ShapeContainer(
                            shape = OrgShape.PEBBLE,
                            size = 46.dp,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sair da conta", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("desconectar do SUAP neste aparelho", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(22.dp), tint = color)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun PaddingValues.plus(other: PaddingValues): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: androidx.compose.ui.unit.LayoutDirection) =
        this@plus.calculateLeftPadding(layoutDirection) + other.calculateLeftPadding(layoutDirection)
    override fun calculateTopPadding() = this@plus.calculateTopPadding() + other.calculateTopPadding()
    override fun calculateRightPadding(layoutDirection: androidx.compose.ui.unit.LayoutDirection) =
        this@plus.calculateRightPadding(layoutDirection) + other.calculateRightPadding(layoutDirection)
    override fun calculateBottomPadding() = this@plus.calculateBottomPadding() + other.calculateBottomPadding()
}
