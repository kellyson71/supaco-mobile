package io.github.kellyson71.supaco.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.github.kellyson71.supaco.MainActivity
import io.github.kellyson71.supaco.R
import io.github.kellyson71.supaco.data.ScheduleData
import io.github.kellyson71.supaco.data.local.NotifyLevel
import io.github.kellyson71.supaco.data.local.SettingsManager
import io.github.kellyson71.supaco.data.local.entity.BoletimEntity
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.calcStatus
import io.github.kellyson71.supaco.ui.dashboard.limiteFaltas

object FaltasNotifier {

    private const val CHANNEL_ID = "faltas_alerts"
    private const val DEDUPE_PREFS = "faltas_notified"

    fun ensureChannel(context: Context) {
        // NotificationChannelCompat é no-op abaixo do Android 8 (minSdk 24).
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.notification_channel_name))
            .setDescription(context.getString(R.string.notification_channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Apaga o estado de deduplicação e as notificações exibidas (logout). */
    fun reset(context: Context) {
        context.getSharedPreferences(DEDUPE_PREFS, Context.MODE_PRIVATE).edit(commit = true) { clear() }
        NotificationManagerCompat.from(context).cancelAll()
    }

    /**
     * Notifica matérias perigosas. Deduplica por (matéria, status): só notifica
     * de novo se o status mudar — sem spam a cada sync.
     */
    fun checkAndNotify(context: Context, settings: SettingsManager, boletim: List<BoletimEntity>) {
        if (!settings.notificationsEnabled.value || !hasPermission(context)) return

        val alertStatuses = when (settings.notifyLevel.value) {
            NotifyLevel.WARN -> setOf(AbsenceStatus.WARN, AbsenceStatus.LAST, AbsenceStatus.NO, AbsenceStatus.REPROVADO)
            NotifyLevel.LAST -> setOf(AbsenceStatus.LAST, AbsenceStatus.NO, AbsenceStatus.REPROVADO)
        }
        val serio = settings.modoSerio.value

        ensureChannel(context)
        val dedupe = context.getSharedPreferences(DEDUPE_PREFS, Context.MODE_PRIVATE)
        val manager = NotificationManagerCompat.from(context)

        boletim.forEach { entity ->
            val status = calcStatus(entity.numeroFaltas, entity.cargaHoraria)
            val key = entity.codigoDiario

            if (status !in alertStatuses) {
                dedupe.edit { remove(key) }
                return@forEach
            }
            if (dedupe.getString(key, null) == status.name) return@forEach

            val nome = ScheduleData.limpaNome(entity.disciplina)
            val restantes = limiteFaltas(entity.cargaHoraria) - entity.numeroFaltas
            val (title, text) = mensagem(status, nome, restantes, serio)

            val contentIntent = PendingIntent.getActivity(
                context,
                key.hashCode(),
                Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_DEST, "materias"),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shortcut_dice)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

            try {
                manager.notify(key.hashCode(), notification)
                dedupe.edit { putString(key, status.name) }
            } catch (e: SecurityException) {
                // Permissão revogada entre a checagem e o notify
            }
        }
    }

    private fun mensagem(status: AbsenceStatus, nome: String, restantes: Int, serio: Boolean): Pair<String, String> =
        if (serio) when (status) {
            AbsenceStatus.WARN -> "Atenção às faltas em $nome" to "Restam $restantes faltas livres nesta matéria."
            AbsenceStatus.LAST -> "Última falta em $nome" to "Resta apenas 1 falta antes do limite de 25%."
            AbsenceStatus.NO -> "Sem faltas livres em $nome" to "Mais uma falta ultrapassa o limite e reprova por falta."
            else -> "Limite de faltas excedido em $nome" to "Você ultrapassou o limite de faltas desta matéria."
        } else when (status) {
            AbsenceStatus.WARN -> "Olho em $nome 👀" to "Você tem só $restantes faltas livres. Administra direito."
            AbsenceStatus.LAST -> "Última falta em $nome ⚠️" to "Sobrou exatamente 1 falta. Use com sabedoria."
            AbsenceStatus.NO -> "NEM PENSE em faltar $nome 🚨" to "Zero faltas livres. Mais uma e já era."
            else -> "$nome já era 💀" to "Você estourou o limite de faltas. F."
        }
}
