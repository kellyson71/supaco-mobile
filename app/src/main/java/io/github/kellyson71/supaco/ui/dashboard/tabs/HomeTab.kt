package io.github.kellyson71.supaco.ui.dashboard.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationImportant
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import io.github.kellyson71.supaco.data.ScheduleData
import io.github.kellyson71.supaco.data.local.CacheStore
import io.github.kellyson71.supaco.ui.components.PulseRing
import io.github.kellyson71.supaco.ui.components.SkeletonShape
import io.github.kellyson71.supaco.ui.components.SupacoPullToRefreshBox
import io.github.kellyson71.supaco.ui.components.enterOnce
import io.github.kellyson71.supaco.ui.components.moodFor
import io.github.kellyson71.supaco.ui.components.pressScale
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.OdometerText
import io.github.kellyson71.supaco.ui.motion.orSnap
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import io.github.kellyson71.supaco.ui.utils.shimmerEffect
import org.koin.compose.koinInject
import androidx.compose.animation.core.animateFloat
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
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
import io.github.kellyson71.supaco.theme.verdictColors
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay
import io.github.kellyson71.supaco.ui.dashboard.MateriaCor
import io.github.kellyson71.supaco.ui.dashboard.verdictMetaFor
import io.github.kellyson71.supaco.ui.dashboard.vereditoDoDia
import io.github.kellyson71.supaco.ui.dashboard.materiaMaisCritica
import androidx.compose.runtime.produceState
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudDone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    nomeUsual: String,
    materias: List<MateriaDisplay>,
    isLoading: Boolean,
    lastSyncAt: Long?,
    syncWarning: String?,
    faltasNovas: Set<String>,
    onDismissFaltasNovas: () -> Unit,
    onSync: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onAskVerdict: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10 } }
    val vc = MaterialTheme.verdictColors

    // "Posso faltar hoje?" olha a pior matéria do dia contando todas as aulas;
    // sem aula hoje, destaca a matéria com menos folga no semestre.
    val materiaHoje = vereditoDoDia(materias)
    val heroMateria = materiaHoje ?: materiaMaisCritica(materias)
    val aulasHojeCount = materias.count { it.aulasHoje > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Olá, ${nomeUsual.split(" ").first()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            fraseDoDia(temAulaHoje = materiaHoje != null, serio = LocalModoSerio.current),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    // Ícone gira enquanto a rede trabalha
                    val spin = if (isLoading && !LocalReduceMotion.current) {
                        rememberInfiniteTransition(label = "sync").animateFloat(
                            0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "sync_spin",
                        ).value
                    } else 0f
                    IconButton(onClick = onSync) {
                        Icon(Icons.Rounded.Sync, contentDescription = "Sincronizar", modifier = Modifier.rotate(-spin))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (scrolled) MaterialTheme.colorScheme.surfaceContainer
                                     else MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        SupacoPullToRefreshBox(
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
                    if (faltasNovas.isNotEmpty()) {
                        item(key = "faltas_novas") {
                            FaltasNovasCard(
                                materias = materias.filter { it.codigoDiario in faltasNovas },
                                onOpen = onOpenDetail,
                                onDismiss = onDismissFaltasNovas,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).animateItem(),
                            )
                        }
                    }

                    item(key = "sync") {
                        SyncStatusLine(
                            lastSyncAt = lastSyncAt,
                            warning = syncWarning,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
                        )
                    }

                    // Hero card
                    heroMateria?.let { m ->
                        item(key = "hero") {
                            HeroCard(
                                materia = m,
                                temAulaHoje = materiaHoje != null,
                                label = when {
                                    materiaHoje == null -> "Sem aula hoje · sua matéria mais crítica"
                                    aulasHojeCount > 1 -> "Hoje · a matéria mais arriscada"
                                    else -> "Aula de hoje"
                                },
                                onAskVerdict = { onAskVerdict(m.id) },
                                modifier = Modifier.padding(16.dp).enterOnce(index = 0),
                            )
                        }
                    }

                    // Semana horários
                    if (materias.isNotEmpty()) {
                        item(key = "semana") {
                            Column(Modifier.enterOnce(index = 2)) {
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
}

private val MateriaDisplay.id get() = codigoDiario

@Composable
private fun HeroCard(
    materia: MateriaDisplay,
    temAulaHoje: Boolean,
    label: String,
    onAskVerdict: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vc = MaterialTheme.verdictColors
    val status = if (temAulaHoje) materia.statusHoje else materia.status
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
                    breathe = true,
                    spin = true,
                    mood = moodFor(status),
                ) {
                    Icon(materia.icone, null, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text(
                        label,
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
                    if (temAulaHoje) {
                        NextClassLine(materia, onContainer)
                    } else if (materia.dia != "—") {
                        Text(
                            "${materia.diaAbbr} · ${materia.horaInicio}–${materia.horaFim}",
                            style = MaterialTheme.typography.bodySmall,
                            color = onContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val meta = verdictMetaFor(status, serio = LocalModoSerio.current)
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
                    val badge = if (temAulaHoje) {
                        val n = materia.aulasHoje
                        val sobra = materia.restantesAposHoje
                        "faltar hoje custa $n · " + if (sobra < 0) "passa do limite" else "sobram $sobra"
                    } else if (materia.restantes <= 0) "sem folga"
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
                // Dado dá uma "rolada" periódica pra chamar atenção — só nas primeiras
                // aberturas do dia, depois some para não cansar
                val cacheStore: CacheStore = koinInject()
                val chamariz = remember { cacheStore.countToday("home_wiggle") < 3 } && !LocalReduceMotion.current
                val diceTransition = rememberInfiniteTransition(label = "dice")
                val wiggleAnim by diceTransition.animateFloat(
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
                val wiggle = if (chamariz) wiggleAnim else 0f
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
private fun SyncStatusLine(lastSyncAt: Long?, warning: String?, modifier: Modifier = Modifier) {
    // Recalcula o "há X min" a cada minuto
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            value = System.currentTimeMillis()
        }
    }
    val text = when {
        warning != null -> warning
        lastSyncAt == null -> return
        else -> "Atualizado ${tempoRelativo(now - lastSyncAt)}"
    }
    // Texto novo entra por baixo, o antigo sobe
    AnimatedContent(
        targetState = text to (warning != null),
        transitionSpec = {
            (slideInVertically(Motion.calma(250)) { it } + fadeIn(Motion.calma(250))) togetherWith
                (slideOutVertically(Motion.calma(200)) { -it } + fadeOut(Motion.calma(150)))
        },
        modifier = modifier,
        label = "sync_status",
    ) { (text, isWarning) ->
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (isWarning) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    }
}

internal fun tempoRelativo(ms: Long): String {
    val min = ms / 60_000
    return when {
        min < 1 -> "agora mesmo"
        min < 60 -> "há $min min"
        min < 60 * 24 -> "há ${min / 60} h"
        else -> "há ${min / (60 * 24)} dia${if (min / (60 * 24) == 1L) "" else "s"}"
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

    val hojeIdx = dias.indexOfFirst { (diaFullMap[it] ?: it) == io.github.kellyson71.supaco.data.ScheduleData.currentDayName() }
        .coerceAtLeast(0)

    var selectedDia by remember { mutableIntStateOf(hojeIdx) }
    val haptics = rememberHaptics()
    fun selectDia(i: Int) {
        if (i != selectedDia && i in dias.indices) {
            haptics.tick()
            selectedDia = i
        }
    }

    Column(modifier = modifier) {
        // Day selector row
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val chipWidth = (maxWidth - gap * (dias.size - 1)) / dias.size
        Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            dias.forEachIndexed { i, abbr ->
                val isSelected = i == selectedDia
                val aulaCount = aulasPorDia[abbr]?.size ?: 0

                FilterChip(
                    selected = isSelected,
                    onClick = { selectDia(i) },
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
        // Indicador que desliza até o dia escolhido
        val indicatorX by animateDpAsState(
            targetValue = (chipWidth + gap) * selectedDia + chipWidth / 2 - 10.dp,
            animationSpec = Motion.viva<Dp>().orSnap(),
            label = "day_indicator",
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .offset(x = indicatorX)
                .size(width = 20.dp, height = 3.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        }
        }

        Spacer(Modifier.height(10.dp))

        // Aulas do dia: trocam deslizando na direção do dia escolhido; swipe troca o dia
        var dragAccum by remember { mutableFloatStateOf(0f) }
        AnimatedContent(
            targetState = selectedDia,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(Motion.calma(300)) { it / 3 * dir } + fadeIn(Motion.calma(250))) togetherWith
                    (slideOutHorizontally(Motion.calma(250)) { -it / 3 * dir } + fadeOut(Motion.calma(150))) using
                    SizeTransform(clip = false)
            },
            modifier = Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, delta -> dragAccum += delta },
                    onDragEnd = {
                        val threshold = 60.dp.toPx()
                        if (dragAccum < -threshold) selectDia(selectedDia + 1)
                        else if (dragAccum > threshold) selectDia(selectedDia - 1)
                    },
                )
            },
            label = "semana_dia",
        ) { selectedDia ->
        Column {

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
                aulasDoDia.forEachIndexed { index, (m, entry) ->
                    val (container, _, solid) = when (m.status) {
                        AbsenceStatus.GO -> Triple(vc.goContainer, vc.onGoContainer, vc.goSolid)
                        AbsenceStatus.WARN -> Triple(vc.warnContainer, vc.onWarnContainer, vc.warnSolid)
                        AbsenceStatus.LAST -> Triple(vc.lastContainer, vc.onLastContainer, vc.lastSolid)
                        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Triple(vc.noContainer, vc.onNoContainer, vc.noSolid)
                    }

                    val interaction = remember { MutableInteractionSource() }
                    OutlinedCard(
                        onClick = { onOpen(m.id) },
                        interactionSource = interaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .enterOnce(index = index + 1)
                            .pressScale(interaction),
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

                            val meta = verdictMetaFor(m.status, serio = LocalModoSerio.current)
                            Icon(meta.icon, null, modifier = Modifier.size(22.dp), tint = solid)
                        }
                    }
                }
            }
        }

        }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Skeleton com a silhueta real da Home: o conteúdo entra no mesmo lugar, sem pulo. */
@Composable
private fun ShimmerHomeContent() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.size(140.dp, 12.dp).shimmerEffect(CircleShape))
        Spacer(Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SkeletonShape(OrgShape.COOKIE, 52.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(90.dp, 10.dp).shimmerEffect(CircleShape))
                        Box(Modifier.size(170.dp, 14.dp).shimmerEffect(CircleShape))
                        Box(Modifier.size(110.dp, 10.dp).shimmerEffect(CircleShape))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(130.dp, 24.dp).shimmerEffect(CircleShape))
                        Box(Modifier.size(100.dp, 10.dp).shimmerEffect(CircleShape))
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(132.dp, 40.dp).shimmerEffect(CircleShape))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(110.dp, 16.dp).shimmerEffect(CircleShape))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(5) { Box(Modifier.weight(1f).height(56.dp).shimmerEffect(MaterialTheme.shapes.small)) }
        }
        Spacer(Modifier.height(14.dp))
        repeat(2) {
            Box(Modifier.fillMaxWidth().height(62.dp).shimmerEffect(MaterialTheme.shapes.medium))
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Subtítulo da saudação: data (modo sério) ou uma frase do dia no tom do app. */
@Composable
private fun fraseDoDia(temAulaHoje: Boolean, serio: Boolean): String {
    val dia = ScheduleData.currentDayName()
    return remember(dia, temAulaHoje, serio) {
        if (serio) {
            java.text.SimpleDateFormat("EEEE, d 'de' MMMM", java.util.Locale("pt", "BR"))
                .format(java.util.Date())
                .replaceFirstChar { it.uppercase() }
        } else when {
            dia == "Sábado" -> "Sábado. Nem o SUAP trabalha."
            dia == "Domingo" -> "Domingo. O dado está descansando."
            !temAulaHoje -> "Hoje ninguém te cobra presença."
            dia == "Sexta" -> "Sexta. O dado está de bom humor."
            else -> listOf(
                "Mais um dia, mais uma chamada.",
                "Hoje tem chamada. Ou não?",
                "Vamos ver o que o destino diz.",
                "Presença é um estado de espírito.",
            )[(System.currentTimeMillis() / 86_400_000L % 4).toInt()]
        }
    }
}

/**
 * Situação da aula de hoje da matéria do hero: "Começa em 12 min" (dígitos rolando),
 * "Em aula · termina em X min" com uma linha de progresso, ou "já acabou".
 */
@Composable
private fun NextClassLine(materia: MateriaDisplay, color: Color) {
    val hoje = ScheduleData.currentDayName()
    val aulas = remember(materia) {
        materia.horarios.filter { it.dia == hoje }.sortedBy { ScheduleData.parseMinutes(it.horaInicio) }
    }
    val now by produceState(ScheduleData.nowMinutes()) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            value = ScheduleData.nowMinutes()
        }
    }
    val atual = aulas.firstOrNull { now >= ScheduleData.parseMinutes(it.horaInicio) && now < ScheduleData.parseMinutes(it.horaFim) }
    val proxima = aulas.firstOrNull { ScheduleData.parseMinutes(it.horaInicio) > now }
    val style = MaterialTheme.typography.bodySmall
    val dim = color.copy(alpha = 0.75f)

    when {
        atual != null -> {
            val ini = ScheduleData.parseMinutes(atual.horaInicio)
            val fim = ScheduleData.parseMinutes(atual.horaFim)
            val restam = fim - now
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                    PulseRing(color, Modifier.size(14.dp))
                    Box(Modifier.size(6.dp).background(color, CircleShape))
                }
                Spacer(Modifier.width(6.dp))
                Text("Em aula · termina em ", style = style, color = dim)
                OdometerText("$restam", style = style, color = dim, fontWeight = FontWeight.SemiBold, increasing = false)
                Text(" min", style = style, color = dim)
            }
            val progress by animateFloatAsState(
                targetValue = ((now - ini).toFloat() / (fim - ini).coerceAtLeast(1)).coerceIn(0f, 1f),
                animationSpec = Motion.calma<Float>(800).orSnap(),
                label = "class_progress",
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .width(160.dp)
                    .height(3.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
            ) {
                Box(Modifier.fillMaxWidth(progress).height(3.dp).background(color, CircleShape))
            }
        }
        proxima != null -> {
            val faltam = ScheduleData.parseMinutes(proxima.horaInicio) - now
            if (faltam <= 120) {
                Row {
                    Text("Começa em ", style = style, color = dim)
                    OdometerText("$faltam", style = style, color = dim, fontWeight = FontWeight.SemiBold, increasing = false)
                    Text(" min · ${proxima.sala}", style = style, color = dim, maxLines = 1)
                }
            } else {
                Text("Hoje · ${proxima.horaInicio}–${proxima.horaFim}", style = style, color = dim)
            }
        }
        else -> Text("A aula de hoje já acabou", style = style, color = dim)
    }
}

/** Aviso de faltas novas registradas no SUAP desde a última visita. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FaltasNovasCard(
    materias: List<MateriaDisplay>,
    onOpen: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (materias.isEmpty()) return
    val vc = MaterialTheme.verdictColors
    ElevatedCard(
        modifier = modifier.fillMaxWidth().enterOnce(),
        colors = CardDefaults.elevatedCardColors(containerColor = vc.warnContainer),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                PulseRing(vc.warnSolid, Modifier.size(24.dp))
                Icon(Icons.Rounded.NotificationImportant, null, tint = vc.warnSolid, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (materias.size == 1) "O SUAP registrou falta nova" else "O SUAP registrou faltas novas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = vc.onWarnContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Dispensar aviso", tint = vc.onWarnContainer)
            }
        }
        FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            materias.forEach { m ->
                AssistChip(
                    onClick = { onOpen(m.codigoDiario) },
                    label = { Text(m.nome, maxLines = 1) },
                    leadingIcon = { Icon(m.icone, null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
}
