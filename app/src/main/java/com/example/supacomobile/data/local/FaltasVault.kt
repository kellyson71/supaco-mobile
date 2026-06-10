package com.example.supacomobile.data.local

import android.content.Context
import androidx.core.content.edit

/**
 * Persiste os ajustes locais de falta ("Faltei"/"Eu fui") por código de diário.
 * Incluído no auto-backup do Android — sobrevive a reinstalação.
 */
class FaltasVault(context: Context) {

    private val prefs = context.getSharedPreferences("faltas_vault", Context.MODE_PRIVATE)

    fun load(): MutableMap<String, Int> =
        prefs.all.mapNotNull { (k, v) -> (v as? Int)?.let { k to it } }
            .toMap()
            .toMutableMap()

    fun save(deltas: Map<String, Int>) {
        prefs.edit {
            clear()
            deltas.filterValues { it != 0 }.forEach { (k, v) -> putInt(k, v) }
        }
    }

    fun clear() = prefs.edit { clear() }
}
