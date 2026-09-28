package io.github.kellyson71.supaco.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.github.kellyson71.supaco.data.ScheduleData
import io.github.kellyson71.supaco.data.ScheduleEntry
import io.github.kellyson71.supaco.data.local.CacheStore
import io.github.kellyson71.supaco.data.local.SettingsManager
import io.github.kellyson71.supaco.data.local.dao.BoletimDao
import io.github.kellyson71.supaco.data.local.dao.HorarioDao
import io.github.kellyson71.supaco.data.local.entity.BoletimEntity
import io.github.kellyson71.supaco.data.local.entity.HorarioEntity
import io.github.kellyson71.supaco.data.model.BoletimItem
import io.github.kellyson71.supaco.data.model.PaginatedResponse
import io.github.kellyson71.supaco.data.model.PeriodoLetivo
import io.github.kellyson71.supaco.data.model.Servidor
import io.github.kellyson71.supaco.data.remote.SuapApi
import io.github.kellyson71.supaco.data.remote.fetchAllPages
import io.github.kellyson71.supaco.notifications.FaltasNotifier
import io.github.kellyson71.supaco.widget.HorariosWidget
import io.github.kellyson71.supaco.widget.SupacoVerdictWidget
import io.github.kellyson71.supaco.widget.SupacoWidget

class AcademicRepository(
    private val suapApi: SuapApi,
    private val boletimDao: BoletimDao,
    private val horarioDao: HorarioDao,
    private val cacheStore: CacheStore,
    private val appContext: Context,
    private val settings: SettingsManager,
) {
    val lastSyncAt: Long? get() = cacheStore.lastSyncAt

    /** Períodos do aluno, do mais recente para o mais antigo. Cai no cache se estiver offline. */
    suspend fun getPeriodosLetivos(): Result<List<PeriodoLetivo>> = runCatching {
        fetchAllPages { suapApi.getPeriodosLetivos(it) }
            .distinct()
            .sortedWith(compareByDescending<PeriodoLetivo> { it.anoLetivo }.thenByDescending { it.periodoLetivo })
            .also { cacheStore.periodos = it }
    }.recoverCatching { error ->
        cacheStore.periodos?.takeIf { it.isNotEmpty() } ?: throw error
    }

    /** Boletim em cache, só se for do período pedido. */
    suspend fun cachedBoletim(periodo: PeriodoLetivo): List<BoletimItem>? =
        boletimDao.getBoletim()
            .takeIf { list -> list.isNotEmpty() && list.all { it.anoLetivo == periodo.anoLetivo && it.periodoLetivo == periodo.periodoLetivo } }
            ?.map { it.toDomain() }

    /**
     * Boletim direto do SUAP. Para o período corrente também grava o cache,
     * atualiza a grade, os widgets e verifica alertas de faltas.
     */
    suspend fun fetchBoletim(periodo: PeriodoLetivo, isCurrent: Boolean): Result<List<BoletimItem>> = runCatching {
        val items = fetchAllPages { suapApi.getBoletim(periodo.anoLetivo, periodo.periodoLetivo, it) }
        if (isCurrent) {
            boletimDao.replaceAll(items.map { BoletimEntity.fromDomain(it, periodo) })
            fetchAndCacheHorarios(periodo)
            cacheStore.lastSyncAt = System.currentTimeMillis()
            refreshWidgets()
            runCatching { FaltasNotifier.checkAndNotify(appContext, settings, boletimDao.getBoletim()) }
        }
        items
    }

    /** Grade de horários do período corrente (sigla → aulas). */
    suspend fun getHorarios(): Map<String, List<ScheduleEntry>> =
        horarioDao.getAll().groupBy(
            keySelector = { it.sigla },
            valueTransform = { ScheduleEntry(it.dia, it.horaInicio, it.horaFim, it.sala, professor = null, aulas = it.aulas) },
        )

    private suspend fun fetchAndCacheHorarios(periodo: PeriodoLetivo) {
        // Falha de rede mantém a grade anterior; sucesso (mesmo vazio) substitui.
        val turmas = runCatching {
            fetchAllPages { suapApi.getTurmasVirtuais(periodo.anoLetivo, periodo.periodoLetivo, it) }
        }.getOrNull() ?: return
        val entities = turmas.flatMap { turma ->
            val sala = ScheduleData.limpaSala(turma.locaisDeAula.firstOrNull())
            ScheduleData.parseSuapHorario(turma.horariosDeAula, sala).map { entry ->
                HorarioEntity(
                    sigla = turma.sigla,
                    dia = entry.dia,
                    horaInicio = entry.horaInicio,
                    horaFim = entry.horaFim,
                    sala = entry.sala,
                    aulas = entry.aulas,
                )
            }
        }
        horarioDao.replaceAll(entities)
    }

    private suspend fun refreshWidgets() {
        runCatching {
            SupacoWidget().updateAll(appContext)
            SupacoVerdictWidget().updateAll(appContext)
            HorariosWidget().updateAll(appContext)
        }
    }

    suspend fun buscarServidores(nome: String?, campus: String?, page: Int = 1): Result<PaginatedResponse<Servidor>> =
        runCatching { suapApi.buscarServidores(nome, campus, page) }
}
