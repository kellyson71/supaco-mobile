package com.example.supacomobile.ui.dashboard.tabs

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supacomobile.theme.verdictColors
import com.example.supacomobile.ui.components.OrgShape
import com.example.supacomobile.ui.components.ShapeContainer
import com.example.supacomobile.ui.dashboard.AbsenceStatus
import com.example.supacomobile.ui.dashboard.MateriaDisplay
import com.example.supacomobile.ui.dashboard.MateriaCor
import com.example.supacomobile.ui.dashboard.verdictMetaFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    nomeUsual: String,
    materias: List<MateriaDisplay>,
    isLoading: Boolean,
    onSync: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onAskVerdict: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10 } }
    val vc = MaterialTheme.verdictColors

    val hojeMateria = materias.firstOrNull { it.ehHoje } ?: materias.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Olá, ${nomeUsual.split(" ").first()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = onSync) {
                        Icon(Icons.Rounded.Sync, contentDescription = "Sincronizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (scrolled) MaterialTheme.colorScheme.surfaceContainer
                                     else MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onSync,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (isLoading && materias.isEmpty()) {
                ShimmerHomeContent()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    // Hero card
                    hojeMateria?.let { m ->
                        item {
                            HeroCard(
                                materia = m,
                                onAskVerdict = { onAskVerdict(m.id) },
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    // Semana horários
                    if (materias.isNotEmpty()) {
                        item {
                            Text(
                                "Minha semana",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                            )
                            SemanaHorarios(
                                materias = materias,
                                onOpen = onOpenDetail,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val MateriaDisplay.id get() = codigoDiario

@Composable
private fun HeroCard(
    materia: MateriaDisplay,
    onAskVerdict: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vc = MaterialTheme.verdictColors
    val status = materia.status
    val container = when (status) {
        AbsenceStatus.GO -> vc.goContainer
        AbsenceStatus.WARN -> vc.warnContainer
        AbsenceStatus.LAST -> vc.lastContainer
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> vc.noContainer
    }
    val onContainer = when (status) {
        AbsenceStatus.GO -> vc.onGoContainer
        AbsenceStatus.WARN -> vc.onWarnContainer
        AbsenceStatus.LAST -> vc.onLastContainer
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> vc.onNoContainer
    }
    val solid = when (status) {
        AbsenceStatus.GO -> vc.goSolid
        AbsenceStatus.WARN -> vc.warnSolid
        AbsenceStatus.LAST -> vc.lastSolid
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> vc.noSolid
    }
    val onSolid = when (status) {
        AbsenceStatus.GO -> vc.onGoSolid
        AbsenceStatus.WARN -> vc.onWarnSolid
        AbsenceStatus.LAST -> vc.onLastSolid
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> vc.onNoSolid
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShapeContainer(
                    shape = OrgShape.COOKIE,
                    size = 52.dp,
                    containerColor = solid.copy(alpha = 0.25f),
                    contentColor = solid,
                ) {
                    Icon(materia.icone, null, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text(
                        if (materia.ehHoje) "Próxima aula" else "Sua matéria mais crítica",
                        style = MaterialTheme.typography.labelMedium,
                        color = onContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        materia.nome,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainer,
                        maxLines = 2,
                    )
                    if (materia.dia != "—") {
                        Text(
                            "${materia.diaAbbr} · ${materia.horaInicio}–${materia.horaFim}",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val meta = verdictMetaFor(materia.status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        meta.word,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = onContainer,
                    )
                    val badge = if (materia.restantes <= 0) "sem folga"
                                else "${materia.restantes} falta${if (materia.restantes == 1) "" else "s"} livre${if (materia.restantes == 1) "" else "s"}"
                    Text(badge, style = MaterialTheme.typography.bodySmall, color = onContainer.copy(alpha = 0.7f))
                }

                // Press com bounce expressivo; contentColor = container garante contraste
                // sobre o solid em ambos os temas (claro: texto claro/fundo escuro; escuro: inverso)
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.85f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 700f),
                    label = "verdict_btn_scale",
                )
                // Dado dá uma "rolada" periódica pra chamar atenção
                val diceTransition = rememberInfiniteTransition(label = "dice")
                val wiggle by diceTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 3200
                            0f at 0
                            0f at 2300
                            -22f at 2450
                            18f at 2600
                            -12f at 2750
                            6f at 2900
                            0f at 3050
                        },
                    ),
                    label = "dice_wiggle",
                )
                val pressSpin by animateFloatAsState(
                    targetValue = if (pressed) 180f else 0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "dice_press_spin",
                )
                Button(
                    onClick = onAskVerdict,
                    interactionSource = interaction,
                    colors = ButtonDefaults.buttonColors(containerColor = solid, contentColor = onSolid),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.scale(scale),
                ) {
                    Icon(Icons.Rounded.Casino, null, modifier = Modifier.size(16.dp).rotate(wiggle + pressSpin))
                    Spacer(Modifier.width(6.dp))
                    Text("Posso faltar?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SemanaHorarios(
    materias: List<MateriaDisplay>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vc = MaterialTheme.verdictColors
    val dias = listOf("Seg", "Ter", "Qua", "Qui", "Sex")
    val diaFullMap = mapOf(
        "Seg" to "Segunda", "Ter" to "Terça", "Qua" to "Quarta",
        "Qui" to "Quinta", "Sex" to "Sexta"
    )

    // Aulas por dia a partir da grade real (uma matéria pode ter mais de um dia)
    val aulasPorDia = remember(materias) {
        dias.associateWith { abbr ->
            val diaFull = diaFullMap[abbr] ?: abbr
            materias.flatMap { m ->
                m.horarios.filter { it.dia == diaFull }.map { m to it }
            }.sortedBy { it.second.horaInicio }
        }
    }

    val hojeIdx = dias.indexOfFirst { (diaFullMap[it] ?: it) == com.example.supacomobile.data.ScheduleData.currentDayName() }
        .coerceAtLeast(0)

    var selectedDia by remember { mutableIntStateOf(hojeIdx) }

    Column(modifier = modifier) {
        // Day selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            dias.forEachIndexed { i, abbr ->
                val isSelected = i == selectedDia
                val aulaCount = aulasPorDia[abbr]?.size ?: 0

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDia = i },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(abbr, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (aulaCount == 0) "—" else "$aulaCount",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val selectedDiaAbbr = dias.getOrElse(selectedDia) { "Seg" }
        val aulasDoDia = aulasPorDia[selectedDiaAbbr].orEmpty()

        if (aulasDoDia.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Bedtime, null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Dia livre. Aproveita pra dormir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                aulasDoDia.forEach { (m, entry) ->
                    val (container, _, solid) = when (m.status) {
                        AbsenceStatus.GO -> Triple(vc.goContainer, vc.onGoContainer, vc.goSolid)
                        AbsenceStatus.WARN -> Triple(vc.warnContainer, vc.onWarnContainer, vc.warnSolid)
                        AbsenceStatus.LAST -> Triple(vc.lastContainer, vc.onLastContainer, vc.lastSolid)
                        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Triple(vc.noContainer, vc.onNoContainer, vc.noSolid)
                    }

                    OutlinedCard(
                        onClick = { onOpen(m.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(48.dp),
                            ) {
                                Text(entry.horaInicio, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Text(entry.horaFim, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Status bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(40.dp)
                                    .padding(0.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = solid,
                                    shape = MaterialTheme.shapes.extraSmall,
                                ) {}
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    m.nome,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                                Text(entry.sala, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            val meta = verdictMetaFor(m.status)
                            Icon(meta.icon, null, modifier = Modifier.size(22.dp), tint = solid)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ShimmerHomeContent() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        repeat(3) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {}
        }
    }
}
