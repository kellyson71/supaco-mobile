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

    /**
     * true só na primeira chamada do dia para [key]. Usado para que suspense,
     * confete e chamarizes apareçam uma vez por dia e não cansem.
     */
    fun firstTimeToday(key: String): Boolean {
        val today = localEpochDay()
        val prefKey = "daily_$key"
        if (prefs.getLong(prefKey, -1L) == today) return false
        prefs.edit { putLong(prefKey, today) }
        return true
    }

    /** Quantas vezes [key] foi registrado hoje (e registra mais uma). */
    fun countToday(key: String): Int {
        val today = localEpochDay()
        val dayKey = "count_day_$key"
        val countKey = "count_$key"
        val count = if (prefs.getLong(dayKey, -1L) == today) prefs.getInt(countKey, 0) else 0
        prefs.edit {
            putLong(dayKey, today)
            putInt(countKey, count + 1)
        }
        return count
    }

    /** Faltas por diário no último sync — para destacar o que mudou. */
    var lastFaltas: Map<String, Int>
        get() = prefs.getString(KEY_LAST_FALTAS, null)?.let {
            runCatching { Json.decodeFromString<Map<String, Int>>(it) }.getOrNull()
        } ?: emptyMap()
        set(value) = prefs.edit { putString(KEY_LAST_FALTAS, Json.encodeToString(value)) }

    /** Diários com falta nova ainda não vista pelo usuário. */
    var unseenChanges: Set<String>
        get() = prefs.getStringSet(KEY_UNSEEN, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit { putStringSet(KEY_UNSEEN, value) }

    /** Conquistas já comemoradas (para anunciar só as novas). */
    var celebratedAchievements: Set<String>?
        get() = prefs.getStringSet(KEY_ACHIEVEMENTS, null)?.toSet()
        set(value) = prefs.edit { putStringSet(KEY_ACHIEVEMENTS, value) }

    /** Último rank exibido no perfil (para o efeito de "carimbo" quando muda). */
    var lastRank: String?
        get() = prefs.getString(KEY_RANK, null)
        set(value) = prefs.edit { putString(KEY_RANK, value) }

    fun clear() = prefs.edit(commit = true) { clear() }

    // Dia local sem java.time (indisponível no minSdk 24 sem desugaring)
    private fun localEpochDay(): Long {
        val now = System.currentTimeMillis()
        return (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    private companion object {
        const val PREFS_NAME = "supaco_cache"
        const val KEY_PERIODOS = "periodos"
        const val KEY_LAST_SYNC = "last_sync_at"
        const val KEY_LAST_FALTAS = "last_faltas"
        const val KEY_UNSEEN = "unseen_changes"
        const val KEY_ACHIEVEMENTS = "celebrated_achievements"
        const val KEY_RANK = "last_rank"
    }
}
