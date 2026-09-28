package io.github.kellyson71.supaco.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class OrgShape {
    FLOWER,  // bumps=8, amp=7, base=41
    COOKIE,  // bumps=12, amp=4, base=44
    CLOVER,  // bumps=4, amp=9, base=40
    PEBBLE,  // bumps=3, amp=7, base=42
}

/** Parâmetros da forma: r(θ) = base + amp·cos(bumps·θ), num viewBox 100×100. */
@Immutable
data class ShapeGeometry(val bumps: Int, val amp: Float, val base: Float)

val OrgShape.geometry: ShapeGeometry
    get() = when (this) {
        OrgShape.FLOWER -> ShapeGeometry(8, 7f, 41f)
        OrgShape.COOKIE -> ShapeGeometry(12, 4f, 44f)
        OrgShape.CLOVER -> ShapeGeometry(4, 9f, 40f)
        OrgShape.PEBBLE -> ShapeGeometry(3, 7f, 42f)
    }

/**
 * Humor da forma por status: pétalas cheias quando está tudo bem, murchas
 * (amplitude menor, quase círculo) quanto pior a situação.
 */
fun moodFor(status: AbsenceStatus): Float = when (status) {
    AbsenceStatus.GO -> 1.15f
    AbsenceStatus.WARN -> 0.9f
    AbsenceStatus.LAST -> 0.6f
    AbsenceStatus.NO -> 0.35f
    AbsenceStatus.REPROVADO -> 0.2f
}

/** Forma que representa o status (usada no morph do veredito). */
fun shapeFor(status: AbsenceStatus): OrgShape = when (status) {
    AbsenceStatus.GO -> OrgShape.FLOWER
    AbsenceStatus.WARN -> OrgShape.COOKIE
    AbsenceStatus.LAST -> OrgShape.CLOVER
    AbsenceStatus.NO, AbsenceStatus.REPROVADO -> OrgShape.PEBBLE
}

/**
 * Desenha a forma.
 * @param ampScale multiplica a amplitude das pétalas (0 = círculo). Base do morph e da respiração.
 * @param wilted quantas pétalas estão murchas (fracionário anima a pétala "murchando").
 */
fun DrawScope.drawScallop(
    geometry: ShapeGeometry,
    color: Color,
    ampScale: Float = 1f,
    wilted: Float = 0f,
) {
    val scale = size.width / 100f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val steps = 180
    val twoPi = 2f * PI.toFloat()
    val bumps = geometry.bumps

    val path = Path()
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * twoPi
        val c = cos(bumps * t)
        // Só a parte "para fora" da pétala murcha; os vales ficam iguais,
        // então a curva continua contínua entre uma pétala e outra.
        val lobe = if (c > 0f && wilted > 0f) {
            val petal = ((t * bumps / twoPi).roundToInt()) % bumps
            c * (1f - (wilted - petal).coerceIn(0f, 1f))
        } else c
        val r = (geometry.base + geometry.amp * ampScale * lobe) * scale
        val x = cx + r * cos(t)
        val y = cy + r * sin(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

fun DrawScope.drawScallopShape(shape: OrgShape, color: Color) = drawScallop(shape.geometry, color)

/**
 * Container com forma orgânica "viva".
 *
 * @param breathe pétalas respiram (±8% da amplitude, ciclo de ~3,5 s). Use só no destaque da tela.
 * @param spin gira bem devagar (uma volta a cada ~24 s). O conteúdo não gira.
 * @param mood multiplicador de amplitude — ver [moodFor].
 * @param wilted pétalas murchas (simulador de faltas).
 * @param morph trocas de [shape] passam pelo círculo em vez de cortar.
 * @param extraRotation rotação adicional em graus (ex.: giro ao tocar).
 */
@Composable
fun ShapeContainer(
    shape: OrgShape,
    size: Dp,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    breathe: Boolean = false,
    spin: Boolean = false,
    mood: Float = 1f,
    wilted: Float = 0f,
    morph: Boolean = false,
    extraRotation: Float = 0f,
    content: @Composable () -> Unit = {},
) {
    val reduce = LocalReduceMotion.current

    var shown by remember { mutableStateOf(shape) }
    val morphScale = remember { Animatable(1f) }
    LaunchedEffect(shape) {
        if (shape == shown) return@LaunchedEffect
        if (!morph || reduce) {
            shown = shape
        } else {
            morphScale.animateTo(0f, tween(170, easing = Motion.EmphasizedAccelerate))
            shown = shape
            morphScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
        }
    }

    val moodAnim by animateFloatAsState(
        targetValue = mood,
        animationSpec = if (reduce) tween(0) else tween(600, easing = Motion.EmphasizedDecelerate),
        label = "shape_mood",
    )
    val wiltAnim by animateFloatAsState(
        targetValue = wilted,
        animationSpec = if (reduce) tween(0) else spring(dampingRatio = 0.6f, stiffness = 260f),
        label = "shape_wilt",
    )

    val living = (breathe || spin) && !reduce
    val infinite = if (living) rememberInfiniteTransition(label = "shape_life") else null
    val breath = if (breathe && infinite != null) {
        infinite.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1750, easing = Motion.Standard), RepeatMode.Reverse),
            label = "shape_breath",
        ).value
    } else 1f
    val spinDeg = if (spin && infinite != null) {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
            label = "shape_spin",
        ).value
    } else 0f

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                rotate(spinDeg + extraRotation) {
                    drawScallop(
                        geometry = shown.geometry,
                        color = containerColor,
                        ampScale = morphScale.value * moodAnim * breath,
                        wilted = wiltAnim,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/* Convenience: colored dot with status ring for timeline */
fun DrawScope.drawStatusDot(solid: Color, container: Color, ringStroke: Float = 4f) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.width / 2f - ringStroke
    drawCircle(color = container, radius = r + ringStroke * 1.5f, center = androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(color = solid, radius = r * 0.55f, center = androidx.compose.ui.geometry.Offset(cx, cy))
}
