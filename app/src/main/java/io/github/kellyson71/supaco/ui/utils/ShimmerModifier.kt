package io.github.kellyson71.supaco.ui.utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion

/** Brilho que atravessa o placeholder, com as cores do tema (claro/escuro). */
@Composable
fun rememberShimmerBrush(width: Float): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    if (LocalReduceMotion.current || width <= 0f) return Brush.linearGradient(listOf(base, base))
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -width,
        targetValue = 2 * width,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmer_anim",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + width / 2f, width / 4f),
    )
}

fun Modifier.shimmerEffect(shape: Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val brush = rememberShimmerBrush(size.width.toFloat().coerceAtLeast(300f))
    background(brush, shape).onGloballyPositioned { size = it.size }
}
