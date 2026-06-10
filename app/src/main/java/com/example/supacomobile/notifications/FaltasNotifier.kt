package com.example.supacomobile.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.supacomobile.MainActivity
import com.example.supacomobile.R
import com.example.supacomobile.data.ScheduleData
import com.example.supacomobile.data.local.NotifyLevel
import com.example.supacomobile.data.local.SettingsManager
import com.example.supacomobile.data.local.entity.BoletimEntity
import com.example.supacomobile.ui.dashboard.AbsenceStatus
import com.example.supacomobile.ui.dashboard.calcStatus
import kotlin.math.floor

object FaltasNotifier {

    private const val CHANNEL_ID = "faltas_alerts"
    private const val DEDUPE_PREFS = "faltas_notified"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Notifica matérias perigosas. Deduplica por (matéria, status): só notifica
     * de novo se o status piorar/mudar — sem spam a cada sync.
     */
    fun checkAndNotify(context: Context, settings: SettingsManager, boletim: List<BoletimEntity>) {
        if (!settings.notificationsEnabled.value || !hasPermission(context)) return

        val alertStatuses = when (settings.notifyLevel.value) {
            NotifyLevel.WARN -> setOf(AbsenceStatus.WARN, AbsenceStatus.LAST, AbsenceStatus.NO, AbsenceStatus.REPROVADO)
            NotifyLevel.LAST -> setOf(AbsenceStatus.LAST, AbsenceStatus.NO, AbsenceStatus.REPROVADO)
        }

        ensureChannel(context)
        val dedupe = context.getSharedPreferences(DEDUPE_PREFS, Context.MODE_PRIVATE)
        val manager = context.getSystemService(NotificationManager::class.java)

        boletim.forEach { entity ->
            val status = calcStatus(entity.numeroFaltas, entity.cargaHoraria)
            val key = entity.codigoDiario

            if (status !in alertStatuses) {
                dedupe.edit { remove(key) }
                return@forEach
            }
            if (dedupe.getString(key, null) == status.name) return@forEach

            val nome = ScheduleData.limpaNome(entity.disciplina)
            val limite = floor(entity.cargaHoraria * 0.25).toInt()
            val restantes = limite - entity.numeroFaltas

            val (title, text) = when (status) {
                AbsenceStatus.WARN -> "Olho em $nome 👀" to
                    "Você tem só $restantes faltas livres. Administra direito."
                AbsenceStatus.LAST -> "Última falta em $nome ⚠️" to
                    "Sobrou exatamente 1 falta. Use com sabedoria."
                AbsenceStatus.NO -> "NEM PENSE em faltar $nome 🚨" to
                    "Zero faltas livres. Mais uma e já era."
                else -> "$nome já era 💀" to
                    "Você estourou o limite de faltas. F."
            }

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

            manager.notify(key.hashCode(), notification)
            dedupe.edit { putString(key, status.name) }
        }
    }
}
