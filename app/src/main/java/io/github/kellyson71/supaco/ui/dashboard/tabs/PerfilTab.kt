package io.github.kellyson71.supaco.ui.dashboard.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.kellyson71.supaco.data.local.CacheStore
import io.github.kellyson71.supaco.ui.components.enterOnce
import io.github.kellyson71.supaco.ui.components.pressScale
import io.github.kellyson71.supaco.ui.components.moodFor
import io.github.kellyson71.supaco.ui.conquistas.computeAchievements
import io.github.kellyson71.supaco.ui.motion.CountUpText
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.orSnap
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.*
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
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
    onOpenResumo: () -> Unit = {},
) {

    val vc = MaterialTheme.verdictColors
    val totalFaltas = materias.sumOf { it.faltas }
    val folga = materias.sumOf { maxOf(0, it.restantes) }
    val naCorda = materias.count { it.status in listOf(AbsenceStatus.NO, AbsenceStatus.LAST, AbsenceStatus.REPROVADO) }
    val piorMateria = materias.minByOrNull { it.restantes }
    val rank = rankDe(totalFaltas, LocalModoSerio.current)
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val cacheStore: CacheStore = koinInject()
    // Frequência geral do semestre (faltas sobre a carga total)
    val cargaTotal = materias.sumOf { it.total }
    val freqGeral = if (cargaTotal > 0) (1f - totalFaltas.toFloat() / cargaTotal).coerceIn(0f, 1f) else 1f

    // Rank mudou desde a última visita: entra "carimbando"
    val stamp = remember { Animatable(1f) }
    LaunchedEffect(rank, materias.isNotEmpty()) {
        if (materias.isEmpty()) return@LaunchedEffect
        val anterior = cacheStore.lastRank
        cacheStore.lastRank = rank
        if (anterior != null && anterior != rank && !reduce) {
            stamp.snapTo(2.2f)
            delay(300)
            stamp.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 700f))
            haptics.pesado()
        }
    }

    val achievements = remember(materias) { computeAchievements(materias) }
    val totalAchievementsCount = achievements.size
    val unlockedCount = achievements.count { it.unlocked }
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
                    modifier = Modifier.fillMaxWidth().enterOnce(0),
                    elevation = CardDefaults.elevatedCardElevation(1.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Avatar: a flor gira devagar dentro de um anel com a frequência geral
                        val ring = remember { Animatable(if (reduce) freqGeral else 0f) }
                        LaunchedEffect(freqGeral) { ring.animateTo(freqGeral, tween(1000, delayMillis = 200, easing = Motion.EmphasizedDecelerate)) }
                        val ringColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .semantics { contentDescription = "Frequência geral ${(freqGeral * 100).toInt()}%" }
                                .drawBehind {
                                    val stroke = 4.dp.toPx()
                                    drawArc(trackColor, 0f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                                    drawArc(ringColor, -90f, 360f * ring.value, false, style = Stroke(stroke, cap = StrokeCap.Round))
                                },
                        ) {
                            ShapeContainer(
                                shape = OrgShape.FLOWER,
                                size = 64.dp,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                spin = true,
                                breathe = true,
                            ) {
                                Icon(Icons.Rounded.Person, null, modifier = Modifier.size(34.dp))
                            }
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
                                modifier = Modifier.graphicsLayer {
                                    scaleX = stamp.value
                                    scaleY = stamp.value
                                    rotationZ = (stamp.value - 1f) * -8f
                                },
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
                    modifier = Modifier.fillMaxWidth().enterOnce(1),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Chama cresce com a sequência e tremula; sem streak, fica apagada
                        val flameSize by animateDpAsState(
                            when {
                                streakDays >= 14 -> 32.dp
                                streakDays >= 7 -> 28.dp
                                streakDays >= 1 -> 24.dp
                                else -> 20.dp
                            },
                            Motion.viva<Dp>().orSnap(),
                            label = "flame_size",
                        )
                        val flicker = if (streakDays > 0 && !reduce) {
                            rememberInfiniteTransition(label = "flame").animateFloat(
                                0.92f, 1.08f,
                                infiniteRepeatable(tween(380, easing = Motion.Standard), RepeatMode.Reverse),
                                label = "flame_flicker",
                            ).value
                        } else 1f
                        ShapeContainer(
                            shape = OrgShape.COOKIE,
                            size = 48.dp,
                            containerColor = if (streakDays > 0) Color(0xFFFFAB40) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (streakDays > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            breathe = streakDays > 0,
                        ) {
                            Icon(
                                Icons.Rounded.Whatshot, null,
                                modifier = Modifier
                                    .size(flameSize)
                                    .graphicsLayer {
                                        scaleX = 2f - flicker
                                        scaleY = flicker
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                                    },
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (streakDays > 0) {
                                CountUpText(
                                    value = streakDays,
                                    format = { "$it dias de streak!" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Text("Comece o seu streak!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
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
                    modifier = Modifier.fillMaxWidth().enterOnce(2),
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

                    OutlinedCard(modifier = Modifier.fillMaxWidth().enterOnce(3)) {
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
                                // No fio da navalha: o trevo fica tenso e treme de leve
                                val tenso = pior.status in listOf(AbsenceStatus.LAST, AbsenceStatus.NO, AbsenceStatus.REPROVADO) && !reduce
                                val tremor = if (tenso) {
                                    rememberInfiniteTransition(label = "tense").animateFloat(
                                        -3f, 3f, infiniteRepeatable(tween(90), RepeatMode.Reverse), label = "tense_jitter",
                                    ).value
                                } else 0f
                                ShapeContainer(
                                    shape = OrgShape.CLOVER,
                                    size = 44.dp,
                                    containerColor = container,
                                    contentColor = solid,
                                    mood = moodFor(pior.status),
                                    extraRotation = tremor,
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

                        val achFill = remember { Animatable(if (reduce) achievementsProgress else 0f) }
                        LaunchedEffect(achievementsProgress) {
                            achFill.animateTo(achievementsProgress, tween(900, delayMillis = 250, easing = Motion.EmphasizedDecelerate))
                        }
                        LinearProgressIndicator(
                            progress = { achFill.value },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Resumo do semestre (estilo "Wrapped")
            if (materias.isNotEmpty()) {
                item {
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Card(
                        onClick = onOpenResumo,
                        interactionSource = interaction,
                        modifier = Modifier.fillMaxWidth().pressScale(interaction),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            ShapeContainer(
                                shape = OrgShape.FLOWER,
                                size = 44.dp,
                                containerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                spin = true,
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Resumo do semestre",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    "seus números em telas pra compartilhar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
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
            val numeric = value.toIntOrNull()
            if (numeric != null) {
                CountUpText(numeric, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
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
