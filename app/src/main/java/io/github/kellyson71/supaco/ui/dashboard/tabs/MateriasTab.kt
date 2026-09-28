package io.github.kellyson71.supaco.ui.dashboard.tabs

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
) {
    val listState = rememberLazyListState()

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
                    if (searchActive) {
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
                },
                actions = {
                    // Seletor de período letivo (semestres anteriores)
                    if (!searchActive && periodos.size > 1 && selectedPeriodo != null) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { menuOpen = true }) {
                                Text(selectedPeriodo.label, fontWeight = FontWeight.SemiBold)
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
                        Icon(
                            if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = if (searchActive) "Fechar busca" else "Buscar",
                        )
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
            item {
                // Segmented button filter
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Filtro.entries.forEachIndexed { i, f ->
                        SegmentedButton(
                            selected = filtro == f,
                            onClick = { filtro = f },
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

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ShapeContainer(OrgShape.CLOVER, 80.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) {
                                Icon(Icons.Rounded.SentimentSatisfied, null, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Nada por aqui. Respira.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.codigoDiario }) { m ->
                    MateriaCard(
                        materia = m,
                        onClick = { onOpenDetail(m.codigoDiario) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
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

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                    if (materia.dia != "—") {
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
                        Text(
                            if (materia.restantes <= 0) "0" else "${materia.restantes}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = solid,
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
            LinearProgressIndicator(
                progress = { freqValue },
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
