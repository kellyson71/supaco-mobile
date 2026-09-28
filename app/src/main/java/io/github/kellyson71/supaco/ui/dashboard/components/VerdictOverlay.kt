package io.github.kellyson71.supaco.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.kellyson71.supaco.theme.verdictColors
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay
import io.github.kellyson71.supaco.ui.dashboard.verdictMetaFor
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class Phase { ROLLING, REVEAL }

@Composable
fun VerdictOverlay(
    materia: MateriaDisplay,
    onClose: () -> Unit,
) {
    val vc = MaterialTheme.verdictColors
    val status = materia.statusVeredito
    val (container, onContainer, solid) = when (status) {
        AbsenceStatus.GO -> Triple(vc.goContainer, vc.onGoContainer, vc.goSolid)
        AbsenceStatus.WARN -> Triple(vc.warnContainer, vc.onWarnContainer, vc.warnSolid)
        AbsenceStatus.LAST -> Triple(vc.lastContainer, vc.onLastContainer, vc.lastSolid)
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Triple(vc.noContainer, vc.onNoContainer, vc.noSolid)
    }

    val meta = verdictMetaFor(materia.statusVeredito, materia.codigoDiario.hashCode(), LocalModoSerio.current)
    val haptics = LocalHapticFeedback.current
    val ehRuim = status == AbsenceStatus.NO || status == AbsenceStatus.REPROVADO

    var phase by remember { mutableStateOf(Phase.ROLLING) }

    // Suspense: dado rola, vibra e revela
    LaunchedEffect(Unit) {
        delay(150)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(500)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(450)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        phase = Phase.REVEAL
        if (ehRuim) {
            delay(120)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Fundo: neutro durante o suspense, cor do veredito na revelação
    val bgTop by animateColorAsState(
        targetValue = if (phase == Phase.REVEAL) container else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(450),
        label = "bg_top",
    )
    val bgBottom by animateColorAsState(
        targetValue = if (phase == Phase.REVEAL) solid.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(650),
        label = "bg_bottom",
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // Tremor da tela inteira nos vereditos ruins
        val screenShake = remember { Animatable(0f) }
        LaunchedEffect(phase) {
            if (phase == Phase.REVEAL && ehRuim) {
                repeat(6) { i ->
                    screenShake.animateTo(if (i % 2 == 0) 10f else -8f, tween(45))
                }
                screenShake.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 500f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = screenShake.value.dp)
                .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
                .pointerInput(phase) {
                    detectTapGestures { if (phase == Phase.REVEAL) onClose() }
                },
            contentAlignment = Alignment.Center,
        ) {
            when (phase) {
                Phase.ROLLING -> RollingDice()
                Phase.REVEAL -> {
                    if (status == AbsenceStatus.GO) {
                        ConfettiLayer(baseColor = solid)
                    }
                    RevealContent(
                        materia = materia,
                        word = meta.word,
                        subtext = meta.subtext,
                        icon = meta.icon,
                        container = container,
                        onContainer = onContainer,
                        solid = solid,
                    )
                }
            }

            // Botão fechar — só na revelação
            AnimatedVisibility(
                visible = phase == Phase.REVEAL,
                enter = fadeIn(tween(400, delayMillis = 600)),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                FilledIconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = container.copy(alpha = 0.6f),
                        contentColor = onContainer,
                    ),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar")
                }
            }
        }
    }
}

@Composable
private fun RollingDice() {
    val transition = rememberInfiniteTransition(label = "rolling")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing)),
        label = "dice_spin",
    )
    val bounce by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(220, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dice_bounce",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.Casino,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                .offset(y = bounce.dp)
                .rotate(rotation),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Consultando o destino…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RevealContent(
    materia: MateriaDisplay,
    word: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    onContainer: Color,
    solid: Color,
) {
    // Palavra entra "batendo": escala de 3x pra 1x com spring
    val wordScale = remember { Animatable(3f) }
    val wordAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        wordAlpha.animateTo(1f, tween(120))
    }
    LaunchedEffect(Unit) {
        wordScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        // Ícone em shape orgânica com pop
        val iconScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
            label = "icon_pop",
        )
        ShapeContainer(
            shape = OrgShape.FLOWER,
            size = 110.dp,
            containerColor = solid.copy(alpha = 0.3f),
            contentColor = onContainer,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .scale(iconScale),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = word,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-2).sp,
            color = onContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .scale(wordScale.value)
                .alpha(wordAlpha.value),
        )

        Spacer(Modifier.height(8.dp))

        // Elementos secundários entram em sequência (stagger)
        StaggeredIn(delayMs = 250) {
            Text(
                text = materia.nome,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(18.dp))

        StaggeredIn(delayMs = 400) {
            Surface(
                color = container.copy(alpha = 0.65f),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                )
            }
        }

        if (materia.aulasHoje > 0) {
            Spacer(Modifier.height(12.dp))
            StaggeredIn(delayMs = 480) {
                val n = materia.aulasHoje
                Text(
                    "Hoje ${if (n == 1) "é 1 aula" else "são $n aulas"}: faltar custa $n falta${if (n == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        StaggeredIn(delayMs = 550) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("${materia.faltas}", "usadas", container, onContainer, solid)
                StatChip("${materia.limite}", "limite", container, onContainer, solid)
                if (materia.aulasHoje > 0) {
                    StatChip(
                        "${materia.restantesAposHoje.coerceAtLeast(0)}",
                        "sobram se faltar",
                        container, onContainer, solid,
                    )
                } else {
                    StatChip(
                        "${materia.restantes.coerceAtLeast(0)}",
                        "sobrando",
                        container, onContainer, solid,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        StaggeredIn(delayMs = 700) {
            Text(
                "toca em qualquer lugar pra fechar",
                style = MaterialTheme.typography.labelMedium,
                color = onContainer.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, container: Color, onContainer: Color, solid: Color) {
    Surface(
        color = container.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = solid,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.8f),
            )
        }
    }
}

/** Entrada com fade + slide de baixo, atrasada para criar o efeito cascata. */
@Composable
private fun StaggeredIn(delayMs: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        ),
    ) {
        content()
    }
}

@Composable
private fun ConfettiLayer(baseColor: Color) {
    val colors = remember(baseColor) {
        listOf(
            baseColor,
            baseColor.copy(green = (baseColor.green * 0.7f).coerceIn(0f, 1f)),
            Color(0xFFFFD54F),
            Color(0xFF4FC3F7),
            Color.White.copy(alpha = 0.9f),
        )
    }
    val pieces = remember {
        (0..70).map {
            ConfettiPiece(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.4f,
                vx = Random.nextFloat() * 0.006f - 0.003f,
                vy = Random.nextFloat() * 0.004f + 0.003f,
                rotation = Random.nextFloat() * 360f,
                spin = Random.nextFloat() * 720f - 360f,
                size = Random.nextFloat() * 10f + 5f,
                colorIndex = Random.nextInt(5),
            )
        }
    }
    val progress by produceState(0f) {
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = (System.currentTimeMillis() - startTime) / 3500f
            value = elapsed.coerceAtMost(1f)
            if (elapsed >= 1f) break
            delay(16)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        pieces.forEach { p ->
            // Gravidade simples: acelera na queda
            val t = progress * 3500f
            val x = (p.x + p.vx * t) * size.width
            val y = (p.y + p.vy * t + 0.0000002f * t * t) * size.height
            rotate(p.rotation + p.spin * progress, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[p.colorIndex].copy(alpha = (1f - progress).coerceIn(0.25f, 1f)),
                    topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.55f),
                )
            }
        }
    }
}

private data class ConfettiPiece(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val rotation: Float, val spin: Float,
    val size: Float, val colorIndex: Int,
)
