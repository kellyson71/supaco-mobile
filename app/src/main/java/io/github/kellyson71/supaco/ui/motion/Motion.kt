package io.github.kellyson71.supaco.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus

/**
 * Linguagem de movimento do Supaco. Três personalidades, ligadas ao significado:
 *
 * - **Viva**: coisas boas e toque direto (GO, conquistas, botões). Spring com quique.
 * - **Calma**: navegação, listas, folhas. Sem quique, desacelerando.
 * - **Firme**: alertas, NO, erro. Rápida e seca.
 *
 * Regra do risco: quanto pior o status, mais lento e pesado o movimento.
 */
object Motion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 500

    fun <T> viva(stiffness: Float = Spring.StiffnessMediumLow): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = stiffness)

    fun <T> calma(durationMs: Int = MEDIUM, delayMs: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMs, delayMs, easing = EmphasizedDecelerate)

    fun <T> firme(durationMs: Int = SHORT): FiniteAnimationSpec<T> =
        tween(durationMs, easing = Standard)

    /** Personalidade de acordo com o status de risco. */
    fun <T> paraStatus(status: AbsenceStatus): FiniteAnimationSpec<T> = when (status) {
        AbsenceStatus.GO -> viva()
        AbsenceStatus.WARN -> spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        AbsenceStatus.LAST -> calma(LONG)
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> firme(200)
    }

    /** Multiplicador de duração: GO rápido e leve, NO lento e pesado. */
    fun pesoDoStatus(status: AbsenceStatus): Float = when (status) {
        AbsenceStatus.GO -> 0.8f
        AbsenceStatus.WARN -> 1f
        AbsenceStatus.LAST -> 1.35f
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> 1.6f
    }
}

/** true quando o sistema ou o usuário pediram menos movimento. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Lê a escala de animação do sistema ("Remover animações" nas opções de acessibilidade). */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Troca a animação por um corte seco quando o movimento está reduzido. */
@Composable
@ReadOnlyComposable
fun <T> AnimationSpec<T>.orSnap(): AnimationSpec<T> = if (LocalReduceMotion.current) snap() else this

@Composable
@ReadOnlyComposable
fun <T> FiniteAnimationSpec<T>.orSnap(): FiniteAnimationSpec<T> = if (LocalReduceMotion.current) snap() else this
