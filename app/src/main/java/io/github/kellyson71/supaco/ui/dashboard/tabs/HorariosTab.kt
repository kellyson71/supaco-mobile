package io.github.kellyson71.supaco.ui.dashboard.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import io.github.kellyson71.supaco.ui.components.PulseRing
import io.github.kellyson71.supaco.ui.components.enterOnce
import io.github.kellyson71.supaco.ui.components.moodFor
import io.github.kellyson71.supaco.ui.components.pressScale
import io.github.kellyson71.supaco.ui.motion.CountUpText
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.orSnap
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import androidx.compose.foundation.Canvas
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kellyson71.supaco.theme.verdictColors
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay
import io.github.kellyson71.supaco.ui.dashboard.verdictMetaFor
import kotlinx.coroutines.launch

private val DIAS_ORDEM = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta")
private val DIA_FULL = mapOf(
    "Segunda" to "Segunda-feira", "Terça" to "Terça-feira", "Quarta" to "Quarta-feira",
    "Quinta" to "Quinta-feira", "Sexta" to "Sexta-feira"
)
private val DIA_ABBR = mapOf(
    "Segunda" to "Seg", "Terça" to "Ter", "Quarta" to "Qua", "Quinta" to "Qui", "Sexta" to "Sex"
)

private data class Aula(
    val materia: MateriaDisplay,
    val entry: io.github.kellyson71.supaco.data.ScheduleEntry,
)

private data class DiaAgenda(
    val dia: String,
    val abbr: String,
    val full: String,
    val ehHoje: Boolean,
    val aulas: List<Aula>,
)

private fun buildAgenda(materias: List<MateriaDisplay>): List<DiaAgenda> {
    val hoje = io.github.kellyson71.supaco.data.ScheduleData.currentDayName()
    return DIAS_ORDEM.map { dia ->
        DiaAgenda(
            dia = dia,
            abbr = DIA_ABBR[dia] ?: dia.take(3),
            full = DIA_FULL[dia] ?: dia,
            ehHoje = dia == hoje,
            aulas = materias.flatMap { m ->
                m.horarios.filter { it.dia == dia }.map { Aula(m, it) }
            }.sortedBy { it.entry.horaInicio },
        )
    }.filter { it.aulas.isNotEmpty() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorariosTab(
    materias: List<MateriaDisplay>,
    onOpenDetail: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val agenda = remember(materias) { buildAgenda(materias) }
    val totalSemana = agenda.sumOf { it.aulas.size }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    // Relógio da timeline: atualiza a cada 30 s
    val now by produceState(io.github.kellyson71.supaco.data.ScheduleData.nowMinutes()) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            value = io.github.kellyson71.supaco.data.ScheduleData.nowMinutes()
        }
    }
    // Incrementa para fazer o cabeçalho de hoje piscar depois do "ir para hoje"
    var flashToday by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Horários",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = {
                        val hojeIdx = agenda.indexOfFirst { it.ehHoje }
                        val targetIdx = if (hojeIdx >= 0) hojeIdx else 0
                        if (agenda.isNotEmpty()) {
                            haptics.tick()
                            scope.launch {
                                listState.animateScrollToItem(targetIdx + 1)
                                flashToday++
                            }
                        }
                    }) {
                        Icon(Icons.Rounded.Today, contentDescription = "Ir para hoje")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            // Summary card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .enterOnce(),
                    elevation = CardDefaults.elevatedCardElevation(1.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ShapeContainer(
                            OrgShape.COOKIE, 44.dp,
                            MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer,
                            breathe = true,
                            spin = true,
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            CountUpText(
                                value = totalSemana,
                                format = { "$it aulas essa semana" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${agenda.size} dias de luta · grade puxada do SUAP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Day sections
            agenda.forEachIndexed { index, dia ->
                item(key = dia.dia) {
                    DiaSection(
                        dia = dia,
                        now = now,
                        flash = if (dia.ehHoje) flashToday else 0,
                        onOpenDetail = onOpenDetail,
                        modifier = Modifier.enterOnce(index + 1),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaSection(
    dia: DiaAgenda,
    now: Int,
    flash: Int,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pisca a cor primária uma vez quando o usuário pede "ir para hoje"
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(flash) {
        if (flash > 0) {
            flashAlpha.snapTo(0.35f)
            flashAlpha.animateTo(0f, tween(900, easing = Motion.Standard))
        }
    }
    val flashColor = MaterialTheme.colorScheme.primary
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Day header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .drawBehind {
                    if (flashAlpha.value > 0f) {
                        drawRoundRect(
                            flashColor.copy(alpha = flashAlpha.value),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                        )
                    }
                }
                .padding(vertical = 4.dp, horizontal = 4.dp),
        ) {
            if (dia.ehHoje) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(
                        dia.full,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    "· hoje",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    dia.full,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider(modifier = Modifier.weight(1f))

            Text(
                "${dia.aulas.size} aula${if (dia.aulas.size > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Timeline items
        dia.aulas.forEachIndexed { i, aula ->
            val startMinutes = io.github.kellyson71.supaco.data.ScheduleData.parseMinutes(aula.entry.horaInicio)
            val endMinutes = io.github.kellyson71.supaco.data.ScheduleData.parseMinutes(aula.entry.horaFim)
            val valid = startMinutes != -1 && endMinutes != -1
            val isHappeningNow = dia.ehHoje && valid && now in startMinutes until endMinutes
            val isPast = dia.ehHoje && valid && now >= endMinutes
            // Quanto do conector até a próxima aula já "passou" (só hoje)
            val nextStart = dia.aulas.getOrNull(i + 1)?.let {
                io.github.kellyson71.supaco.data.ScheduleData.parseMinutes(it.entry.horaInicio)
            }
            val connectorProgress = when {
                !dia.ehHoje || !valid -> 0f
                now <= startMinutes -> 0f
                nextStart == null || nextStart <= startMinutes -> if (now >= endMinutes) 1f else 0f
                else -> ((now - startMinutes).toFloat() / (nextStart - startMinutes)).coerceIn(0f, 1f)
            }

            TimelineRow(
                materia = aula.materia,
                entry = aula.entry,
                isLast = i == dia.aulas.size - 1,
                isHappeningNow = isHappeningNow,
                isPast = isPast,
                isToday = dia.ehHoje,
                connectorProgress = connectorProgress,
                onOpen = { onOpenDetail(aula.materia.codigoDiario) },
            )
        }
    }
}

@Composable
private fun TimelineRow(
    materia: MateriaDisplay,
    entry: io.github.kellyson71.supaco.data.ScheduleEntry,
    isLast: Boolean,
    isHappeningNow: Boolean,
    isPast: Boolean,
    isToday: Boolean,
    connectorProgress: Float,
    onOpen: () -> Unit,
) {
    val vc = MaterialTheme.verdictColors
    val (container, onContainer, solid) = when (materia.status) {
        AbsenceStatus.GO -> Triple(vc.goContainer, vc.onGoContainer, vc.goSolid)
        AbsenceStatus.WARN -> Triple(vc.warnContainer, vc.onWarnContainer, vc.warnSolid)
        AbsenceStatus.LAST -> Triple(vc.lastContainer, vc.onLastContainer, vc.lastSolid)
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Triple(vc.noContainer, vc.onNoContainer, vc.noSolid)
    }
    val meta = verdictMetaFor(materia.status, serio = LocalModoSerio.current)
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val doneColor = MaterialTheme.colorScheme.primary
    val progress by animateFloatAsState(connectorProgress, Motion.calma<Float>(800).orSnap(), label = "connector")
    val pastAlpha by animateFloatAsState(if (isPast) 0.55f else 1f, Motion.calma<Float>(400).orSnap(), label = "past_alpha")

    Row(
        modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = pastAlpha },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Time rail
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(52.dp).padding(top = 4.dp),
        ) {
            Text(entry.horaInicio, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(entry.horaFim, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Connector column: dot + vertical line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp),
        ) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                // Aula acontecendo: anel pulsando em volta do ponto
                if (isHappeningNow) PulseRing(solid, Modifier.requiredSize(36.dp))
                Canvas(modifier = Modifier.size(20.dp, 20.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    if (isHappeningNow) {
                        drawCircle(color = solid.copy(alpha = 0.2f), radius = cx * 2.2f, center = Offset(cx, cy))
                    }
                    drawCircle(color = container, radius = cx * 1.5f, center = Offset(cx, cy))
                    drawCircle(color = solid, radius = cx * 0.7f, center = Offset(cx, cy))
                }
            }
            if (!isLast) {
                // Hoje: trecho já vivido preenchido, o resto tracejado
                Canvas(modifier = Modifier.width(2.dp).height(80.dp)) {
                    val x = size.width / 2f
                    val split = size.height * progress
                    if (isToday) {
                        drawLine(
                            color = lineColor,
                            start = Offset(x, split),
                            end = Offset(x, size.height),
                            strokeWidth = size.width,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                        )
                        if (split > 0f) {
                            drawLine(doneColor, Offset(x, 0f), Offset(x, split), strokeWidth = size.width)
                        }
                    } else {
                        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = size.width)
                    }
                }
            }
        }

        // Card
        val interaction = remember { MutableInteractionSource() }
        Card(
            onClick = onOpen,
            interactionSource = interaction,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 16.dp else 14.dp)
                .pressScale(interaction),
            colors = CardDefaults.cardColors(
                containerColor = if (isHappeningNow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                 else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = if (isHappeningNow) androidx.compose.foundation.BorderStroke(2.dp, solid) else null,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShapeContainer(
                    shape = materia.shape,
                    size = 42.dp,
                    containerColor = container,
                    contentColor = solid,
                    mood = moodFor(materia.status),
                    breathe = isHappeningNow,
                ) {
                    Icon(materia.icone, null, modifier = Modifier.size(22.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            materia.nome,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isHappeningNow) {
                            Surface(
                                color = solid,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    "AGORA",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = onContainer,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(entry.sala, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(meta.icon, null, modifier = Modifier.size(22.dp), tint = solid)
                    Text(
                        if (materia.restantes <= 0) "0" else "${materia.restantes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = solid,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
