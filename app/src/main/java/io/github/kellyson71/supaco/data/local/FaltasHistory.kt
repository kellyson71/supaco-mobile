package io.github.kellyson71.supaco.data.local

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

/**
 * Histórico local de faltas para calcular streak ("X dias sem falta nova").
 * Atualizado a cada sync bem-sucedido do período corrente.
 */
class FaltasHistory(context: Context) {

    private val prefs = context.getSharedPreferences("faltas_history", Context.MODE_PRIVATE)

    private fun todayEpochDay(): Long = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

    fun registerSync(totalFaltas: Int) {
        val today = todayEpochDay()
        val lastTotal = prefs.getInt(KEY_LAST_TOTAL, -1)
        prefs.edit {
            if (lastTotal == -1) {
                // primeiro sync: âncora do streak
                putLong(KEY_LAST_INCREASE, today)
            } else if (totalFaltas > lastTotal) {
                putLong(KEY_LAST_INCREASE, today)
            }
            putInt(KEY_LAST_TOTAL, totalFaltas)
        }
    }

    /** Dias desde a última falta nova registrada (0 se faltou hoje ou sem dados). */
    fun streakDays(): Int {
        val lastIncrease = prefs.getLong(KEY_LAST_INCREASE, -1L)
        if (lastIncrease == -1L) return 0
        return (todayEpochDay() - lastIncrease).toInt().coerceAtLeast(0)
    }

    private companion object {
        const val KEY_LAST_TOTAL = "last_total"
        const val KEY_LAST_INCREASE = "last_increase_day"
    }
}
