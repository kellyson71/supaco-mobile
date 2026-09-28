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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import io.github.kellyson71.supaco.ui.utils.rememberShimmerBrush
import kotlinx.coroutines.delay

/**
 * Entrada suave na primeira composição (fade + sobe 16 dp). Com [index], cria cascata.
 */
fun Modifier.enterOnce(index: Int = 0, stepMs: Int = 50, offsetDp: Float = 16f): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay((index * stepMs).toLong().coerceAtMost(400))
        progress.animateTo(1f, Motion.calma(350))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetDp * density
    }
}

/** Escala de 0,97 com spring enquanto pressionado — mesma sensação em todos os cards. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressed: Float = 0.97f): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !LocalReduceMotion.current) pressed else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "press_scale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Placeholder no formato de uma forma orgânica, com brilho. */
@Composable
fun SkeletonShape(shape: OrgShape, size: Dp, modifier: Modifier = Modifier) {
    var width by remember { mutableStateOf(0f) }
    val brush = rememberShimmerBrush(width.coerceAtLeast(200f))
    Box(
        modifier
            .size(size)
            .onSizeChanged { width = it.width.toFloat() }
            .drawBehind { drawScallopBrush(shape.geometry, brush) },
    )
}

/**
 * Pull-to-refresh com a identidade do app: a flor cresce e gira conforme o puxão,
 * gira sozinha enquanto sincroniza e termina num check com haptic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupacoPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    var showDone by remember { mutableStateOf(false) }
    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (wasRefreshing && !isRefreshing) {
            showDone = true
            haptics.confirmar()
            delay(600)
            showDone = false
        }
        wasRefreshing = isRefreshing
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            val fraction = if (isRefreshing || showDone) 1f else state.distanceFraction.coerceIn(0f, 1.3f)
            val spin = if (isRefreshing && !reduce) {
                rememberInfiniteTransition(label = "ptr").animateFloat(
                    0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "ptr_spin",
                ).value
            } else fraction * 220f
            if (fraction > 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .graphicsLayer {
                            translationY = (fraction.coerceAtMost(1f) * 56f - 16f) * density
                            val s = fraction.coerceAtMost(1f)
                            scaleX = s
                            scaleY = s
                            alpha = s
                        },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp,
                ) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        if (showDone) {
                            Icon(Icons.Rounded.Check, contentDescription = "Sincronizado", tint = MaterialTheme.colorScheme.primary)
                        } else {
                            ShapeContainer(
                                shape = OrgShape.FLOWER,
                                size = 28.dp,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                extraRotation = spin,
                                mood = 0.6f + 0.6f * fraction.coerceAtMost(1f),
                            )
                        }
                    }
                }
            }
        },
        content = content,
    )
}

/** Anel que se expande e some a cada 2 s — marca "acontecendo agora". */
@Composable
fun PulseRing(color: Color, modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) return
    val t = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = Motion.EmphasizedDecelerate), RepeatMode.Restart),
        label = "pulse_t",
    ).value
    Canvas(modifier) {
        drawCircle(
            color = color.copy(alpha = (1f - t) * 0.6f),
            radius = size.minDimension / 2f * (0.5f + t * 0.8f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
