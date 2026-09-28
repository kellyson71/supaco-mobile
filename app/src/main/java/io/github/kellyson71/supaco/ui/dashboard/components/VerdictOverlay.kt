package io.github.kellyson71.supaco.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.kellyson71.supaco.data.local.CacheStore
import io.github.kellyson71.supaco.theme.verdictColors
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.components.moodFor
import io.github.kellyson71.supaco.ui.components.shapeFor
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay
import io.github.kellyson71.supaco.ui.dashboard.verdictMetaFor
import io.github.kellyson71.supaco.ui.motion.CountUpText
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.hypot
import kotlin.random.Random

private enum class Phase { ROLLING, REVEAL }

/** Face em que o dado para: 6 = pode faltar … 1 = nem pense. */
private fun faceFor(status: AbsenceStatus) = when (status) {
    AbsenceStatus.GO -> 6
    AbsenceStatus.WARN -> 4
    AbsenceStatus.LAST -> 2
    AbsenceStatus.NO, AbsenceStatus.REPROVADO -> 1
}

/**
 * "Posso faltar?" — sequência em 3 atos:
 * 1. o dado rola (mais lento e hesitante quanto pior o status) e para na face do veredito;
 * 2. a cor do status se espalha a partir do dado e a forma se transforma na do status;
 * 3. a palavra entra (quicando se é bom, batendo seco se é ruim), seguida da barra de
 *    "gasto do dia" e dos contadores.
 *
 * O suspense só acontece na primeira consulta do dia por matéria; tocar na tela pula direto.
 */
@Composable
fun VerdictOverlay(
    materia: MateriaDisplay,
    onClose: () -> Unit,
) {
    val cacheStore: CacheStore = koinInject()
    val reduce = LocalReduceMotion.current
    val serio = LocalModoSerio.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    val status = materia.statusVeredito
    val vc = MaterialTheme.verdictColors
    val (container, onContainer, solid) = when (status) {
        AbsenceStatus.GO -> Triple(vc.goContainer, vc.onGoContainer, vc.goSolid)
        AbsenceStatus.WARN -> Triple(vc.warnContainer, vc.onWarnContainer, vc.warnSolid)
        AbsenceStatus.LAST -> Triple(vc.lastContainer, vc.onLastContainer, vc.lastSolid)
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Triple(vc.noContainer, vc.onNoContainer, vc.noSolid)
    }
    val meta = verdictMetaFor(status, materia.codigoDiario.hashCode(), serio)
    val ehRuim = status == AbsenceStatus.NO || status == AbsenceStatus.REPROVADO

    // Decisões tomadas uma vez por abertura
    val withSuspense = remember { !reduce && cacheStore.firstTimeToday("verdict_${materia.codigoDiario}") }
    val withConfetti = remember { status == AbsenceStatus.GO && !serio && !reduce && cacheStore.firstTimeToday("confetti") }

    var phase by remember { mutableStateOf(if (withSuspense) Phase.ROLLING else Phase.REVEAL) }
    var diceFace by remember { mutableIntStateOf(Random.nextInt(1, 7)) }
    val diceSpin = remember { Animatable(0f) }

    // Entrada e saída do overlay inteiro
    val presence = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { presence.animateTo(1f, Motion.calma(250)) }
    fun close() {
        scope.launch {
            if (!reduce) presence.animateTo(0f, tween(180, easing = Motion.EmphasizedAccelerate))
            onClose()
        }
    }

    // Ato 1: dado rolando
    LaunchedEffect(Unit) {
        if (phase != Phase.ROLLING) {
            haptics.paraStatus(status)
            return@LaunchedEffect
        }
        val peso = Motion.pesoDoStatus(status)
        var interval = 60L
        val total = (650 * peso).toLong()
        var elapsed = 0L
        while (elapsed < total && phase == Phase.ROLLING) {
            diceFace = (1..6).filter { it != diceFace }.random()
            haptics.tick()
            launch { diceSpin.animateTo(diceSpin.value + 90f, tween(interval.toInt())) }
            delay(interval)
            elapsed += interval
            interval = (interval * 1.18f).toLong() // desacelera
        }
        if (phase != Phase.ROLLING) return@LaunchedEffect
        // Hesitação: nos vereditos ruins o dado quase para… e vira mais uma vez
        if (status == AbsenceStatus.LAST || ehRuim) {
            delay((220 * peso).toLong())
            diceFace = (1..6).filter { it != faceFor(status) }.random()
            haptics.tick()
            diceSpin.animateTo(diceSpin.value + 45f, tween(180))
            delay(160)
        }
        diceFace = faceFor(status)
        diceSpin.animateTo((diceSpin.value / 90f).let { kotlin.math.round(it) } * 90f, spring(0.4f, 500f))
        haptics.medium()
        delay(180)
        phase = Phase.REVEAL
        haptics.paraStatus(status)
    }

    // Ato 2: a cor se espalha a partir do centro
    val reveal = remember { Animatable(if (withSuspense) 0f else 1f) }
    LaunchedEffect(phase) {
        if (phase == Phase.REVEAL) reveal.animateTo(1f, tween(if (reduce) 0 else 520, easing = Motion.EmphasizedDecelerate))
    }

    // Tremor da tela inteira nos vereditos ruins (não no modo sério)
    val screenShake = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        if (phase == Phase.REVEAL && ehRuim && !serio && !reduce) {
            delay(80)
            repeat(6) { i -> screenShake.animateTo(if (i % 2 == 0) 10f else -8f, tween(45)) }
            screenShake.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 500f))
        }
    }

    val neutral = MaterialTheme.colorScheme.surfaceContainerHighest

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = presence.value
                    val s = 0.94f + 0.06f * presence.value
                    scaleX = s
                    scaleY = s
                    translationX = screenShake.value * density
                }
                .clipToBounds()
                .drawBehind {
                    drawRect(neutral)
                    // Círculo da cor do status crescendo a partir do dado
                    val maxRadius = hypot(size.width, size.height) / 2f
                    drawCircle(container, radius = maxRadius * reveal.value, center = center)
                    drawCircle(
                        solid.copy(alpha = 0.35f * reveal.value),
                        radius = maxRadius * reveal.value * 0.9f,
                        center = Offset(center.x, size.height),
                    )
                }
                .pointerInput(phase) {
                    detectTapGestures {
                        if (phase == Phase.ROLLING) phase = Phase.REVEAL else close()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            when (phase) {
                Phase.ROLLING -> if (serio) CalculatingIndicator() else RollingDice(diceFace, diceSpin.value)
                Phase.REVEAL -> {
                    if (withConfetti) ConfettiLayer(baseColor = solid)
                    RevealContent(
                        materia = materia,
                        status = status,
                        word = meta.word,
                        subtext = meta.subtext,
                        icon = meta.icon,
                        container = container,
                        onContainer = onContainer,
                        solid = solid,
                        animate = withSuspense || !reduce,
                    )
                }
            }

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
                    onClick = ::close,
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

// ── Ato 1 ──

@Composable
private fun RollingDice(face: Int, spin: Float) {
    val transition = rememberInfiniteTransition(label = "rolling")
    val bounce by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(200, easing = Motion.Standard), RepeatMode.Reverse),
        label = "dice_bounce",
    )
    val diceColor = MaterialTheme.colorScheme.primary
    val pipColor = MaterialTheme.colorScheme.onPrimary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    translationY = bounce * density
                    rotationZ = spin
                }
                .semantics { contentDescription = "Dado rolando" },
        ) { drawDice(face, diceColor, pipColor) }
        Spacer(Modifier.height(28.dp))
        Text(
            "Consultando o destino…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "toque para pular",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

/** Modo sério: sem dado, só um indicador calmo. */
@Composable
private fun CalculatingIndicator() {
    val transition = rememberInfiniteTransition(label = "calc")
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(500, easing = Motion.Standard), RepeatMode.Reverse),
        label = "calc_pulse",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.EventAvailable,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse },
        )
        Spacer(Modifier.height(24.dp))
        Text("Calculando…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Dado desenhado (face de 1 a 6), para poder parar na face certa. */
private fun DrawScope.drawDice(face: Int, body: Color, pip: Color) {
    val side = size.minDimension
    drawRoundRect(body, size = Size(side, side), cornerRadius = CornerRadius(side * 0.22f))
    val r = side * 0.085f
    val lo = side * 0.27f
    val mid = side * 0.5f
    val hi = side * 0.73f
    val pips = when (face) {
        1 -> listOf(mid to mid)
        2 -> listOf(lo to lo, hi to hi)
        3 -> listOf(lo to lo, mid to mid, hi to hi)
        4 -> listOf(lo to lo, hi to lo, lo to hi, hi to hi)
        5 -> listOf(lo to lo, hi to lo, mid to mid, lo to hi, hi to hi)
        else -> listOf(lo to lo, hi to lo, lo to mid, hi to mid, lo to hi, hi to hi)
    }
    pips.forEach { (x, y) -> drawCircle(pip, r, Offset(x, y)) }
}

// ── Atos 2 e 3 ──

@Composable
private fun RevealContent(
    materia: MateriaDisplay,
    status: AbsenceStatus,
    word: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    onContainer: Color,
    solid: Color,
    animate: Boolean,
) {
    val reduce = LocalReduceMotion.current
    val good = status == AbsenceStatus.GO || status == AbsenceStatus.WARN

    // Palavra: quica quando é bom, "bate" seco quando é ruim
    val wordScale = remember { Animatable(if (reduce) 1f else if (good) 0.5f else 2.6f) }
    val wordAlpha = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(80)
        launch { wordAlpha.animateTo(1f, tween(140)) }
        wordScale.animateTo(
            1f,
            if (good) spring(dampingRatio = 0.4f, stiffness = 420f) else spring(dampingRatio = 0.75f, stiffness = 900f),
        )
    }

    // Forma: começa neutra e se transforma na forma do status
    var shape by remember { mutableStateOf(if (reduce) shapeFor(status) else OrgShape.FLOWER) }
    LaunchedEffect(Unit) {
        delay(120)
        shape = shapeFor(status)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .widthIn(max = 480.dp),
    ) {
        ShapeContainer(
            shape = shape,
            size = 116.dp,
            containerColor = solid.copy(alpha = 0.3f),
            contentColor = onContainer,
            morph = true,
            breathe = status == AbsenceStatus.GO,
            mood = moodFor(status),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(56.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = word,
            fontSize = 52.sp,
            lineHeight = 56.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-2).sp,
            color = onContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = wordScale.value
                scaleY = wordScale.value
                alpha = wordAlpha.value
            },
        )

        Spacer(Modifier.height(8.dp))

        StaggeredIn(delayMs = 250) {
            Text(
                text = materia.nome,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))

        StaggeredIn(delayMs = 380) {
            Surface(color = container.copy(alpha = 0.65f), shape = MaterialTheme.shapes.extraLarge) {
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
            Spacer(Modifier.height(14.dp))
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

        if (materia.limite > 0) {
            Spacer(Modifier.height(14.dp))
            StaggeredIn(delayMs = 540) {
                DayCostBar(
                    materia = materia,
                    solid = solid,
                    onContainer = onContainer,
                    animate = animate,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        StaggeredIn(delayMs = 640) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(materia.faltas, "usadas", container, onContainer, solid, delayMs = 700)
                StatChip(materia.limite, "limite", container, onContainer, solid, delayMs = 760)
                if (materia.aulasHoje > 0) {
                    // Desce ao vivo das faltas livres de hoje até o que sobraria
                    StatChip(
                        value = materia.restantesAposHoje.coerceAtLeast(0),
                        label = "sobram se faltar",
                        container = container, onContainer = onContainer, solid = solid,
                        from = materia.restantes.coerceAtLeast(0),
                        delayMs = 900,
                    )
                } else {
                    StatChip(materia.restantes.coerceAtLeast(0), "sobrando", container, onContainer, solid, delayMs = 820)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        StaggeredIn(delayMs = 800) {
            Text(
                "toca em qualquer lugar pra fechar",
                style = MaterialTheme.typography.labelMedium,
                color = onContainer.copy(alpha = 0.55f),
            )
        }
    }
}

/**
 * Barra de faltas do semestre: usadas (apagadas), livres (cor do status) e, se houver
 * aula hoje, os segmentos que faltar hoje consumiria — "comidos" da direita para a
 * esquerda. O que passa do limite fica vermelho e treme.
 */
@Composable
private fun DayCostBar(
    materia: MateriaDisplay,
    solid: Color,
    onContainer: Color,
    animate: Boolean,
) {
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val limite = materia.limite
    val usadas = materia.faltas.coerceIn(0, limite)
    val livres = (limite - usadas).coerceAtLeast(0)
    val hoje = materia.aulasHoje
    val consumidas = hoje.coerceAtMost(livres)
    val excesso = (hoje - livres).coerceAtLeast(0) + (materia.faltas - limite).coerceAtLeast(0)
    val danger = MaterialTheme.verdictColors.noSolid

    // Quantos segmentos de "hoje" já foram comidos na animação
    val eaten by produceState(if (!animate || reduce) consumidas else 0, consumidas) {
        if (!animate || reduce) { value = consumidas; return@produceState }
        delay(750)
        for (i in 1..consumidas) {
            value = i
            haptics.tick()
            delay(110)
        }
    }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(excesso) {
        if (excesso > 0 && animate && !reduce) {
            delay(750L + consumidas * 110L)
            haptics.rejeitar()
            repeat(4) { i -> shake.animateTo(if (i % 2 == 0) 5f else -5f, tween(40)) }
            shake.animateTo(0f, spring(0.3f, 600f))
        }
    }

    val total = limite + excesso
    val segmented = total in 1..40
    val usedColor = onContainer.copy(alpha = 0.18f)
    val eatenStroke = onContainer.copy(alpha = 0.7f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .graphicsLayer { translationX = shake.value * density }
                .semantics {
                    contentDescription = "Faltas: $usadas usadas de $limite" +
                        if (hoje > 0) ", faltar hoje consome $hoje" else ""
                },
        ) {
            val gap = if (segmented) 3.dp.toPx() else 0f
            val n = if (segmented) total else 1
            val segW = (size.width - gap * (n - 1)) / n
            val radius = CornerRadius(size.height / 2f)
            if (segmented) {
                for (i in 0 until total) {
                    val x = i * (segW + gap)
                    val tl = Offset(x, 0f)
                    val sz = Size(segW, size.height)
                    // Ordem: usadas | livres que sobram | livres consumidas hoje | excesso
                    val freeEnd = limite
                    val eatenStart = freeEnd - eaten
                    when {
                        i < usadas -> drawRoundRect(usedColor, tl, sz, radius)
                        i < eatenStart -> drawRoundRect(solid, tl, sz, radius)
                        i < freeEnd -> {
                            drawRoundRect(solid.copy(alpha = 0.18f), tl, sz, radius)
                            drawRoundRect(eatenStroke, tl, sz, radius, style = Stroke(1.5.dp.toPx()))
                        }
                        else -> drawRoundRect(danger, tl, sz, radius)
                    }
                }
            } else {
                // Muitas faltas: barra contínua com as mesmas cores
                fun w(v: Int) = size.width * v / total
                drawRoundRect(usedColor, size = size, cornerRadius = radius)
                drawRoundRect(solid, Offset(w(usadas), 0f), Size(w(livres - eaten), size.height), radius)
                if (excesso > 0) drawRoundRect(danger, Offset(w(limite), 0f), Size(w(excesso), size.height), radius)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("usadas", style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.6f))
            if (hoje > 0) {
                Text(
                    if (excesso > 0) "hoje passa do limite" else "hoje",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (excesso > 0) danger else onContainer.copy(alpha = 0.8f),
                )
            }
            Text("limite", style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun StatChip(
    value: Int,
    label: String,
    container: Color,
    onContainer: Color,
    solid: Color,
    from: Int = 0,
    delayMs: Int = 0,
) {
    Surface(color = container.copy(alpha = 0.65f), shape = MaterialTheme.shapes.large) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            CountUpText(
                value = value,
                from = from,
                delayMs = delayMs,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = solid,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.8f))
        }
    }
}

/** Entrada com fade + slide de baixo, atrasada para criar o efeito cascata. */
@Composable
private fun StaggeredIn(delayMs: Int, content: @Composable () -> Unit) {
    val reduce = LocalReduceMotion.current
    var visible by remember { mutableStateOf(reduce) }
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
            val t = progress * 3500f
            val x = (p.x + p.vx * t) * size.width
            val y = (p.y + p.vy * t + 0.0000002f * t * t) * size.height
            rotate(p.rotation + p.spin * progress, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[p.colorIndex].copy(alpha = (1f - progress).coerceIn(0.25f, 1f)),
                    topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                    size = Size(p.size, p.size * 0.55f),
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
