package io.github.kellyson71.supaco.ui.motion

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus

/**
 * Vocabulário tátil do app — sempre o mesmo gesto para o mesmo significado.
 * Usa as constantes do sistema, que já respeitam a configuração de vibração do usuário.
 *
 * | Momento                         | Haptic          |
 * |---------------------------------|-----------------|
 * | Tocar em aba, chip, face do dado | [tick]          |
 * | Cruzar faixa no simulador        | [medium]        |
 * | Veredito GO / algo bom           | [alegre]        |
 * | Veredito NO / limite estourado   | [pesado]        |
 * | Sync concluído                   | [confirmar]     |
 * | Erro                             | [rejeitar]      |
 */
class Haptics(private val view: View) {

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }

    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    fun medium() = perform(HapticFeedbackConstants.CONTEXT_CLICK)

    fun confirmar() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.CONTEXT_CLICK
    )

    fun rejeitar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.REJECT)
        } else {
            perform(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /** Dois toques leves: alegre. */
    fun alegre() {
        tick()
        view.postDelayed({ tick() }, 90)
    }

    /** Um toque longo e pesado. */
    fun pesado() = perform(HapticFeedbackConstants.LONG_PRESS)

    fun paraStatus(status: AbsenceStatus) = when (status) {
        AbsenceStatus.GO -> alegre()
        AbsenceStatus.WARN -> medium()
        AbsenceStatus.LAST -> confirmar()
        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> pesado()
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
