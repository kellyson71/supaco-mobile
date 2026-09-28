package io.github.kellyson71.supaco.ui.dashboard.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import io.github.kellyson71.supaco.ui.components.SkeletonShape
import io.github.kellyson71.supaco.ui.components.enterOnce
import io.github.kellyson71.supaco.ui.components.moodFor
import io.github.kellyson71.supaco.ui.components.pressScale
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.OdometerText
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import io.github.kellyson71.supaco.ui.utils.shimmerEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kellyson71.supaco.theme.verdictColors
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay
import io.github.kellyson71.supaco.ui.dashboard.MateriaCor
import io.github.kellyson71.supaco.ui.dashboard.verdictMetaFor
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriasTab(
    materias: List<MateriaDisplay>,
    onOpenDetail: (String) -> Unit,
    periodos: List<io.github.kellyson71.supaco.data.model.PeriodoLetivo> = emptyList(),
    selectedPeriodo: io.github.kellyson71.supaco.data.model.PeriodoLetivo? = null,
    onSelectPeriodo: (io.github.kellyson71.supaco.data.model.PeriodoLetivo) -> Unit = {},
    isLoading: Boolean = false,
    faltasNovas: Set<String> = emptySet(),
) {
    val haptics = rememberHaptics()
    // Cards que já fizeram a animação de entrada (a barra enche só na primeira vez)
    val seen = remember { mutableSetOf<String>() }

    var filtro by remember { mutableStateOf(Filtro.TODAS) }
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = when (filtro) {
        Filtro.TODAS -> materias
        Filtro.NO_FIO -> materias.filter { it.status in listOf(AbsenceStatus.WARN, AbsenceStatus.LAST, AbsenceStatus.NO, AbsenceStatus.REPROVADO) }
        Filtro.FOLGADAS -> materias.filter { it.status == AbsenceStatus.GO }
    }.let { list ->
        if (query.isBlank()) list
        else list.filter {
            it.nome.contains(query, ignoreCase = true) ||
                it.sigla.contains(query, ignoreCase = true) ||
                (it.professor?.contains(query, ignoreCase = true) ?: false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // A busca se abre a partir da lupa; o título volta ao fechar
                    AnimatedContent(
                        targetState = searchActive,
                        transitionSpec = {
                            (fadeIn(Motion.calma(220)) + expandHorizontally(Motion.calma(300), expandFrom = Alignment.End)) togetherWith
                                fadeOut(Motion.calma(120)) using SizeTransform(clip = false)
                        },
                        label = "search_title",
                    ) { searching ->
                    if (searching) {
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Buscar matéria…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                        )
                    } else {
                        Text(
                            "Matérias",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    }
                },
                actions = {
                    // Seletor de período letivo (semestres anteriores)
                    if (!searchActive && periodos.size > 1 && selectedPeriodo != null) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { menuOpen = true }) {
                                // Período "vira" na vertical: mais novo desce, mais antigo sobe
                                AnimatedContent(
                                    targetState = selectedPeriodo,
                                    transitionSpec = {
                                        val newer = periodos.indexOf(targetState) < periodos.indexOf(initialState)
                                        val dir = if (newer) -1 else 1
                                        (slideInVertically(Motion.viva()) { it * dir } + fadeIn()) togetherWith
                                            (slideOutVertically(Motion.calma(200)) { -it * dir } + fadeOut())
                                    },
                                    label = "periodo_label",
                                ) { p -> Text(p.label, fontWeight = FontWeight.SemiBold) }
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Trocar período")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                periodos.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (p == periodos.first()) "${p.label} · atual" else p.label,
                                                fontWeight = if (p == selectedPeriodo) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            menuOpen = false
                                            onSelectPeriodo(p)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = {
                        if (searchActive) {
                            searchActive = false
                            query = ""
                        } else {
                            searchActive = true
                        }
                    }) {
                        // Lupa ↔ X girando
                        AnimatedContent(
                            targetState = searchActive,
                            transitionSpec = {
                                (fadeIn(Motion.calma(200)) + scaleIn(Motion.viva(), initialScale = 0.6f)) togetherWith
                                    (fadeOut(Motion.calma(120)) + scaleOut(targetScale = 0.6f))
                            },
                            label = "search_icon",
                        ) { searching ->
                            Icon(
                                if (searching) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = if (searching) "Fechar busca" else "Buscar",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        // Trocar de período: a lista sai para um lado e a nova entra do outro
        AnimatedContent(
            targetState = selectedPeriodo,
            transitionSpec = {
                val newer = periodos.indexOf(targetState) < periodos.indexOf(initialState)
                val dir = if (newer) -1 else 1
                (slideInHorizontally(Motion.calma(320)) { it / 4 * dir } + fadeIn(Motion.calma(250))) togetherWith
                    (slideOutHorizontally(Motion.calma(250)) { -it / 4 * dir } + fadeOut(Motion.calma(150)))
            },
            modifier = Modifier.fillMaxSize().padding(padding),
            label = "periodo_lista",
        ) { periodoDaTela ->
        // Cada tela guarda a lista do próprio período: a que sai não mostra os dados novos
        val listaAtual = filtered
        var snapshot by remember { mutableStateOf(listaAtual) }
        if (periodoDaTela == selectedPeriodo) snapshot = listaAtual
        val filtered = snapshot
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item {
                // Segmented button filter
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Filtro.entries.forEachIndexed { i, f ->
                        SegmentedButton(
                            selected = filtro == f,
                            onClick = {
                                if (filtro != f) haptics.tick()
                                filtro = f
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = Filtro.entries.size),
                            icon = {
                                SegmentedButtonDefaults.Icon(active = filtro == f) {
                                    Icon(f.icon, null, modifier = Modifier.size(16.dp))
                                }
                            },
                        ) {
                            Text(f.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (isLoading && materias.isEmpty()) {
                items(4) { i ->
                    MateriaCardSkeleton(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).enterOnce(i))
                }
            } else if (filtered.isEmpty()) {
                item(key = "vazio") {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // "Respira" — o trevo respira junto
                            ShapeContainer(
                                OrgShape.CLOVER, 80.dp,
                                MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant,
                                breathe = true,
                            ) {
                                Icon(Icons.Rounded.SentimentSatisfied, null, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Nada por aqui. Respira.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                itemsIndexed(filtered, key = { _, m -> m.codigoDiario }) { index, m ->
                    val firstTime = remember(m.codigoDiario) { seen.add(m.codigoDiario) }
                    MateriaCard(
                        materia = m,
                        onClick = { onOpenDetail(m.codigoDiario) },
                        faltaNova = m.codigoDiario in faltasNovas,
                        animateEntrance = firstTime,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = Motion.calma(250),
                                placementSpec = Motion.viva(),
                                fadeOutSpec = Motion.calma(150),
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .then(if (firstTime) Modifier.enterOnce(index) else Modifier),
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun MateriaCardSkeleton(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SkeletonShape(OrgShape.FLOWER, 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth(0.7f).height(14.dp).shimmerEffect(CircleShape))
                    Box(Modifier.fillMaxWidth(0.35f).height(10.dp).shimmerEffect(CircleShape))
                }
                Box(Modifier.size(44.dp, 40.dp).shimmerEffect(MaterialTheme.shapes.extraSmall))
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).shimmerEffect(CircleShape))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.4f).height(10.dp).shimmerEffect(CircleShape))
        }
    }
}

private enum class Filtro(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TODAS("Todas", Icons.Rounded.Apps),
    NO_FIO("No fio", Icons.Rounded.Warning),
    FOLGADAS("Folgadas", Icons.Rounded.Weekend),
}

@Composable
fun MateriaCard(
    materia: MateriaDisplay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    faltaNova: Boolean = false,
    animateEntrance: Boolean = false,
) {
    val vc = MaterialTheme.verdictColors
    val (container, onContainer, solid) = when (materia.status) {
        AbsenceStatus.GO -> Triple(vc.goContainer, vc.onGoContainer, vc.goSolid)
        AbsenceStatus.WARN -> Triple(vc.warnContainer, vc.onWarnContainer, vc.warnSolid)
        AbsenceStatus.LAST -> Triple(vc.lastContainer, vc.onLastContainer, vc.lastSolid)
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Triple(vc.noContainer, vc.onNoContainer, vc.noSolid)
    }
    val (contColor, onContColor) = when (materia.cor) {
        MateriaCor.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        MateriaCor.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        MateriaCor.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    val meta = verdictMetaFor(materia.status, serio = LocalModoSerio.current)

    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .then(if (faltaNova) Modifier.sheenOnce(solid) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShapeContainer(
                    shape = materia.shape,
                    size = 48.dp,
                    containerColor = container,
                    contentColor = solid,
                    mood = moodFor(materia.status),
                ) {
                    Icon(materia.icone, null, modifier = Modifier.size(24.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        materia.nome,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    if (faltaNova) {
                        Surface(color = solid, shape = CircleShape, modifier = Modifier.padding(top = 2.dp)) {
                            Text(
                                "falta nova",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = container,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    } else if (materia.dia != "—") {
                        Text(
                            "${materia.diaAbbr} · ${materia.horaInicio}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Verdict badge
                Surface(
                    color = container,
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        OdometerText(
                            if (materia.restantes <= 0) "0" else "${materia.restantes}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = solid,
                            increasing = false,
                        )
                        Text(
                            "livre",
                            style = MaterialTheme.typography.labelSmall,
                            color = onContainer,
                            letterSpacing = 0.3.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            val freqValue = (materia.frequencia / 100.0).toFloat().coerceIn(0f, 1f)
            // Enche de 0 até o valor só na primeira vez que o card aparece
            val reduce = LocalReduceMotion.current
            val fill = remember { Animatable(if (animateEntrance && !reduce) 0f else freqValue) }
            LaunchedEffect(freqValue) { fill.animateTo(freqValue, tween(700, delayMillis = 120, easing = Motion.EmphasizedDecelerate)) }
            LinearProgressIndicator(
                progress = { fill.value },
                modifier = Modifier.fillMaxWidth(),
                color = solid,
                trackColor = container,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${materia.frequencia.toInt()}% de frequência",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${materia.faltas}/${materia.limite} faltas",
                    style = MaterialTheme.typography.bodySmall,
                    color = solid,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (materia.notaEtapa1 != null || materia.notaEtapa2 != null || materia.notaEtapa3 != null || materia.notaEtapa4 != null || (materia.media != null && materia.media != "--")) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Grade,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = buildString {
                            val list = mutableListOf<String>()
                            if (materia.notaEtapa1 != null) list.add("N1: ${materia.notaEtapa1.roundToInt()}")
                            if (materia.notaEtapa2 != null) list.add("N2: ${materia.notaEtapa2.roundToInt()}")
                            if (materia.notaEtapa3 != null) list.add("N3: ${materia.notaEtapa3.roundToInt()}")
                            if (materia.notaEtapa4 != null) list.add("N4: ${materia.notaEtapa4.roundToInt()}")
                            if (materia.media != null && materia.media != "--") {
                                val cleanMedia = materia.media.toDoubleOrNull()?.roundToInt()?.toString() ?: materia.media
                                list.add("Média: $cleanMedia")
                            }
                            append(list.joinToString("  ·  "))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Faixa de luz que atravessa o card uma vez — chama atenção para o que mudou. */
private fun Modifier.sheenOnce(color: androidx.compose.ui.graphics.Color): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val t = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(350)
        t.animateTo(1f, tween(1100, easing = Motion.Standard))
    }
    drawWithContent {
        drawContent()
        if (t.value in 0.001f..0.999f) {
            val x = size.width * (t.value * 1.6f - 0.3f)
            drawRect(
                Brush.linearGradient(
                    listOf(androidx.compose.ui.graphics.Color.Transparent, color.copy(alpha = 0.22f), androidx.compose.ui.graphics.Color.Transparent),
                    start = Offset(x - size.width * 0.25f, 0f),
                    end = Offset(x + size.width * 0.25f, size.height),
                ),
            )
        }
    }
}
