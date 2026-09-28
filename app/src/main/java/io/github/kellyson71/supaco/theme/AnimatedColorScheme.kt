package io.github.kellyson71.supaco.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion

/**
 * Troca de tema/paleta com transição: todas as cores do esquema viajam juntas
 * até as novas (~450 ms), em vez de cortar de uma vez.
 */
@Composable
fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec: AnimationSpec<Color> =
        if (LocalReduceMotion.current) snap() else tween(450, easing = Motion.Standard)

    @Composable
    fun Color.animated(label: String) = animateColorAsState(this, spec, label = label).value

    return target.copy(
        primary = target.primary.animated("primary"),
        onPrimary = target.onPrimary.animated("onPrimary"),
        primaryContainer = target.primaryContainer.animated("primaryContainer"),
        onPrimaryContainer = target.onPrimaryContainer.animated("onPrimaryContainer"),
        inversePrimary = target.inversePrimary.animated("inversePrimary"),
        secondary = target.secondary.animated("secondary"),
        onSecondary = target.onSecondary.animated("onSecondary"),
        secondaryContainer = target.secondaryContainer.animated("secondaryContainer"),
        onSecondaryContainer = target.onSecondaryContainer.animated("onSecondaryContainer"),
        tertiary = target.tertiary.animated("tertiary"),
        onTertiary = target.onTertiary.animated("onTertiary"),
        tertiaryContainer = target.tertiaryContainer.animated("tertiaryContainer"),
        onTertiaryContainer = target.onTertiaryContainer.animated("onTertiaryContainer"),
        background = target.background.animated("background"),
        onBackground = target.onBackground.animated("onBackground"),
        surface = target.surface.animated("surface"),
        onSurface = target.onSurface.animated("onSurface"),
        surfaceVariant = target.surfaceVariant.animated("surfaceVariant"),
        onSurfaceVariant = target.onSurfaceVariant.animated("onSurfaceVariant"),
        surfaceTint = target.surfaceTint.animated("surfaceTint"),
        inverseSurface = target.inverseSurface.animated("inverseSurface"),
        inverseOnSurface = target.inverseOnSurface.animated("inverseOnSurface"),
        error = target.error.animated("error"),
        onError = target.onError.animated("onError"),
        errorContainer = target.errorContainer.animated("errorContainer"),
        onErrorContainer = target.onErrorContainer.animated("onErrorContainer"),
        outline = target.outline.animated("outline"),
        outlineVariant = target.outlineVariant.animated("outlineVariant"),
        surfaceBright = target.surfaceBright.animated("surfaceBright"),
        surfaceDim = target.surfaceDim.animated("surfaceDim"),
        surfaceContainer = target.surfaceContainer.animated("surfaceContainer"),
        surfaceContainerHigh = target.surfaceContainerHigh.animated("surfaceContainerHigh"),
        surfaceContainerHighest = target.surfaceContainerHighest.animated("surfaceContainerHighest"),
        surfaceContainerLow = target.surfaceContainerLow.animated("surfaceContainerLow"),
        surfaceContainerLowest = target.surfaceContainerLowest.animated("surfaceContainerLowest"),
    )
}
