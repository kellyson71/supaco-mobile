package io.github.kellyson71.supaco.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * Número que conta a partir de [from] até [value] ao aparecer e anima as mudanças seguintes.
 */
@Composable
fun CountUpText(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    from: Int = 0,
    durationMs: Int = 700,
    delayMs: Int = 0,
    format: (Int) -> String = { it.toString() },
) {
    val reduce = LocalReduceMotion.current
    var target by remember { mutableIntStateOf(if (reduce) value else from) }
    LaunchedEffect(value) {
        if (!reduce && delayMs > 0 && target == from) delay(delayMs.toLong())
        target = value
    }
    val shown by animateIntAsState(
        targetValue = target,
        animationSpec = if (reduce) tween(0) else tween(durationMs, easing = Motion.EmphasizedDecelerate),
        label = "count_up",
    )
    Text(format(shown), modifier = modifier, style = style, color = color, fontWeight = fontWeight)
}

/**
 * Texto em que cada caractere que muda rola na vertical, como um odômetro.
 * Subindo o valor, os dígitos sobem; descendo, descem.
 */
@Composable
fun OdometerText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    increasing: Boolean = true,
) {
    val reduce = LocalReduceMotion.current
    Row(modifier) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (reduce) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val dir = if (increasing) 1 else -1
                        (slideInVertically(Motion.calma(250)) { it * dir } + fadeIn(Motion.calma(250))) togetherWith
                            (slideOutVertically(Motion.calma(250)) { -it * dir } + fadeOut(Motion.calma(200)))
                    }
                },
                label = "odometer_$index",
            ) { c ->
                Text(c.toString(), style = style, color = color, fontWeight = fontWeight)
            }
        }
    }
}
