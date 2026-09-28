package io.github.kellyson71.supaco.data.local

import android.content.Context
import androidx.core.content.edit
import io.github.kellyson71.supaco.data.model.PeriodoLetivo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Pequenos caches ligados à conta logada (fora do backup, apagados no logout).
 * Preferências visuais ficam no [SettingsManager].
 */
class CacheStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var periodos: List<PeriodoLetivo>?
        get() = prefs.getString(KEY_PERIODOS, null)?.let {
            runCatching { Json.decodeFromString<List<PeriodoLetivo>>(it) }.getOrNull()
        }
        set(value) = prefs.edit {
            if (value == null) remove(KEY_PERIODOS) else putString(KEY_PERIODOS, Json.encodeToString(value))
        }

    /** Momento (epoch ms) do último sync bem-sucedido do boletim corrente. */
    var lastSyncAt: Long?
        get() = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 }
        set(value) = prefs.edit { putLong(KEY_LAST_SYNC, value ?: 0L) }

    fun clear() = prefs.edit(commit = true) { clear() }

    private companion object {
        const val PREFS_NAME = "supaco_cache"
        const val KEY_PERIODOS = "periodos"
        const val KEY_LAST_SYNC = "last_sync_at"
    }
}
