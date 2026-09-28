package io.github.kellyson71.supaco.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.kellyson71.supaco.MainActivity
import io.github.kellyson71.supaco.data.local.AppDatabase
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.widget.carregarVereditoHoje
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Tile de configurações rápidas: mostra o veredito do dia no subtítulo e fica
 * "aceso" quando dá para faltar. Tocar abre o veredito direto.
 */
class VerdictTileService : TileService() {

    private val database: AppDatabase by inject()
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = tileScope
        tileScope.launch {
            val veredito = withContext(Dispatchers.IO) { runCatching { carregarVereditoHoje(database) }.getOrNull() }
            val tile = qsTile ?: return@launch
            val materia = veredito?.materia
            tile.state = when {
                materia == null -> Tile.STATE_INACTIVE
                materia.status == AbsenceStatus.GO -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = when {
                    materia == null -> "Abra o Supaco"
                    !veredito.temAulaHoje -> "Sem aula hoje"
                    else -> "Hoje: " + when (materia.status) {
                        AbsenceStatus.GO -> "pode faltar"
                        AbsenceStatus.WARN -> "com cautela"
                        AbsenceStatus.LAST -> "última falta"
                        AbsenceStatus.NO, AbsenceStatus.REPROVADO -> "não falte"
                    }
                }
            }
            tile.updateTile()
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_DEST, "verdict")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
