package io.github.kellyson71.supaco.ui.dashboard.components

import androidx.compose.foundation.layout.*
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kellyson71.supaco.theme.verdictColors
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaCor
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay
import io.github.kellyson71.supaco.ui.dashboard.calcStatus
import io.github.kellyson71.supaco.ui.dashboard.verdictMetaFor
import io.github.kellyson71.supaco.ui.dashboard.calculateMD
import io.github.kellyson71.supaco.ui.dashboard.calculateMFD
import io.github.kellyson71.supaco.ui.dashboard.calculateNafNecessaria
import kotlin.math.ceil
import kotlin.math.roundToInt
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriaDetailSheet(
    materia: MateriaDisplay,
    onClose: () -> Unit,
) {
    val vc = MaterialTheme.verdictColors
    val (container, onContainer, solid) = statusColors(materia.status, vc)

    val meta = verdictMetaFor(materia.status, serio = LocalModoSerio.current)
    val freqValue = (materia.frequencia / 100.0).toFloat().coerceIn(0f, 1f)

    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShapeContainer(
                    shape = materia.shape,
                    size = 56.dp,
                    containerColor = container,
                    contentColor = solid,
                ) {
                    Icon(materia.icone, null, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        materia.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    materia.professor?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    color = container,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        meta.word,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainer,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Circular progress + stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circular frequency ring
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { freqValue },
                        modifier = Modifier.size(100.dp),
                        strokeWidth = 8.dp,
                        color = solid,
                        trackColor = container,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${materia.frequencia.toInt()}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "freq.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Stats grid
                Column(
                    modifier = Modifier.weight(1f).padding(start = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatRow(label = "Faltas", value = "${materia.faltas}", color = solid)
                    StatRow(label = "Limite", value = "${materia.limite}", color = MaterialTheme.colorScheme.onSurface)
                    StatRow(
                        label = "Restam",
                        value = if (materia.restantes < 0) "0 (${materia.restantes})" else "${materia.restantes}",
                        color = if (materia.restantes <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    materia.media?.let { StatRow(label = "Média", value = it, color = MaterialTheme.colorScheme.onSurface) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Info row: schedule + room
            if (materia.dia != "—") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${materia.dia} · ${materia.horaInicio}–${materia.horaFim}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(materia.sala, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Verdict text
            Text(
                meta.subtext,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            // ── Simulador de faltas ──
            SimuladorFaltas(materia = materia)

            Spacer(Modifier.height(20.dp))

            // ── Notas ──
            NotasSection(materia = materia)
        }
    }
}

@Composable
private fun SimuladorFaltas(materia: MateriaDisplay) {
    val vc = MaterialTheme.verdictColors
    val minExtras = -materia.faltas
    val maxExtras = (materia.restantes + 5).coerceIn(5, 20)
    var faltasExtras by remember(materia.codigoDiario) { mutableIntStateOf(0) }

    val faltasFuturas = (materia.faltas + faltasExtras).coerceAtLeast(0)
    val statusFuturo = calcStatus(faltasFuturas, materia.total)
    val restantesFuturos = materia.limite - faltasFuturas
    val (fContainer, fOnContainer, fSolid) = statusColors(statusFuturo, vc)
    val metaFuturo = verdictMetaFor(statusFuturo, serio = LocalModoSerio.current)
    val freqFutura = if (materia.total > 0) {
        ((materia.total - faltasFuturas).toDouble() / materia.total.toDouble() * 100.0).coerceIn(0.0, 100.0)
    } else 100.0

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Casino, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Simulador de Presença/Faltas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Slider(
                    value = faltasExtras.toFloat(),
                    onValueChange = { faltasExtras = it.roundToInt() },
                    valueRange = minExtras.toFloat()..maxExtras.toFloat(),
                    steps = (maxExtras - minExtras - 1).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = if (faltasExtras < 0) vc.goSolid.copy(alpha = 0.15f) else fContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        if (faltasExtras > 0) "+$faltasExtras" else "$faltasExtras",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (faltasExtras < 0) vc.goSolid else fOnContainer,
                    )
                }
            }

            // Resultado da simulação
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(metaFuturo.icon, null, modifier = Modifier.size(18.dp), tint = fSolid)
                Text(
                    "Frequência simulada: ${freqFutura.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = fSolid,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (restantesFuturos < 0) "reprovado por falta"
                    else "$restantesFuturos sobrando",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (faltasExtras != 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        faltasExtras < 0 -> "Simulando assistir/abonar ${-faltasExtras} aulas."
                        else -> "Simulando levar mais $faltasExtras faltas."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NotasSection(materia: MateriaDisplay) {
    val vc = MaterialTheme.verdictColors
    var simularAtivo by remember { mutableStateOf(false) }
    val isAnnual = materia.quantidadeAvaliacoes == 4

    // Inicializa notas simuladas com os valores reais
    var n1Simulada by remember(materia.codigoDiario) { mutableDoubleStateOf(materia.notaEtapa1 ?: 0.0) }
    var n2Simulada by remember(materia.codigoDiario) { mutableDoubleStateOf(materia.notaEtapa2 ?: 0.0) }
    var n3Simulada by remember(materia.codigoDiario) { mutableDoubleStateOf(materia.notaEtapa3 ?: 0.0) }
    var n4Simulada by remember(materia.codigoDiario) { mutableDoubleStateOf(materia.notaEtapa4 ?: 0.0) }
    var nafSimulada by remember(materia.codigoDiario) { mutableDoubleStateOf(0.0) }

    val mdSimulada = calculateMD(n1Simulada, n2Simulada, n3Simulada, n4Simulada, numStages = materia.quantidadeAvaliacoes)
    val situacaoMateria = when {
        mdSimulada >= 60.0 -> AbsenceStatus.GO
        mdSimulada < 20.0 -> AbsenceStatus.REPROVADO
        else -> AbsenceStatus.WARN // Exame Final
    }

    val nafNecessaria = remember(n1Simulada, n2Simulada, n3Simulada, n4Simulada, mdSimulada) {
        calculateNafNecessaria(n1Simulada, n2Simulada, n3Simulada, n4Simulada, numStages = materia.quantidadeAvaliacoes)
    }

    val mfdSimulada = if (situacaoMateria == AbsenceStatus.WARN) {
        calculateMFD(mdSimulada, n1Simulada, n2Simulada, n3Simulada, n4Simulada, nafSimulada, numStages = materia.quantidadeAvaliacoes)
    } else mdSimulada

    val aprovadoFinal = when {
        situacaoMateria == AbsenceStatus.GO -> true
        situacaoMateria == AbsenceStatus.REPROVADO -> false
        else -> mfdSimulada >= 60.0
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Grade, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        if (simularAtivo) "Simulador de Notas" else "Notas", 
                        style = MaterialTheme.typography.titleSmall, 
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = { simularAtivo = !simularAtivo }) {
                    Text(if (simularAtivo) "Ver oficiais" else "Simular")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (!simularAtivo) {
                // Modo Exibição das Notas Oficiais do SUAP
                if (isAnnual) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotaChip("Etapa 1", materia.notaEtapa1, Modifier.weight(1f))
                            NotaChip("Etapa 2", materia.notaEtapa2, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotaChip("Etapa 3", materia.notaEtapa3, Modifier.weight(1f))
                            NotaChip("Etapa 4", materia.notaEtapa4, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotaChip("Média", materia.media?.toDoubleOrNull(), Modifier.weight(1f))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NotaChip("Etapa 1", materia.notaEtapa1, Modifier.weight(1f))
                        NotaChip("Etapa 2", materia.notaEtapa2, Modifier.weight(1f))
                        NotaChip("Média", materia.media?.toDoubleOrNull(), Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Mensagem baseada no estado oficial
                val n1 = materia.notaEtapa1
                val n2 = materia.notaEtapa2
                val n3 = materia.notaEtapa3
                val n4 = materia.notaEtapa4
                val mdOficial = materia.media?.toDoubleOrNull() ?: 0.0

                val (msgText, msgIcon, msgIconColor) = when {
                    isAnnual -> {
                        when {
                            n1 == null -> Triple("Aguardando nota da Etapa 1.", Icons.Rounded.Info, MaterialTheme.colorScheme.primary)
                            n2 == null -> Triple("Aguardando nota da Etapa 2.", Icons.Rounded.Info, MaterialTheme.colorScheme.primary)
                            n3 == null -> Triple("Aguardando nota da Etapa 3.", Icons.Rounded.Info, MaterialTheme.colorScheme.primary)
                            n4 == null -> {
                                val currentSum = 2.0 * (n1 ?: 0.0) + 2.0 * (n2 ?: 0.0) + 3.0 * (n3 ?: 0.0)
                                val precisa = ceil((600.0 - currentSum) / 3.0).toInt().coerceIn(0, 100)
                                if (precisa <= 0) Triple("Aprovado! Média garantida por nota.", Icons.Rounded.WorkspacePremium, vc.goSolid)
                                else Triple("Você precisa de $precisa na Etapa 4 para passar direto.", Icons.Rounded.Info, MaterialTheme.colorScheme.primary)
                            }
                            else -> {
                                if (mdOficial >= 60) Triple("Aprovado por nota! Média final: ${mdOficial.toInt()}", Icons.Rounded.CheckCircle, vc.goSolid)
                                else if (mdOficial >= 20) Triple("Média ${mdOficial.toInt()}: Exame Final necessário.", Icons.Rounded.Warning, vc.warnSolid)
                                else Triple("Média ${mdOficial.toInt()}: Reprovado direto por nota.", Icons.Rounded.Error, vc.noSolid)
                            }
                        }
                    }
                    else -> { // Semestral
                        when {
                            n1 == null -> Triple("Aguardando nota da Etapa 1.", Icons.Rounded.Info, MaterialTheme.colorScheme.primary)
                            n2 == null -> {
                                val precisa = ceil((300.0 - 2.0 * n1) / 3.0).toInt().coerceIn(0, 100)
                                if (precisa <= 0) Triple("Aprovado! Média garantida por nota.", Icons.Rounded.WorkspacePremium, vc.goSolid)
                                else Triple("Você precisa de $precisa na Etapa 2 para passar direto.", Icons.Rounded.Info, MaterialTheme.colorScheme.primary)
                            }
                            else -> {
                                if (mdOficial >= 60) Triple("Aprovado por nota! Média final: ${mdOficial.toInt()}", Icons.Rounded.CheckCircle, vc.goSolid)
                                else if (mdOficial >= 20) Triple("Média ${mdOficial.toInt()}: Exame Final necessário.", Icons.Rounded.Warning, vc.warnSolid)
                                else Triple("Média ${mdOficial.toInt()}: Reprovado direto por nota.", Icons.Rounded.Error, vc.noSolid)
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(msgIcon, contentDescription = null, modifier = Modifier.size(18.dp), tint = msgIconColor)
                    Text(
                        msgText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // Modo Simulador de Notas Ativo
                if (isAnnual) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotaChipSimulada("Etapa 1", n1Simulada, Modifier.weight(1f))
                            NotaChipSimulada("Etapa 2", n2Simulada, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotaChipSimulada("Etapa 3", n3Simulada, Modifier.weight(1f))
                            NotaChipSimulada("Etapa 4", n4Simulada, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotaChipSimulada(
                                label = if (situacaoMateria == AbsenceStatus.WARN) "Média Final" else "Média",
                                nota = mfdSimulada,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NotaChipSimulada("Etapa 1", n1Simulada, Modifier.weight(1f))
                        NotaChipSimulada("Etapa 2", n2Simulada, Modifier.weight(1f))
                        NotaChipSimulada(
                            label = if (situacaoMateria == AbsenceStatus.WARN) "Média Final" else "Média",
                            nota = mfdSimulada,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Controles dos Sliders para Etapa 1 e Etapa 2
                Text("Simular Etapa 1: ${n1Simulada.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Slider(
                    value = n1Simulada.toFloat(),
                    onValueChange = { n1Simulada = it.roundToInt().toDouble() },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text("Simular Etapa 2: ${n2Simulada.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Slider(
                    value = n2Simulada.toFloat(),
                    onValueChange = { n2Simulada = it.roundToInt().toDouble() },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isAnnual) {
                    Spacer(Modifier.height(8.dp))

                    Text("Simular Etapa 3: ${n3Simulada.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Slider(
                        value = n3Simulada.toFloat(),
                        onValueChange = { n3Simulada = it.roundToInt().toDouble() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Simular Etapa 4: ${n4Simulada.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Slider(
                        value = n4Simulada.toFloat(),
                        onValueChange = { n4Simulada = it.roundToInt().toDouble() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Se estiver em recuperação/Exame Final, mostra o controle do exame
                if (situacaoMateria == AbsenceStatus.WARN) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Exame Final Requerido",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    
                    if (nafNecessaria != -1) {
                        Text(
                            "Você precisa de no mínimo $nafNecessaria no Exame para ser aprovado.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            "Matematicamente impossível passar, mesmo com 100 no Exame.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text("Simular Nota no Exame: ${nafSimulada.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Slider(
                        value = nafSimulada.toFloat(),
                        onValueChange = { nafSimulada = it.roundToInt().toDouble() },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Banner de Status da Simulação
                val bannerColor = when {
                    aprovadoFinal -> vc.goContainer
                    situacaoMateria == AbsenceStatus.REPROVADO -> vc.noContainer
                    else -> vc.warnContainer
                }
                val bannerText = when {
                    aprovadoFinal && situacaoMateria == AbsenceStatus.GO -> "Situação: Aprovado Direto!"
                    aprovadoFinal -> "Situação: Aprovado no Exame Final!"
                    situacaoMateria == AbsenceStatus.REPROVADO -> "Situação: Reprovado Direto por Nota"
                    else -> "Situação: Recuperação (Exame) — Precisa estudar!"
                }
                val bannerTextColor = when {
                    aprovadoFinal && situacaoMateria == AbsenceStatus.GO -> vc.onGoContainer
                    aprovadoFinal -> vc.onGoContainer
                    situacaoMateria == AbsenceStatus.REPROVADO -> vc.onNoContainer
                    else -> vc.onWarnContainer
                }
                val bannerIcon = when {
                    aprovadoFinal && situacaoMateria == AbsenceStatus.GO -> Icons.Rounded.WorkspacePremium
                    aprovadoFinal -> Icons.Rounded.School
                    situacaoMateria == AbsenceStatus.REPROVADO -> Icons.Rounded.ErrorOutline
                    else -> Icons.Rounded.Warning
                }

                Surface(
                    color = bannerColor,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            bannerIcon,
                            contentDescription = null,
                            tint = bannerTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            bannerText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = bannerTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotaChipSimulada(label: String, nota: Double, modifier: Modifier = Modifier) {
    val aprovada = nota >= 60.0
    Surface(
        color = if (aprovada) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            Text(
                "${nota.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (!aprovada) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotaChip(label: String, nota: Double?, modifier: Modifier = Modifier) {
    val temNota = nota != null
    Surface(
        color = if (temNota && nota >= 60) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            Text(
                if (temNota) "${nota.toInt()}" else "--",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (temNota && nota < 60) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun statusColors(
    status: AbsenceStatus,
    vc: io.github.kellyson71.supaco.theme.VerdictColors
): Triple<Color, Color, Color> = when (status) {
    AbsenceStatus.GO -> Triple(vc.goContainer, vc.onGoContainer, vc.goSolid)
    AbsenceStatus.WARN -> Triple(vc.warnContainer, vc.onWarnContainer, vc.warnSolid)
    AbsenceStatus.LAST -> Triple(vc.lastContainer, vc.onLastContainer, vc.lastSolid)
    AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Triple(vc.noContainer, vc.onNoContainer, vc.noSolid)
}
