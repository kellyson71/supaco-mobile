package io.github.kellyson71.supaco.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class OrgShape {
    FLOWER,  // bumps=8, amp=7, base=41
    COOKIE,  // bumps=12, amp=4, base=44
    CLOVER,  // bumps=4, amp=9, base=40
    PEBBLE,  // bumps=3, amp=7, base=42
}

private data class ShapePreset(val bumps: Int, val amp: Float, val base: Float)

private val PRESETS = mapOf(
    OrgShape.FLOWER to ShapePreset(8, 7f, 41f),
    OrgShape.COOKIE to ShapePreset(12, 4f, 44f),
    OrgShape.CLOVER to ShapePreset(4, 9f, 40f),
    OrgShape.PEBBLE to ShapePreset(3, 7f, 42f),
)

/* Scallop formula: r(θ) = base + amp·cos(bumps·θ). SVG viewBox 0 0 100 100. */
fun DrawScope.drawScallopShape(shape: OrgShape, color: Color) {
    val preset = PRESETS[shape] ?: PRESETS[OrgShape.FLOWER]!!
    val scale = size.width / 100f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val steps = 160

    val path = Path()
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * 2f * PI.toFloat()
        val r = (preset.base + preset.amp * cos(preset.bumps * t)) * scale
        val x = cx + r * cos(t)
        val y = cy + r * sin(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
fun ShapeContainer(
    shape: OrgShape,
    size: Dp,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind { drawScallopShape(shape, containerColor) },
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
