package com.example.supacomobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.supacomobile.data.ScheduleEntry
import com.example.supacomobile.data.local.FaltasHistory
import com.example.supacomobile.data.model.PeriodoLetivo
import com.example.supacomobile.data.model.Profile
import com.example.supacomobile.data.repository.AcademicRepository
import com.example.supacomobile.data.repository.AuthRepository
import com.example.supacomobile.data.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val materias: List<MateriaDisplay> = emptyList(),
    val periodos: List<PeriodoLetivo> = emptyList(),
    val selectedPeriodo: PeriodoLetivo? = null,
    val streakDays: Int = 0,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val detailMateriaId: String? = null,
    val verdictMateriaId: String? = null,
    val snackMessage: String? = null,
    val servers: List<com.example.supacomobile.data.model.Servidor> = emptyList(),
    val isSearchingServers: Boolean = false,
    val searchServersError: String? = null,
)


class DashboardViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val academicRepository: AcademicRepository,
    private val faltasHistory: FaltasHistory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var horarios: Map<String, List<ScheduleEntry>> = emptyMap()

    init {
        fetchData()
    }

    fun fetchData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val profileResult = profileRepository.getProfile()
            if (profileResult.isSuccess) {
                _uiState.update { it.copy(profile = profileResult.getOrNull()) }
                fetchAcademicData(forceRefresh)
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "Erro ao buscar perfil: ${profileResult.exceptionOrNull()?.message}")
                }
            }
        }
    }

    private suspend fun fetchAcademicData(forceRefresh: Boolean = false) {
        val periodosResult = academicRepository.getPeriodosLetivos()
        if (periodosResult.isFailure) {
            _uiState.update { it.copy(isLoading = false, error = "Erro ao buscar períodos letivos") }
            return
        }
        val periodos = periodosResult.getOrNull().orEmpty()
        val atual = _uiState.value.selectedPeriodo ?: periodos.firstOrNull()
        _uiState.update { it.copy(periodos = periodos, selectedPeriodo = atual) }
        if (atual == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        loadBoletim(atual, isCurrent = atual == periodos.firstOrNull(), forceRefresh = forceRefresh)
    }

    private suspend fun loadBoletim(periodo: PeriodoLetivo, isCurrent: Boolean, forceRefresh: Boolean = false) {
        val useCache = isCurrent && !forceRefresh
        val boletimResult = academicRepository.getBoletim(periodo.anoLetivo, periodo.periodoLetivo, useCache = useCache)
        if (boletimResult.isSuccess) {
            val items = boletimResult.getOrNull() ?: emptyList()
            horarios = academicRepository.getHorarios()
            if (isCurrent) {
                faltasHistory.registerSync(items.sumOf { it.numeroFaltas })
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    materias = buildMaterias(items, horarios),
                    streakDays = faltasHistory.streakDays(),
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false, error = "Erro ao buscar boletim") }
        }
    }

    fun selectPeriodo(periodo: PeriodoLetivo) {
        val state = _uiState.value
        if (periodo == state.selectedPeriodo) return
        _uiState.update { it.copy(selectedPeriodo = periodo, isLoading = true, error = null) }
        viewModelScope.launch {
            loadBoletim(periodo, isCurrent = periodo == state.periodos.firstOrNull())
        }
    }

    fun sync(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            fetchData(forceRefresh = forceRefresh)
            delay(800)
            _uiState.update { it.copy(
                isSyncing = false, 
                snackMessage = if (forceRefresh) "Sincronização forçada realizada." else "Boletim sincronizado com o SUAP."
            ) }
            delay(2600)
            _uiState.update { it.copy(snackMessage = null) }
        }
    }

    fun openDetail(id: String) = _uiState.update { it.copy(detailMateriaId = id) }
    fun closeDetail() = _uiState.update { it.copy(detailMateriaId = null) }
    fun openVerdict(id: String) = _uiState.update { it.copy(verdictMateriaId = id) }
    fun closeVerdict() = _uiState.update { it.copy(verdictMateriaId = null) }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun searchServidores(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(servers = emptyList(), searchServersError = null, isSearchingServers = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingServers = true, searchServersError = null) }
            val campus = _uiState.value.profile?.vinculo?.campus
            val result = academicRepository.buscarServidores(nome = query, campus = campus)
            if (result.isSuccess) {
                _uiState.update { it.copy(servers = result.getOrNull().orEmpty(), isSearchingServers = false) }
            } else {
                val fallbackResult = academicRepository.buscarServidores(nome = query, campus = null)
                if (fallbackResult.isSuccess) {
                    _uiState.update { it.copy(servers = fallbackResult.getOrNull().orEmpty(), isSearchingServers = false) }
                } else {
                    _uiState.update { 
                        it.copy(
                            isSearchingServers = false, 
                            searchServersError = fallbackResult.exceptionOrNull()?.message ?: "Erro desconhecido"
                        ) 
                    }
                }
            }
        }
    }
}

