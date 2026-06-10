package com.example.supacomobile.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.supacomobile.data.ScheduleData
import com.example.supacomobile.data.ScheduleEntry
import com.example.supacomobile.data.local.SettingsManager
import com.example.supacomobile.data.local.dao.BoletimDao
import com.example.supacomobile.data.local.dao.HorarioDao
import com.example.supacomobile.data.local.entity.BoletimEntity
import com.example.supacomobile.data.local.entity.HorarioEntity
import com.example.supacomobile.data.model.BoletimItem
import com.example.supacomobile.data.model.PeriodoLetivo
import com.example.supacomobile.data.remote.SuapApi
import com.example.supacomobile.notifications.FaltasNotifier
import com.example.supacomobile.widget.HorariosWidget
import com.example.supacomobile.widget.SupacoVerdictWidget
import com.example.supacomobile.widget.SupacoWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class AcademicRepository(
    private val suapApi: SuapApi,
    private val boletimDao: BoletimDao,
    private val horarioDao: HorarioDao,
    private val appContext: Context,
    private val settings: SettingsManager,
) {
    private val prefs = appContext.getSharedPreferences("supaco_settings", Context.MODE_PRIVATE)

    private fun getPeriodosLetivosCache(): List<PeriodoLetivo>? {
        val json = prefs.getString("cached_periodos_letivos", null) ?: return null
        return try {
            Json.decodeFromString<List<PeriodoLetivo>>(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun savePeriodosLetivosCache(list: List<PeriodoLetivo>) {
        try {
            val json = Json.encodeToString(list)
            prefs.edit().putString("cached_periodos_letivos", json).apply()
        } catch (e: Exception) {
            // Ignora falhas de serialização
        }
    }

    suspend fun getPeriodosLetivos(): Result<List<PeriodoLetivo>> {
        return try {
            val periodos = suapApi.getPeriodosLetivos().results
            savePeriodosLetivosCache(periodos)
            Result.success(periodos)
        } catch (e: Exception) {
            val cached = getPeriodosLetivosCache()
            if (cached != null && cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Boletim do período. Só o período corrente usa cache do Room (e alimenta
     * widgets/notificações); semestres antigos vão direto à rede.
     */
    suspend fun getBoletim(ano: Int, periodo: Int, useCache: Boolean = true): Result<List<BoletimItem>> {
        if (!useCache) {
            return runCatching { suapApi.getBoletim(ano, periodo).results }
        }
        return try {
            val cached = boletimDao.getBoletim()
            if (cached.isNotEmpty()) {
                // retorna cache imediatamente e atualiza em background
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { fetchAndCache(ano, periodo) }
                }
                Result.success(cached.map { it.toDomain() })
            } else {
                fetchAndCache(ano, periodo)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /** Grade de horários real (sigla → aulas), com fallback hardcoded. */
    suspend fun getHorarios(): Map<String, List<ScheduleEntry>> {
        val stored = horarioDao.getAll()
        if (stored.isEmpty()) return ScheduleData.BY_SIGLA.mapValues { listOf(it.value) }
        return stored.groupBy(
            keySelector = { it.sigla },
            valueTransform = { ScheduleEntry(it.dia, it.horaInicio, it.horaFim, it.sala, professor = null) },
        )
    }

    private suspend fun fetchAndCache(ano: Int, periodo: Int): Result<List<BoletimItem>> = runCatching {
        val items = suapApi.getBoletim(ano, periodo).results
        boletimDao.clear()
        boletimDao.insertBoletim(items.map { BoletimEntity.fromDomain(it) })
        fetchAndCacheHorarios(ano, periodo)
        refreshWidgets()
        runCatching { FaltasNotifier.checkAndNotify(appContext, settings, boletimDao.getBoletim()) }
        items
    }

    private suspend fun fetchAndCacheHorarios(ano: Int, periodo: Int) {
        runCatching {
            val turmas = suapApi.getTurmasVirtuais(ano, periodo).results
            val entities = turmas.flatMap { turma ->
                val sala = ScheduleData.limpaSala(turma.locaisDeAula.firstOrNull())
                ScheduleData.parseSuapHorario(turma.horariosDeAula, sala).map { entry ->
                    HorarioEntity(
                        sigla = turma.sigla,
                        dia = entry.dia,
                        horaInicio = entry.horaInicio,
                        horaFim = entry.horaFim,
                        sala = entry.sala,
                    )
                }
            }
            if (entities.isNotEmpty()) {
                horarioDao.clear()
                horarioDao.insertAll(entities)
            }
        }
    }

    private suspend fun refreshWidgets() {
        runCatching {
            SupacoWidget().updateAll(appContext)
            SupacoVerdictWidget().updateAll(appContext)
            HorariosWidget().updateAll(appContext)
        }
    }

    suspend fun buscarServidores(nome: String?, campus: String?): Result<List<com.example.supacomobile.data.model.Servidor>> = runCatching {
        suapApi.buscarServidores(nome, campus).results
    }
}

